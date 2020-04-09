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
package org.codice.ddf.security.jaxrs.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import org.apache.commons.io.IOUtils;

/** Provides methods that help with securing RESTful (jaxrs) communications. */
public final class SamlSecurity implements org.codice.ddf.security.jaxrs.SamlSecurity {

  public static final boolean GZIP_COMPATIBLE = true;

  /**
   * Deflates a value and Base64 encodes the result.
   *
   * @param value value to deflate and Base64 encode
   * @return String
   * @throws IOException if the value cannot be converted
   */
  public String deflateAndBase64Encode(String value) throws IOException {
    ByteArrayOutputStream valueBytes = new ByteArrayOutputStream();
    try (OutputStream tokenStream =
        new DeflaterOutputStream(valueBytes, new Deflater(Deflater.DEFLATED, GZIP_COMPATIBLE))) {
      tokenStream.write(value.getBytes(StandardCharsets.UTF_8));
      tokenStream.close();

      return Base64.getEncoder().encodeToString(valueBytes.toByteArray());
    }
  }

  public String inflateBase64(String base64EncodedValue) throws IOException {
    if (base64EncodedValue == null) {
      return "";
    }

    byte[] deflatedValue =
        Base64.getMimeDecoder().decode(base64EncodedValue.getBytes(StandardCharsets.UTF_8));
    InputStream is =
        new InflaterInputStream(
            new ByteArrayInputStream(deflatedValue), new Inflater(GZIP_COMPATIBLE));
    return IOUtils.toString(is, StandardCharsets.UTF_8.name());
  }

  /**
   * Decodes Base64 encoded values
   *
   * @param base64EncodedValue value to decode
   * @return decoded value
   */
  public String base64Decode(String base64EncodedValue) {
    if (base64EncodedValue == null) {
      return "";
    }
    return new String(
        Base64.getMimeDecoder().decode(base64EncodedValue.getBytes(StandardCharsets.UTF_8)),
        StandardCharsets.UTF_8);
  }
}
