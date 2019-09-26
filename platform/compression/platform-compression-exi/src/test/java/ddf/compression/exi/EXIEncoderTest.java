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
package ddf.compression.exi;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.sax.EXIReader;
import org.xml.sax.InputSource;

/** Tests out the functionality of the EXIEncoder class. */
public class EXIEncoderTest {

  private static final String TEST_FILE = "/atom-example.xml";

  @BeforeClass
  public static void setUp() {
    XMLUnit.setControlParser("org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
    XMLUnit.setTestParser("org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
    XMLUnit.setSAXParserFactory("org.apache.xerces.jaxp.SAXParserFactoryImpl");
    XMLUnit.setTransformerFactory("org.apache.xalan.processor.TransformerFactoryImpl");
  }

  /**
   * Tests that the encode method converts xml into exi-compressed xml.
   *
   * @throws Exception
   */
  @Test
  public void testEncode() throws Exception {

    ByteArrayOutputStream exiStream = new ByteArrayOutputStream();

    InputStream xmlStream = getClass().getResourceAsStream(TEST_FILE);

    EXIEncoder.encode(xmlStream, exiStream);

    StringWriter stringWriter = new StringWriter();

    GrammarCache grammarCache;

    SAXTransformerFactory saxTransformerFactory =
        (SAXTransformerFactory) SAXTransformerFactory.newInstance();
    SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
    saxParserFactory.setNamespaceAware(true);
    TransformerHandler transformerHandler = saxTransformerFactory.newTransformerHandler();

    EXIReader reader = new EXIReader();

    grammarCache = new GrammarCache(null, GrammarOptions.DEFAULT_OPTIONS);

    reader.setGrammarCache(grammarCache);

    transformerHandler.setResult(new StreamResult(stringWriter));

    reader.setContentHandler(transformerHandler);

    reader.parse(new InputSource(new ByteArrayInputStream(exiStream.toByteArray())));
    XMLUnit.setNormalize(true);
    XMLUnit.setNormalizeWhitespace(true);
    InputStream stream = getClass().getResourceAsStream(TEST_FILE);
    Diff diff =
        XMLUnit.compareXML(
            IOUtils.toString(stream, StandardCharsets.UTF_8), stringWriter.getBuffer().toString());
    IOUtils.closeQuietly(stream);
    assertTrue(
        "The XML input file (" + TEST_FILE + ") did not match the EXI-decoded output",
        diff.similar());
  }

  /** Tests that the decode method converts exi-compressed xml into 'normal' xml. */
  @Test(expected = IllegalArgumentException.class)
  public void testDecode() {
    InputStream exiStream = mock(InputStream.class);
    OutputStream xmlStream = mock(OutputStream.class);
    EXIEncoder.decode(exiStream, xmlStream);
  }
}
