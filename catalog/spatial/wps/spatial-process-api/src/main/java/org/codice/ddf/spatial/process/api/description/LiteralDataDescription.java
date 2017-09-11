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
package org.codice.ddf.spatial.process.api.description;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;

/** This class is Experimental and subject to change */
public class LiteralDataDescription extends AbstractDataDescription {

  private Range range;

  private Serializable defaultValue;

  private Set<String> enumeratedValues = Collections.emptySet();

  private DataType type;

  private String unitOfMeasure;

  private ValueReference reference;

  public LiteralDataDescription(String id, String name, String description, DataType type) {
    super(
        id,
        name,
        description,
        Collections.singletonList(
            new DataFormatDefinition(TEXT_PLAIN, StandardCharsets.UTF_8.displayName())));
    this.type = type;
  }

  public LiteralDataDescription(
      String id, String name, String description, ValueReference reference) {
    super(
        id,
        name,
        description,
        Collections.singletonList(
            new DataFormatDefinition(TEXT_PLAIN, StandardCharsets.UTF_8.displayName())));
    this.reference = reference;
  }

  public LiteralDataDescription(
      String id, String name, String description, DataType type, Serializable defaultValue) {
    this(id, name, description, type);
    this.defaultValue = defaultValue;
  }

  public LiteralDataDescription(
      String id,
      String name,
      String description,
      DataType type,
      Serializable defaultValue,
      Set<String> enumeratedValues) {
    this(id, name, description, type, defaultValue);
    this.enumeratedValues = enumeratedValues;
  }

  public LiteralDataDescription(
      String id,
      String name,
      String description,
      DataType type,
      Serializable defaultValue,
      Range range) {
    this(id, name, description, type, defaultValue);
    this.range = range;
  }

  @Nullable
  public Range getRange() {
    return range;
  }

  public void setRange(Range range) {
    this.range = range;
  }

  public LiteralDataDescription range(Range range) {
    this.range = range;
    return this;
  }

  public Set<String> getEnumeratedValues() {
    return Collections.unmodifiableSet(enumeratedValues);
  }

  public void setEnumeratedValues(Set<String> enumeratedValues) {
    this.enumeratedValues = new HashSet<>(enumeratedValues);
  }

  public LiteralDataDescription enumeratedValues(String... enumeratedValuesArry) {
    this.enumeratedValues = new HashSet<>(Arrays.asList(enumeratedValuesArry));
    return this;
  }

  @Nullable
  public ValueReference getReference() {
    return reference;
  }

  public void setReference(ValueReference reference) {
    this.reference = reference;
  }

  public LiteralDataDescription reference(ValueReference reference) {
    this.reference = reference;
    return this;
  }

  @Nullable
  public Serializable getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(Serializable defaultValue) {
    this.defaultValue = defaultValue;
  }

  public LiteralDataDescription defaultValue(Serializable defaultValue) {
    this.defaultValue = defaultValue;
    return this;
  }

  @Nullable
  public DataType getType() {
    return type;
  }

  public void setType(DataType type) {
    this.type = type;
  }

  public LiteralDataDescription type(DataType type) {
    this.type = type;
    return this;
  }

  @Nullable
  public String getUnitOfMeasure() {
    return unitOfMeasure;
  }

  public void setUnitOfMeasure(String unitOfMeasure) {
    this.unitOfMeasure = unitOfMeasure;
  }

  public LiteralDataDescription unitOfMeasure(String unitOfMeasure) {
    this.unitOfMeasure = unitOfMeasure;
    return this;
  }
}
