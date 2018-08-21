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
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang.WordUtils;

public class ExportCategory implements RtfCategory {

  private String title;
  private List<String> attributes;

  public abstract class ExportValue<T, R extends ValueType> {
    public abstract T getValue();

    public abstract R getType();
  }

  public enum ValueType {
    SIMPLE,
    MEDIA,
    EMPTY
  }

  class JustValue<T> extends ExportValue {

    private final String value;
    private final Function<String, T> fromString;
    private final ValueType valueType;

    public JustValue(String value, ValueType type, Function<String, T> fromString) {
      this.value = value;
      this.valueType = type;
      this.fromString = fromString;
    }

    @Override
    public T getValue() {
      return this.fromString.apply(this.value);
    }

    @Override
    public ValueType getType() {
      return this.valueType;
    }
  }

  public ExportCategory() {}

  public void init() {
    // Thanks blueprint!
  }

  public void destroy(int code) {
    // Thanks blueprint!
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public List<String> getAttributes() {
    return attributes;
  }

  public void setAttributes(List<String> attributes) {
    this.attributes = attributes;
  }

  public Map<String, ExportValue> toExportMap(Metacard metacard) {
    return attributes
        .stream()
        .map(
            key ->
                Collections.singletonMap(
                    attributeKeyFrom(key),
                    attributeExportValueFrom(key, metacard.getAttribute(key))))
        .flatMap(map -> map.entrySet().stream())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private String attributeKeyFrom(String key) {
    String formattedAttribute =
        Stream.of(key.split("\\."))
            .map(part -> Stream.of(part.split("-")).collect(Collectors.joining(" ")))
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
    return new JustValue<>(
        Base64.getEncoder().encodeToString(image),
        ValueType.MEDIA,
        (stringImage) -> Base64.getDecoder().decode(stringImage));
  }

  private ExportValue emptyValue() {
    return new JustValue("", ValueType.EMPTY, (empty) -> "--");
  }

  private ExportValue simpleValue(Attribute attribute) {
    return new JustValue(
        Optional.ofNullable(attribute.getValue()).map(Object::toString).orElse(null),
        ValueType.SIMPLE,
        (value) -> value);
  }
}
