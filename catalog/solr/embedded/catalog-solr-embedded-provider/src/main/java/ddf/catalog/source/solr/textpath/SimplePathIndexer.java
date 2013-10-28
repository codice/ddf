/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.catalog.source.solr.textpath;

import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.log4j.Logger;

/**
 * Class is used to create simple text path index strings to be searched upon.
 * 
 * @author Phillip Klinefelter
 * @author ddf.isgs@lmco.com
 * 
 */
public class SimplePathIndexer {

    public static final char SELECTOR = '/';

    public static final char LEAF_TEXT_DELIMITER = '|';

    //DDF-216 MODULARITY: changed from XmlInputFactory2
    private XMLInputFactory xmlInputFactory = null;

    private final Deque<String> stack = new ArrayDeque<String>();

    private static final Logger LOGGER = Logger.getLogger(SimplePathIndexer.class);

    public SimplePathIndexer(XMLInputFactory xmlInputFactory) {
        this.xmlInputFactory = xmlInputFactory;
    }

    public List<String> indexTextPath(String xmlData) {
        XMLStreamReader xmlStreamReader;
        TextPathState state = new TextPathState();

        try {
            // xml parser does not handle leading whitespace
            xmlStreamReader = xmlInputFactory
                    .createXMLStreamReader(new StringReader(xmlData));

            while (xmlStreamReader.hasNext()) {
                int event = xmlStreamReader.next();

                if (event == XMLStreamConstants.START_ELEMENT) {
                    StringBuffer element = new StringBuffer();
                    //DDF-216 MODULARITY: Only available with XmlStreamReader2
//                    element.append(previous()).append(SELECTOR)
//                            .append(xmlStreamReader.getPrefixedName());
                    String prefix = xmlStreamReader.getPrefix();
                    String prefixName = xmlStreamReader.getLocalName();
                    if (prefix != null && prefix.length() > 0) {
                        prefixName = prefix + ":" + prefixName;
                    }
                    element.append(previous()).append(SELECTOR).append(prefixName);
                    stack.push(element.toString());
                    state.startElement(stack.peek());
                }

                if (event == XMLStreamConstants.CHARACTERS) {
                    state.setCharacters(xmlStreamReader.getText());
                }

                if (event == XMLStreamConstants.END_ELEMENT) {
                    stack.pop();
                    state.endElement(LEAF_TEXT_DELIMITER);
                }

            }
        } catch (XMLStreamException e1) {
            LOGGER.warn(
                    "Failure occurred in parsing the XML data. No data has been stored or indexed.",
                    e1);
        }

        assert stack.isEmpty();
        return state.getTextPathValues();
    }

    protected String previous() {
        return (stack.peek() != null) ? stack.peek() : "";
    }

}
