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
package org.codice.ddf.platform.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

public class XMLUtilsTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(XMLUtilsTest.class);

  private static final String XML = "<node>Valid-XML</node>";

  private static final String PRETTY_XML_SOURCE =
      String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?><node>Valid-XML</node>%n");

  private static final String PRETTY_XML_NODE =
      String.format(
          "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>%n"
              + "<node>Valid-XML</node>%n");

  private static final String XML_WITH_NAMESPACE =
      "<?xml version=\"1.0\"?><dog:Dog xmlns:dog=\"doggy-namespace\"></dog:Dog>";

  private static final String XML_MULTIPLE_NODES =
      "<bookstore>\n"
          + "  <book category=\"children\">\n"
          + "    <title>Harry Potter</title>\n"
          + "    <author>J K. Rowling</author>\n"
          + "    <year>2005</year>\n"
          + "    <price>29.99</price>\n"
          + "  </book>";

  private static final String XML_XXE_EXPANSION =
      "<!DOCTYPE Qs [\n"
          + "<!ENTITY Q \"Q\">\n"
          + "<!ENTITY Q10 \"&Q;&Q;&Q;&Q;&Q;&Q;&Q;&Q;&Q;&Q;\">\n"
          + "<!ENTITY Q100 \"&Q10;&Q10;&Q10;&Q10;&Q10;&Q10;&Q10;&Q10;&Q10;&Q10;\">\n"
          + "<!ENTITY Q1000 \"&Q100;&Q100;&Q100;&Q100;&Q100;&Q100;&Q100;&Q100;&Q100;&Q100;\">\n"
          + "<!ENTITY Q10000 \"&Q1000;&Q1000;&Q1000;&Q1000;&Q1000;&Q1000;&Q1000;&Q1000;&Q1000;&Q1000;\">\n"
          + "<!ENTITY Q100000 \"&Q10000;&Q10000;&Q10000;&Q10000;&Q10000;&Q10000;&Q10000;&Q10000;&Q10000;&Q10000;\">]>\n"
          + "<Qs>&Q100000;</Qs>\n";

  private static final String XML_XXE_INJECTION =
      "<!DOCTYPE foo [<!ENTITY bar SYSTEM \"%s\" >]>\n" + "<foo>&bar;</foo>\n";

  private static final XMLUtils XML_UTILS = XMLUtils.getInstance();

  @Test
  public void testFormatSource() throws ParserConfigurationException, IOException, SAXException {
    Source xmlSource = new StreamSource(new StringReader(XML));

    assertThat(XML_UTILS.format(xmlSource, setTransformerProperties()), is(equalTo(XML)));
  }

  @Test
  public void testFormatNode() throws ParserConfigurationException, IOException, SAXException {
    Node xmlNode =
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(new ByteArrayInputStream(XML.getBytes()));

    assertThat(XML_UTILS.format(xmlNode, setTransformerProperties()), is(equalTo(XML)));
  }

  @Test
  public void testPrettyFormatSource()
      throws ParserConfigurationException, IOException, SAXException {
    Source xmlSource = new StreamSource(new StringReader(PRETTY_XML_SOURCE));

    assertThat(XML_UTILS.prettyFormat(xmlSource), is(equalTo(PRETTY_XML_SOURCE)));
  }

  @Test
  public void testPrettyFormatNode()
      throws ParserConfigurationException, IOException, SAXException {
    Node xmlNode =
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(new ByteArrayInputStream(PRETTY_XML_NODE.getBytes()));

    assertThat(XML_UTILS.prettyFormat(xmlNode), is(equalTo(PRETTY_XML_NODE)));
  }

  @Test
  public void testTransformSource() throws ParserConfigurationException, IOException, SAXException {
    Source xmlSource = new StreamSource(new StringReader(XML));
    StreamResult result =
        (StreamResult)
            XML_UTILS.transform(
                xmlSource, setTransformerProperties(), new StreamResult(new StringWriter()));

    assertThat(result.getWriter().toString(), is(equalTo(XML)));
  }

  @Test
  public void testTransformNode() throws ParserConfigurationException, IOException, SAXException {
    Node xmlNode =
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(new ByteArrayInputStream(XML.getBytes()));
    StreamResult result =
        (StreamResult)
            XML_UTILS.transform(
                xmlNode, setTransformerProperties(), new StreamResult(new StringWriter()));

    assertThat(result.getWriter().toString(), is(equalTo(XML)));
  }

  @Test
  public void testGetRootNamespace() {
    assert "doggy-namespace".equals(XML_UTILS.getRootNamespace(XML_WITH_NAMESPACE));
  }

  @Test
  public void testProcessElementException() {

    Object returnValue = XML_UTILS.processElements("", (result, xmlReader) -> true);
    assertThat(
        "Processing result should be null if an exception was thrown",
        returnValue,
        is(nullValue()));
  }

  @Test
  public void testProcessingStop() {
    int expectedValue = 3;
    int returnValue =
        XML_UTILS.processElements(
            XML_MULTIPLE_NODES,
            (XMLUtils.ResultHolder<Integer> result, XMLStreamReader xmlStreamReader) -> {
              result.setIfEmpty(0);
              result.set((result.get() + 1));
              return result.get() < expectedValue;
            });

    assertThat(
        String.format("Expected result value to be %s", expectedValue),
        returnValue,
        equalTo(expectedValue));
  }

  @Test(expected = org.xml.sax.SAXParseException.class)
  public void testDocumentBuilderLimitsEntityExpansion()
      throws IOException, SAXException, ParserConfigurationException {
    InputStream is = new ByteArrayInputStream(XML_XXE_EXPANSION.getBytes(StandardCharsets.UTF_8));
    XML_UTILS.parseDocument(is, false);
  }

  @Test(expected = org.xml.sax.SAXParseException.class)
  public void testDocumentBuilderDisallowsEntityInjection()
      throws IOException, SAXException, ParserConfigurationException {
    URL resource = XMLUtilsTest.class.getClassLoader().getResource("xxe_injection.txt");
    String xmlStr = String.format(XML_XXE_INJECTION, resource.toString());

    InputStream is = new ByteArrayInputStream(xmlStr.getBytes(StandardCharsets.UTF_8));
    XML_UTILS.parseDocument(is, false);
  }

  @Test(expected = javax.xml.transform.TransformerException.class)
  public void testXMLTransformerLimitsEntityExpansion() throws TransformerException {
    Source xmlSource = new StreamSource(new StringReader(XML_XXE_EXPANSION));
    StreamResult result = new StreamResult(new StringWriter());

    Transformer transformer = XML_UTILS.getXmlTransformer(true);
    transformer.transform(xmlSource, result);
  }

  @Test(expected = javax.xml.transform.TransformerException.class)
  public void testXMLTransformerDisallowsEntityInjection() throws TransformerException {
    URL resource = XMLUtilsTest.class.getClassLoader().getResource("xxe_injection.txt");
    String xmlStr = String.format(XML_XXE_INJECTION, resource.toString());
    Source xmlSource = new StreamSource(new StringReader(xmlStr));
    StreamResult result = new StreamResult(new StringWriter());

    Transformer transformer = XML_UTILS.getXmlTransformer(true);
    transformer.transform(xmlSource, result);
  }

  @Test(expected = org.xml.sax.SAXParseException.class)
  public void testSaxParserLimitsEntityExpansion()
      throws ParserConfigurationException, SAXException, IOException {
    InputStream is = new ByteArrayInputStream(XML_XXE_EXPANSION.getBytes(StandardCharsets.UTF_8));
    XML_UTILS.getSecureSAXParser(false).parse(is, new DefaultHandler());
  }

  @Test
  public void testSaxParserDisallowsEntityInjection()
      throws IOException, ParserConfigurationException, SAXException {
    URL resource = XMLUtilsTest.class.getClassLoader().getResource("xxe_injection.txt");
    String xmlStr = String.format(XML_XXE_INJECTION, resource.toString());
    String injectedContent = IOUtils.toString(resource, StandardCharsets.UTF_8);

    InputStream is = new ByteArrayInputStream(xmlStr.getBytes(StandardCharsets.UTF_8));
    StringBuilder strBuf = new StringBuilder();
    XML_UTILS
        .getSecureSAXParser(false)
        .parse(
            is,
            new DefaultHandler() {
              @Override
              public void characters(char ch[], int start, int length) {
                strBuf.append(ch, start, length);
              }
            });

    assertThat(
        "External XML Entity should not be injected",
        strBuf.toString(),
        not(containsString(injectedContent)));
  }

  @Test(expected = org.xml.sax.SAXParseException.class)
  public void testXMLReaderLimitsEntityExpansion() throws SAXException, IOException {
    InputStream is = new ByteArrayInputStream(XML_XXE_EXPANSION.getBytes(StandardCharsets.UTF_8));
    InputSource ins = new InputSource(is);
    XML_UTILS.getSecureXmlParser().parse(ins);
  }

  @Test
  public void testXMLReaderDisallowsEntityInjection() throws SAXException, IOException {
    URL resource = XMLUtilsTest.class.getClassLoader().getResource("xxe_injection.txt");
    String injectedContent = IOUtils.toString(resource, StandardCharsets.UTF_8);
    String xmlStr = String.format(XML_XXE_INJECTION, resource.toString());

    InputSource ins =
        new InputSource(new ByteArrayInputStream(xmlStr.getBytes(StandardCharsets.UTF_8)));
    XMLReader xmlReader = XML_UTILS.getSecureXmlParser();

    StringBuilder strBuf = new StringBuilder();
    DefaultHandler handler =
        new DefaultHandler() {
          @Override
          public void characters(char ch[], int start, int length) {
            strBuf.append(ch, start, length);
          }
        };

    xmlReader.setErrorHandler(handler);
    xmlReader.setContentHandler(handler);
    xmlReader.setDTDHandler(handler);
    xmlReader.setEntityResolver(handler);
    xmlReader.parse(ins);

    assertThat(
        "External XML Entity should not be injected",
        strBuf.toString(),
        not(containsString(injectedContent)));
  }

  private TransformerProperties setTransformerProperties() {
    TransformerProperties transformerProperties = new TransformerProperties();
    transformerProperties.addOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    transformerProperties.setErrorListener(
        new ErrorListener() {
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
