/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.catalog.transformer.input.geojson;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.BasicTypes;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardImpl;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.QualifiedMetacardType;
import ddf.catalog.data.MetacardTypeRegistry;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.geo.formatter.CompositeGeometry;

/**
 * Converts standard GeoJSON (geojson.org) into a Metacard. The limitation on the GeoJSON is that it
 * must conform to the {@link BasicTypes#BASIC_METACARD} {@link MetacardType}.
 * 
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 * @since 0.1.0
 */
public class GeoJsonInputTransformer implements InputTransformer {

    private static final String METACARD_TYPE_PROPERTY_KEY = "metacard-type";

    protected static final JSONParser PARSER = new JSONParser();

    private static final String ID = "geojson";

    private static final String MIME_TYPE = "application/json";

    private static final String SOURCE_ID_PROPERTY = "source-id";

    private MetacardTypeRegistry mTypeRegistry;

    public static final SimpleDateFormat ISO_8601_DATE_FORMAT = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    static {
        ISO_8601_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private static final Logger LOGGER = Logger.getLogger(GeoJsonInputTransformer.class);

    public GeoJsonInputTransformer(MetacardTypeRegistry mTypeRegistry) {
        this.mTypeRegistry = mTypeRegistry;
    }

    /**
     * Transforms GeoJson (http://www.geojson.org/) into a {@link Metacard}
     */
    @Override
    public Metacard transform(InputStream input) throws IOException, CatalogTransformerException {
        return transform(input, null);
    }

    @Override
    public Metacard transform(InputStream input, String id) throws IOException,
        CatalogTransformerException {

        if (input == null) {
            throw new CatalogTransformerException("Cannot transform null input.");
        }

        JSONObject rootObject = null;

        try {
            rootObject = (JSONObject) PARSER.parse(IOUtils.toString(input));
        } catch (ParseException e) {
            LOGGER.error(e);
            throw new CatalogTransformerException("Could not parse json text:", e);
        }

        Object typeValue = rootObject.get(CompositeGeometry.TYPE_KEY);
        if (typeValue == null || !typeValue.equals("Feature")) {
            throw new CatalogTransformerException(new UnsupportedOperationException(
                    "Only supported type is Feature, not [" + typeValue + "]"));
        }

        JSONObject properties = (JSONObject) rootObject.get(CompositeGeometry.PROPERTIES_KEY);

        if (properties == null) {
            throw new CatalogTransformerException("Properties are required to create a Metacard.");
        }

        String metacardTypeName = (String) properties.get(METACARD_TYPE_PROPERTY_KEY);
        MetacardImpl metacard = null;

        if (metacardTypeName == null || metacardTypeName.isEmpty() || mTypeRegistry == null) {
            LOGGER.debug("MetacardType specified in input is null or empty.  Assuming default MetacardType");
            metacard = new MetacardImpl();
        } else {
            QualifiedMetacardType metacardType = mTypeRegistry.lookup(metacardTypeName);
            if (metacardType == null) {
                String message = "MetacardType specified in input has not been registered with the system.  Cannot parse input.  MetacardType name: "
                        + metacardTypeName;
                LOGGER.warn(message);
                throw new CatalogTransformerException(message);
            }
            LOGGER.debug("Found registered MetacardType: " + metacardTypeName);
            metacard = new MetacardImpl(metacardType);
        }

        MetacardType metacardType = metacard.getMetacardType();
        metacardTypeName = metacardType.getName();
        LOGGER.debug("Metacard type name: " + metacardType.getName());

        // retrieve geometry
        JSONObject geometryJson = (JSONObject) rootObject.get(CompositeGeometry.GEOMETRY_KEY);
        CompositeGeometry geoJsonGeometry = null;
        if (geometryJson != null) {
            if (geometryJson.get(CompositeGeometry.TYPE_KEY) != null
                    && geometryJson.get(CompositeGeometry.COORDINATES_KEY) != null) {

                String geometryTypeJson = geometryJson.get(CompositeGeometry.TYPE_KEY).toString();

                geoJsonGeometry = CompositeGeometry.getCompositeGeometry(geometryTypeJson,
                        geometryJson);

            } else {
                LOGGER.warn("Could not find geometry type.");
            }
        }

        // find where the geometry goes
        String geoAttributeName = null;
        for (AttributeDescriptor ad : metacardType.getAttributeDescriptors()) {
            if (AttributeFormat.GEOMETRY.equals(ad.getType().getAttributeFormat())) {
                geoAttributeName = ad.getName();
            }
        }

        if (geoJsonGeometry != null) {
            if (geoAttributeName != null) {
                metacard.setAttribute(geoAttributeName, geoJsonGeometry.toWkt());
            } else {
                LOGGER.warn("Loss of data, could not place geometry [" + geoJsonGeometry.toWkt()
                        + "] in metacard");
            }
        }

        // TODO read which metatype they need, find the metatype and use it for
        // reading the data format

        for (AttributeDescriptor ad : metacardType.getAttributeDescriptors()) {

            try {
                if (properties.containsKey(ad.getName())) {

                    Object attributeValue = properties.get(ad.getName());
                    if (attributeValue != null) {

                        String attributeString = attributeValue.toString();

                        switch (ad.getType().getAttributeFormat()) {
                        case BINARY:
                            metacard.setAttribute(ad.getName(),
                                    DatatypeConverter.parseBase64Binary(attributeString));
                            break;
                        case DATE:
                            try {
                                metacard.setAttribute(ad.getName(),
                                        ISO_8601_DATE_FORMAT.parse(attributeString));
                            } catch (java.text.ParseException e) {
                                throw new CatalogTransformerException("Could not parse Date:", e);
                            }
                            break;
                        case GEOMETRY:
                            break;
                        case STRING:
                        case XML:
                            metacard.setAttribute(ad.getName(), attributeString);
                            break;
                        case BOOLEAN:
                            metacard.setAttribute(ad.getName(),
                                    Boolean.parseBoolean(attributeString));
                            break;
                        case SHORT:
                            metacard.setAttribute(ad.getName(), Short.parseShort(attributeString));
                            break;
                        case INTEGER:
                            metacard.setAttribute(ad.getName(), Integer.parseInt(attributeString));
                            break;
                        case LONG:
                            metacard.setAttribute(ad.getName(), Long.parseLong(attributeString));
                            break;
                        case FLOAT:
                            metacard.setAttribute(ad.getName(), Float.parseFloat(attributeString));
                            break;
                        case DOUBLE:
                            metacard.setAttribute(ad.getName(), Double.parseDouble(attributeString));
                            break;
                        default:
                            break;
                        }
                    }
                }
            } catch (NumberFormatException e) {
                LOGGER.info(
                        "GeoJSON input for attribute name '"
                                + ad.getName()
                                + "' does not match expected AttributeType defined in MetacardType: "
                                + metacardTypeName
                                + ".  This attribute will not be added to the metacard.", e);
            }
        }

        if (metacard.getSourceId() != null && !"".equals(metacard.getSourceId())) {
            properties.put(SOURCE_ID_PROPERTY, metacard.getSourceId());
        }

        if (id != null) {
            metacard.setId(id);
        }

        return metacard;
    }

    @Override
    public String toString() {
        return "InputTransformer {Impl=" + this.getClass().getName() + ", id=" + ID
                + ", mime-type=" + MIME_TYPE + "}";
    }

}
