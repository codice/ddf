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
package org.codice.ddf.transformer.xml.streaming.lib;

import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.xml.stream.XMLStreamException;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;
import org.xml.sax.helpers.XMLReaderFactory;

public class TestSaxParser extends DefaultHandler2 {

  private XMLReader reader;

  private SaxEventToXmlElementConverter converter;

  @Override
  public void startPrefixMapping(String prefix, String uri) throws SAXException {
    try {
      converter.addNamespace(prefix, uri);
    } catch (XMLStreamException e) {
      fail("Failed to add namespace");
    }
  }

  @Override
  public void endPrefixMapping(String prefix) throws SAXException {
    converter.removeNamespace(prefix);
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes atts)
      throws SAXException {
    try {
      converter.toElement(uri, localName, atts);
    } catch (XMLStreamException e) {
      fail(
          String.format(
              "Failed on startElement with pieces: %s %s %s %s", uri, localName, qName, atts));
    }
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    try {
      converter.toElement(uri, localName);
    } catch (XMLStreamException e) {
      fail(String.format("Failed on endElement with pieces: %s %s %s", uri, localName, qName));
    }
  }

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    try {
      converter.toElement(ch, start, length);
    } catch (XMLStreamException e) {
      fail(String.format("Failed on endElement with pieces: %s", new String(ch, start, length)));
    }
  }

  public String parseAndReconstruct(String xmlSnippet)
      throws IOException, XMLStreamException, SAXException {
    converter = new SaxEventToXmlElementConverter();
    reader = XMLReaderFactory.createXMLReader();
    reader.setContentHandler(this);
    reader.setErrorHandler(this);
    reader.parse(new InputSource(new ByteArrayInputStream(xmlSnippet.getBytes())));
    return converter.toString();
  }
}
