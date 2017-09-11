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

import org.apache.tika.sax.ToXMLContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class XmlMetadataContentHandler extends ToXMLContentHandler {

  private static final String BODY_TAG = "body";

  private boolean inBody = false;

  private boolean limitReached = false;

  private int writeCount;

  private int writeLimit;

  /**
   * Constructs a new Xml Metadata content handler using the spcified encoding
   *
   * @param encoding encoding to use
   */
  public XmlMetadataContentHandler(String encoding, int writeLimit) {
    super(encoding);
    this.writeCount = 0;
    this.writeLimit = writeLimit;
  }

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {

    if (!inBody) {
      if (okToWrite(length)) {
        super.characters(ch, start, length);
        this.writeCount += length;
      }
    }
  }

  @Override
  protected void write(char ch) throws SAXException {
    if (!inBody) {
      if (okToWrite(1)) {
        super.write(ch);
        this.writeCount += 1;
      }
    }
  }

  @Override
  protected void write(String string) throws SAXException {
    if (!inBody) {
      if (okToWrite(string.length())) {
        super.write(string);
        this.writeCount += 1;
      }
    }
  }

  @Override
  public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
    if (!inBody) {
      if (okToWrite(length)) {
        super.ignorableWhitespace(ch, start, length);
        this.writeCount += length;
      }
    }
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes atts)
      throws SAXException {

    if (!inBody) {
      if (BODY_TAG.equalsIgnoreCase(localName)) {
        inBody = true;
      }
      super.startElement(uri, localName, qName, atts);
    }
  }

  @Override
  public void endElement(String uri, String localName, String name) throws SAXException {
    if (inBody) {
      if (BODY_TAG.equalsIgnoreCase(localName)) {
        inBody = false;
      }
    }
    if ((!inBody)) {
      super.endElement(uri, localName, name);
    }
  }

  public boolean okToWrite(int length) {
    if (!limitReached) {
      limitReached = (this.writeLimit != -1 && this.writeCount + length > this.writeLimit);
    }

    return !limitReached;
  }

  public boolean isWriteLimitReached() {
    return limitReached;
  }
}
