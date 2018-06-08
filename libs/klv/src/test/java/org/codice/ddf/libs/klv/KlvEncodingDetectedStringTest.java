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
package org.codice.ddf.libs.klv;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.io.UnsupportedEncodingException;
import org.codice.ddf.libs.klv.data.Klv;
import org.codice.ddf.libs.klv.data.text.KlvEncodingDetectedString;
import org.junit.Test;

public class KlvEncodingDetectedStringTest {
  private String testString = "USA";

  @Test
  public void testUtf8CountryCodeRemainsUTF8()
      throws UnsupportedEncodingException, KlvDecodingException {
    byte[] bytes = testString.getBytes("UTF-8");

    KlvEncodingDetectedString klvEncodingDetectedstring = createTestString("test", bytes);
    assertThat(klvEncodingDetectedstring.getValue(), is(testString));
  }

  @Test
  public void testUtf16beCountryCodeIsHandled()
      throws UnsupportedEncodingException, KlvDecodingException {
    byte[] bytes = testString.getBytes("UTF-16BE");

    KlvEncodingDetectedString klvEncodingDetectedstring = createTestString("test", bytes);
    assertThat(klvEncodingDetectedstring.getValue(), is(testString));
  }

  @Test
  public void testUtf16leCountryCodeIsHandled()
      throws UnsupportedEncodingException, KlvDecodingException {
    byte[] bytes = testString.getBytes("UTF-16LE");

    KlvEncodingDetectedString klvEncodingDetectedstring = createTestString("test", bytes);
    assertThat(klvEncodingDetectedstring.getValue(), is(testString));
  }

  private KlvEncodingDetectedString createTestString(String name, byte[] bytes)
      throws KlvDecodingException {
    final byte[] byteArray = new byte[bytes.length + 2];
    byteArray[0] = -8;
    byteArray[1] = (byte) bytes.length;
    System.arraycopy(bytes, 0, byteArray, 2, bytes.length);

    final KlvEncodingDetectedString string = new KlvEncodingDetectedString(new byte[] {-8}, name);
    final KlvContext decodedKlvContext =
        decodeKLV(Klv.KeyLength.OneByte, Klv.LengthEncoding.OneByte, string, byteArray);

    return (KlvEncodingDetectedString) decodedKlvContext.getDataElementByName(name);
  }

  private KlvContext decodeKLV(
      final Klv.KeyLength keyLength,
      final Klv.LengthEncoding lengthEncoding,
      final KlvDataElement dataElement,
      final byte[] encodedBytes)
      throws KlvDecodingException {
    final KlvContext klvContext = new KlvContext(keyLength, lengthEncoding);
    klvContext.addDataElement(dataElement);
    return new KlvDecoder(klvContext).decode(encodedBytes);
  }
}
