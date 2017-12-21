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

import java.io.InputStream;
import java.net.URI;
import javax.annotation.Nullable;
import org.codice.ddf.spatial.process.api.description.TransmissionMode;

/** This class is Experimental and subject to change */
public class OutputDefinition {

  private String id;

  private DataFormat format;

  private TransmissionMode transmissionMode = TransmissionMode.VALUE;

  public OutputDefinition(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @Nullable
  public DataFormat getFormat() {
    return format;
  }

  public void setFormat(DataFormat format) {
    this.format = format;
  }

  public TransmissionMode getTransmissionMode() {
    return transmissionMode;
  }

  public void setTransmissionMode(TransmissionMode transmissionMode) {
    this.transmissionMode = transmissionMode;
  }

  public Data createOutputData() {
    return transmissionMode.createOutputData(id, format, (String) null);
  }

  public Data createOutputData(String value) {
    return transmissionMode.createOutputData(id, format, value);
  }

  public Data createOutputData(URI uri) {
    return transmissionMode.createOutputData(id, format, uri);
  }

  /**
   * @param inputStream
   * @return
   * @throws ProcessException
   */
  public Data createOutputData(InputStream inputStream) {
    return transmissionMode.createOutputData(id, format, inputStream);
  }
}
