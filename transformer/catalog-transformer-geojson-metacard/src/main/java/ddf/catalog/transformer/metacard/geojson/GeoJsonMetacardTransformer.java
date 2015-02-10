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
package ddf.catalog.transformer.metacard.geojson;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.xml.bind.DatatypeConverter;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import ddf.geo.formatter.CompositeGeometry;

/**
 * Implements the {@link MetacardTransformer} interface to transform a single {@link Metacard}
 * instance to GeoJSON. This class places what is returned by {@link Metacard#getLocation()} in the
 * geometry JSON object in the GeoJSON output. The rest of the attributes of the Metacard are placed
 * in the properties object in the JSON. See geojson.org for the GeoJSON specification.
 * 
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 * 
 * @see MetacardTransformer
 * @see Metacard
 * @see Attribute
 * 
 */
public class GeoJsonMetacardTransformer implements MetacardTransformer {

    public static final String ISO_8601_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    protected static final String METACARD_TYPE_PROPERTY_KEY = "metacard-type";

    public static final String ID = "geojson";

    public static MimeType DEFAULT_MIME_TYPE = null;

    private static final Logger LOGGER = LoggerFactory.getLogger(GeoJsonMetacardTransformer.class);

    private static final String SOURCE_ID_PROPERTY = "source-id";

    static {
        try {
            DEFAULT_MIME_TYPE = new MimeType("application/json");
        } catch (MimeTypeParseException e) {
            LOGGER.warn("MimeType exception during static setup", e);
        }
    }

    @Override
    public BinaryContent transform(Metacard metacard, Map<String, Serializable> arguments)
        throws CatalogTransformerException {

        JSONObject rootObject = convertToJSON(metacard);

        String jsonText = JSONValue.toJSONString(rootObject);

        return new ddf.catalog.data.BinaryContentImpl(
                new ByteArrayInputStream(jsonText.getBytes(StandardCharsets.UTF_8)), DEFAULT_MIME_TYPE);
    }

    public static JSONObject convertToJSON(Metacard metacard) throws CatalogTransformerException {
        if (metacard == null) {
            throw new CatalogTransformerException("Cannot transform null metacard.");
        }

        // must be LinkedHashMap to maintain order
        JSONObject rootObject = new JSONObject();
        rootObject.put("type", "Feature");
        JSONObject properties = new JSONObject();

        for (AttributeDescriptor ad : metacard.getMetacardType().getAttributeDescriptors()) {

            Attribute attribute = metacard.getAttribute(ad.getName());

            if (attribute != null) {
                switch (ad.getType().getAttributeFormat()) {
                case BOOLEAN:
                    properties.put(attribute.getName(), (Boolean) attribute.getValue());
                    break;
                case DATE:
                    if (attribute.getValue() != null) {
                        SimpleDateFormat dateFormat = new SimpleDateFormat(ISO_8601_DATE_FORMAT);
                        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                        properties.put(attribute.getName(),
                                dateFormat.format(((Date) attribute.getValue())));
                    }
                    break;
                case BINARY:
                    if (attribute.getValue() != null) {
                        byte[] bytes = ((byte[]) attribute.getValue());

                        String base64 = DatatypeConverter.printBase64Binary(bytes);

                        properties.put(attribute.getName(), base64);
                    }
                    break;
                // Since we just need the string version of Doubles, Longs, Floats, Integers, or
                // Shorts, no conversion
                // or processing is necessary. The toString method on each of those classes
                // suffices.
                case DOUBLE:
                case LONG:
                case FLOAT:
                case INTEGER:
                case SHORT:
                case STRING:
                case XML:
                    if (attribute.getValue() != null) {
                        // xml is automatically escaped by json library
                        properties.put(attribute.getName(), attribute.getValue().toString());
                    }
                    break;
                case GEOMETRY:
                    if (attribute.getValue() != null) {
                        WKTReader reader = new WKTReader();
                        try {
                            Geometry geometry = reader.read(attribute.getValue().toString());
                            CompositeGeometry geoJsonGeometry = CompositeGeometry
                                    .getCompositeGeometry(geometry);
                            if (geoJsonGeometry == null) {
                                throw new CatalogTransformerException(
                                        "Could not perform transform: unsupported geometry ["
                                                + attribute.getValue() + "]");
                            }
                            rootObject.put(CompositeGeometry.GEOMETRY_KEY,
                                    geoJsonGeometry.toJsonMap());
                        } catch (ParseException e) {
                            LOGGER.warn("Parse exception during reading of geometry", e);
                            throw new CatalogTransformerException(
                                    "Could not perform transform: could not parse geometry.", e);
                        }

                    }

                    break;
                default:
                    break;
                }

            }

        }

        if (rootObject.get(CompositeGeometry.GEOMETRY_KEY) == null) {
            rootObject.put(CompositeGeometry.GEOMETRY_KEY, null);
        }

        properties.put(METACARD_TYPE_PROPERTY_KEY, metacard.getMetacardType().getName());

        if (metacard.getSourceId() != null && !"".equals(metacard.getSourceId())) {
            properties.put(SOURCE_ID_PROPERTY, metacard.getSourceId());
        }

        rootObject.put(CompositeGeometry.PROPERTIES_KEY, properties);
        return rootObject;
    }

    @Override
    public String toString() {
        return MetacardTransformer.class.getName() + " {Impl=" + this.getClass().getName()
                + ", id=" + ID + ", MIME Type=" + DEFAULT_MIME_TYPE + "}";
    }

}
