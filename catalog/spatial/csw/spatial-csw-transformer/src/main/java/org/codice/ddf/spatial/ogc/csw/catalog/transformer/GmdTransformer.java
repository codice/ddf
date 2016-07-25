/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package org.codice.ddf.spatial.ogc.csw.catalog.transformer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.activation.MimeType;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GmdMetacardType;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.CswUnmarshallHelper;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.GmdConverter;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.XstreamPathConverter;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.XstreamPathValueTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.converters.DataHolder;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.core.TreeMarshaller;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.naming.NoNameCoder;
import com.thoughtworks.xstream.io.path.Path;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import com.thoughtworks.xstream.io.xml.QNameMap;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import com.thoughtworks.xstream.io.xml.StaxReader;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.WKTWriter;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Contact;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.Location;
import ddf.catalog.data.types.Media;
import ddf.catalog.data.types.Topic;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.catalog.transform.MetacardTransformer;

public class GmdTransformer implements InputTransformer, MetacardTransformer {

    private static final Logger LOGGER = LoggerFactory.getLogger(GmdTransformer.class);

    private static final String COLON = ":";

    private static final GmdMetacardType GMD_METACARD_TYPE = new GmdMetacardType();

    private static final String GCO_COLON = GmdMetacardType.GCO_PREFIX + ":";

    private static final String TRANSFORM_EXCEPTION_MSG =
            "Unable to transform from GMD Metadata to Metacard";

    private final XStream xstream;

    private final DataHolder argumentHolder;

    private final XMLInputFactory xmlFactory;

    public GmdTransformer() {
        QNameMap qmap = new QNameMap();
        qmap.setDefaultNamespace(GmdMetacardType.GMD_NAMESPACE);
        qmap.setDefaultPrefix("");
        StaxDriver staxDriver = new StaxDriver(qmap);

        xstream = new XStream(staxDriver);
        xstream.setClassLoader(this.getClass()
                .getClassLoader());
        XstreamPathConverter converter = new XstreamPathConverter();
        xstream.registerConverter(converter);
        xstream.alias("MD_Metadata", XstreamPathValueTracker.class);
        argumentHolder = xstream.newDataHolder();
        argumentHolder.put(XstreamPathConverter.PATH_KEY, buildPaths());
        xmlFactory = XMLInputFactory.newInstance();
    }

    private LinkedHashSet<Path> buildPaths() {
        LinkedHashSet<Path> paths = new LinkedHashSet<>();

        paths.add(new Path(GmdMetacardType.FILE_IDENTIFIER_PATH));

        Arrays.asList(GmdMetacardType.FILE_IDENTIFIER_PATH,
                GmdMetacardType.DATE_TIME_STAMP_PATH,
                GmdMetacardType.DATE_STAMP_PATH,
                GmdMetacardType.CODE_LIST_VALUE_PATH,
                GmdMetacardType.CRS_AUTHORITY_PATH,
                GmdMetacardType.CRS_VERSION_PATH,
                GmdMetacardType.CRS_CODE_PATH,
                GmdMetacardType.TITLE_PATH,
                GmdMetacardType.ABSTRACT_PATH,
                GmdMetacardType.FORMAT_PATH,
                GmdMetacardType.LINKAGE_URI_PATH,
                GmdMetacardType.KEYWORD_PATH,
                GmdMetacardType.TOPIC_CATEGORY_PATH,
                GmdMetacardType.BBOX_WEST_LON_PATH,
                GmdMetacardType.BBOX_EAST_LON_PATH,
                GmdMetacardType.BBOX_SOUTH_LAT_PATH,
                GmdMetacardType.BBOX_NORTH_LAT_PATH,
                GmdMetacardType.POINT_OF_CONTACT_PATH)
                .forEach(path -> paths.add(toPath(path)));

        return paths;
    }

    @Override
    public Metacard transform(InputStream inputStream)
            throws IOException, CatalogTransformerException {
        return transform(inputStream, null);
    }

