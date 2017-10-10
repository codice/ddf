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
package org.codice.ddf.libs.klv.data.text;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import org.apache.tika.parser.txt.CharsetDetector;
import org.apache.tika.parser.txt.CharsetMatch;
import org.codice.ddf.libs.klv.KlvDataElement;
import org.codice.ddf.libs.klv.data.Klv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a KLV data element that has a <strong>String</strong> value. The string may not be the
 * machine's encoding, so it should be detected.
 */
public class KlvEncodingDetectedString extends KlvDataElement<String> {
  private static final Logger LOGGER = LoggerFactory.getLogger(KlvEncodingDetectedString.class);

  /**
   * Constructs a {@code KlvDataElement} that describes how to interpret the value of a data element
   * with the given key.
   *
   * @param key the data element's key
   * @param name a name describing the data element's value
   * @throws IllegalArgumentException if any arguments are null
   */
  public KlvEncodingDetectedString(byte[] key, String name) {
    super(key, name);
  }

  @Override
  protected void decodeValue(Klv klv) {
    byte[] bytes = klv.getValue();
    CharsetDetector charsetDetector = new CharsetDetector();
    charsetDetector.setText(bytes);
    CharsetMatch charsetMatch = charsetDetector.detect();
    try {
      value = new String(bytes, charsetMatch.getName());
      return;
    } catch (UnsupportedEncodingException e) {
      LOGGER.trace(
          "Unsupported encoding of %s, falling back to default encoding", charsetMatch.getName());
    }
    value = new String(bytes, Charset.defaultCharset());
  }

  @Override
  protected KlvDataElement copy() {
    return new KlvEncodingDetectedString(keyBytes, name);
  }
}
