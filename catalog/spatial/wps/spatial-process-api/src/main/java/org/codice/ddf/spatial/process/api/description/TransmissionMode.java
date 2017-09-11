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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Optional;
import javax.annotation.Nullable;
import org.codice.ddf.spatial.process.api.ProcessException;
import org.codice.ddf.spatial.process.api.request.Data;
import org.codice.ddf.spatial.process.api.request.DataFormat;
import org.codice.ddf.spatial.process.api.request.Literal;
import org.codice.ddf.spatial.process.api.request.Reference;

/** This class is Experimental and subject to change */
public enum TransmissionMode {
  VALUE,
  REFERENCE;

  static final int FIVE_MB = 1024 * 1024 * 5;

  public Data createOutputData(String id, @Nullable DataFormat format, String value) {
    if (this == REFERENCE) {
      final Reference reference = new Reference(id).format(format);
      if (value != null) {
        reference.setUri(URI.create(value));
      }
      return reference;
    } else {
      return new Literal(id).format(format).value(value);
    }
  }

  public Data createOutputData(String id, @Nullable DataFormat format, URI uri) {
    if (!REFERENCE.equals(this)) {
      throw new ProcessException(
          Collections.singleton(id), "TransmissionMode is Document but its being returned as URI");
    }
    return new Reference(id).format(format).reference(uri);
  }

  /**
   * @param inputStream
   * @return
   * @throws ProcessException
   */
  public Data createOutputData(String id, @Nullable DataFormat format, InputStream inputStream) {
    if (REFERENCE.equals(this)) {
      throw new ProcessException(
          Collections.singleton(id),
          "TransmissionMode is Reference but its being returned as raw data");
    }
    return new Literal(id).format(format).value(inputStreamToString(format, inputStream));
  }

  /**
   * inputStreamToString assumes encoding isn't base64 or raw
   *
   * @param inputStream
   * @return
   * @throws ProcessException
   */
  private String inputStreamToString(@Nullable DataFormat format, InputStream inputStream) {
    String encoding =
        Optional.ofNullable(format)
            .map(DataFormat::getEncoding)
            .orElse(Charset.defaultCharset().toString());
    return inputStreamToString(inputStream, encoding);
  }

  private String inputStreamToString(InputStream inputStream, String encoding)
      throws ProcessException {
    try (BufferedInputStream bis = new BufferedInputStream(inputStream);
        ByteArrayOutputStream buf = new ByteArrayOutputStream()) {
      int result = bis.read();
      while (result != -1) {
        buf.write(result);
        result = bis.read();
      }
      return buf.toString(encoding);
    } catch (IOException e) {
      throw new ProcessException(e);
    }
  }
}
