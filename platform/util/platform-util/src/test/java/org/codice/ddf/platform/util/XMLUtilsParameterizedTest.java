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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

@RunWith(Enclosed.class)
public class XMLUtilsParameterizedTest {

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

  @RunWith(Parameterized.class)
  public static class TestDocumentBuilderFactoryXXE {

    private String implClass;

    @Parameters(name = "{index}: {0}")
    public static Collection valuesToTestWith() {
      return XMLUtils.DOCUMENT_BUILDER_FACTORY_IMPL_WHITELIST;
    }

    public TestDocumentBuilderFactoryXXE(String className) {
      implClass = className;
    }

    @Test(expected = org.xml.sax.SAXParseException.class)
    public void testDocumentBuilderLimitEntityExpansion()
        throws IOException, SAXException, ParserConfigurationException {
      InputStream is = new ByteArrayInputStream(XML_XXE_EXPANSION.getBytes(StandardCharsets.UTF_8));

      DocumentBuilderFactory dbf =
          XML_UTILS.getSecureDocumentBuilderFactory(implClass, XMLUtils.class.getClassLoader());
      dbf.newDocumentBuilder().parse(is);
    }

    @Test
    public void testDocumentBuilderDisallowsEntityInjection()
        throws IOException, SAXException, ParserConfigurationException {
      URL resource = XMLUtilsTest.class.getClassLoader().getResource("xxe_injection.txt");
      String xmlStr = String.format(XML_XXE_INJECTION, resource.toString());

      InputStream is = new ByteArrayInputStream(xmlStr.getBytes(StandardCharsets.UTF_8));
      DocumentBuilderFactory dbf =
          XML_UTILS.getSecureDocumentBuilderFactory(implClass, XMLUtils.class.getClassLoader());
      Document doc = null;
      try {
        doc = dbf.newDocumentBuilder().parse(is);
      } catch (org.xml.sax.SAXParseException e) {
        // This specific implementation throws a TransformerException when external entity injection
        // is attempted
        // If you aren't this implementation, assume that the exception was actually a bad thing
        if (!dbf.getClass()
            .getName()
            .equals("com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl")) {
          throw e;
        }
      }

      if (doc != null
          && doc.getElementsByTagName("foo") != null
          && doc.getElementsByTagName("foo").getLength() > 0) {
        String injectedContent = IOUtils.toString(resource, StandardCharsets.UTF_8);
        assertThat(
            "External XML Entity should not be injected",
            doc.getElementsByTagName("foo").item(0).getTextContent(),
            not(containsString(injectedContent)));
      }
    }
  }

  @RunWith(Parameterized.class)
  public static class TestXMLTransformerFactoryXXE {

    private String implClass;

    @Parameters(name = "{index}: {0}")
    public static Collection valuesToTestWith() {
      return XMLUtils.TRANSFORMER_FACTORY_IMPL_WHITELIST;
    }

    public TestXMLTransformerFactoryXXE(String className) {
      implClass = className;
    }

    @Test(expected = javax.xml.transform.TransformerException.class)
    public void testXMLTransformerLimitsEntityExpansion() throws TransformerException {
      Source xmlSource = new StreamSource(new StringReader(XML_XXE_EXPANSION));
      StreamResult result = new StreamResult(new StringWriter());

      TransformerFactory tf =
          XML_UTILS.getSecureXmlTransformerFactory(implClass, XMLUtils.class.getClassLoader());
      tf.newTransformer().transform(xmlSource, result);
    }

    @Test
    public void testXMLTransformerDisallowsEntityInjection()
        throws TransformerException, IOException {
      URL resource = XMLUtilsTest.class.getClassLoader().getResource("xxe_injection.txt");
      String xmlStr = String.format(XML_XXE_INJECTION, resource.toString());
      Source xmlSource = new StreamSource(new StringReader(xmlStr));
      StreamResult result = new StreamResult(new StringWriter());

      TransformerFactory tf =
          XML_UTILS.getSecureXmlTransformerFactory(implClass, XMLUtils.class.getClassLoader());
      try {
        tf.newTransformer().transform(xmlSource, result);
      } catch (TransformerException e) {
        // This specific implementation throws a TransformerException when external entity injection
        // is attempted
        // If you aren't this implementation, assume that the exception was actually a bad thing
        if (!tf.getClass()
            .getName()
            .equals("com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl")) {
          throw e;
        }
      }

      String injectedContent = IOUtils.toString(resource, StandardCharsets.UTF_8);
      assertThat(
          "External XML Entity should not be injected",
          result.getWriter().toString(),
          not(containsString(injectedContent)));
    }
  }

  @RunWith(Parameterized.class)
  public static class TestSAXParserFactoryXXE {

    private String implClass;

    @Parameters(name = "{index}: {0}")
    public static Collection valuesToTestWith() {
      return XMLUtils.SAX_PARSER_FACTORY_IMPL_WHITELIST;
    }

    public TestSAXParserFactoryXXE(String className) {
      implClass = className;
    }

    @Test(expected = org.xml.sax.SAXParseException.class)
    public void testSaxParserLimitsEntityExpansion()
        throws ParserConfigurationException, SAXException, IOException {
      InputStream is = new ByteArrayInputStream(XML_XXE_EXPANSION.getBytes(StandardCharsets.UTF_8));

      XML_UTILS
          .getSecureSAXParserFactory(implClass, XMLUtils.class.getClassLoader())
          .newSAXParser()
          .parse(is, new DefaultHandler());
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
          .getSecureSAXParserFactory(implClass, XMLUtils.class.getClassLoader())
          .newSAXParser()
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
  }

  @RunWith(Parameterized.class)
  public static class TestXMLReaderXXE {

    private String implClass;

    @Parameters(name = "{index}: {0}")
    public static Collection valuesToTestWith() {
      return XMLUtils.XML_READER_IMPL_WHITELIST;
    }

    public TestXMLReaderXXE(String className) {
      implClass = className;
    }

    @Test(expected = org.xml.sax.SAXParseException.class)
    public void testXMLReaderLimitsEntityExpansion() throws SAXException, IOException {
      InputStream is = new ByteArrayInputStream(XML_XXE_EXPANSION.getBytes(StandardCharsets.UTF_8));
      InputSource ins = new InputSource(is);
      XML_UTILS.getSecureXmlParser(implClass).parse(ins);
    }

    @Test
    public void testXMLReaderDisallowsEntityInjection() throws SAXException, IOException {
      URL resource = XMLUtilsTest.class.getClassLoader().getResource("xxe_injection.txt");
      String injectedContent = IOUtils.toString(resource, StandardCharsets.UTF_8);
      String xmlStr = String.format(XML_XXE_INJECTION, resource.toString());

      InputSource ins =
          new InputSource(new ByteArrayInputStream(xmlStr.getBytes(StandardCharsets.UTF_8)));
      XMLReader xmlReader = XML_UTILS.getSecureXmlParser(implClass);

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
  }
}
