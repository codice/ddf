/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.transformer.generic.xml;

import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;

import ddf.catalog.data.Attribute;

public interface SaxEventHandler extends EntityResolver, DTDHandler, ContentHandler, ErrorHandler {

    /**
     * @return a list of attributes that has been constructed during the parsing of an XML document.
     */
    List<Attribute> getAttributes();

    void startElement(String uri, String localName, String qName, Attributes attributes);

    void endElement(String namespaceURI, String localName, String qName);

    void characters(char ch[], int start, int length);

}
