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
package org.codice.ddf.platform.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class XMLUtilsTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(XMLUtilsTest.class);

    private static final String XML = "<node>Valid-XML</node>";

    private static final String PRETTY_XML_SOURCE = String.format(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><node>Valid-XML</node>%n");

    private static final String PRETTY_XML_NODE = String.format(
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>%n"
                    + "<node>Valid-XML</node>%n");

    private static final String XML_WITH_NAMESPACE =
            "<?xml version=\"1.0\"?><dog:Dog xmlns:dog=\"doggy-namespace\"></dog:Dog>";

    @Test
    public void testFormatSource() throws ParserConfigurationException, IOException, SAXException {
        Source xmlSource = new StreamSource(new StringReader(XML));

        assertThat(XMLUtils.format(xmlSource, setTransformerProperties()), is(equalTo(XML)));
    }

    @Test
    public void testFormatNode() throws ParserConfigurationException, IOException, SAXException {
        Node xmlNode = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream(XML.getBytes()));

        assertThat(XMLUtils.format(xmlNode, setTransformerProperties()), is(equalTo(XML)));
    }

    @Test
    public void testPrettyFormatSource()
            throws ParserConfigurationException, IOException, SAXException {
        Source xmlSource = new StreamSource(new StringReader(PRETTY_XML_SOURCE));

        assertThat(XMLUtils.prettyFormat(xmlSource), is(equalTo(PRETTY_XML_SOURCE)));
    }

    @Test
    public void testPrettyFormatNode()
            throws ParserConfigurationException, IOException, SAXException {
        Node xmlNode = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream(PRETTY_XML_NODE.getBytes()));

        assertThat(XMLUtils.prettyFormat(xmlNode), is(equalTo(PRETTY_XML_NODE)));
    }

    @Test
    public void testTransformSource()
            throws ParserConfigurationException, IOException, SAXException {
        Source xmlSource = new StreamSource(new StringReader(XML));
        StreamResult result = (StreamResult) XMLUtils.transform(xmlSource,
                setTransformerProperties(),
                new StreamResult(new StringWriter()));

        assertThat(result.getWriter()
                .toString(), is(equalTo(XML)));
    }

    @Test
    public void testTransformNode() throws ParserConfigurationException, IOException, SAXException {
        Node xmlNode = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream(XML.getBytes()));
        StreamResult result = (StreamResult) XMLUtils.transform(xmlNode,
                setTransformerProperties(),
                new StreamResult(new StringWriter()));

        assertThat(result.getWriter()
                .toString(), is(equalTo(XML)));
    }

    @Test
    public void testGetRootNamespace() {
        assert "doggy-namespace".equals(XMLUtils.getRootNamespace(XML_WITH_NAMESPACE));
    }

    private TransformerProperties setTransformerProperties() {
        TransformerProperties transformerProperties = new TransformerProperties();
        transformerProperties.addOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformerProperties.setErrorListener(new ErrorListener() {
            @Override
            public void warning(TransformerException exception) throws TransformerException {
                LOGGER.debug("Problem occurred during transformation.", exception);
            }

            @Override
            public void error(TransformerException exception) throws TransformerException {
                LOGGER.debug("Error occurred during transformation.", exception);
            }

            @Override
            public void fatalError(TransformerException exception) throws TransformerException {
                LOGGER.debug("Fatal error occurred during transformation.", exception);
            }
        });
        return transformerProperties;
    }
}
