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
package ddf.catalog.data.impl;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;

/**
 * Constants for basic types, both {@link MetacardType} and {@link AttributeType}
 * 
 * @author ddf.isgs@lmco.com
 * 
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

    static {

        AttributeType<Boolean> booleanType = new AttributeType<Boolean>() {
            private static final long serialVersionUID = 1L;

            @Override
            public AttributeFormat getAttributeFormat() {
                return AttributeFormat.BOOLEAN;
            }

            @Override
            public Class<Boolean> getBinding() {
                return Boolean.class;
            }
        };
        BOOLEAN_TYPE = booleanType;

        AttributeType<Double> doubleType = new AttributeType<Double>() {
            private static final long serialVersionUID = 1L;

            @Override
            public AttributeFormat getAttributeFormat() {
                return AttributeFormat.DOUBLE;
            }

            @Override
            public Class<Double> getBinding() {
                return Double.class;
            }
        };
        DOUBLE_TYPE = doubleType;

        AttributeType<Float> floatType = new AttributeType<Float>() {
            private static final long serialVersionUID = 1L;

            @Override
            public AttributeFormat getAttributeFormat() {
                return AttributeFormat.FLOAT;
            }

            @Override
            public Class<Float> getBinding() {
                return Float.class;
            }
        };
        FLOAT_TYPE = floatType;

        AttributeType<Integer> integerType = new AttributeType<Integer>() {
            private static final long serialVersionUID = 1L;

            @Override
            public AttributeFormat getAttributeFormat() {
                return AttributeFormat.INTEGER;
            }

            @Override
            public Class<Integer> getBinding() {
                return Integer.class;
            }
        };
        INTEGER_TYPE = integerType;

        AttributeType<Serializable> objectType = new AttributeType<Serializable>() {
            private static final long serialVersionUID = 1L;

            @Override
            public AttributeFormat getAttributeFormat() {
                return AttributeFormat.OBJECT;
            }

            @Override
            public Class<Serializable> getBinding() {
                return Serializable.class;
            }
        };
        OBJECT_TYPE = objectType;

        AttributeType<Short> shortType = new AttributeType<Short>() {
            private static final long serialVersionUID = 1L;

            @Override
            public AttributeFormat getAttributeFormat() {
                return AttributeFormat.SHORT;
            }

            @Override
            public Class<Short> getBinding() {
                return Short.class;
            }
        };
        SHORT_TYPE = shortType;

        AttributeType<Date> dateType = new AttributeType<Date>() {
            private static final long serialVersionUID = 1L;

            @Override
            public AttributeFormat getAttributeFormat() {
                return AttributeFormat.DATE;
            }

            @Override
            public Class<Date> getBinding() {
                return Date.class;
            }
        };
        DATE_TYPE = dateType;

        AttributeType<String> stringType = new AttributeType<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public AttributeFormat getAttributeFormat() {
                return AttributeFormat.STRING;
            }

            @Override
            public Class<String> getBinding() {
                return String.class;
            }
        };
        STRING_TYPE = stringType;

        AttributeType<String> xmlType = new AttributeType<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public AttributeFormat getAttributeFormat() {
                return AttributeFormat.XML;
            }

            @Override
            public Class<String> getBinding() {
                return String.class;
            }
        };
        XML_TYPE = xmlType;

        AttributeType<Long> longType = new AttributeType<Long>() {
            private static final long serialVersionUID = 1L;

            @Override
            public AttributeFormat getAttributeFormat() {
                return AttributeFormat.LONG;
            }

            @Override
            public Class<Long> getBinding() {
                return Long.class;
            }
        };
        LONG_TYPE = longType;

        AttributeType<byte[]> binaryType = new AttributeType<byte[]>() {
            private static final long serialVersionUID = 1L;

            @Override
            public AttributeFormat getAttributeFormat() {
                return AttributeFormat.BINARY;
            }

            @Override
            public Class<byte[]> getBinding() {
                return byte[].class;
            }
        };
        BINARY_TYPE = binaryType;

        AttributeType<String> geoType = new AttributeType<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public AttributeFormat getAttributeFormat() {
                return AttributeFormat.GEOMETRY;
            }

            @Override
            public Class<String> getBinding() {
                return String.class;
            }
        };
        GEO_TYPE = geoType;

        MetacardType basic = null;
        HashSet<AttributeDescriptor> descriptors = new HashSet<AttributeDescriptor>();
        descriptors.add(new AttributeDescriptorImpl(Metacard.MODIFIED, true /* indexed */,
                true /* stored */, false /* tokenized */, false /* multivalued */, DATE_TYPE));
        descriptors.add(new AttributeDescriptorImpl(Metacard.EXPIRATION, true /* indexed */,
                true /* stored */, false /* tokenized */, false /* multivalued */, DATE_TYPE));
        descriptors.add(new AttributeDescriptorImpl(Metacard.EFFECTIVE, true /* indexed */,
                true /* stored */, false /* tokenized */, false /* multivalued */, DATE_TYPE));
        descriptors.add(new AttributeDescriptorImpl(Metacard.CREATED, true /* indexed */,
                true /* stored */, false /* tokenized */, false /* multivalued */, DATE_TYPE));
        descriptors.add(new AttributeDescriptorImpl(Metacard.ID, true /* indexed */,
                true /* stored */, false /* tokenized */, false /* multivalued */, STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(Metacard.TITLE, true /* indexed */,
                true /* stored */, true /* tokenized */, false /* multivalued */, STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(Metacard.POINT_OF_CONTACT, true /* indexed */,
                true /* stored */, false /* tokenized */, false /* multivalued */, STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(Metacard.CONTENT_TYPE, true /* indexed */,
                true /* stored */, false /* tokenized */, false /* multivalued */, STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(Metacard.CONTENT_TYPE_VERSION,
                true /* indexed */, true /* stored */, false /* tokenized */,
                false /* multivalued */, STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(Metacard.TARGET_NAMESPACE, true /* indexed */,
                true /* stored */, false /* tokenized */, false /* multivalued */, STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(Metacard.METADATA, true /* indexed */,
                true /* stored */, false /* tokenized */, false /* multivalued */, XML_TYPE));
        descriptors.add(new AttributeDescriptorImpl(Metacard.RESOURCE_URI, true /* indexed */,
                true /* stored */, false /* tokenized */, false /* multivalued */, STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(Metacard.RESOURCE_SIZE, false /* indexed */,
                true /* stored */, false /* tokenized */, false /* multivalued */, STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(Metacard.THUMBNAIL, false /* indexed */,
                true /* stored */, false /* tokenized */, false /* multivalued */, BINARY_TYPE));
        descriptors.add(new AttributeDescriptorImpl(Metacard.GEOGRAPHY, true /* indexed */,
                true /* stored */, false /* tokenized */, false /* multivalued */, GEO_TYPE));

        basic = new MetacardTypeImpl(MetacardType.DEFAULT_METACARD_TYPE_NAME, descriptors);

        BASIC_METACARD = basic;
    }

    /**
     * Constructor - does nothing
     */
    public BasicTypes() {
    }
}
