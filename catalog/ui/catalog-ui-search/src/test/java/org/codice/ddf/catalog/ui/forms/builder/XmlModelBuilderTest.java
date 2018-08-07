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
package org.codice.ddf.catalog.ui.forms.builder;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import net.opengis.filter.v_2_0.FilterType;
import org.codice.ddf.catalog.ui.forms.filter.FilterWriter;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class XmlModelBuilderTest {
  private static final String AND = "AND";

  private static final String OR = "OR";

  private static final String JSON_EQUAL = "=";

  private XmlModelBuilder builder;

  @Before
  public void setup() {
    builder = new XmlModelBuilder();
  }

  @Test
  public void testBinaryComparisonType() throws Exception {
    // @formatter:off
    JAXBElement filter = builder.isEqualTo(false).property("name").value("value").end().getResult();
    // @formatter:on

    assertTrue(filter.getDeclaredType().equals(FilterType.class));

    forNode(filter).verifyTerminalNode("/Filter/PropertyIsEqualTo", "name", "value");
  }

  @Test
  public void testBinaryComparisonTypeTemplated() throws Exception {
    JAXBElement filter =
        builder
            .isEqualTo(false)
            .property("name")
            .function("template.value.v1")
            .value("5")
            .value("id")
            .value(true)
            .value(false)
            .end()
            .end()
            .getResult();

    assertTrue(filter.getDeclaredType().equals(FilterType.class));

    forNode(filter)
        .verifyTerminalTemplatedNode("/Filter/PropertyIsEqualTo", "name", "5", "id", true, false);
  }

  @Test
  public void testBinaryLogicTypeAnd() throws Exception {
    JAXBElement filter =
        builder
            .and()
            .isEqualTo(false)
            .property("name")
            .value("value")
            .end()
            .isNotEqualTo(false)
            .property("type")
            .value("imagery")
            .end()
            .end()
            .getResult();

    assertTrue(filter.getDeclaredType().equals(FilterType.class));

    forNode(filter).verifyTerminalNode("/Filter/And/PropertyIsEqualTo", "name", "value");
  }

  @Test
  public void testComplexEmbeddedLogicNodes() throws Exception {
    JAXBElement filter =
        builder
            .and()
            .isEqualTo(false)
            .property("name")
            .value("Bob")
            .end()
            .isGreaterThanOrEqualTo(false)
            .property("benumber")
            .value("0.45")
            .end()
            .or()
            .isLessThan(false)
            .property("length")
            .value("120")
            .end()
            .isLessThanOrEqualTo(false)
            .property("width")
            .value("20")
            .end()
            .end()
            .end()
            .getResult();

    assertTrue(filter.getDeclaredType().equals(FilterType.class));

    forNode(filter)
        .verifyTerminalNode("/Filter/And/PropertyIsEqualTo", "name", "Bob")
        .verifyTerminalNode("/Filter/And/PropertyIsGreaterThanOrEqualTo", "benumber", "0.45")
        .verifyTerminalNode("/Filter/And/Or/PropertyIsLessThan", "length", "120")
        .verifyTerminalNode("/Filter/And/Or/PropertyIsLessThanOrEqualTo", "width", "20");
  }

  private static XPathAssertionSupport forNode(JAXBElement node) throws Exception {
    return new XPathAssertionSupport(node);
  }

  private static class XPathAssertionSupport {
    private final XPath xPath;
    private final DocumentBuilderFactory factory;
    private final DocumentBuilder builder;
    private final FilterWriter filterWriter;
    private final Document document;

    private XPathAssertionSupport(final JAXBElement element)
        throws IOException, JAXBException, SAXException, ParserConfigurationException {
      this.xPath = XPathFactory.newInstance().newXPath();
      this.factory = DocumentBuilderFactory.newInstance();
      this.builder = this.factory.newDocumentBuilder();
      this.filterWriter = new FilterWriter(false);

      String rawXml = filterWriter.marshal(element);
      try (InputStream inputStream = new ByteArrayInputStream(rawXml.getBytes())) {
        this.document = builder.parse(inputStream);
      }
    }

    private XPathAssertionSupport verifyTerminalNode(
        String xpathToNode, String valueRef, String literal) throws XPathExpressionException {
      String actualValueRef =
          (String)
              xPath
                  .compile(xpathToNode + "/ValueReference")
                  .evaluate(document, XPathConstants.STRING);
      String actualLiteral =
          (String)
              xPath.compile(xpathToNode + "/Literal").evaluate(document, XPathConstants.STRING);
      assertThat(actualValueRef, is(valueRef));
      assertThat(actualLiteral, is(literal));
      return this;
    }

    private XPathAssertionSupport verifyTerminalTemplatedNode(
        String xpathToNode,
        String valueRef,
        String defaultValue,
        String id,
        boolean isVisible,
        boolean isReadOnly)
        throws XPathExpressionException {
      String actualValueRef =
          (String)
              xPath
                  .compile(xpathToNode + "/ValueReference")
                  .evaluate(document, XPathConstants.STRING);

      // Template values (in order)
      String literal1 =
          (String)
              xPath
                  .compile(xpathToNode + "/Function/Literal[1]")
                  .evaluate(document, XPathConstants.STRING);
      String literal2 =
          (String)
              xPath
                  .compile(xpathToNode + "/Function/Literal[2]")
                  .evaluate(document, XPathConstants.STRING);
      String literal3 =
          (String)
              xPath
                  .compile(xpathToNode + "/Function/Literal[3]")
                  .evaluate(document, XPathConstants.STRING);
      String literal4 =
          (String)
              xPath
                  .compile(xpathToNode + "/Function/Literal[4]")
                  .evaluate(document, XPathConstants.STRING);

      assertThat(actualValueRef, is(valueRef));
      assertThat(literal1, is(defaultValue));
      assertThat(literal2, is(id));
      assertThat(literal3, is(Boolean.toString(isVisible)));
      assertThat(literal4, is(Boolean.toString(isReadOnly)));
      return this;
    }
  }
}
