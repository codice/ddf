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

import javax.annotation.Nullable;

/** This class is Experimental and subject to change */
public class DataFormat {

  private String mimeType;

  private String encoding;

  private String schema;

  public DataFormat(String mimeType, String encoding, String schema) {
    this.mimeType = mimeType;
    this.encoding = encoding;
    this.schema = schema;
  }

  public DataFormat(String mimeType, String encoding) {
    this.mimeType = mimeType;
    this.encoding = encoding;
  }

  public DataFormat() {}

  @Nullable
  public String getMimeType() {
    return mimeType;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  public DataFormat mimeType(String mimeType) {
    this.mimeType = mimeType;
    return this;
  }

  @Nullable
  public String getEncoding() {
    return encoding;
  }

  public void setEncoding(String encoding) {
    this.encoding = encoding;
  }

  public DataFormat encoding(String encoding) {
    this.encoding = encoding;
    return this;
  }

  public DataFormat schema(String schema) {
    this.schema = schema;
    return this;
  }

  @Nullable
  public String getSchema() {
    return schema;
  }

  public void setSchema(String schema) {
    this.schema = schema;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DataFormat)) {
      return false;
    }

    DataFormat that = (DataFormat) o;

    if (mimeType != null ? !mimeType.equals(that.mimeType) : that.mimeType != null) {
      return false;
    }
    if (encoding != null ? !encoding.equals(that.encoding) : that.encoding != null) {
      return false;
    }
    return schema != null ? schema.equals(that.schema) : that.schema == null;
  }

  @Override
  public int hashCode() {
    int result = mimeType != null ? mimeType.hashCode() : 0;
    result = 31 * result + (encoding != null ? encoding.hashCode() : 0);
    result = 31 * result + (schema != null ? schema.hashCode() : 0);
    return result;
  }
}
