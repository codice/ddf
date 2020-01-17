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
package org.codice.ddf.security.jaxrs;

import java.io.IOException;

public interface RestSecurity {
  /**
   * Deflates a value and Base64 encodes the result.
   *
   * @param value value to deflate and Base64 encode
   * @return String
   * @throws IOException if the value cannot be converted
   */
  String deflateAndBase64Encode(String value) throws IOException;

  /**
   * Decodes from base64 and then inflates the result
   *
   * @param base64EncodedValue
   * @return String
   * @throws IOException
   */
  String inflateBase64(String base64EncodedValue) throws IOException;

  /**
   * Decodes Base64 encoded values
   *
   * @param base64EncodedValue value to decode
   * @return decoded value
   */
  String base64Decode(String base64EncodedValue);
}
