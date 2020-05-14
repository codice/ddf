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
package ddf.catalog.transformer.response.query.atom;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathNotExists;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.action.Action;
import ddf.action.ActionProvider;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.apache.abdera.model.Link;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.configuration.SystemInfo;
import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class AtomTransformerTest {

  private static final String TEXT_TYPE = "text";

  private static final String BAD_WKT = "POLYGON 30 10, 10 20, 20 40, 40 40, 30 10))";

  private static final String SCHEMA_DIRECTORY = "src/test/resources/schemas/";

  private static final String ATOM_EXTENSION = ".atom";

  private static final String TARGET_FOLDER = "target/";

  private static final DateTime SAMPLE_DATE_TIME = new DateTime(2000, 1, 1, 1, 0, 0, 0);

  private static final String SAMPLE_SOURCE_ID = "local";

  private static final String DEFAULT_TEST_VERSION = "2.1.0";

  private static final String DEFAULT_TEST_SITE = "currentSite";

  private static final String DEFAULT_TEST_ORGANIZATION = "Lockheed Martin";

  private static final String TRUE = "true";

  private static final String SAMPLE_ID = "12345678900987654321abcdeffedcba";

  private static final FilterBuilder FILTER_BUILDER = new GeotoolsFilterBuilder();

  private static final Logger LOGGER = LoggerFactory.getLogger(AtomTransformerTest.class);

  static DocumentBuilder parser = null; // thread-unsafe

  static Validator validator = null; // thread-unsafe

  private static SimpleDateFormat atomDateFormat = null;

  static {
    atomDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    atomDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  static {
    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    documentBuilderFactory.setNamespaceAware(true);
    documentBuilderFactory.setValidating(false);
    try {
      parser = documentBuilderFactory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      LOGGER.info("Parser exception during static setup", e);
    }
  }

  static {
    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    Source[] schemas = {
      new StreamSource(new File(SCHEMA_DIRECTORY + "atom.xsd")),
      new StreamSource(new File(SCHEMA_DIRECTORY + "xml.xsd")),
      // for sample metacard xml (from mock metacard transformer)
      new StreamSource(new File(SCHEMA_DIRECTORY + "sample.xsd")),
      new StreamSource(new File(SCHEMA_DIRECTORY + "os-federation.xsd"))
    };
    try {
      Schema schema = schemaFactory.newSchema(schemas);
      validator = schema.newValidator();
    } catch (SAXException e) {
      LOGGER.info("SAX exception creating validator", e);
    }
  }

  @After
  public void tearDown() throws Exception {
    System.clearProperty(SystemInfo.ORGANIZATION);
    System.clearProperty(SystemInfo.SITE_NAME);
    System.clearProperty(SystemInfo.VERSION);
  }

  @BeforeClass
  public static void setupTestClass() {

    // makes xpaths easier to write when prefixes are declared beforehand.
    HashMap map = new HashMap();
    map.put("gml", "http://www.opengis.net/gml");
    map.put("georss", "http://www.georss.org/georss");
    map.put("", "http://www.w3.org/2005/Atom");
    map.put("atom", "http://www.w3.org/2005/Atom");
    map.put("relevance", "http://a9.com/-/opensearch/extensions/relevance/1.0/");
    map.put("os", "http://a9.com/-/spec/opensearch/1.1/");
    map.put("fs", "http://a9.com/-/opensearch/extensions/federation/1.0/");
    NamespaceContext ctx = new SimpleNamespaceContext(map);
    XMLUnit.setXpathNamespaceContext(ctx);
  }

  /**
   * Tests actions when given <code>null</code> input
   *
   * @throws CatalogTransformerException
   */
  @Test(expected = CatalogTransformerException.class)
  public void testNullInput() throws CatalogTransformerException {

    new AtomTransformer().transform(null, null);
  }

  /**
   * Tests what happens when no system configuration can be found.
   *
   * @throws IOException
   * @throws CatalogTransformerException
   * @throws XpathException
   * @throws SAXException
   */
  @Test
  public void testNoDdfConfiguration()
      throws IOException, CatalogTransformerException, XpathException, SAXException {
    // given
    MetacardTransformer metacardTransformer = getXmlMetacardTransformerStub();

    String currentSiteName = System.getProperty(SystemInfo.SITE_NAME);

    try {
      System.setProperty(SystemInfo.SITE_NAME, "");

      AtomTransformer transformer = new AtomTransformer();
      transformer.setMetacardTransformer(metacardTransformer);
      SourceResponse response = getSourceResponseStub(SAMPLE_ID, null);

      // when
      BinaryContent binaryContent = transformer.transform(response, null);

      // then
      assertThat(binaryContent.getMimeType(), is(AtomTransformer.MIME_TYPE));

      byte[] bytes = binaryContent.getByteArray();

      /* used to visualize */
      IOUtils.write(
          bytes, new FileOutputStream(new File(TARGET_FOLDER + getMethodName() + ATOM_EXTENSION)));

      String output = new String(bytes);

      assertFeedCompliant(output);
      assertEntryCompliant(output);
      validateAgainstAtomSchema(bytes);
      assertXpathNotExists("/atom:feed/atom:generator", output);
      assertXpathEvaluatesTo(
          AtomTransformer.DEFAULT_AUTHOR, "/atom:feed/atom:author/atom:name", output);

    } finally {
      if (currentSiteName != null) {
        System.setProperty(SystemInfo.SITE_NAME, currentSiteName);
      }
    }
  }

  @Test
  public void testNoSiteName()
      throws IOException, CatalogTransformerException, XpathException, SAXException {
    // given
    MetacardTransformer metacardTransformer = getXmlMetacardTransformerStub();

    AtomTransformer transformer = getConfiguredAtomTransformer(metacardTransformer, true);

    SourceResponse response1 = mock(SourceResponse.class);

    when(response1.getHits()).thenReturn(new Long(1));

    when(response1.getRequest()).thenReturn(getStubRequest());

    ResultImpl result1 = new ResultImpl();

    MetacardStub metacard = new MetacardStub("");
    metacard.setId(SAMPLE_ID);
    metacard.setSourceId(null);

    result1.setMetacard(metacard);
    when(response1.getResults()).thenReturn(Arrays.asList((Result) result1));

    SourceResponse response = response1;
    Double relevanceScore = 0.3345;
    result1.setRelevanceScore(relevanceScore);

    // when
    BinaryContent binaryContent = transformer.transform(response, null);

    // then
    assertThat(binaryContent.getMimeType(), is(AtomTransformer.MIME_TYPE));

    byte[] bytes = binaryContent.getByteArray();

    /* used to visualize */
    IOUtils.write(
        bytes, new FileOutputStream(new File(TARGET_FOLDER + getMethodName() + ATOM_EXTENSION)));

    String output = new String(bytes);

    assertFeedCompliant(output);
    assertEntryCompliant(output);
    validateAgainstAtomSchema(bytes);
    assertXpathEvaluatesTo(
        AtomTransformer.DEFAULT_SOURCE_ID,
        "/atom:feed/atom:entry/fs:resultSource/@fs:sourceId",
        output);
  }

  @Test
  public void testNoMetacardTransformer()
      throws IOException, CatalogTransformerException, XpathException, SAXException {
    // given
    AtomTransformer transformer = getConfiguredAtomTransformer(null, true);

    SourceResponse response = getSourceResponseStub(SAMPLE_ID, null);

    // when
    BinaryContent binaryContent = transformer.transform(response, null);

    // then
    assertThat(binaryContent.getMimeType(), is(AtomTransformer.MIME_TYPE));

    byte[] bytes = binaryContent.getByteArray();

    String output = new String(bytes);

    assertFeedCompliant(output);
    assertEntryCompliant(output);
    validateAgainstAtomSchema(bytes);
    assertXpathEvaluatesTo(TEXT_TYPE, "/atom:feed/atom:entry/atom:content/@type", output);
    assertXpathEvaluatesTo(SAMPLE_ID, "/atom:feed/atom:entry/atom:content", output);
  }

  @Test
  public void testThrowMetacardTransformerCatalogTransformerException()
      throws IOException, CatalogTransformerException, XpathException, SAXException {

    // given
    MetacardTransformer metacardTransformer = mock(MetacardTransformer.class);
    when(metacardTransformer.transform(isA(Metacard.class), isNull()))
        .thenThrow(CatalogTransformerException.class);

    AtomTransformer transformer = getConfiguredAtomTransformer(metacardTransformer, true);

    SourceResponse response = getSourceResponseStub(SAMPLE_ID, null);

    // when
    BinaryContent binaryContent = transformer.transform(response, null);

    // then
    assertThat(binaryContent.getMimeType(), is(AtomTransformer.MIME_TYPE));

    byte[] bytes = binaryContent.getByteArray();

    /* used to visualize */
    IOUtils.write(
        bytes, new FileOutputStream(new File(TARGET_FOLDER + getMethodName() + ATOM_EXTENSION)));

    String output = new String(bytes);

    assertFeedCompliant(output);
    assertEntryCompliant(output);
    validateAgainstAtomSchema(bytes);
    assertXpathEvaluatesTo(TEXT_TYPE, "/atom:feed/atom:entry/atom:content/@type", output);
    assertXpathEvaluatesTo(SAMPLE_ID, "/atom:feed/atom:entry/atom:content", output);
  }

  @Test
  public void testThrowMetacardTransformerRuntimeException()
      throws IOException, CatalogTransformerException, XpathException, SAXException {

    // given
    MetacardTransformer metacardTransformer = mock(MetacardTransformer.class);
    when(metacardTransformer.transform(isA(Metacard.class), isNull()))
        .thenThrow(RuntimeException.class);

    AtomTransformer transformer = getConfiguredAtomTransformer(metacardTransformer, true);

    SourceResponse response = getSourceResponseStub(SAMPLE_ID, null);

    // when
    BinaryContent binaryContent = transformer.transform(response, null);

    // then
    assertThat(binaryContent.getMimeType(), is(AtomTransformer.MIME_TYPE));

    byte[] bytes = binaryContent.getByteArray();

    String output = new String(bytes);

    assertFeedCompliant(output);
    assertEntryCompliant(output);
    validateAgainstAtomSchema(bytes);
    assertXpathEvaluatesTo(TEXT_TYPE, "/atom:feed/atom:entry/atom:content/@type", output);
    assertXpathEvaluatesTo(SAMPLE_ID, "/atom:feed/atom:entry/atom:content", output);
  }

  @Test
  public void testMetacardTransformerBytesNull()
      throws IOException, CatalogTransformerException, XpathException, SAXException {

    // given
    MetacardTransformer metacardTransformer = mock(MetacardTransformer.class);
    BinaryContent metacardTransformation = mock(BinaryContent.class);
    when(metacardTransformation.getByteArray()).thenReturn(null);
    when(metacardTransformer.transform(isA(Metacard.class), isNull()))
        .thenReturn(metacardTransformation);

    AtomTransformer transformer = getConfiguredAtomTransformer(metacardTransformer, true);

    SourceResponse response = getSourceResponseStub(SAMPLE_ID, null);

    // when
    BinaryContent binaryContent = transformer.transform(response, null);

    // then
    assertThat(binaryContent.getMimeType(), is(AtomTransformer.MIME_TYPE));

    byte[] bytes = binaryContent.getByteArray();

    String output = new String(bytes);

    assertFeedCompliant(output);
    assertEntryCompliant(output);
    validateAgainstAtomSchema(bytes);
    assertXpathEvaluatesTo(TEXT_TYPE, "/atom:feed/atom:entry/atom:content/@type", output);
    assertXpathEvaluatesTo(SAMPLE_ID, "/atom:feed/atom:entry/atom:content", output);
  }

  @Test
  public void testMetacardTransformerBytesZero()
      throws IOException, CatalogTransformerException, XpathException, SAXException {

    // given
    MetacardTransformer metacardTransformer = mock(MetacardTransformer.class);
    BinaryContent metacardTransformation = mock(BinaryContent.class);
    when(metacardTransformation.getByteArray()).thenReturn(new byte[0]);
    when(metacardTransformer.transform(isA(Metacard.class), isNull()))
        .thenReturn(metacardTransformation);

    AtomTransformer transformer = getConfiguredAtomTransformer(metacardTransformer, true);

    SourceResponse response = getSourceResponseStub(SAMPLE_ID, null);

    // when
    BinaryContent binaryContent = transformer.transform(response, null);

    // then
    assertThat(binaryContent.getMimeType(), is(AtomTransformer.MIME_TYPE));

    byte[] bytes = binaryContent.getByteArray();

    String output = new String(bytes);

    assertFeedCompliant(output);
    assertEntryCompliant(output);
    validateAgainstAtomSchema(bytes);
    assertXpathEvaluatesTo(TEXT_TYPE, "/atom:feed/atom:entry/atom:content/@type", output);
    assertXpathEvaluatesTo(SAMPLE_ID, "/atom:feed/atom:entry/atom:content", output);
  }

  @Test
  public void testMetacardTransformerThrowsIoException()
      throws IOException, CatalogTransformerException, XpathException, SAXException {

    // given
    MetacardTransformer metacardTransformer = mock(MetacardTransformer.class);
    BinaryContent metacardTransformation = mock(BinaryContent.class);
    when(metacardTransformation.getByteArray()).thenThrow(IOException.class);
    when(metacardTransformer.transform(isA(Metacard.class), isNull()))
        .thenReturn(metacardTransformation);

    AtomTransformer transformer = getConfiguredAtomTransformer(metacardTransformer, true);

    SourceResponse response = getSourceResponseStub(SAMPLE_ID, null);

    // when
    BinaryContent binaryContent = transformer.transform(response, null);

    // then
    assertThat(binaryContent.getMimeType(), is(AtomTransformer.MIME_TYPE));

    byte[] bytes = binaryContent.getByteArray();

    String output = new String(bytes);

    assertFeedCompliant(output);
    assertEntryCompliant(output);
    validateAgainstAtomSchema(bytes);
    assertXpathEvaluatesTo(TEXT_TYPE, "/atom:feed/atom:entry/atom:content/@type", output);
    assertXpathEvaluatesTo(SAMPLE_ID, "/atom:feed/atom:entry/atom:content", output);
  }

  @Test
  public void testNoBinaryContentXml()
      throws IOException, CatalogTransformerException, XpathException, SAXException {
    // given
    MetacardTransformer metacardTransformer = mock(MetacardTransformer.class);
    when(metacardTransformer.transform(isA(Metacard.class), isNull())).thenReturn(null);

    AtomTransformer transformer = getConfiguredAtomTransformer(metacardTransformer, true);

    SourceResponse response = getSourceResponseStub(SAMPLE_ID, null);

    // when
    BinaryContent binaryContent = transformer.transform(response, null);

    // then
    assertThat(binaryContent.getMimeType(), is(AtomTransformer.MIME_TYPE));

    byte[] bytes = binaryContent.getByteArray();

    String output = new String(bytes);

    assertFeedCompliant(output);
    assertEntryCompliant(output);
    validateAgainstAtomSchema(bytes);
    assertXpathEvaluatesTo(TEXT_TYPE, "/atom:feed/atom:entry/atom:content/@type", output);
    assertXpathEvaluatesTo(SAMPLE_ID, "/atom:feed/atom:entry/atom:content", output);
  }

  @Test
  public void testTotalResultsNegative()
      throws IOException, CatalogTransformerException, XpathException, SAXException {
    // given
    AtomTransformer transformer = new AtomTransformer();
    MetacardTransformer metacardTransformer = getXmlMetacardTransformerStub();
    transformer.setMetacardTransformer(metacardTransformer);
    setDefaultSystemConfiguration();

    SourceResponse response = mock(SourceResponse.class);

    when(response.getHits()).thenReturn(new Long(-1));

    QueryImpl query = new QueryImpl(FILTER_BUILDER.attribute(Metacard.METADATA).text("you"));
    query.setPageSize(1);
    query.setStartIndex(2);
    query.setRequestsTotalResultsCount(true);

    QueryRequestImpl queryRequestImpl = new QueryRequestImpl(query);
    when(response.getRequest()).thenReturn(queryRequestImpl);

    ResultImpl result1 = new ResultImpl();

    MetacardStub metacard = new MetacardStub("");
    metacard.setId(SAMPLE_ID);

    result1.setMetacard(metacard);

    when(response.getResults()).thenReturn(Arrays.asList((Result) result1));

    // when
    BinaryContent binaryContent = transformer.transform(response, null);

    // then
    assertThat(binaryContent.getMimeType(), is(AtomTransformer.MIME_TYPE));

    byte[] bytes = binaryContent.getByteArray();

    String output = new String(bytes);

    assertFeedCompliant(output);
    assertEntryCompliant(output);
    validateAgainstAtomSchema(bytes);
    assertXpathNotExists("/atom:feed/os:totalResults", output);
  }

  @Test
  public void testItemsPerPageNegativeInteger()
      throws IOException, CatalogTransformerException, XpathException, SAXException {
    // given
    AtomTransformer transformer = new AtomTransformer();
    MetacardTransformer metacardTransformer = getXmlMetacardTransformerStub();
    transformer.setMetacardTransformer(metacardTransformer);
    setDefaultSystemConfiguration();

    SourceResponse response = mock(SourceResponse.class);

    when(response.getHits()).thenReturn(new Long(1));

    QueryImpl query = new QueryImpl(FILTER_BUILDER.attribute(Metacard.METADATA).text("you"));
    query.setPageSize(-1);
    query.setStartIndex(2);
    query.setRequestsTotalResultsCount(true);

    QueryRequestImpl queryRequestImpl = new QueryRequestImpl(query);
    when(response.getRequest()).thenReturn(queryRequestImpl);

    ResultImpl result1 = new ResultImpl();

    MetacardStub metacard = new MetacardStub("");
    metacard.setId(SAMPLE_ID);

    result1.setMetacard(metacard);

    when(response.getResults()).thenReturn(Arrays.asList((Result) result1));

    // when
    BinaryContent binaryContent = transformer.transform(response, null);

    // then
    assertThat(binaryContent.getMimeType(), is(AtomTransformer.MIME_TYPE));

    byte[] bytes = binaryContent.getByteArray();

    String output = new String(bytes);

    assertFeedCompliant(output);
    assertEntryCompliant(output);
    validateAgainstAtomSchema(bytes);
    assertXpathEvaluatesTo("1", "/atom:feed/os:itemsPerPage", output);
  }

  @Test
  public void testNoCreatedDate()
      throws IOException, CatalogTransformerException, XpathException, SAXException {
    // given
    AtomTransformer transformer = new AtomTransformer();
    MetacardTransformer metacardTransformer = getXmlMetacardTransformerStub();
    transformer.setMetacardTransformer(metacardTransformer);
    setDefaultSystemConfiguration();

    SourceResponse response = mock(SourceResponse.class);

    when(response.getHits()).thenReturn(new Long(1));

    when(response.getRequest()).thenReturn(getStubRequest());

    ResultImpl result1 = new ResultImpl();

    MetacardStub metacard = new MetacardStub("");
    metacard.setId(SAMPLE_ID);
    metacard.setCreatedDate(null);

    result1.setMetacard(metacard);

    when(response.getResults()).thenReturn(Arrays.asList((Result) result1));

    result1.setRelevanceScore(0.3345);

    // when
    BinaryContent binaryContent = transformer.transform(response, null);

    // then
    assertThat(binaryContent.getMimeType(), is(AtomTransformer.MIME_TYPE));

    byte[] bytes = binaryContent.getByteArray();

    String output = new String(bytes);

    assertFeedCompliant(output);
    assertEntryCompliant(output);
    validateAgainstAtomSchema(bytes);
    assertXpathNotExists("atom:feed/atom:entry/atom:published", output);
  }

  @Test
  public void testNoModifiedDate()
      throws IOException, CatalogTransformerException, XpathException, SAXException {
    // given
    AtomTransformer transformer = new AtomTransformer();
    MetacardTransformer metacardTransformer = getXmlMetacardTransformerStub();
    transformer.setMetacardTransformer(metacardTransformer);
    setDefaultSystemConfiguration();

    SourceResponse response = mock(SourceResponse.class);

    when(response.getHits()).thenReturn(new Long(1));

    when(response.getRequest()).thenReturn(getStubRequest());

    ResultImpl result1 = new ResultImpl();

    MetacardStub metacard = new MetacardStub("");
    metacard.setId(SAMPLE_ID);
    metacard.setModifiedDate(null);

    result1.setMetacard(metacard);

    when(response.getResults()).thenReturn(Arrays.asList((Result) result1));

    result1.setRelevanceScore(0.3345);

    // when
    BinaryContent binaryContent = transformer.transform(response, null);

    // then
    assertThat(binaryContent.getMimeType(), is(AtomTransformer.MIME_TYPE));

    byte[] bytes = binaryContent.getByteArray();

    String output = new String(bytes);

    assertFeedCompliant(output);
    assertEntryCompliant(output);
    validateAgainstAtomSchema(bytes);
    assertXpathExists("atom:feed/atom:entry/atom:updated", output);
  }

  /**
   * The following rules must be followed in order to be compliant with the Atom specification as
   * defined by http://tools.ietf.org/html/rfc4287#section-4.1.2 <br>
   * "The following child elements are defined by this specification (note that the presence of some
   * of these elements is required):
   *
   * <p>
   * <li/>atom:entry elements MUST contain one or more atom:author elements, unless the atom:entry
   *     contains an atom:source element that contains an atom:author element or, in an Atom Feed
   *     Document, the atom:feed element contains an atom:author element itself.
   *
   *     <p>
   * <li/>atom:entry elements MAY contain any number of atom:category elements.
   *
   *     <p>
   * <li/>atom:entry elements MUST NOT contain more than one atom:content element.
   *
   *     <p>
   * <li/>atom:entry elements MAY contain any number of atom:contributor elements.
   *
   *     <p>
   * <li/>atom:entry elements MUST contain exactly one atom:id element.
   *
   *     <p>
   * <li/>atom:entry elements that contain no child atom:content element MUST contain at least one
   *     atom:link element with a rel attribute value of "alternate".
   *
   *     <p>
   * <li/>atom:entry elements MUST NOT contain more than one atom:link element with a rel attribute
   *     value of "alternate" that has the same combination of type and hreflang attribute values.
   *
   *     <p>
   * <li/>atom:entry elements MAY contain additional atom:link elements beyond those described
   *     above.
   *
   *     <p>
   * <li/>atom:entry elements MUST NOT contain more than one atom:published element.
   *
   *     <p>
   * <li/>atom:entry elements MUST NOT contain more than one atom:rights element.
   *
   *     <p>
   * <li/>atom:entry elements MUST NOT contain more than one atom:source element.
   *
   *     <p>
   * <li/>atom:entry elements MUST contain an atom:summary element in either of the following cases:
   *
   *     <p>
   *
   *     <ul>
   *       the atom:entry contains an atom:content that has a "src" attribute (and is thus empty).
   * </ul>
   *
   * <p>
   *
   * <ul>
   *   the atom:entry contains content that is encoded in Base64; i.e., the "type" attribute of
   *   atom:content is a MIME media type [MIMEREG], but is not an XML media type [RFC3023], does not
   *   begin with "text/", and does not end with "/xml" or "+xml".
   * </ul>
   *
   * <p>
   * <li/>atom:entry elements MUST NOT contain more than one atom:summary element.
   *
   *     <p>
   * <li/>atom:entry elements MUST contain exactly one atom:title element.
   *
   *     <p>
   * <li/>atom:entry elements MUST contain exactly one atom:updated element."
   *
   * @throws CatalogTransformerException
   * @throws IOException
   * @throws SAXException
   * @throws XpathException
   */
  @Test
  public void testMetacardIsNull()
      throws IOException, CatalogTransformerException, XpathException, SAXException {

    // given
    AtomTransformer transformer = new AtomTransformer();
    MetacardTransformer metacardTransformer = getXmlMetacardTransformerStub();
    transformer.setMetacardTransformer(metacardTransformer);

    setDefaultSystemConfiguration();

    SourceResponse response = mock(SourceResponse.class);

    when(response.getRequest()).thenReturn(getStubRequest());

    ResultImpl result1 = new ResultImpl();
    ResultImpl result2 = new ResultImpl();

    MetacardStub metacard = new MetacardStub("");
    metacard.setId(SAMPLE_ID);
    metacard.setSourceId(SAMPLE_SOURCE_ID);

    result1.setMetacard(metacard);
    result2.setMetacard(null);

    when(response.getResults()).thenReturn(Arrays.asList((Result) result1, result2));

    // when
    BinaryContent binaryContent = transformer.transform(response, null);

    // then
    byte[] bytes = binaryContent.getByteArray();

    /* used to visualize */
    IOUtils.write(
        bytes, new FileOutputStream(new File(TARGET_FOLDER + getMethodName() + ATOM_EXTENSION)));

    String output = new String(bytes);

    assertFeedCompliant(output);
    assertEntryCompliant(output);
    validateAgainstAtomSchema(bytes);
    assertXpathEvaluatesTo(
        SAMPLE_SOURCE_ID, "/atom:feed/atom:entry/fs:resultSource/@fs:sourceId", output);
    assertXpathEvaluatesTo("", "/atom:feed/atom:entry/fs:resultSource", output);
    assertXpathEvaluatesTo(
        AtomTransformer.URN_CATALOG_ID + SAMPLE_ID, "/atom:feed/atom:entry/atom:id", output);
    assertXpathEvaluatesTo(MetacardStub.DEFAULT_TITLE, "/atom:feed/atom:entry/atom:title", output);
    assertXpathExists("/atom:feed/atom:entry/atom:updated", output);
    assertXpathExists("/atom:feed/atom:entry/atom:content", output);
  }

  @Test
  public void testNoEntries()
      throws IOException, CatalogTransformerException, XpathException, SAXException {

    // given
    AtomTransformer transformer = new AtomTransformer();
    MetacardTransformer metacardTransformer = getXmlMetacardTransformerStub();
    transformer.setMetacardTransformer(metacardTransformer);

    setDefaultSystemConfiguration();

    SourceResponse response = mock(SourceResponse.class);

    when(response.getRequest()).thenReturn(getStubRequest());

    when(response.getResults()).thenReturn(new ArrayList<Result>());

    // when
    BinaryContent binaryContent = transformer.transform(response, null);

    // then
    byte[] bytes = binaryContent.getByteArray();

    /* used to visualize */
    IOUtils.write(
        bytes, new FileOutputStream(new File(TARGET_FOLDER + getMethodName() + ATOM_EXTENSION)));

    String output = new String(bytes);

    assertFeedCompliant(output);
    assertEntryCompliant(output);
    validateAgainstAtomSchema(bytes);
    assertBasicFeedInfo(output, "0");
  }

  @Test
  public void testDistanceInMeters() {
    // TODO research if there is a way to display this information
  }

  /**
   * The following rules must be followed in order to be compliant with the Atom specification as
   * defined by http://tools.ietf.org/html/rfc4287#section-4.1.2 <br>
   * "The following child elements are defined by this specification (note that the presence of some
   * of these elements is required):
   *
   * <p>
   * <li/>atom:entry elements MUST contain one or more atom:author elements, unless the atom:entry
   *     contains an atom:source element that contains an atom:author element or, in an Atom Feed
   *     Document, the atom:feed element contains an atom:author element itself.
   *
   *     <p>
   * <li/>atom:entry elements MAY contain any number of atom:category elements.
   *
   *     <p>
   * <li/>atom:entry elements MUST NOT contain more than one atom:content element.
   *
   *     <p>
   * <li/>atom:entry elements MAY contain any number of atom:contributor elements.
   *
   *     <p>
   * <li/>atom:entry elements MUST contain exactly one atom:id element.
   *
   *     <p>
   * <li/>atom:entry elements that contain no child atom:content element MUST contain at least one
   *     atom:link element with a rel attribute value of "alternate".
   *
   *     <p>
   * <li/>atom:entry elements MUST NOT contain more than one atom:link element with a rel attribute
   *     value of "alternate" that has the same combination of type and hreflang attribute values.
   *
   *     <p>
   * <li/>atom:entry elements MAY contain additional atom:link elements beyond those described
   *     above.
   *
   *     <p>
   * <li/>atom:entry elements MUST NOT contain more than one atom:published element.
   *
   *     <p>
   * <li/>atom:entry elements MUST NOT contain more than one atom:rights element.
   *
   *     <p>
   * <li/>atom:entry elements MUST NOT contain more than one atom:source element.
   *
   *     <p>
   * <li/>atom:entry elements MUST contain an atom:summary element in either of the following cases:
   *
   *     <p>
   *
   *     <ul>
   *       the atom:entry contains an atom:content that has a "src" attribute (and is thus empty).
   * </ul>
   *
   * <p>
   *
   * <ul>
   *   the atom:entry contains content that is encoded in Base64; i.e., the "type" attribute of
   *   atom:content is a MIME media type [MIMEREG], but is not an XML media type [RFC3023], does not
   *   begin with "text/", and does not end with "/xml" or "+xml".
   * </ul>
   *
   * <p>
   * <li/>atom:entry elements MUST NOT contain more than one atom:summary element.
   *
   *     <p>
   * <li/>atom:entry elements MUST contain exactly one atom:title element.
   *
   *     <p>
   * <li/>atom:entry elements MUST contain exactly one atom:updated element."
   *
   * @throws CatalogTransformerException
   * @throws IOException
   * @throws SAXException
   * @throws XpathException
   */
  @Test
  public void testEntryElementsComplyToAtomSpecification()
      throws IOException, CatalogTransformerException, XpathException, SAXException {

    // given
    AtomTransformer transformer = new AtomTransformer();
    MetacardTransformer metacardTransformer = getXmlMetacardTransformerStub();
    transformer.setMetacardTransformer(metacardTransformer);

    setDefaultSystemConfiguration();

    SourceResponse response = mock(SourceResponse.class);

    when(response.getRequest()).thenReturn(getStubRequest());

    ResultImpl result1 = new ResultImpl();
    ResultImpl result2 = new ResultImpl();

    MetacardStub metacard = new MetacardStub("");
    metacard.setId(SAMPLE_ID);

    MetacardStub metacard2 = new MetacardStub("");
    metacard2.setId(SAMPLE_ID + 1);

    result1.setMetacard(metacard);
    result2.setMetacard(metacard2);

    when(response.getResults()).thenReturn(Arrays.asList((Result) result1, result2));

    // when
    BinaryContent binaryContent = transformer.transform(response, null);

    // then
    byte[] bytes = binaryContent.getByteArray();

    /* used to visualize */
    IOUtils.write(
        bytes, new FileOutputStream(new File(TARGET_FOLDER + getMethodName() + ATOM_EXTENSION)));

    String output = new String(bytes);

    assertEntryCompliant(output);
  }

  @Test
  public void testNoResourceActionProvider()
      throws CatalogTransformerException, IOException, XpathException, SAXException {

    // given
    MetacardTransformer metacardTransformer = getXmlMetacardTransformerStub();

    Action viewAction = mock(Action.class);
    when(viewAction.getUrl()).thenReturn(new URL("http://host:80/" + SAMPLE_ID));

    ActionProvider viewActionProvider = mock(ActionProvider.class);
    when(viewActionProvider.getAction(isA(Metacard.class))).thenReturn(viewAction);

    AtomTransformer transformer = new AtomTransformer();

    transformer.setViewMetacardActionProvider(viewActionProvider);

    transformer.setMetacardTransformer(metacardTransformer);

    setDefaultSystemConfiguration();

    SourceResponse response1 = mock(SourceResponse.class);

    when(response1.getHits()).thenReturn(new Long(1));

    when(response1.getRequest()).thenReturn(getStubRequest());

    ResultImpl result1 = new ResultImpl();

    MetacardStub metacard = new MetacardStub("");
    metacard.setId(SAMPLE_ID);
    metacard.setSourceId(SAMPLE_SOURCE_ID);
    metacard.setCreatedDate(SAMPLE_DATE_TIME.toDate());
    metacard.setModifiedDate(SAMPLE_DATE_TIME.toDate());

    result1.setMetacard(metacard);
    when(response1.getResults()).thenReturn(Arrays.asList((Result) result1));

    SourceResponse response = response1;
    Double relevanceScore = 0.3345;
    result1.setRelevanceScore(relevanceScore);

    // when
    BinaryContent binaryContent = transformer.transform(response, null);

    // then
    assertThat(binaryContent.getMimeType(), is(AtomTransformer.MIME_TYPE));

    byte[] bytes = binaryContent.getByteArray();

    /* used to visualize */
    IOUtils.write(
        bytes, new FileOutputStream(new File(TARGET_FOLDER + getMethodName() + ATOM_EXTENSION)));

    String output = new String(bytes);

    assertFeedCompliant(output);
    assertEntryCompliant(output);
    validateAgainstAtomSchema(bytes);

    /* feed */
    assertBasicFeedInfo(output, "1");

    /* entry */
    assertXpathEvaluatesTo(
        SAMPLE_SOURCE_ID, "/atom:feed/atom:entry/fs:resultSource/@fs:sourceId", output);
    assertXpathEvaluatesTo("", "/atom:feed/atom:entry/fs:resultSource", output);
    assertXpathEvaluatesTo(
        AtomTransformer.URN_CATALOG_ID + SAMPLE_ID, "/atom:feed/atom:entry/atom:id", output);
    assertXpathEvaluatesTo(MetacardStub.DEFAULT_TITLE, "/atom:feed/atom:entry/atom:title", output);
    assertXpathEvaluatesTo(
        Double.toString(relevanceScore), "/atom:feed/atom:entry/relevance:score", output);
    assertXpathExists("/atom:feed/atom:entry/atom:content", output);
    assertXpathEvaluatesTo(
        atomDateFormat.format(SAMPLE_DATE_TIME.toDate()),
        "/atom:feed/atom:entry/atom:published",
        output);
    assertXpathEvaluatesTo(
        atomDateFormat.format(SAMPLE_DATE_TIME.toDate()),
        "/atom:feed/atom:entry/atom:updated",
        output);
    assertXpathEvaluatesTo("application/xml", "/atom:feed/atom:entry/atom:content/@type", output);
    assertXpathEvaluatesTo(
        MetacardStub.DEFAULT_TYPE, "/atom:feed/atom:entry/atom:category/@term", output);
    assertXpathEvaluatesTo("1", "count(/atom:feed/atom:entry/georss:where)", output);
    assertXpathEvaluatesTo("1", "count(/atom:feed/atom:entry/georss:where/gml:Point)", output);
    assertXpathEvaluatesTo("56.3 13.3", "/atom:feed/atom:entry/georss:where/gml:Point", output);
    assertXpathEvaluatesTo("1", "count(/atom:feed/atom:entry/atom:link)", output);
    assertXpathExists("/atom:feed/atom:entry/atom:link[@rel='alternate']", output);
    assertXpathNotExists("/atom:feed/atom:entry/atom:link[@rel='related']", output);
    assertXpathEvaluatesTo(
        "http://host:80/" + SAMPLE_ID, "/atom:feed/atom:entry/atom:link/@href", output);
  }

  @Test
  public void testSimple()
      throws CatalogTransformerException, IOException, XpathException, SAXException {

    // given
    MetacardTransformer metacardTransformer = getXmlMetacardTransformerStub();

    Action viewAction = mock(Action.class);
    when(viewAction.getUrl()).thenReturn(new URL("http://host:80/" + SAMPLE_ID));

    ActionProvider viewActionProvider = mock(ActionProvider.class);
    when(viewActionProvider.getAction(isA(Metacard.class))).thenReturn(viewAction);

    ActionProvider resourceActionProvider = mock(ActionProvider.class);
    when(resourceActionProvider.getAction(isA(Metacard.class))).thenReturn(viewAction);

    ActionProvider thumbnailActionProvider = mock(ActionProvider.class);
    when(thumbnailActionProvider.getAction(isA(Metacard.class))).thenReturn(viewAction);

    AtomTransformer transformer = new AtomTransformer();

    transformer.setViewMetacardActionProvider(viewActionProvider);

    transformer.setResourceActionProvider(resourceActionProvider);

    transformer.setMetacardTransformer(metacardTransformer);

    transformer.setThumbnailActionProvider(thumbnailActionProvider);

    setDefaultSystemConfiguration();

    SourceResponse response1 = mock(SourceResponse.class);

    when(response1.getHits()).thenReturn(new Long(1));

    when(response1.getRequest()).thenReturn(getStubRequest());

    ResultImpl result1 = new ResultImpl();

    MetacardStub metacard = new MetacardStub("");
    metacard.setId(SAMPLE_ID);
    metacard.setSourceId(SAMPLE_SOURCE_ID);
    metacard.setCreatedDate(SAMPLE_DATE_TIME.toDate());
    metacard.setModifiedDate(SAMPLE_DATE_TIME.toDate());

    result1.setMetacard(metacard);
    when(response1.getResults()).thenReturn(Arrays.asList((Result) result1));

    SourceResponse response = response1;
    Double relevanceScore = 0.3345;
    result1.setRelevanceScore(relevanceScore);

    // when
    BinaryContent binaryContent = transformer.transform(response, null);

    // then
    assertThat(binaryContent.getMimeType(), is(AtomTransformer.MIME_TYPE));

    byte[] bytes = binaryContent.getByteArray();

    /* used to visualize */
    IOUtils.write(
        bytes, new FileOutputStream(new File(TARGET_FOLDER + getMethodName() + ATOM_EXTENSION)));

    String output = new String(bytes);

    assertFeedCompliant(output);
    assertEntryCompliant(output);
    validateAgainstAtomSchema(bytes);

    /* feed */
    assertBasicFeedInfo(output, "1");

    /* entry */
    assertXpathEvaluatesTo(
        SAMPLE_SOURCE_ID, "/atom:feed/atom:entry/fs:resultSource/@fs:sourceId", output);
    assertXpathEvaluatesTo("", "/atom:feed/atom:entry/fs:resultSource", output);
    assertXpathEvaluatesTo(
        AtomTransformer.URN_CATALOG_ID + SAMPLE_ID, "/atom:feed/atom:entry/atom:id", output);
    assertXpathEvaluatesTo(MetacardStub.DEFAULT_TITLE, "/atom:feed/atom:entry/atom:title", output);
    assertXpathEvaluatesTo(
        Double.toString(relevanceScore), "/atom:feed/atom:entry/relevance:score", output);
    assertXpathExists("/atom:feed/atom:entry/atom:content", output);
    assertXpathEvaluatesTo(
        atomDateFormat.format(SAMPLE_DATE_TIME.toDate()),
        "/atom:feed/atom:entry/atom:published",
        output);
    assertXpathEvaluatesTo(
        atomDateFormat.format(SAMPLE_DATE_TIME.toDate()),
        "/atom:feed/atom:entry/atom:updated",
        output);
    assertXpathEvaluatesTo("application/xml", "/atom:feed/atom:entry/atom:content/@type", output);
    assertXpathEvaluatesTo(
        MetacardStub.DEFAULT_TYPE, "/atom:feed/atom:entry/atom:category/@term", output);
    assertXpathEvaluatesTo("1", "count(/atom:feed/atom:entry/georss:where)", output);
    assertXpathEvaluatesTo("1", "count(/atom:feed/atom:entry/georss:where/gml:Point)", output);
    assertXpathEvaluatesTo("56.3 13.3", "/atom:feed/atom:entry/georss:where/gml:Point", output);
    assertXpathEvaluatesTo("3", "count(/atom:feed/atom:entry/atom:link)", output);
    assertXpathExists("/atom:feed/atom:entry/atom:link[@rel='alternate']", output);
    assertXpathExists("/atom:feed/atom:entry/atom:link[@rel='related']", output);
    assertXpathEvaluatesTo(
        "http://host:80/" + SAMPLE_ID, "/atom:feed/atom:entry/atom:link/@href", output);
  }

  /**
   * Smoke test for polygon
   *
   * @throws CatalogTransformerException
   * @throws IOException
   * @throws XpathException
   * @throws SAXException
   */
  @Test
  public void testGeo()
      throws CatalogTransformerException, IOException, XpathException, SAXException {

    AtomTransformer atomTransformer = new AtomTransformer();

    atomTransformer.setMetacardTransformer(getXmlMetacardTransformerStub());

    setDefaultSystemConfiguration();

    SourceResponse response =
        getSourceResponseStub(SAMPLE_ID, "POLYGON ((30 10, 10 20, 20 40, 40 40, 30 10))");

    byte[] bytes = atomTransformer.transform(response, null).getByteArray();

    String output = new String(bytes);

    assertFeedCompliant(output);
    assertEntryCompliant(output);
    validateAgainstAtomSchema(bytes);

    assertXpathEvaluatesTo("1", "count(//gml:Polygon)", output);
  }

  /**
   * @throws CatalogTransformerException
   * @throws IOException
   * @throws XpathException
   * @throws SAXException
   */
  @Test
  public void testNoGeo()
      throws CatalogTransformerException, IOException, XpathException, SAXException {

    AtomTransformer atomTransformer = new AtomTransformer();

    atomTransformer.setMetacardTransformer(getXmlMetacardTransformerStub());

    setDefaultSystemConfiguration();

    SourceResponse response = getSourceResponseStub(SAMPLE_ID, null);

    byte[] bytes = atomTransformer.transform(response, null).getByteArray();

    String output = new String(bytes);

    assertFeedCompliant(output);
    assertEntryCompliant(output);
    validateAgainstAtomSchema(bytes);

    assertXpathEvaluatesTo("0", "count(//gml:where)", output);
  }

  @Test
  public void testBadGeo()
      throws CatalogTransformerException, IOException, XpathException, SAXException {

    AtomTransformer atomTransformer = new AtomTransformer();

    atomTransformer.setMetacardTransformer(getXmlMetacardTransformerStub());

    setDefaultSystemConfiguration();

    SourceResponse response = getSourceResponseStub(SAMPLE_ID, BAD_WKT);

    byte[] bytes = atomTransformer.transform(response, null).getByteArray();

    String output = new String(bytes);

    /* used to visualize */
    IOUtils.write(
        bytes, new FileOutputStream(new File(TARGET_FOLDER + getMethodName() + ATOM_EXTENSION)));

    assertFeedCompliant(output);
    assertEntryCompliant(output);
    validateAgainstAtomSchema(bytes);

    assertXpathNotExists("//gml:Polygon", output);
    assertXpathNotExists("//georss:where", output);
  }

  @Test
  public void testZeroCount()
      throws CatalogTransformerException, IOException, XpathException, SAXException {
    AtomTransformer atomTransformer = new AtomTransformer();

    atomTransformer.setMetacardTransformer(getXmlMetacardTransformerStub());

    setDefaultSystemConfiguration();

    SourceResponse response = getSourceResponseStubZeroCount(SAMPLE_ID);

    byte[] bytes = atomTransformer.transform(response, null).getByteArray();

    String output = new String(bytes);

    /* used to visualize */
    IOUtils.write(
        bytes, new FileOutputStream(new File(TARGET_FOLDER + getMethodName() + ATOM_EXTENSION)));

    assertFeedCompliant(output);
    assertEntryCompliant(output);
    validateAgainstAtomSchema(bytes);

    assertXpathNotExists("/atom:feed/atom:entry", output);
  }

  private void validateAgainstAtomSchema(byte[] output) throws SAXException, IOException {

    Document document = parser.parse(new ByteArrayInputStream(output));

    try {
      validator.validate(new DOMSource(document));
    } catch (Exception e) {
      fail("Xml is not valid. " + e.getLocalizedMessage());
    }
  }

  private AtomTransformer getConfiguredAtomTransformer(
      MetacardTransformer metacardTransformer, boolean setProperties) {
    AtomTransformer transformer = new AtomTransformer();
    transformer.setMetacardTransformer(metacardTransformer);
    if (setProperties) {
      setDefaultSystemConfiguration();
    }
    return transformer;
  }

  private void assertBasicFeedInfo(String output, String totalResults)
      throws SAXException, IOException, XpathException {
    assertXpathEvaluatesTo(AtomTransformer.DEFAULT_FEED_TITLE, "/atom:feed/atom:title", output);
    assertXpathExists("/atom:feed/atom:updated", output);

    // check if the urn prefix has been added
    assertXpathEvaluatesTo(
        TRUE, "starts-with(/atom:feed/atom:id,'" + AtomTransformer.URN_UUID + "')", output);

    // check the valid length of a uuid
    assertXpathEvaluatesTo(
        "36",
        "string-length( substring( /atom:feed/atom:id, "
            + (AtomTransformer.URN_UUID.length() + 1)
            + " ) )",
        output);

    assertXpathEvaluatesTo(Link.REL_SELF, "/atom:feed/atom:link/@rel", output);
    assertXpathExists("/atom:feed/atom:link/@href", output);

    assertXpathEvaluatesTo(DEFAULT_TEST_ORGANIZATION, "/atom:feed/atom:author/atom:name", output);
    assertXpathEvaluatesTo(DEFAULT_TEST_VERSION, "/atom:feed/atom:generator/@version", output);
    assertXpathEvaluatesTo(DEFAULT_TEST_SITE, "/atom:feed/atom:generator", output);
    assertXpathEvaluatesTo(totalResults, "/atom:feed/os:totalResults", output);
    assertXpathEvaluatesTo("25", "/atom:feed/os:itemsPerPage", output);
    assertXpathEvaluatesTo("2", "/atom:feed/os:startIndex", output);
  }

  private SourceResponse basicSetup(AtomTransformer transformer)
      throws IOException, CatalogTransformerException {
    MetacardTransformer metacardTransformer = getXmlMetacardTransformerStub();
    transformer.setMetacardTransformer(metacardTransformer);

    setDefaultSystemConfiguration();

    SourceResponse response = mock(SourceResponse.class);

    when(response.getRequest()).thenReturn(getStubRequest());

    ResultImpl result = new ResultImpl();

    MetacardStub metacard = new MetacardStub("");
    metacard.setId(SAMPLE_ID);
    metacard.setSourceId(SAMPLE_SOURCE_ID);

    result.setMetacard(metacard);
    when(response.getResults()).thenReturn(Arrays.asList((Result) result));
    return response;
  }

  /**
   * The following rules must be followed in order to be compliant with the Atom specification as
   * defined by http://tools.ietf.org/html/rfc4287#section-4.1.1
   *
   * <p>"The following child elements are defined by this specification (note that the presence of
   * some of these elements is required):
   *
   * <p>
   * <li/>atom:feed elements MUST contain one or more atom:author elements, unless all of the
   *     atom:feed element's child atom:entry elements contain at least one atom:author element.
   *
   *     <p>
   * <li/>atom:feed elements MAY contain any number of atom:category elements.
   *
   *     <p>
   * <li/>atom:feed elements MAY contain any number of atom:contributor elements.
   *
   *     <p>
   * <li/>atom:feed elements MUST NOT contain more than one atom:generator element.
   *
   *     <p>
   * <li/>atom:feed elements MUST NOT contain more than one atom:icon element.
   *
   *     <p>
   * <li/>atom:feed elements MUST NOT contain more than one atom:logo element.
   *
   *     <p>
   * <li/>atom:feed elements MUST contain exactly one atom:id element.
   *
   *     <p>
   * <li/>atom:feed elements SHOULD contain one atom:link element with a rel attribute value of
   *     "self". This is the preferred URI for retrieving Atom Feed Documents representing this Atom
   *     feed.
   *
   *     <p>
   * <li/>atom:feed elements MUST NOT contain more than one atom:link element with a rel attribute
   *     value of "alternate" that has the same combination of type and hreflang attribute values.
   *
   *     <p>
   * <li/>atom:feed elements MAY contain additional atom:link elements beyond those described above.
   *
   *     <p>
   * <li/>atom:feed elements MUST NOT contain more than one atom:rights element.
   *
   *     <p>
   * <li/>atom:feed elements MUST NOT contain more than one atom:subtitle element.
   *
   *     <p>
   * <li/>atom:feed elements MUST contain exactly one atom:title element.
   *
   *     <p>
   * <li/>atom:feed elements MUST contain exactly one atom:updated element."
   *
   * @throws IOException
   * @throws SAXException
   * @throws XpathException
   */
  private void assertFeedCompliant(String output) throws IOException, SAXException, XpathException {
    assertXpathExists("/atom:feed[atom:author or not(atom:entry[not(atom:author)])] ", output);
    assertXpathEvaluatesTo(TRUE, "count(/atom:feed/atom:generator) <= 1", output);
    assertXpathEvaluatesTo(TRUE, "count(/atom:feed/atom:icon) <= 1", output);
    assertXpathEvaluatesTo(TRUE, "count(/atom:feed/atom:logo) <= 1", output);
    assertXpathEvaluatesTo(TRUE, "count(/atom:feed/atom:id) = 1", output);
    assertXpathEvaluatesTo(TRUE, "count(/atom:feed/atom:link[@rel='self']) = 1", output);
    /*
     * If necessary, add test for "atom:feed elements MUST NOT contain more than one atom:link
     * element with a rel attribute value of "alternate" that has the same combination of type
     * and hreflang attribute values."
     */
    assertXpathEvaluatesTo(TRUE, "count(/atom:feed/atom:rights) <= 1", output);
    assertXpathEvaluatesTo(TRUE, "count(/atom:feed/atom:subtitle) <= 1", output);
    assertXpathEvaluatesTo(TRUE, "count(/atom:feed/atom:title) = 1", output);
    assertXpathEvaluatesTo(TRUE, "count(/atom:feed/atom:updated) = 1", output);
  }

  private void assertEntryCompliant(String output)
      throws SAXException, IOException, XpathException {

    assertXpathEvaluatesTo(
        TRUE,
        "count(/atom:feed/atom:entry[atom:author] | //atom:entry[atom:source] | /atom:feed[atom:author]) > 0",
        output);
    assertXpathNotExists("/atom:feed/atom:entry[count(atom:content) > 1] ", output);
    assertXpathNotExists("/atom:feed/atom:entry[count(atom:id) != 1 ]", output);
    // "atom:entry elements MUST NOT contain more than
    // one atom:link element with a rel attribute value of "alternate" that
    // has the same combination of type and hreflang attribute values."
    assertXpathNotExists(
        "/atom:feed/atom:entry[ not(atom:content or atom:link[not(@rel)] or atom:link[@rel='alternate']) ]",
        output);

    assertXpathNotExists("/atom:feed/atom:entry[count(atom:published) > 1]", output);
    assertXpathNotExists("/atom:feed/atom:entry[count(atom:rights) > 1]", output);
    assertXpathNotExists("/atom:feed/atom:entry[count(atom:source) > 1]", output);
    assertXpathNotExists("/atom:feed/atom:entry[count(atom:summary) > 1]", output);
    // Test must be added for this reqt:
    // "atom:entry elements MUST contain an atom:summary element in either
    // of the following cases:
    // * the atom:entry contains an atom:content that has a "src"
    // attribute (and is thus empty).
    // * the atom:entry contains content that is encoded in Base64;
    // i.e., the "type" attribute of atom:content is a MIME media type
    // [MIMEREG], but is not an XML media type [RFC3023], does not
    // begin with "text/", and does not end with "/xml" or "+xml"."
    assertXpathNotExists("/atom:feed/atom:entry[count(atom:title) != 1]", output);
    assertXpathNotExists("/atom:feed/atom:entry[count(atom:updated) != 1]", output);

    // OpenSearch Specific
    assertXpathNotExists("/atom:feed[os:itemsPerPage < 0]", output);
    assertXpathNotExists("/atom:feed[os:totalResults < 0]", output);
  }

  private SourceResponse getSourceResponseStub(String id, String wkt) {
    SourceResponse response = mock(SourceResponse.class);

    when(response.getHits()).thenReturn(new Long(1));

    when(response.getRequest()).thenReturn(getStubRequest());

    ResultImpl result = new ResultImpl();

    MetacardStub metacard = new MetacardStub("");

    metacard.setId(id);

    metacard.setLocation(wkt);

    result.setMetacard(metacard);

    when(response.getResults()).thenReturn(Arrays.asList((Result) result));
    return response;
  }

  private SourceResponse getSourceResponseStubZeroCount(String id) {
    SourceResponse response = mock(SourceResponse.class);

    when(response.getHits()).thenReturn(new Long(1));

    Map<String, Serializable> properties = new HashMap<>();
    properties.put("count", "0");

    QueryRequest queryRequest = mock(QueryRequest.class);

    when(queryRequest.getProperties()).thenReturn(properties);

    when(response.getRequest()).thenReturn(queryRequest);

    ResultImpl result = new ResultImpl();

    MetacardStub metacard = new MetacardStub("");

    metacard.setId(id);

    result.setMetacard(metacard);

    when(response.getResults()).thenReturn(Arrays.asList((Result) result));
    return response;
  }

  private MetacardTransformer getXmlMetacardTransformerStub()
      throws IOException, CatalogTransformerException {
    MetacardTransformer metacardTransformer = mock(MetacardTransformer.class);
    BinaryContent metacardTransformation = mock(BinaryContent.class);
    when(metacardTransformation.getByteArray())
        .thenReturn(
            "<sample:note xmlns:sample=\"http://www.lockheedmartin.com/schema/sample\"><to>me</to><from>you</from></sample:note>"
                .getBytes());
    when(metacardTransformer.transform(isA(Metacard.class), isA(Map.class)))
        .thenReturn(metacardTransformation);
    return metacardTransformer;
  }

  private void setDefaultSystemConfiguration() {
    System.setProperty(SystemInfo.ORGANIZATION, DEFAULT_TEST_ORGANIZATION);
    System.setProperty(SystemInfo.SITE_NAME, DEFAULT_TEST_SITE);
    System.setProperty(SystemInfo.VERSION, DEFAULT_TEST_VERSION);
  }

  private String getMethodName() {

    return Thread.currentThread().getStackTrace()[2].getMethodName();
  }

  private QueryRequest getStubRequest() {

    QueryImpl query = new QueryImpl(FILTER_BUILDER.attribute(Metacard.METADATA).text("you"));
    query.setPageSize(25);
    query.setStartIndex(2);
    query.setRequestsTotalResultsCount(true);

    return new QueryRequestImpl(query);
  }
}
