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
package ddf.catalog.transform.xml;

import static org.junit.Assert.assertEquals;

import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transformer.api.PrintWriter;
import ddf.catalog.transformer.xml.EscapingPrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EscapingPrintWriterTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(EscapingPrintWriterTest.class);

  static {
    LOGGER.info("defaultCharset: {}", Charset.defaultCharset());

    int definedSurrogateCount = 0;
    int definedIsoCount = 0;
    int undefinedCount = 0;
    int bmpCount = 0;
    int validCount = 0;

    boolean pickedUndef = false;
    boolean pickedSurrogate = false;
    boolean pickedISO = false;

    int sampleUndef = -1;
    int sampleSurrogate = -1;
    int sampleISO = -1;

    for (int i = 0; i < Character.MAX_CODE_POINT + 1; i++) {
      if (Character.isBmpCodePoint(i)) {
        bmpCount += 1;

        if (Character.isValidCodePoint(i)) {
          validCount += 1;
        }

        char theChar = (char) i;

        if (!Character.isDefined(theChar)) {
          undefinedCount += 1;
          if (!pickedUndef) {
            pickedUndef = true;
            sampleUndef = i;
          }
        }
        if (Character.isSurrogate(theChar)) {
          definedSurrogateCount += 1;
          if (!pickedSurrogate) {
            pickedSurrogate = true;
            sampleSurrogate = i;
          }
        }
        if (Character.isISOControl(theChar)) {
          definedIsoCount += 1;
          if (!pickedISO) {
            pickedISO = true;
            sampleISO = i;
          }
        }
      }
    }

    LOGGER.info("num code points (BMP) representable by char type: {}", bmpCount);
    LOGGER.info("num valid (BMP) code points: {}", validCount);
    LOGGER.info("num undefined BMP chars: {}, example: {}", undefinedCount, sampleUndef);
    LOGGER.info("num surrogate BMP chars: {}, example: {}", definedSurrogateCount, sampleSurrogate);
    LOGGER.info("num ISO BMP chars: {}, example: {}", definedIsoCount, sampleISO);
  }

  @Test
  public void testXmlMetaCharacters() throws CatalogTransformerException {
    String unescaped = "& > < \" \'";
    String escaped = "&amp; &gt; &lt; &quot; &apos;";

    StringWriter stringWriter = new StringWriter(128);
    PrintWriter escapingPrintWriter = new EscapingPrintWriter(stringWriter);
    escapingPrintWriter.setValue(unescaped);

    escapingPrintWriter.flush();
    String processed = stringWriter.toString();

    assertEquals(escaped, processed);
  }
}
