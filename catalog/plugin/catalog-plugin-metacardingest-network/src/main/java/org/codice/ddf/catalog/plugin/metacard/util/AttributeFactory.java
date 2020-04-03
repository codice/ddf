/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.catalog.plugin.metacard.util;

import static org.apache.commons.lang.Validate.notEmpty;
import static org.apache.commons.lang.Validate.notNull;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.impl.AttributeImpl;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.xml.bind.DatatypeConverter;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Support class for working with and constructing {@link Attribute}s. The default method for
 * handling multi-valued attributes as a single string is to delimit each value with a comma.
 */
public class AttributeFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(AttributeFactory.class);

  private static final String MULTI_VALUED_SPLIT_REGEX = ",";

  /**
   * Attempts to create an {@link Attribute} according to the provided {@link AttributeDescriptor}
   * whose value is represented by the given string {@param value}. Throws an exception if {@param
   * value} could not be parsed.
   *
   * @param attributeDescriptor The descriptor used to create the attribute.
   * @param value The non-empty string to use to create an attribute value. Multi-valued entities
   *     should be separated by commas.
   * @return An {@link Attribute} conforming to the given {@link AttributeDescriptor} with a value
   *     of {@param value} that was parsed and processed to {@link Serializable}.
   * @throws IllegalArgumentException If the given value could not be parsed or if the inputs were
   *     null or empty.
   */
  public Attribute createAttribute(AttributeDescriptor attributeDescriptor, String value) {
    notNull(attributeDescriptor);
    notEmpty(value);

    if (attributeDescriptor.isMultiValued()) {
      LOGGER.trace(
          "Found multi-valued attribute descriptor {} for value {}",
          attributeDescriptor.getName(),
          value);
      List<String> values = convertMultiValuedStringToList(value);
      notEmpty(values);
      List<Serializable> serializables =
          values.stream()
              .map(string -> parseAttributeValue(attributeDescriptor, string))
              .peek(Validate::notNull)
              .collect(Collectors.toList());
      return doCreate(attributeDescriptor, serializables);
    }

    Serializable attributeValue = parseAttributeValue(attributeDescriptor, value);
    notNull(attributeValue);
    LOGGER.trace(
        "Creating single-valued attribute using descriptor {} for value {}",
        attributeDescriptor.getName(),
        attributeValue);
    return doCreate(attributeDescriptor, Collections.singletonList(attributeValue));
  }

  /**
   * Attempts to parse a value for an {@link Attribute} according to the provided {@link
   * AttributeDescriptor} whose value is represented by the given string {@param value}. Returns
   * {@code null} if {@param value} could not be parsed.
   *
   * <p>The input {@param value} must be effectively {@link Serializable}.
   *
   * @param attributeDescriptor The descriptor to use to parse the value.
   * @param value The string containing the value to be parsed.
   * @return The deserialized object of {@param value} according to {@param attributeDescriptor}.
   */
  @Nullable
  public Serializable parseAttributeValue(AttributeDescriptor attributeDescriptor, String value) {
    try {
      notNull(attributeDescriptor);
      notEmpty(value);

      Serializable deserializedValue;
      AttributeType attributeType = attributeDescriptor.getType();
      AttributeType.AttributeFormat attributeFormat = attributeType.getAttributeFormat();

      switch (attributeFormat) {
        case INTEGER:
          deserializedValue = Integer.parseInt(value);
          break;

        case FLOAT:
          deserializedValue = Float.parseFloat(value);
          break;

        case DOUBLE:
          deserializedValue = Double.parseDouble(value);
          break;

        case SHORT:
          deserializedValue = Short.parseShort(value);
          break;

        case LONG:
          deserializedValue = Long.parseLong(value);
          break;

        case DATE:
          Calendar calendar = DatatypeConverter.parseDateTime(value);
          deserializedValue = calendar.getTime();
          break;

        case BOOLEAN:
          deserializedValue = Boolean.parseBoolean(value);
          break;

        case BINARY:
          deserializedValue = value.getBytes(Charset.forName("UTF-8"));
          break;

        case OBJECT:
        case STRING:
        case GEOMETRY:
        case XML:
          deserializedValue = value;
          break;

        default:
          return null;
      }

      return deserializedValue;

    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * Any time a single string is presented for any {@link AttributeDescriptor} where that
   * descriptor's {@link AttributeDescriptor#isMultiValued()} is true, this method defines the
   * parsing strategy.
   *
   * <p>The default is to treat the string as a comma-separated list of values.
   *
   * <p><i>This method could get promoted to <b>protected</b> in the future.</i>
   *
   * @param valueWithMultipleValues A comma-separated list of values to insert on attributes.
   * @return The list of values that were in the string as {@link Serializable}s.
   */
  private List<String> convertMultiValuedStringToList(String valueWithMultipleValues) {
    String[] entities = valueWithMultipleValues.split(MULTI_VALUED_SPLIT_REGEX, 0);
    return Arrays.stream(entities)
        .map(String::trim)
        .filter(str -> !str.isEmpty())
        .collect(Collectors.toList());
  }

  /**
   * Factory-method for performing the actual instantiation of the desired {@link Attribute}
   * instance. Separated from the rest of the code since it causes coupling to a DDF core impl.
   *
   * <p><i>This method could get promoted to <b>protected</b> in the future.</i>
   *
   * @param descriptor The descriptor used to create and validate the resulting {@link Attribute}.
   * @param values The values for the resulting {@link Attribute}.
   * @return A new instance of {@link Attribute} containing {@param values} and conforming to
   *     {@param descriptor}.
   */
  private Attribute doCreate(AttributeDescriptor descriptor, List<Serializable> values) {
    return new AttributeImpl(descriptor.getName(), values);
  }
}
