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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLStreamReader2;

/**
 * Class is used to create text path index strings to be searched upon.
 * 
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 * 
 */
public class TextPathIndexer {

    public static final char ATTRIBUTE_SELECTOR = '@';

    public static final char ATTRIBUTE_START_SIGNAL = '{';

    public static final char ATTRIBUTE_END_SIGNAL = '}';

    public static final char SELECTOR = '/';

    public static final char ATTRIBUTE_DELIMITER = '|';

    public static final char LEAF_TEXT_DELIMITER = ' ';

    private XMLInputFactory2 xmlInputFactory = null;

    private final Deque<String> stack = new ArrayDeque<String>();

    private static final char DOUBLE_QUOTE = '"';

    private static final Logger LOGGER = Logger.getLogger(TextPathIndexer.class);

    public TextPathIndexer(XMLInputFactory2 xmlInputFactory) {
        this.xmlInputFactory = xmlInputFactory;
    }

    public List<String> indexTextPath(String xmlData) {

        XMLStreamReader2 xmlStreamReader;

        TextPathState state = new TextPathState();

        try {
            // xml parser does not handle leading whitespace
            xmlStreamReader = (XMLStreamReader2) xmlInputFactory
                    .createXMLStreamReader(new StringReader(xmlData));

            while (xmlStreamReader.hasNext()) {
                int event = xmlStreamReader.next();

                if (event == XMLStreamConstants.START_ELEMENT) {

                    StringBuffer element = new StringBuffer();

                    element.append(previous()).append(SELECTOR)
                            .append(xmlStreamReader.getPrefixedName())
                            .append(ATTRIBUTE_START_SIGNAL);

                    List<String> attributesList = getSortedAttributesList(xmlStreamReader);

                    for (int i = 0; i < attributesList.size(); i++) {

                        if (i != 0) {
                            element.append(ATTRIBUTE_DELIMITER);
                        }

                        element.append(attributesList.get(i));
                    }

                    element.append(ATTRIBUTE_END_SIGNAL);

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
                    "Failure occurred in parsing the xml data. No data has been stored or indexed.",
                    e1);
        }

        assert stack.isEmpty();

        return state.getTextPathValues();
    }

    private List<String> getSortedAttributesList(XMLStreamReader2 xmlStreamReader) {

        List<String> attributesList = new ArrayList<String>();

        for (int i = 0; i < xmlStreamReader.getAttributeCount(); i++) {

            StringBuffer attributeBuffer = new StringBuffer();

            String attributePrefix = xmlStreamReader.getAttributePrefix(i);

            attributeBuffer.append((attributePrefix != null) ? attributePrefix : "")
                    .append((attributePrefix != null) ? ":" : "")
                    .append(xmlStreamReader.getAttributeLocalName(i)).append("=")
                    .append(DOUBLE_QUOTE).append(xmlStreamReader.getAttributeValue(i))
                    .append(DOUBLE_QUOTE);

            attributesList.add(attributeBuffer.toString());
        }

        Collections.sort(attributesList);

        return attributesList;
    }

    protected String previous() {
        return (stack.peek() != null) ? stack.peek() : "";
    }

}