    @Override
    public Metacard transform(InputStream inputStream, String id)
            throws IOException, CatalogTransformerException {

        String xml;
        XstreamPathValueTracker pathValueTracker = null;
        xml = IOUtils.toString(inputStream);

        try {
            XMLStreamReader streamReader = xmlFactory.createXMLStreamReader(new StringReader(xml));
            HierarchicalStreamReader reader = new StaxReader(new QNameMap(), streamReader);
            pathValueTracker = (XstreamPathValueTracker) xstream.unmarshal(reader,
                    null,
                    argumentHolder);
        } catch (XStreamException | XMLStreamException e) {
            throw new CatalogTransformerException(TRANSFORM_EXCEPTION_MSG, e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }

        MetacardImpl metacard = toMetacard(pathValueTracker, id);

        metacard.setAttribute(Metacard.METADATA, xml);

        return metacard;
    }

    @Override
    public BinaryContent transform(Metacard metacard, Map<String, Serializable> arguments)
            throws CatalogTransformerException {
        StringWriter stringWriter = new StringWriter();
        Boolean omitXmlDec = null;
        if (MapUtils.isNotEmpty(arguments)) {
            omitXmlDec = (Boolean) arguments.get(CswConstants.OMIT_XML_DECLARATION);
        }

        if (omitXmlDec == null || !omitXmlDec) {
            stringWriter.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        }

        PrettyPrintWriter writer = new PrettyPrintWriter(stringWriter, new NoNameCoder());

        MarshallingContext context = new TreeMarshaller(writer, null, null);
        copyArgumentsToContext(context, arguments);

        new GmdConverter().marshal(metacard, writer, context);

        BinaryContent transformedContent;

        ByteArrayInputStream bais = new ByteArrayInputStream(stringWriter.toString()
                .getBytes(StandardCharsets.UTF_8));
        transformedContent = new BinaryContentImpl(bais, new MimeType());
        return transformedContent;
    }

    private void copyArgumentsToContext(MarshallingContext context,
            Map<String, Serializable> arguments) {

        if (context == null || arguments == null) {
            return;
        }

        for (Map.Entry<String, Serializable> entry : arguments.entrySet()) {
            context.put(entry.getKey(), entry.getValue());
        }
    }

    private MetacardImpl toMetacard(final XstreamPathValueTracker pathValueTracker,
            final String id) {
        MetacardImpl metacard = new MetacardImpl(GMD_METACARD_TYPE);

        if (StringUtils.isNotEmpty(id)) {
            metacard.setAttribute(Metacard.ID, id);
        } else {
            metacard.setId(pathValueTracker.getFirstValue(toPath(GmdMetacardType.FILE_IDENTIFIER_PATH)));
        }
        addMetacardDates(pathValueTracker, metacard);

        addMetacardType(pathValueTracker, metacard);

        addMetacardCrs(pathValueTracker, metacard);

        addMetacardTitle(pathValueTracker, metacard);

        addMetacardAbstract(pathValueTracker, metacard);

        addMetacardFormat(pathValueTracker, metacard);

        addMetacardResourceUri(pathValueTracker, metacard);

        addMetacardLocation(pathValueTracker, metacard);

        addMetacardSubject(pathValueTracker, metacard);

        addLanguage(pathValueTracker, metacard);

        setPointOfContact(pathValueTracker, metacard);

        return metacard;
    }

    private Path toPath(String stringPath) {
        return new Path(stringPath.replace(GCO_COLON, ""));

    }

    private void addMetacardDates(final XstreamPathValueTracker pathValueTracker,
            MetacardImpl metacard) {

        String dateTimeStr =
                pathValueTracker.getFirstValue(toPath(GmdMetacardType.DATE_TIME_STAMP_PATH));
        String dateStr = pathValueTracker.getFirstValue(toPath(GmdMetacardType.DATE_STAMP_PATH));

        Date date = new Date();
        if (StringUtils.isNotBlank(dateTimeStr)) {
            date = CswUnmarshallHelper.convertToDate(dateTimeStr);

        } else if (StringUtils.isNotBlank(dateStr)) {
            date = CswUnmarshallHelper.convertToDate(dateStr);

        }
        metacard.setModifiedDate(date);
        metacard.setCreatedDate(date);
        metacard.setEffectiveDate(date);
        metacard.setAttribute(Core.METACARD_MODIFIED, date);
        metacard.setAttribute(Core.METACARD_CREATED, date);
    }

    private void setPointOfContact(final XstreamPathValueTracker pathValueTracker,
            MetacardImpl metacard) {

        String pointOfContact =
                pathValueTracker.getFirstValue(toPath(GmdMetacardType.POINT_OF_CONTACT_PATH));
        if (StringUtils.isNotEmpty(pointOfContact)) {
            metacard.setPointOfContact(pointOfContact);
            metacard.setAttribute(Contact.POINT_OF_CONTACT_NAME, pointOfContact);
        }

    }

    private void addMetacardType(final XstreamPathValueTracker pathValueTracker,
            MetacardImpl metacard) {

        String type = pathValueTracker.getFirstValue(toPath(GmdMetacardType.CODE_LIST_VALUE_PATH));
        if (StringUtils.isNotEmpty(type)) {
            metacard.setContentTypeName(type);
            metacard.setAttribute(Media.TYPE, type);
        }

    }

    private void addMetacardCrs(final XstreamPathValueTracker pathValueTracker,
            MetacardImpl metacard) {

        String authority = StringUtils.defaultString(pathValueTracker.getFirstValue(toPath(
                GmdMetacardType.CRS_AUTHORITY_PATH)));
        String code = StringUtils.defaultString(pathValueTracker.getFirstValue(toPath(
                GmdMetacardType.CRS_CODE_PATH)));

        metacard.setAttribute(Location.COORDINATE_REFERENCE_SYSTEM_CODE, authority + COLON + code);
    }

    private void addMetacardTitle(final XstreamPathValueTracker pathValueTracker,
            MetacardImpl metacard) {
        String title = pathValueTracker.getFirstValue(toPath(GmdMetacardType.TITLE_PATH));
        if (StringUtils.isNotEmpty(title)) {
            metacard.setTitle(title);
        }
    }

    private void addMetacardAbstract(final XstreamPathValueTracker pathValueTracker,
            MetacardImpl metacard) {
        String gmdAbstract = pathValueTracker.getFirstValue(toPath(GmdMetacardType.ABSTRACT_PATH));
        if (StringUtils.isNotEmpty(gmdAbstract)) {
            metacard.setDescription(gmdAbstract);
        }

    }

    private void addMetacardFormat(final XstreamPathValueTracker pathValueTracker,
            MetacardImpl metacard) {
        String format = pathValueTracker.getFirstValue(toPath(GmdMetacardType.FORMAT_PATH));
        if (StringUtils.isNotEmpty((format))) {
            metacard.setAttribute(Media.FORMAT, format);
        }
    }

    private void addMetacardResourceUri(final XstreamPathValueTracker pathValueTracker,
            MetacardImpl metacard) {
        String linkage = pathValueTracker.getFirstValue(toPath(GmdMetacardType.LINKAGE_URI_PATH));
        if (StringUtils.isNotEmpty(linkage)) {
            try {
                metacard.setResourceURI(new URI(linkage.trim()));
            } catch (URISyntaxException e) {
                LOGGER.info("Unable to read resource URI {}", linkage, e);
            }
        }

    }

    private List<Serializable> toSerializableList(List<String> in) {
        return in.stream()
                .filter(Serializable.class::isInstance)
                .map(Serializable.class::cast)
                .collect(Collectors.toList());
    }

    private void addMetacardSubject(final XstreamPathValueTracker pathValueTracker,
            MetacardImpl metacard) {

        List<String> keywords = pathValueTracker.getAllValues(toPath(GmdMetacardType.KEYWORD_PATH));
        List<String> topics =
                pathValueTracker.getAllValues(toPath(GmdMetacardType.TOPIC_CATEGORY_PATH));

        if (CollectionUtils.isNotEmpty(keywords)) {
            metacard.setAttribute(new AttributeImpl(Topic.KEYWORD, toSerializableList(keywords)));
        }

        if (CollectionUtils.isNotEmpty(topics)) {
            metacard.setAttribute(new AttributeImpl(Topic.CATEGORY, toSerializableList(topics)));
        }

    }

    private void addLanguage(final XstreamPathValueTracker pathValueTracker,
            MetacardImpl metacard) {
        String language = pathValueTracker.getFirstValue(toPath(GmdMetacardType.LANGUAGE_PATH));
        if (StringUtils.isNotEmpty(language)) {
            metacard.setAttribute(Core.LANGUAGE, language);
        }
    }

    private void addMetacardLocation(final XstreamPathValueTracker pathValueTracker,
            MetacardImpl metacard) {
        String westLon = pathValueTracker.getFirstValue(toPath(GmdMetacardType.BBOX_WEST_LON_PATH));
        String eastLon = pathValueTracker.getFirstValue(toPath(GmdMetacardType.BBOX_EAST_LON_PATH));
        String southLat =
                pathValueTracker.getFirstValue(toPath(GmdMetacardType.BBOX_SOUTH_LAT_PATH));
        String northLat =
                pathValueTracker.getFirstValue(toPath(GmdMetacardType.BBOX_NORTH_LAT_PATH));

        if (westLon != null && eastLon != null && southLat != null && northLat != null) {
            WKTWriter wktWriter = new WKTWriter();

            GeometryFactory factory = new GeometryFactory();
            try {
                Envelope envelope = new Envelope(Double.parseDouble(eastLon.trim()),
                        Double.parseDouble(westLon.trim()),
                        Double.parseDouble(southLat.trim()),
                        Double.parseDouble(northLat.trim()));
                String wkt = wktWriter.write(factory.toGeometry(envelope));
                if (wkt != null) {
                    metacard.setLocation(wkt);
                }
            } catch (NumberFormatException nfe) {
                LOGGER.info("Unable to parse double from GMD metadata {}, {}, {}, {}",
                        westLon,
                        eastLon,
                        southLat,
                        northLat);
            }
        }
    }

}
