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
package org.codice.ddf.security.common.jaxrs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.rs.security.saml.DeflateEncoderDecoder;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class RestSecurityTest {

  public static Document readXml(InputStream is)
      throws SAXException, IOException, ParserConfigurationException {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

    dbf.setValidating(false);
    dbf.setIgnoringComments(false);
    dbf.setIgnoringElementContentWhitespace(true);
    dbf.setNamespaceAware(true);
    // dbf.setCoalescing(true);
    // dbf.setExpandEntityReferences(true);

    DocumentBuilder db = null;
    db = dbf.newDocumentBuilder();
    db.setEntityResolver(new DOMUtils.NullResolver());

    // db.setErrorHandler( new MyErrorHandler());

    return db.parse(is);
  }

  @Test
  public void testInflateDeflateWithTokenDuplication() throws Exception {
    String token = "valid_grant valid_grant valid_grant valid_grant valid_grant valid_grant";

    DeflateEncoderDecoder deflateEncoderDecoder = new DeflateEncoderDecoder();
    byte[] deflatedToken = deflateEncoderDecoder.deflateToken(token.getBytes());

    String cxfInflatedToken = IOUtils.toString(deflateEncoderDecoder.inflateToken(deflatedToken));

    String streamInflatedToken =
        IOUtils.toString(
            new InflaterInputStream(new ByteArrayInputStream(deflatedToken), new Inflater(true)));

    assertNotSame(cxfInflatedToken, token);
    assertEquals(streamInflatedToken, token);
  }

  @Test
  public void testInflateDeflate() throws Exception {
    String token = "valid_grant";

    String encodedToken = RestSecurity.deflateAndBase64Encode(token);
    String decodedToken = RestSecurity.inflateBase64(encodedToken);

    assertThat(decodedToken, is(token));
  }

  /**
   * Reads a classpath resource into a Document.
   *
   * @param name the name of the classpath resource
   */
  private Document readDocument(String name)
      throws SAXException, IOException, ParserConfigurationException {
    InputStream inStream = getClass().getResourceAsStream(name);
    return readXml(inStream);
  }
}
