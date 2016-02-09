/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package ddf.catalog.registry.transformer;

import static java.util.stream.Collectors.toList;
import static org.joda.time.format.ISODateTimeFormat.dateOptionalTimeParser;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.copy.HierarchicalStreamCopier;
import com.thoughtworks.xstream.io.xml.CompactWriter;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.registry.common.metacard.RegistryMetacardImpl;
import ddf.catalog.registry.common.metacard.RegistryObjectMetacardType;
import ddf.catalog.registry.common.metacard.RegistryServiceMetacardType;

public class RegistryServiceConverter implements Converter {

    private final Gml3ToWkt gml3ToWkt;

    private static final RegistryServiceMetacardType RSMT = new RegistryServiceMetacardType();

    private static final Map<String, String> CLASSIFICATIONS;

    private static final String ID_PATTERN = "urn:uuid:(.*)";

    private static final Pattern PATTERN = Pattern.compile(ID_PATTERN);

    static {
        CLASSIFICATIONS = new HashMap<>();
        CLASSIFICATIONS.put("liveDate", RegistryServiceMetacardType.LIVE_DATE);
        CLASSIFICATIONS.put("dataStartDate", RegistryServiceMetacardType.DATA_START_DATE);
        CLASSIFICATIONS.put("dataEndDate", RegistryServiceMetacardType.DATA_END_DATE);
        CLASSIFICATIONS.put("links", RegistryServiceMetacardType.LINKS);
        CLASSIFICATIONS.put("region", RegistryServiceMetacardType.REGION);
        CLASSIFICATIONS.put("dataRegion", RegistryServiceMetacardType.DATA_REGION);
        CLASSIFICATIONS.put("inputDataSources", RegistryServiceMetacardType.DATA_SOURCES);
        CLASSIFICATIONS.put("dataTypes", RegistryServiceMetacardType.DATA_TYPES);
        CLASSIFICATIONS.put("securityLevel", RegistryServiceMetacardType.SECURITY_LEVEL);
    }

    public RegistryServiceConverter(Gml3ToWkt gml3ToWkt) {
        this.gml3ToWkt = gml3ToWkt;
    }

    @Override
    public void marshal(Object o, HierarchicalStreamWriter hierarchicalStreamWriter,
            MarshallingContext marshallingContext) {
    }

    /**
     * Read and format address info from a rim:Address node.
     *
     * @param reader
     * @return Formatted address
     */
    private String readAddress(HierarchicalStreamReader reader) {
        String street = reader.getAttribute("street");
        String city = reader.getAttribute("city");
        String stateOrProvince = reader.getAttribute("stateOrProvince");
        String postalCode = reader.getAttribute("postalCode");
        String country = reader.getAttribute("country");

        return String.format("%s, %s, %s %s, %s",
                street,
                city,
                stateOrProvince,
                postalCode,
                country);
    }

    /**
     * Read and format phone info from a rim:TelephoneNumber node.
     *
     * @param reader
     * @return Formatted phone number
     */
    private String readPhoneNumber(HierarchicalStreamReader reader) {
        String areaCode = reader.getAttribute("areaCode");
        String number = reader.getAttribute("number");
        String extension = reader.getAttribute("extension");

        if (StringUtils.isEmpty(extension)) {
            return String.format("(%s) %s", areaCode, number);
        } else {
            return String.format("(%s) %s extension %s", areaCode, number, extension);
        }
    }

    private void safeMove(HierarchicalStreamReader reader, Integer amount, Runnable callback) {
        for (int i = 0; i < amount; i++) {
            reader.moveDown();
        }
        try {
            callback.run();
        } finally {
            for (int i = 0; i < amount; i++) {
                reader.moveUp();
            }
        }
    }

