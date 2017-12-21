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
import java.util.Collections;
import java.util.List;
import javax.ws.rs.core.MediaType;

/** This class is Experimental and subject to change */
public class AbstractDataDescription implements DataDescription {
  private String id;

  private String name;

  private String description;

  private BigInteger minOccurs = BigInteger.ONE;

  private BigInteger maxOccurs = BigInteger.ONE;

  private Metadata metadata = new Metadata();

  private List<DataFormatDefinition> dataFormats =
      Collections.singletonList(new DataFormatDefinition(MediaType.APPLICATION_OCTET_STREAM));

  public AbstractDataDescription(String id, String name, String description) {
    this.id = id;
    this.name = name;
    this.description = description;
  }

  public AbstractDataDescription(
      String id, String name, String description, List<DataFormatDefinition> dataFormats) {
    this(id, name, description);
    this.dataFormats = new ArrayList<>(dataFormats);
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
  public Metadata getMetadata() {
    return metadata;
  }

  public void setMetadata(Metadata metadata) {
    this.metadata = metadata;
  }

  public AbstractDataDescription metadata(Metadata metadata) {
    this.metadata = metadata;
    return this;
  }

  @Override
  public List<DataFormatDefinition> getDataFormats() {
    return Collections.unmodifiableList(dataFormats);
  }

  @Override
  public BigInteger getMinOccurs() {
    return minOccurs;
  }

  public void setMinOccurs(BigInteger minOccurs) {
    this.minOccurs = minOccurs;
    if (minOccurs.signum() == -1) {
      throw new IllegalArgumentException("minOccurs must be non negative.");
    }
    this.maxOccurs = minOccurs.max(maxOccurs);
  }

  public AbstractDataDescription minOccurs(BigInteger minOccurs) {
    setMinOccurs(minOccurs);
    return this;
  }

  @Override
  public BigInteger getMaxOccurs() {
    return maxOccurs;
  }

  public void setMaxOccurs(BigInteger maxOccurs) {
    this.maxOccurs = maxOccurs;
    if (maxOccurs.signum() < 1) {
      throw new IllegalArgumentException("maxOccurs must be greater than 1.");
    }
    if (maxOccurs.compareTo(minOccurs) < 0) {
      throw new IllegalArgumentException("maxOccurs must be greater than minOccurs.");
    }
  }

  public AbstractDataDescription maxOccurs(BigInteger maxOccurs) {
    setMaxOccurs(maxOccurs);
    return this;
  }
}
