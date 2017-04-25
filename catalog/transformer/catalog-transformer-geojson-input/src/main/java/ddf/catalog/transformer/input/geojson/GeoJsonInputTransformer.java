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
 */
package ddf.catalog.transformer.input.geojson;

import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.isNotEmpty;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.function.Function;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang3.StringUtils;
import org.boon.json.JsonFactory;
import org.boon.json.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.geo.formatter.CompositeGeometry;

/**
 * Converts standard GeoJSON (geojson.org) into a Metacard. The limitation on the GeoJSON is that it
 * must conform to the {@link ddf.catalog.data.impl.BasicTypes#BASIC_METACARD} {@link MetacardType}.
 */
public class GeoJsonInputTransformer implements InputTransformer {

    static final String ISO_8601_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    private static final ObjectMapper MAPPER = JsonFactory.create();

    private static final String METACARD_TYPE_PROPERTY_KEY = "metacard-type";

    private static final String ID = "geojson";

    private static final String MIME_TYPE = "application/json";

    private static final String SOURCE_ID_PROPERTY = "source-id";

    private static final Logger LOGGER = LoggerFactory.getLogger(GeoJsonInputTransformer.class);

    private List<MetacardType> metacardTypes;

    private AttributeRegistry attributeRegistry;

    /**
     * Transforms GeoJson (http://www.geojson.org/) into a {@link Metacard}
     */
    @Override
    public Metacard transform(InputStream input) throws IOException, CatalogTransformerException {
        return transform(input, null);
    }

    @Override
    public Metacard transform(InputStream input, String id)
            throws IOException, CatalogTransformerException {

        if (input == null) {
            throw new CatalogTransformerException("Cannot transform null input.");
        }

        Map<String, Object> rootObject = MAPPER.parser()
                .parseMap(input);

        if (rootObject == null) {
            throw new CatalogTransformerException("Unable to parse JSON for metacard.");
        }

        Object typeValue = rootObject.get(CompositeGeometry.TYPE_KEY);
        if (typeValue == null || !typeValue.equals("Feature")) {
            throw new CatalogTransformerException(new UnsupportedOperationException(
                    "Only supported type is Feature, not [" + typeValue + "]"));
        }

        Map<String, Object> properties =
                (Map<String, Object>) rootObject.get(CompositeGeometry.PROPERTIES_KEY);

        if (properties == null) {
            throw new CatalogTransformerException("Properties are required to create a Metacard.");
        }

        final String propertyTypeName = (String) properties.get(METACARD_TYPE_PROPERTY_KEY);
        MetacardImpl metacard;

        if (isEmpty(propertyTypeName) || metacardTypes == null) {
            LOGGER.debug(
                    "MetacardType specified in input is null or empty.  Assuming default MetacardType");
            metacard = new MetacardImpl();
        } else {
            MetacardType metacardType = metacardTypes.stream()
                    .filter(type -> type.getName()
                            .equals(propertyTypeName))
                    .findFirst()
                    .orElseThrow(() -> new CatalogTransformerException(
                            "MetacardType specified in input has not been registered with the system.  Cannot parse input.  MetacardType name: "
                                    + propertyTypeName));

            LOGGER.debug("Found registered MetacardType: {}", propertyTypeName);
            metacard = new MetacardImpl(metacardType);
        }

        MetacardType metacardType = metacard.getMetacardType();
        LOGGER.debug("Metacard type name: {}", metacardType.getName());

        // retrieve geometry
        Map<String, Object> geometryJson =
                (Map<String, Object>) rootObject.get(CompositeGeometry.GEOMETRY_KEY);
        CompositeGeometry geoJsonGeometry = null;
        if (geometryJson != null) {
            if (geometryJson.get(CompositeGeometry.TYPE_KEY) != null && (geometryJson.get(
                    CompositeGeometry.COORDINATES_KEY) != null
                    || geometryJson.get(CompositeGeometry.GEOMETRIES_KEY) != null)) {

                String geometryTypeJson = geometryJson.get(CompositeGeometry.TYPE_KEY)
                        .toString();

                geoJsonGeometry = CompositeGeometry.getCompositeGeometry(geometryTypeJson,
                        geometryJson);

            } else {
                LOGGER.debug("Could not find geometry type.");
            }
        }

        if (geoJsonGeometry != null && StringUtils.isNotEmpty(geoJsonGeometry.toWkt())) {
            metacard.setLocation(geoJsonGeometry.toWkt());
        }

        Map<String, AttributeDescriptor> attributeDescriptorMap =
                metacardType.getAttributeDescriptors()
                        .stream()
                        .collect(toMap(AttributeDescriptor::getName, Function.identity()));

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            final String key = entry.getKey();
            final Object value = entry.getValue();
            try {
                if (attributeDescriptorMap.containsKey(key)) {
                    AttributeDescriptor ad = attributeDescriptorMap.get(key);
                    metacard.setAttribute(key, convertProperty(value, ad));
                } else {
                    Optional<AttributeDescriptor> optional = attributeRegistry.lookup(key);
                    if (optional.isPresent()) {
                        metacard.setAttribute(key, convertProperty(value, optional.get()));
                    }
                }
            } catch (NumberFormatException | ParseException e) {
                LOGGER.info(
                        "GeoJSON input for attribute name '{}' does not match the expected AttributeType. This attribute will not be added to the metacard.",
                        key,
                        e);
            }
        }

        if (isNotEmpty(metacard.getSourceId())) {
            properties.put(SOURCE_ID_PROPERTY, metacard.getSourceId());
        }

        if (id != null) {
            metacard.setId(id);
        }

        return metacard;
    }

    public void setMetacardTypes(List<MetacardType> metacardTypes) {
        this.metacardTypes = metacardTypes;
    }

    public void setAttributeRegistry(AttributeRegistry attributeRegistry) {
        this.attributeRegistry = attributeRegistry;
    }

    @Override
    public String toString() {
        return "InputTransformer {Impl=" + this.getClass()
                .getName() + ", id=" + ID + ", mime-type=" + MIME_TYPE + "}";
    }

    private Serializable convertProperty(Object property, AttributeDescriptor descriptor)
            throws ParseException {
        AttributeFormat format = descriptor.getType()
                .getAttributeFormat();
        if (descriptor.isMultiValued() && property instanceof List) {
            List<Serializable> values = new ArrayList<>();
            for (Object value : (List) property) {
                values.add(convertValue(value, format));
            }
            return (Serializable) values;
        } else {
            return convertValue(property, format);
        }
    }

    private Serializable convertValue(Object value, AttributeFormat format) throws ParseException {
        if (value == null) {
            return null;
        }

        switch (format) {
        case BINARY:
            return DatatypeConverter.parseBase64Binary(value.toString());
        case DATE:
            SimpleDateFormat dateFormat = new SimpleDateFormat(ISO_8601_DATE_FORMAT);
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            return dateFormat.parse(value.toString());
        case GEOMETRY:
        case STRING:
        case XML:
            return value.toString();
        case BOOLEAN:
            return Boolean.parseBoolean(value.toString());
        case SHORT:
            return Short.parseShort(value.toString());
        case INTEGER:
            return Integer.parseInt(value.toString());
        case LONG:
            return Long.parseLong(value.toString());
        case FLOAT:
            return Float.parseFloat(value.toString());
        case DOUBLE:
            return Double.parseDouble(value.toString());
        default:
            return null;
        }
    }
}
