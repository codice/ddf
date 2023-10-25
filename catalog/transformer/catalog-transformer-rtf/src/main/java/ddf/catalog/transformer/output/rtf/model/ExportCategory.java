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
package ddf.catalog.transformer.output.rtf.model;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Core;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;

public class ExportCategory implements RtfCategory {

  public static final String EXTENDED_ATTRIBUTE_PREFIX = "ext.";

  public static final String EMPTY_VALUE = "--";

  private String title;
  private List<String> attributes;
  private Map<String, String> aliases = Collections.EMPTY_MAP;

  public interface ExportValue<T, R extends ValueType> {
    T getValue();

    R getType();
  }

  public enum ValueType {
    SIMPLE,
    MEDIA,
    EMPTY
  }

  class JustValue<T> implements ExportValue {

    private final T value;
    private final ValueType valueType;

    public JustValue(T value, ValueType type) {
      this.value = value;
      this.valueType = type;
    }

    @Override
    public T getValue() {
      return this.value;
    }

    @Override
    public ValueType getType() {
      return this.valueType;
    }
  }

  @Override
  public String getTitle() {
    return title;
  }

  @Override
  public void setTitle(String title) {
    this.title = title;
  }

  @Override
  public List<String> getAttributes() {
    return attributes;
  }

  @Override
  public void setAttributes(List<String> attributes) {
    this.attributes = attributes;
  }

  @Override
  public Map<String, ExportValue> toExportMap(Metacard metacard) {
    return attributes.stream()
        .filter(a -> isNonEmptyValue(metacard, a))
        .map(
            key ->
                new AbstractMap.SimpleEntry<>(
                    attributeKeyFrom(key),
                    attributeExportValueFrom(key, metacard.getAttribute(key))))
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
  }

  public void setAliases(Map<String, String> aliases) {
    this.aliases = aliases;
  }

  private String attributeKeyFrom(String key) {
    if (aliases.containsKey(key)) {
      return aliases.get(key);
    } else if (key.startsWith(EXTENDED_ATTRIBUTE_PREFIX)) {
      key = key.replaceFirst(EXTENDED_ATTRIBUTE_PREFIX, "");
    }

    String formattedAttribute =
        Stream.of(key.split("\\."))
            .map(part -> part.replaceAll("-", " "))
            .collect(Collectors.joining(" "));

    return WordUtils.capitalize(formattedAttribute);
  }

  private ExportValue attributeExportValueFrom(String attributeKey, Attribute attribute) {
    if (attributeKey.equals(Core.THUMBNAIL)) {
      byte[] image = (byte[]) attribute.getValue();

      return image.length > 0 ? mediaValue(image) : emptyValue();
    }

    return simpleValue(attribute);
  }

  private ExportValue mediaValue(byte[] image) {
    return new JustValue<>(image, ValueType.MEDIA);
  }

  private ExportValue emptyValue() {
    return new JustValue(EMPTY_VALUE, ValueType.EMPTY);
  }

  private ExportValue simpleValue(Attribute attribute) {
    return new JustValue(
        Optional.ofNullable(
                attribute.getValues().stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(", ")))
            .orElse(null),
        ValueType.SIMPLE);
  }

  private static boolean isNonEmptyValue(Metacard metacard, String attrName) {
    AttributeDescriptor descriptor = metacard.getMetacardType().getAttributeDescriptor(attrName);
    final Attribute attribute = metacard.getAttribute(attrName);
    if (descriptor == null) {
      return attribute != null
          && attribute.getValue() != null
          && StringUtils.isNotEmpty(attribute.getValue().toString());
    }

    switch (descriptor.getType().getAttributeFormat()) {
      case STRING:
      case XML:
      case GEOMETRY:
        return attribute != null && StringUtils.isNotEmpty((String) attribute.getValue());
      case INTEGER:
      case LONG:
      case DOUBLE:
      case FLOAT:
      case SHORT:
      case DATE:
      case BOOLEAN:
      case BINARY:
        return attribute != null && attribute.getValue() != null;
      default:
        return false;
    }
  }
}
