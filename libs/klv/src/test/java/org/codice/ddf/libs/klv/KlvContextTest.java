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

import static org.codice.ddf.libs.klv.data.Klv.KeyLength;
import static org.codice.ddf.libs.klv.data.Klv.LengthEncoding;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.HashSet;
import org.codice.ddf.libs.klv.data.numerical.KlvDouble;
import org.codice.ddf.libs.klv.data.numerical.KlvInt;
import org.junit.Test;

public class KlvContextTest {
  @Test
  public void testAddSingleDataElement() {
    final KlvContext klvContext = new KlvContext(KeyLength.OneByte, LengthEncoding.OneByte);
    final KlvInt klvInt = new KlvInt(new byte[] {1}, "first");
    klvContext.addDataElement(klvInt);
    verifyKLVContextDataElements(klvContext, klvInt);
  }

  @Test
  public void testAddDataElementCollection() {
    final KlvContext klvContext = new KlvContext(KeyLength.OneByte, LengthEncoding.OneByte);
    final Collection<KlvDataElement> dataElements = new HashSet<>();
    final KlvInt klvInt = new KlvInt(new byte[] {1}, "first");
    dataElements.add(klvInt);
    final KlvDouble klvDouble = new KlvDouble(new byte[] {2}, "second");
    dataElements.add(klvDouble);
    klvContext.addDataElements(dataElements);
    verifyKLVContextDataElements(klvContext, klvInt, klvDouble);
  }

  private void verifyKLVContextDataElements(
      final KlvContext klvContext, final KlvDataElement... dataElements) {
    assertThat(klvContext.getDataElements().size(), is(dataElements.length));
    assertThat(klvContext.getKeyToDataElementMap().size(), is(dataElements.length));

    for (final KlvDataElement dataElement : dataElements) {
      assertThat(klvContext.hasDataElement(dataElement.getName()), is(true));
      assertThat(klvContext.getDataElementByName(dataElement.getName()), is(dataElement));

      assertThat(klvContext.getKeyToDataElementMap(), hasKey(dataElement.getKeyAsString()));
      assertThat(
          klvContext.getKeyToDataElementMap().get(dataElement.getKeyAsString()), is(dataElement));
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullKeyLength() {
    new KlvContext(null, LengthEncoding.OneByte, new HashSet<>()).addDataElements(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullLengthEncoding() {
    new KlvContext(KeyLength.OneByte, null, new HashSet<>()).addDataElements(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullDataElementCollection() {
    new KlvContext(KeyLength.OneByte, LengthEncoding.OneByte, null).addDataElements(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddNullDataElement() {
    new KlvContext(KeyLength.OneByte, LengthEncoding.OneByte).addDataElements(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddNullDataElementCollection() {
    new KlvContext(KeyLength.OneByte, LengthEncoding.OneByte).addDataElement(null);
  }
}
