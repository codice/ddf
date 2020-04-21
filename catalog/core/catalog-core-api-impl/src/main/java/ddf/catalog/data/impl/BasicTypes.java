/*
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
package ddf.catalog.data.impl;

import ddf.catalog.data.AttributeType;
import ddf.catalog.data.AttributeType.AttributeFormat;
import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/** Constants for basic types, both {@link MetacardType} and {@link AttributeType} */
public class BasicTypes {

  /** A Constant for an {@link AttributeType} with {@link AttributeFormat#DATE} . */
  public static final AttributeType<Date> DATE_TYPE;

  /** A Constant for an {@link AttributeType} with {@link AttributeFormat#STRING}. */
  public static final AttributeType<String> STRING_TYPE;

  /** A Constant for an {@link AttributeType} with {@link AttributeFormat#XML}. */
  public static final AttributeType<String> XML_TYPE;

  /** A Constant for an {@link AttributeType} with {@link AttributeFormat#LONG} . */
  public static final AttributeType<Long> LONG_TYPE;

  /** A Constant for an {@link AttributeType} with {@link AttributeFormat#BINARY}. */
  public static final AttributeType<byte[]> BINARY_TYPE;

  /** A Constant for an {@link AttributeType} with {@link AttributeFormat#GEOMETRY}. */
  public static final AttributeType<String> GEO_TYPE;

  /** A Constant for an {@link AttributeType} with {@link AttributeFormat#BOOLEAN}. */
  public static final AttributeType<Boolean> BOOLEAN_TYPE;

  /** A Constant for an {@link AttributeType} with {@link AttributeFormat#DOUBLE}. */
  public static final AttributeType<Double> DOUBLE_TYPE;

  /** A Constant for an {@link AttributeType} with {@link AttributeFormat#FLOAT}. */
  public static final AttributeType<Float> FLOAT_TYPE;

  /** A Constant for an {@link AttributeType} with {@link AttributeFormat#INTEGER}. */
  public static final AttributeType<Integer> INTEGER_TYPE;

  /** A Constant for an {@link AttributeType} with {@link AttributeFormat#OBJECT}. */
  public static final AttributeType<Serializable> OBJECT_TYPE;

  /** A Constant for an {@link AttributeType} with {@link AttributeFormat#SHORT}. */
  public static final AttributeType<Short> SHORT_TYPE;

  /** A Constant for an {@link AttributeType} with {@link AttributeFormat#COUNTRY}. */
  public static final AttributeType<String> COUNTRY_TYPE;

  private static final Map<String, AttributeType> ATTRIBUTE_TYPE_MAP;

  static {
    Map<String, AttributeType> attributeTypeMap = new HashMap<>();

    DATE_TYPE = addAttributeType(AttributeFormat.DATE, Date.class, attributeTypeMap);
    STRING_TYPE = addAttributeType(AttributeFormat.STRING, String.class, attributeTypeMap);
    XML_TYPE = addAttributeType(AttributeFormat.XML, String.class, attributeTypeMap);
    LONG_TYPE = addAttributeType(AttributeFormat.LONG, Long.class, attributeTypeMap);
    BINARY_TYPE = addAttributeType(AttributeFormat.BINARY, byte[].class, attributeTypeMap);
    GEO_TYPE = addAttributeType(AttributeFormat.GEOMETRY, String.class, attributeTypeMap);
    BOOLEAN_TYPE = addAttributeType(AttributeFormat.BOOLEAN, Boolean.class, attributeTypeMap);
    DOUBLE_TYPE = addAttributeType(AttributeFormat.DOUBLE, Double.class, attributeTypeMap);
    FLOAT_TYPE = addAttributeType(AttributeFormat.FLOAT, Float.class, attributeTypeMap);
    INTEGER_TYPE = addAttributeType(AttributeFormat.INTEGER, Integer.class, attributeTypeMap);
    OBJECT_TYPE = addAttributeType(AttributeFormat.OBJECT, Serializable.class, attributeTypeMap);
    SHORT_TYPE = addAttributeType(AttributeFormat.SHORT, Short.class, attributeTypeMap);
    COUNTRY_TYPE = addAttributeType(AttributeFormat.COUNTRY, String.class, attributeTypeMap);

    ATTRIBUTE_TYPE_MAP = Collections.unmodifiableMap(attributeTypeMap);
  }

  private BasicTypes() {}

  private static <T extends Serializable> AttributeType<T> addAttributeType(
      final AttributeFormat format, final Class<T> bindingClass, Map<String, AttributeType> map) {
    final AttributeType<T> attributeType =
        new AttributeType<T>() {
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

    map.put(format.name(), attributeType);

    return attributeType;
  }

  public static AttributeType getAttributeType(String type) {
    return ATTRIBUTE_TYPE_MAP.get(type);
  }
}
