/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * </p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.data.impl;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;

/**
 * Constants for basic types, both {@link MetacardType} and {@link AttributeType}
 *
 * @author ddf.isgs@lmco.com
 */
public class BasicTypes {
    /**
     * A Constant for a {@link MetacardType} with the required {@link AttributeType}s.
     */
    public static final MetacardType BASIC_METACARD;

    /**
     * A Constant for an {@link AttributeType} with {@link AttributeFormat#DATE} .
     */
    public static final AttributeType<Date> DATE_TYPE;

    /**
     * A Constant for an {@link AttributeType} with {@link AttributeFormat#STRING}.
     */
    public static final AttributeType<String> STRING_TYPE;

    /**
     * A Constant for an {@link AttributeType} with {@link AttributeFormat#XML}.
     */
    public static final AttributeType<String> XML_TYPE;

    /**
     * A Constant for an {@link AttributeType} with {@link AttributeFormat#LONG} .
     */
    public static final AttributeType<Long> LONG_TYPE;

    /**
     * A Constant for an {@link AttributeType} with {@link AttributeFormat#BINARY}.
     */
    public static final AttributeType<byte[]> BINARY_TYPE;

    /**
     * A Constant for an {@link AttributeType} with {@link AttributeFormat#GEOMETRY}.
     */
    public static final AttributeType<String> GEO_TYPE;

    /**
     * A Constant for an {@link AttributeType} with {@link AttributeFormat#BOOLEAN}.
     */
    public static final AttributeType<Boolean> BOOLEAN_TYPE;

    /**
     * A Constant for an {@link AttributeType} with {@link AttributeFormat#DOUBLE}.
     */
    public static final AttributeType<Double> DOUBLE_TYPE;

    /**
     * A Constant for an {@link AttributeType} with {@link AttributeFormat#FLOAT}.
     */
    public static final AttributeType<Float> FLOAT_TYPE;

    /**
     * A Constant for an {@link AttributeType} with {@link AttributeFormat#INTEGER}.
     */
    public static final AttributeType<Integer> INTEGER_TYPE;

    /**
     * A Constant for an {@link AttributeType} with {@link AttributeFormat#OBJECT}.
     */
    public static final AttributeType<Serializable> OBJECT_TYPE;

    /**
     * A Constant for an {@link AttributeType} with {@link AttributeFormat#SHORT}.
     */
    public static final AttributeType<Short> SHORT_TYPE;

    public static final String VALIDATION_WARNINGS = "validation-warnings";

    public static final String VALIDATION_ERRORS = "validation-errors";

    private static final Map<String, AttributeType> ATTRIBUTE_TYPE_MAP = new HashMap<>();

    static {
        DATE_TYPE = addAttributeType("DATE_TYPE", AttributeFormat.DATE, Date.class);

        STRING_TYPE = addAttributeType("STRING_TYPE", AttributeFormat.STRING, String.class);

        XML_TYPE = addAttributeType("XML_TYPE", AttributeFormat.XML, String.class);

        LONG_TYPE = addAttributeType("LONG_TYPE", AttributeFormat.LONG, Long.class);

        BINARY_TYPE = addAttributeType("BINARY_TYPE", AttributeFormat.BINARY, byte[].class);

        GEO_TYPE = addAttributeType("GEO_TYPE", AttributeFormat.GEOMETRY, String.class);

        BOOLEAN_TYPE = addAttributeType("BOOLEAN_TYPE", AttributeFormat.BOOLEAN, Boolean.class);

        DOUBLE_TYPE = addAttributeType("DOUBLE_TYPE", AttributeFormat.DOUBLE, Double.class);

        FLOAT_TYPE = addAttributeType("FLOAT_TYPE", AttributeFormat.FLOAT, Float.class);

        INTEGER_TYPE = addAttributeType("INTEGER_TYPE", AttributeFormat.INTEGER, Integer.class);

        OBJECT_TYPE = addAttributeType("OBJECT_TYPE", AttributeFormat.OBJECT, Serializable.class);

        SHORT_TYPE = addAttributeType("SHORT_TYPE", AttributeFormat.SHORT, Short.class);

        BASIC_METACARD = new MetacardTypeImpl(MetacardType.DEFAULT_METACARD_TYPE_NAME,
                getBasicAttributeDescriptors());
    }

    private static <T extends Serializable> AttributeType<T> addAttributeType(final String typeName,
            final AttributeFormat format, final Class<T> bindingClass) {
        final AttributeType<T> attributeType = new AttributeType<T>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Class<T> getBinding() {
                return bindingClass;
            }

            @Override
            public AttributeFormat getAttributeFormat() {
                return format;
            }
        };

        ATTRIBUTE_TYPE_MAP.put(typeName, attributeType);

        return attributeType;
    }

    private static Set<AttributeDescriptor> getBasicAttributeDescriptors() {
        Set<AttributeDescriptor> descriptors = new HashSet<>();
        descriptors.add(new AttributeDescriptorImpl(Metacard.MODIFIED,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                DATE_TYPE));
        descriptors.add(new AttributeDescriptorImpl(Metacard.EXPIRATION,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                DATE_TYPE));
        descriptors.add(new AttributeDescriptorImpl(Metacard.EFFECTIVE,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                DATE_TYPE));
        descriptors.add(new AttributeDescriptorImpl(Metacard.CREATED,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                DATE_TYPE));
        descriptors.add(new AttributeDescriptorImpl(Metacard.ID,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(Metacard.TITLE,
                true /* indexed */,
                true /* stored */,
                true /* tokenized */,
                false /* multivalued */,
                STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(Metacard.POINT_OF_CONTACT,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(Metacard.CONTENT_TYPE,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(Metacard.CONTENT_TYPE_VERSION,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(Metacard.TARGET_NAMESPACE,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(Metacard.METADATA,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                XML_TYPE));
        descriptors.add(new AttributeDescriptorImpl(Metacard.RESOURCE_URI,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(Metacard.RESOURCE_DOWNLOAD_URL,
                false /* indexed */,
                false /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(Metacard.RESOURCE_SIZE,
                false /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(Metacard.THUMBNAIL,
                false /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BINARY_TYPE));
        descriptors.add(new AttributeDescriptorImpl(Metacard.GEOGRAPHY,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                GEO_TYPE));
        descriptors.add(new AttributeDescriptorImpl(Metacard.DESCRIPTION,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(VALIDATION_WARNINGS,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                true /* multivalued */,
                STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(VALIDATION_ERRORS,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                true /* multivalued */,
                STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(Metacard.TAGS,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                true /* multivalued */,
                STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(Metacard.CHECKSUM_ALGORITHM,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(Metacard.CHECKSUM,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(Metacard.DERIVED_RESOURCE_URI,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                true /* multivalued */,
                STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(Metacard.DERIVED_RESOURCE_DOWNLOAD_URL,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                true /* multivalued */,
                STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(Metacard.DERIVED,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                true /* multivalued */,
                STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(Metacard.RELATED,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                true /* multivalued */,
                STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(Metacard.RESOURCE_CACHE_STATUS,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BOOLEAN_TYPE));
        return descriptors;
    }

    public static AttributeType getAttributeType(String type) {
        return ATTRIBUTE_TYPE_MAP.get(type);
    }
}
