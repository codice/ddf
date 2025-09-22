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
package ddf.catalog.transformer.common.tika.handler;

import ddf.catalog.transformer.common.tika.TikaMetadataExtractor;
import java.nio.charset.StandardCharsets;
import org.apache.tika.exception.WriteLimitReachedException;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.WriteOutContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class BodyAndMetadataContentHandler extends DefaultHandler {

  private final BodyContentHandler bodyContentHandler;

  private final WriteOutContentHandler writeOutContentHandler;

  private final XmlMetadataContentHandler xmlMetadataContentHandler;

  private boolean bodyWriteLimitReached = false;

  public BodyAndMetadataContentHandler(int bodyWriteLimit, int metadataWriteLimit) {
    this.xmlMetadataContentHandler =
        new XmlMetadataContentHandler(StandardCharsets.UTF_8.toString(), metadataWriteLimit);
    this.writeOutContentHandler = new WriteOutContentHandler(bodyWriteLimit);
    this.bodyContentHandler = new BodyContentHandler(writeOutContentHandler);
  }

  @Override
  public void startPrefixMapping(String prefix, String uri) throws SAXException {
    if (!xmlMetadataContentHandler.isWriteLimitReached()) {
      xmlMetadataContentHandler.startPrefixMapping(prefix, uri);
    }

    if (!bodyWriteLimitReached) {
      bodyContentHandler.startPrefixMapping(prefix, uri);
    }
  }

  @Override
  public void endPrefixMapping(String prefix) throws SAXException {
    if (!xmlMetadataContentHandler.isWriteLimitReached()) {
      xmlMetadataContentHandler.endPrefixMapping(prefix);
    }

    if (!bodyWriteLimitReached) {
      bodyContentHandler.endPrefixMapping(prefix);
    }
  }

  @Override
  public void startDocument() throws SAXException {
    if (!xmlMetadataContentHandler.isWriteLimitReached()) {
      xmlMetadataContentHandler.startDocument();
    }

    if (!bodyWriteLimitReached) {
      bodyContentHandler.startDocument();
    }
  }

  @Override
  public void endDocument() throws SAXException {
    if (!xmlMetadataContentHandler.isWriteLimitReached()) {
      xmlMetadataContentHandler.endDocument();
    }

    if (!bodyWriteLimitReached) {
      bodyContentHandler.endDocument();
    }
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes)
      throws SAXException {
    if (!xmlMetadataContentHandler.isWriteLimitReached()) {
      xmlMetadataContentHandler.startElement(uri, localName, qName, attributes);
    }

    if (!bodyWriteLimitReached) {
      bodyContentHandler.startElement(uri, localName, qName, attributes);
    }
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    super.endElement(uri, localName, qName);
    if (!xmlMetadataContentHandler.isWriteLimitReached()) {
      xmlMetadataContentHandler.endElement(uri, localName, qName);
    }

    if (!bodyWriteLimitReached) {
      bodyContentHandler.endElement(uri, localName, qName);
    }
  }

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    if (xmlMetadataContentHandler.okToWrite(length)) {
      xmlMetadataContentHandler.characters(ch, start, length);
    }

    if (bodyWriteLimitReached) {
      return;
    }

    try {
      bodyContentHandler.characters(ch, start, length);
    } catch (SAXException se) {
      handleException(se);
    }
  }

  @Override
  public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
    if (xmlMetadataContentHandler.okToWrite(length)) {
      xmlMetadataContentHandler.ignorableWhitespace(ch, start, length);
    }

    if (bodyWriteLimitReached) {
      return;
    }

    try {
      bodyContentHandler.ignorableWhitespace(ch, start, length);
    } catch (SAXException se) {
      handleException(se);
    }
  }

  public String getBodyText() {
    return bodyContentHandler.toString();
  }

  public String getMetadataText() {
    if (xmlMetadataContentHandler.isWriteLimitReached()) {
      return TikaMetadataExtractor.METADATA_LIMIT_REACHED_MSG;
    }

    return xmlMetadataContentHandler.toString();
  }

  private void handleException(SAXException se) throws SAXException {
    if (se instanceof WriteLimitReachedException) {
      bodyWriteLimitReached = true;
    } else {
      throw se;
    }
  }
}
