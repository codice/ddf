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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** This class is Experimental and subject to change */
public class DataDescriptionGroup implements DataDescription {
  private String id;

  private String name;

  private String description;

  private BigInteger minOccurs = BigInteger.ONE;

  private BigInteger maxOccurs = BigInteger.ONE;

  private List<DataDescription> dataDescriptions;

  private Metadata metadata = new Metadata();

  private List<DataFormatDefinition> dataFormats = Collections.emptyList();

  public DataDescriptionGroup(
      String id, String name, String description, List<DataDescription> dataDescriptions) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.dataDescriptions = new ArrayList<>(dataDescriptions);
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public BigInteger getMinOccurs() {
    return minOccurs;
  }

  public void setMinOccurs(BigInteger minOccurs) {
    this.minOccurs = minOccurs;
  }

  public DataDescription minOccurs(BigInteger minOccurs) {
    this.minOccurs = minOccurs;
    return this;
  }

  @Override
  public BigInteger getMaxOccurs() {
    return maxOccurs;
  }

  public void setMaxOccurs(BigInteger maxOccurs) {
    this.maxOccurs = maxOccurs;
  }

  public DataDescription maxOccurs(BigInteger maxOccurs) {
    this.maxOccurs = maxOccurs;
    return this;
  }

  public List<DataDescription> getDataDescriptions() {
    return Collections.unmodifiableList(dataDescriptions);
  }

  @Override
  public Metadata getMetadata() {
    return metadata;
  }

  public void setMetadata(Metadata metadata) {
    this.metadata = metadata;
  }

  public DataDescription metadata(Metadata metadata) {
    this.metadata = metadata;
    return this;
  }

  @Override
  public List<DataFormatDefinition> getDataFormats() {
    return Collections.unmodifiableList(dataFormats);
  }

  public void setDataFormats(List<DataFormatDefinition> dataFormats) {
    this.dataFormats = new ArrayList<>(dataFormats);
  }

  public DataDescription dataFormats(DataFormatDefinition... dataFormats) {
    this.dataFormats = Arrays.asList(dataFormats);
    return this;
  }
}
