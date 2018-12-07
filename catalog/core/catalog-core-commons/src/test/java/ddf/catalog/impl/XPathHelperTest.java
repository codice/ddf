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
package ddf.catalog.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import ddf.util.XPathCache;
import ddf.util.XPathHelper;
import java.io.File;
import java.io.FileInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XPathHelperTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(XPathHelperTest.class);

  private static final String TEST_DATA_PATH = "src/test/resources/data/";

  private static final String INPUT_FILE = "IngestMetadata_WithContent.xml";

  // private static final String XPATH_EXPRESSION =
  // "/*[local-name()='Resource' and namespace-uri()='http://metadata.abc.com/mdr/ns/ns1/2.0/']/*"
  private static final String XPATH_EXPRESSION =
      "/*[local-name()='Resource'"
          + "[local-name() != 'identifier' and "
          + "local-name() != 'language' and "
          + "local-name() != 'dates' and "
          + "local-name() != 'rights' and "
          + "local-name() != 'format' and "
          + "local-name() != 'subjectCoverage' and "
          + "local-name() != 'temporalCoverage' and "
          + "local-name() != 'geospatialCoverage' ]";

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    XPathCache.setNamespaceResolver(new MockNamespaceResolver());
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {}

  @Before
  public void setUp() throws Exception {}

  @After
  public void tearDown() throws Exception {}

  @Test
  public void testXPathHelperWithDocument() throws Exception {
    try {
      Document document = getDocument(INPUT_FILE);

      XPathHelper xHelper = new XPathHelper(document);
      NodeList nodeList =
          (NodeList)
              xHelper.evaluate(
                  XPATH_EXPRESSION, XPathConstants.NODESET, new MockNamespaceResolver());
      LOGGER.debug("testXPathHelper_WithDocument() - nodeList length = {}", nodeList.getLength());
      assertEquals(6, nodeList.getLength());
    } catch (Exception e1) {
      LOGGER.error("Exception thrown during testXPathHelper_WithDocument", e1);
    }
  }

  @Test
  public void testXPathHelperWithXmlFile() throws Exception {
    try {
      String xmlString = getFileContentsAsString(TEST_DATA_PATH + INPUT_FILE);

      XPathHelper xHelper = new XPathHelper(xmlString);
      NodeList nodeList =
          (NodeList)
              xHelper.evaluate(
                  XPATH_EXPRESSION, XPathConstants.NODESET, new MockNamespaceResolver());
      LOGGER.debug("testXPathHelper_WithXmlFile() - nodeList length = {}", nodeList.getLength());
      assertEquals(6, nodeList.getLength());
    } catch (Exception e1) {
      LOGGER.error("Exception thrown during testXPathHelper_WithXmlFile", e1);
    }
  }

  @Test
  public void testXPathHelperPrintAttributeNode() throws Exception {
    try {
      String xmlString = getFileContentsAsString(TEST_DATA_PATH + INPUT_FILE);

      XPathHelper xHelper = new XPathHelper(xmlString);
      Node node =
          (Node)
              xHelper.evaluate(
                  "//ddms:publisher/@ICISM:classification",
                  XPathConstants.NODE,
                  new MockNamespaceResolver());
      String printNode = XPathHelper.print(node);
      LOGGER.debug("testXPathHelperPrintAttributeNode() - string value = {}", printNode);
      assertEquals("U", printNode);
    } catch (Exception e1) {
      LOGGER.error("Exception thrown during testXPathHelperPrintAttributeNode()", e1);
    }
  }

  @Test
  public void testXPathHelperPrintElementNode() throws Exception {
    try {
      String xmlString = getFileContentsAsString(TEST_DATA_PATH + INPUT_FILE);

      XPathHelper xHelper = new XPathHelper(xmlString);
      Node node =
          (Node)
              xHelper.evaluate(
                  "//ddms:publisher/ddms:Organization/ddms:name/text()",
                  XPathConstants.NODE,
                  new MockNamespaceResolver());
      String printNode = XPathHelper.print(node);
      LOGGER.debug("testXPathHelperPrintElementNode() - string value = {}", printNode);
      assertEquals("American Forces Press Service", printNode);
    } catch (Exception e1) {
      LOGGER.error("Exception thrown during testXPathHelperPrintElementNode()", e1);
    }
  }

  @Test
  public void testXPathHelperWithNoNamespaceTextPath() throws Exception {
    try {
      String xmlString = getFileContentsAsString(TEST_DATA_PATH + INPUT_FILE);

      XPathHelper xHelper = new XPathHelper(xmlString);
      NodeList nodeList =
          (NodeList)
              xHelper.evaluate("//fileTitle", XPathConstants.NODESET, new MockNamespaceResolver());
      LOGGER.debug(
          "testXPathHelper_WithNoNamespaceTextPath() - nodeList length = {}", nodeList.getLength());
      assertEquals(0, nodeList.getLength());

    } catch (Exception e1) {
      LOGGER.error("Exception thrown during testXPathHelper_WithNoNamespaceTextPath", e1);
    }
  }

  @Test
  public void testXPathHelperWithNamespaceTextPath() throws Exception {
    try {
      String xmlString = getFileContentsAsString(TEST_DATA_PATH + INPUT_FILE);

      XPathHelper xHelper = new XPathHelper(xmlString);
      NodeList nodeList =
          (NodeList)
              xHelper.evaluate(
                  "//abc:fileTitle", XPathConstants.NODESET, new MockNamespaceResolver());
      LOGGER.debug(
          "testXPathHelper_WithNamespaceTextPath() - nodeList length = {}", nodeList.getLength());
      assertEquals(1, nodeList.getLength());
    } catch (Exception e1) {
      LOGGER.error("Exception thrown during testXPathHelper_WithNamespaceTextPath", e1);
    }
  }

  @Test
  public void testXPathHelperWithAnyNamespaceTextPath() throws Exception {
    try {
      String xmlString = getFileContentsAsString(TEST_DATA_PATH + INPUT_FILE);

      XPathHelper xHelper = new XPathHelper(xmlString);
      NodeList nodeList = (NodeList) xHelper.evaluate("//xyz:fileTitle", XPathConstants.NODESET);
      LOGGER.debug(
          "testXPathHelper_WithAnyNamespaceTextPath() - nodeList length = {}",
          nodeList.getLength());
      fail("Expected an XPathExpressionException");
    } catch (XPathExpressionException e1) {
      LOGGER.error("Exception thrown during testXPathHelper_WithAnyNamespaceTextPath", e1);
    }
  }

  @Test
  public void testXPathHelperNoTitle() throws Exception {
    try {
      String xmlString = getFileContentsAsString(TEST_DATA_PATH + "IngestMetadata_NoTitle.xml");

      XPathHelper xHelper = new XPathHelper(xmlString);
      String title = (String) xHelper.evaluate("//ns1:title", new MockNamespaceResolver());
      LOGGER.debug("testXPathHelper_NoTitle() - title = [{}]", title);
      assertNotNull(title);
      assertTrue(title.length() == 0);
    } catch (XPathExpressionException e1) {
      LOGGER.error("Exception thrown during testXPathHelper_NoTitle", e1);
    }
  }

  // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private String getFileContentsAsString(String filename) throws Exception {
    FileInputStream file = new FileInputStream(filename);
    byte[] b = new byte[file.available()];
    file.read(b);
    file.close();

    return new String(b);
  }

  private Document getDocument(String xmlFilename) throws Exception {
    String qualifiedFilename = TEST_DATA_PATH + xmlFilename;

    return xmlFileToDocument(qualifiedFilename);
  }

  private Document xmlFileToDocument(String xmlFilename) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(new File(xmlFilename));
    doc.getDocumentElement().normalize();

    return doc;
  }
}
