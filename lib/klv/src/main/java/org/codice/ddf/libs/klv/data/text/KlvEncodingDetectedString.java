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

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
  private final Charset utf8 = Charset.forName("UTF-8");
  private final Charset utf16 = Charset.forName("UTF-16");
  private final Charset utf16be = Charset.forName("UTF-16BE");
  private final Charset utf16le = Charset.forName("UTF-16LE");

  private final List<String> possibleCharsets =
      Arrays.asList(utf8.name(), utf16.name(), utf16be.name(), utf16le.name());

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
    CharsetMatch[] charsetMatches = charsetDetector.detectAll();

    Optional<CharsetMatch> charsetMatch =
        Arrays.stream(charsetMatches)
            .filter(match -> possibleCharsets.contains(match.getName()))
            .findFirst();

    Charset charset = utf8;
    if (charsetMatch.isPresent()) {
      try {
        charset = Charset.forName(charsetMatch.get().getName());
      } catch (IllegalArgumentException e) {
        LOGGER.trace("Unsupported encoding, falling back to default encoding");
      }
    }
    value = new String(bytes, charset);
  }

  @Override
  protected KlvDataElement copy() {
    return new KlvEncodingDetectedString(keyBytes, name);
  }
}
