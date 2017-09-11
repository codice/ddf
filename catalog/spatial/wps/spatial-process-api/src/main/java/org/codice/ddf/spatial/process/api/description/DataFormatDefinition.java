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
import javax.annotation.Nullable;
import org.codice.ddf.spatial.process.api.request.DataFormat;

/** This class is Experimental and subject to change */
public class DataFormatDefinition extends DataFormat {
  private BigInteger maximumMegabytes;

  public DataFormatDefinition(String mimeType, String encoding, String schema) {
    super(mimeType, encoding, schema);
  }

  public DataFormatDefinition(String mimeType) {
    super(mimeType, "raw");
  }

  public DataFormatDefinition(String mimeType, String encoding) {
    super(mimeType, encoding);
  }

  public DataFormatDefinition() {}

  @Nullable
  public BigInteger getMaximumMegabytes() {
    return maximumMegabytes;
  }

  public void setMaximumMegabytes(BigInteger maximumMegabytes) {
    this.maximumMegabytes = maximumMegabytes;
  }

  public DataFormatDefinition maximumMegabytes(BigInteger maximumMegabytes) {
    this.maximumMegabytes = maximumMegabytes;
    return this;
  }
}