    private void readOrgInfo(HierarchicalStreamReader reader, RegistryMetacardImpl meta) {
        safeMove(reader, 3, () -> {
            while (reader.hasMoreChildren()) {
                safeMove(reader, 1, () -> {
                    switch (reader.getNodeName()) {
                    case "rim:Name":
                        meta.setAttribute(RegistryObjectMetacardType.ORGANIZATION_NAME,
                                readValue(reader));
                        break;
                    case "rim:EmailAddress":
                        meta.setAttribute(RegistryObjectMetacardType.ORGANIZATION_EMAIL,
                                reader.getAttribute("address"));
                        break;
                    case "rim:TelephoneNumber":
                        meta.setAttribute(RegistryObjectMetacardType.ORGANIZATION_PHONE_NUMBER,
                                readPhoneNumber(reader));
                        break;
                    case "rim:Address":
                        meta.setAttribute(RegistryObjectMetacardType.ORGANIZATION_ADDRESS,
                                readAddress(reader));
                        break;
                    }
                });
            }
        });
    }

    String readValue(HierarchicalStreamReader reader) {
        reader.moveDown();
        String value = reader.getAttribute("value");
        reader.moveUp();
        return value;
    }

    /**
     * Read gml and return a wkt.
     *
     * @param reader
     * @return
     */
    private void readGml(HierarchicalStreamReader reader, MetacardImpl meta, String attributeKey) {

        safeMove(reader, 3, () -> {

            // buffer up the current gml node and all its children
            Writer out = new StringWriter();
            HierarchicalStreamWriter writer = new CompactWriter(out);
            HierarchicalStreamCopier copier = new HierarchicalStreamCopier();
            copier.copy(reader, writer);
            String xml = out.toString();

            xml = xml.replaceFirst(">", " xmlns:gml=\"http://www.opengis.net/gml\">");

            meta.setAttribute(attributeKey, gml3ToWkt.convert(xml));
        });
    }

    /**
     * Read classification info from a rim:Classification node and attach to metacard.
     *
     * @param reader
     * @param meta
     */
    private void readClassification(HierarchicalStreamReader reader, MetacardImpl meta) {
        while (reader.hasMoreChildren()) {
            reader.moveDown();

            switch (reader.getNodeName()) {

            case "rim:Slot":
                String name = reader.getAttribute("name");
                switch (name) {
                case "location":
                    readGml(reader, meta, Metacard.GEOGRAPHY);
                    break;
                default:
                    readClassificationValue(reader, meta, name);
                }
                break;

            }

            reader.moveUp();
        }
    }

    private void readClassificationValue(HierarchicalStreamReader reader, MetacardImpl meta, String name) {
        String key = CLASSIFICATIONS.get(name);
        if (key != null) {
            if ("xs:dateTime".equals(reader.getAttribute("slotType"))) {
                meta.setAttribute(key, readDates(reader).get(0));
            } else {
                meta.setAttribute(key, String.join(", ", readValueList(reader)));
            }
        }
    }

    private List<String> readValueList(HierarchicalStreamReader reader) {

        reader.moveDown();

        List<String> l = new ArrayList<>();
        while (reader.hasMoreChildren()) {
            reader.moveDown();

            switch (reader.getNodeName()) {
            case "rim:Value":
                l.add(reader.getValue());
                break;
            }

            reader.moveUp();
        }

        reader.moveUp();

        return l;
    }

    /**
     * Read name, telephone number, and email from point of contact to attach to metacard.
     *
     * @param reader
     * @param meta
     */
    private void readPointOfContact(HierarchicalStreamReader reader, MetacardImpl meta) {
        reader.moveDown();
        reader.moveDown();
        reader.moveDown();

        Map<String, String> map = new HashMap<>();

        map.put("name", "no name");
        map.put("telephone", "no telephone number");
        map.put("email", "no email address");

        while (reader.hasMoreChildren()) {
            reader.moveDown();

            switch (reader.getNodeName()) {
            case "rim:PersonName":
                map.put("name",
                        reader.getAttribute("firstName") + " " + reader.getAttribute("lastName"));
                break;
            case "rim:TelephoneNumber":
                map.put("telephone", readPhoneNumber(reader));
                break;
            case "rim:EmailAddress":
                map.put("email", reader.getAttribute("address"));
                break;
            }

            reader.moveUp();
        }

        String contact = String.format("%s, %s, %s",
                map.get("name"),
                map.get("telephone"),
                map.get("email"));

        meta.setAttribute(Metacard.POINT_OF_CONTACT, contact);

        reader.moveUp();
        reader.moveUp();
        reader.moveUp();
    }

