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

package org.codice.ddf.transformer.xml.streaming.lib;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;

/**
 * A library class used to turn SAX events back into their corresponding XML snippets
 */
public class SaxEventToXmlElementConverter {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(SaxEventToXmlElementConverter.class);

    private ByteArrayOutputStream outputStream;

    private XMLOutputFactory xmlOutputFactory;

    private XMLStreamWriter out;

    /*
     * Map of namespace mappings, used to keep settings across XMLStreamWriters
     */
    private static Map<String, String> namespaces = new HashMap();

    /*
     * Set of namespace prefixes that have been already declared in the XML snippet
     * Used to ensure there aren't superfluous "xmlns" declarations.
     */
    private Set<String> namespacesAdded = new HashSet<>();

    public SaxEventToXmlElementConverter() throws UnsupportedEncodingException, XMLStreamException {

        outputStream = new ByteArrayOutputStream();
        xmlOutputFactory = XMLOutputFactory.newInstance();
        out =
                xmlOutputFactory.createXMLStreamWriter(new BufferedWriter(new OutputStreamWriter(new BufferedOutputStream(
                        outputStream), StandardCharsets.UTF_8)));
    }

    /**
     * Used to reconstruct the start tag of an XML element.
     *
     * @param uri       the URI that is passed in by {@link SaxEventHandler}
     * @param localName the localName that is passed in by {@link SaxEventHandler}
     * @param atts      the attributes that are passed in by {@link SaxEventHandler}
     * @return this
     * {@see SaxEventHandler#startElement}
     */
    public SaxEventToXmlElementConverter toElement(String uri, String localName, Attributes atts)
            throws XMLStreamException {

        /*
         * Use the uri to look up the namespace prefix and append it and the localName to the start tag
         */
        out.writeStartElement(uri, localName);
        if (!namespacesAdded.contains(uri)) {
            out.writeNamespace(out.getNamespaceContext()
                    .getPrefix(uri), uri);
            namespacesAdded.add(uri);
        }
        /*
         * Loop through the attributes and append them, prefixed with the proper namespace
         * We loop through the attributes twice to ensure all "xmlns" attributes are declared before
         * other attributes
         */
        for (int i = 0; i < atts.getLength(); i++) {
            if (atts.getURI(i)
                    .isEmpty()) {
                out.writeAttribute(atts.getLocalName(i), atts.getValue(i));
            } else {
                String attUri = atts.getURI(i);

                if (!namespacesAdded.contains(attUri)) {
                    out.writeNamespace(out.getNamespaceContext()
                            .getPrefix(attUri), attUri);
                    namespacesAdded.add(attUri);
                }
                out.writeAttribute(attUri, atts.getLocalName(i), atts.getValue(i));

            }
        }
        return this;
    }

    /**
     * Method used to reconstruct the end tag of an XML element.
     *
     * @param uri       the namespaceURI that is passed in by {@link SaxEventHandler}
     * @param localName the localName that is passed in by {@link SaxEventHandler}
     * @return this
     * {@see SaxEventHandler#endElement}
     */
    public SaxEventToXmlElementConverter toElement(String uri, String localName)
            throws XMLStreamException {
        /*
         * Append the properly prefixed end tag to the XML snippet
         */
        out.writeEndElement();
        return this;
    }

    /**
     * Method used to reconstruct the characters/value of an XML element.
     *
     * @param ch     the ch that is passed in by {@link SaxEventHandler}
     * @param start  the start that is passed in by {@link SaxEventHandler}
     * @param length the length that is passed in by {@link SaxEventHandler}
     * @return this
     * {@see SaxEventHandler#characters}
     */
    public SaxEventToXmlElementConverter toElement(char[] ch, int start, int length)
            throws XMLStreamException {
        out.writeCharacters(ch, start, length);
        return this;
    }

    /**
     * Overridden toString method to return the XML snippet that has been reconstructed
     *
     * @return the reconstructed XML snippet
     */
    @Override
    public String toString() {
        try {
            out.flush();
            return outputStream.toString(String.valueOf(StandardCharsets.UTF_8));

        } catch (XMLStreamException | UnsupportedEncodingException e) {
            LOGGER.warn("Could not convert XML Stream writer to String");
            return "";
        }
    }

    /**
     * Resets all stateful variables of the {@link SaxEventToXmlElementConverter}
     * Should be used before expecting a fresh XML snippet
     * Can be used instead of declaring a new one
     *
     * @return this
     */
    public SaxEventToXmlElementConverter reset() {
        outputStream.reset();
        namespacesAdded = new HashSet<>();
        try {
            out = xmlOutputFactory.createXMLStreamWriter(outputStream);
            for (Map.Entry<String, String> mapping : namespaces.entrySet()) {
                out.setPrefix(mapping.getKey(), mapping.getValue());

            }
        } catch (XMLStreamException e) {
            LOGGER.warn("Could not reset XMLStreamWriter");
        }
        return this;
    }

    /**
     * Method used in a {@link SaxEventHandler#startElement(String, String, String, Attributes)}
     * to populate the {@link SaxEventToXmlElementConverter#namespaceMapping}, which allows namespaceURI/prefix lookup. (Could potentially be used elsewhere, but one would have to ensure correct use)
     *
     * @param prefix the namespace prefix that is passed in by {@link SaxEventHandler}
     * @param uri    the namespace uri that is passed in by {@link SaxEventHandler}
     */
    public void addNamespace(String prefix, String uri) throws XMLStreamException {
        out.setPrefix(prefix, uri);
        namespaces.put(prefix, uri);
    }
}
