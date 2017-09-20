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
package org.codice.ddf.transformer.xml.streaming;

import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public abstract class AbstractSaxEventHandler implements SaxEventHandler {

  @Override
  public void setDocumentLocator(Locator locator) {}

  @Override
  public void startDocument() throws SAXException {}

  @Override
  public void endDocument() throws SAXException {}

  @Override
  public void startPrefixMapping(String prefix, String uri) throws SAXException {}

  @Override
  public void endPrefixMapping(String prefix) throws SAXException {}

  @Override
  public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {}

  @Override
  public void processingInstruction(String target, String data) throws SAXException {}

  @Override
  public void skippedEntity(String name) throws SAXException {}
}