    /**
     * Quickly validate that a service binding has:
     * - a bindingType slot
     *
     * @param reader
     * @throws ConversionException
     */
    private String validateServiceBinding(HierarchicalStreamReader reader)
            throws ConversionException {

        String id = reader.getAttribute("id");
        String bindingType = null;

        int bindingTypes = 0;
        while (reader.hasMoreChildren()) {
            reader.moveDown();

            if ("bindingType".equals(reader.getAttribute("name"))) {

                List<String> types = readValueList(reader);

                if (types.size() != 1) {
                    throw new ConversionException(
                            "ServiceBinding with id = " + id + " has invalid bindingType value.");
                }

                bindingType = types.get(0);
                bindingTypes += 1;
            }

            reader.moveUp();

            if (bindingTypes > 1) {
                throw new ConversionException(
                        "ServiceBinding with id = " + id + " has too many bindingType.");
            }
        }

        if (bindingTypes == 0) {
            throw new ConversionException(
                    "ServiceBinding with id = " + id + " has no bindingType.");
        }

        return bindingType;
    }

    /**
     * Validate that a service has a correct object type and id.
     * <p>
     * NOTE: if the id matches the ID_PATTERN, then it pulls out the uuid.
     *
     * @param reader
     * @return id value
     * @throws ConversionException
     */
    private String validateService(HierarchicalStreamReader reader) throws ConversionException {

        String id = reader.getAttribute("id");

        if (StringUtils.isEmpty(id)) {
            throw new ConversionException("Service missing id.");
        }

        Matcher matcher = PATTERN.matcher(id);

        if (matcher.find()) {
            id = matcher.group(1);
        }

        String type = reader.getAttribute("objectType");

        if (!"urn:service:catalog".equals(type)) {
            throw new ConversionException("Service with id = " + id + " bad objectType = " + type);
        }

        return id;
    }

    private static Date parseISO8601(String str) {
        return dateOptionalTimeParser().parseDateTime(str)
                .toDate();
    }

    private List<Date> readDates(HierarchicalStreamReader reader) {
        return readValueList(reader).stream()
                .map(RegistryServiceConverter::parseISO8601)
                .collect(toList());
    }

    private void readTopLevelSlots(HierarchicalStreamReader reader, RegistryMetacardImpl meta) {
        switch (reader.getAttribute("name")) {
        case "organization":
            readOrgInfo(reader, meta);
            break;
        case "pointOfContact":
            readPointOfContact(reader, meta);
            break;
        case "lastUpdated":
            List<Date> ds = readDates(reader);
            meta.setModifiedDate(ds.get(0));
            break;
        }

    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context)
            throws ConversionException {

        RegistryMetacardImpl meta = new RegistryMetacardImpl(RSMT);

        List<String> bindingTypes = new ArrayList<>();
        String id = validateService(reader);

        meta.setAttribute(Metacard.ID, id);

        while (reader.hasMoreChildren()) {
            reader.moveDown();

            switch (reader.getNodeName()) {
            case "rim:Slot":
                readTopLevelSlots(reader, meta);
                break;
            case "rim:Description":
                meta.setAttribute(Metacard.DESCRIPTION, readValue(reader));
                break;
            case "rim:Name":
                meta.setAttribute(Metacard.TITLE, readValue(reader));
                break;
            case "rim:VersionInfo":
                meta.setAttribute(Metacard.CONTENT_TYPE_VERSION,
                        reader.getAttribute("versionName"));
                break;
            case "rim:Classification":
                readClassification(reader, meta);
                break;
            case "rim:ServiceBinding":
                bindingTypes.add(validateServiceBinding(reader));
                break;
            }

            reader.moveUp();
        }

        meta.setAttribute(RegistryServiceMetacardType.SERVICE_BINDING_TYPES,
                String.join(", ", bindingTypes));

        return meta;
    }

    @Override
    public boolean canConvert(Class clazz) {
        return Metacard.class.isAssignableFrom(clazz);
    }
}
