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
import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Core;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang.WordUtils;

public class ExportCategory implements RtfCategory {

  public static final String EXTENDED_ATTRIBUTE_PREFIX = "ext.";

  public static final String EMPTY_VALUE = "--";

  private String title;
  private List<String> attributes;

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
        .map(
            key ->
                new AbstractMap.SimpleEntry<>(
                    attributeKeyFrom(key),
                    attributeExportValueFrom(key, metacard.getAttribute(key))))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private String attributeKeyFrom(String key) {
    if (key.startsWith(EXTENDED_ATTRIBUTE_PREFIX)) {
      key = key.replaceFirst(EXTENDED_ATTRIBUTE_PREFIX, "");
    }

    String formattedAttribute =
        Stream.of(key.split("\\."))
            .map(part -> part.replaceAll("-", " "))
            .collect(Collectors.joining(" "));

    return WordUtils.capitalize(formattedAttribute);
  }

  private ExportValue attributeExportValueFrom(String attributeKey, Attribute attribute) {
    if (attributeKey == null || attribute == null) {
      return emptyValue();
    }

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
        Optional.ofNullable(attribute.getValue()).map(Object::toString).orElse(null),
        ValueType.SIMPLE);
  }
}
