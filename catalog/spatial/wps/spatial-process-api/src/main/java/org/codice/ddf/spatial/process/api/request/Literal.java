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
package org.codice.ddf.spatial.process.api.request;

import java.io.Serializable;
import javax.annotation.Nullable;

/** This class is Experimental and subject to change */
public class Literal implements Data {

  private String id;

  private Serializable value;

  private DataFormat format;

  public Literal(String id) {
    this.id = id;
  }

  public Literal value(Serializable value) {
    this.value = value;
    return this;
  }

  @Nullable
  public Serializable getValue() {
    return value;
  }

  public void setValue(Serializable value) {
    this.value = value;
  }

  public Literal format(DataFormat format) {
    this.format = format;
    return this;
  }

  @Nullable
  @Override
  public DataFormat getFormat() {
    return format;
  }

  public void setFormat(DataFormat format) {
    this.format = format;
  }

  @Override
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Literal id(String id) {
    this.id = id;
    return this;
  }
}
