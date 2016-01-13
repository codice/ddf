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
package ddf.catalog.transformer.generic.xml.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.transformer.generic.xml.SaxEventHandler;

public class XMLSaxEventHandlerImpl implements SaxEventHandler {

    private List<Attribute> attributes;

    private Boolean stillInterested = true;

    private String reading;

    private StringBuffer stringBuffer;

    private Map<String, String> xmlToMetacard;

    @Override
    public List<Attribute> getAttributes() {
        return attributes;
    }

    @Override
    public void setDocumentLocator(Locator locator) {

    }

    protected XMLSaxEventHandlerImpl() {
        xmlToMetacard = new HashMap<>();
        xmlToMetacard.put("title", Metacard.TITLE);
        xmlToMetacard.put("point-of-contact", Metacard.POINT_OF_CONTACT);
        xmlToMetacard.put("description", Metacard.DESCRIPTION);
        xmlToMetacard.put("source", Metacard.RESOURCE_URI);
    }

    protected XMLSaxEventHandlerImpl(Map<String, String> xmlToMetacardMap) {
        this.xmlToMetacard = xmlToMetacardMap;
    }

    @Override
    public void startDocument() {
        stringBuffer = new StringBuffer();
        attributes = new ArrayList<>();

    }

    @Override
    public void endDocument() throws SAXException {

    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {

    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {

    }

    /* +++++++++++++++++++++++++++++++++++++++++++++++++++++ */

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        if (!stillInterested) {
            return;
        }
        if (xmlToMetacard.get(localName.toLowerCase()) != null) {
            reading = localName.toLowerCase();
            return;
        }
        String attribute = attributes.getValue("name");
        if (attribute != null && xmlToMetacard.get(attribute) != null) {
            reading = attribute;
        }
    }

    @Override
    public void endElement(String namespaceURI, String localName, String qName) {
        if (!stillInterested) {
            return;
        }
        if (reading != null) {
            String result = stringBuffer.toString().trim();
            stringBuffer.setLength(0);
            if (xmlToMetacard.get(reading) != null) {
                attributes.add(new AttributeImpl(xmlToMetacard.get(reading), result));
            }
            reading = null;

        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        if (!stillInterested) {
            return;
        }
        if (reading != null) {
            stringBuffer.append(new String(ch, start, length));
        }

    }

    /* +++++++++++++++++++++++++++++++++++++++++++++++++++++ */

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {

    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {

    }

    @Override
    public void skippedEntity(String name) throws SAXException {

    }

    @Override
    public void notationDecl(String name, String publicId, String systemId) throws SAXException {

    }

    @Override
    public void unparsedEntityDecl(String name, String publicId, String systemId,
            String notationName) throws SAXException {

    }

    @Override
    public InputSource resolveEntity(String publicId, String systemId)
            throws SAXException, IOException {
        return null;
    }

    @Override
    public void warning(SAXParseException exception) throws SAXException {

    }

    @Override
    public void error(SAXParseException exception) throws SAXException {

    }

    @Override
    public void fatalError(SAXParseException exception) throws SAXException {

    }
}
