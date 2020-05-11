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
package ddf.catalog.transform.xml;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathNotExists;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transformer.api.MetacardMarshaller;
import ddf.catalog.transformer.api.PrintWriterProvider;
import ddf.catalog.transformer.xml.MetacardMarshallerImpl;
import ddf.catalog.transformer.xml.PrintWriterProviderImpl;
import ddf.catalog.transformer.xml.XmlResponseQueueTransformer;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.xml.XmlParser;
import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParserException;

/** Tests the {@link XmlResponseQueueTransformer} transformations */
public class XmlResponseQueueTransformerTest {

  private static final String DEFAULT_ID = "myID";

  private boolean verboseDebug = false;

  private static final String DEFAULT_TYPE_NAME = MetacardImpl.BASIC_METACARD.getName();

  private static final Date DEFAULT_EXPIRATION_DATE = new DateTime(123456789).toDate();

  private static final String DEFAULT_TITLE = "myTitle";

  private static final String DEFAULT_GEO =
      "POLYGON ((35 10, 10 20, 15 40, 45 45, 35 10),(20 30, 35 35, 30 20, 20 30))";

  private static final String DEFAULT_METADATA =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><foo><bar/></foo>";

  private static final byte[] DEFAULT_BYTES = new byte[] {0, 0, 1};

  private static final String DEFAULT_BASE64 = "AAAB";

  private static final Logger LOGGER =
      LoggerFactory.getLogger(XmlResponseQueueTransformerTest.class);

  private static final String DEFAULT_SOURCE_ID = "mySourceId";

  private XmlResponseQueueTransformer transformer;

  private MimeType mimeType;

  private Parser parser;

  @BeforeClass
  public static void setupTestClass() {

    // makes xpaths easier to write when prefixes are declared beforehand.
    HashMap<String, String> map = new HashMap<>();
    map.put("gml", "http://www.opengis.net/gml");
    map.put("mc", "urn:catalog:metacard");

    NamespaceContext ctx = new SimpleNamespaceContext(map);
    XMLUnit.setXpathNamespaceContext(ctx);
  }

  @Before
  public void setup() throws MimeTypeParseException {
    parser = new XmlParser();
    PrintWriterProvider printWriterProvider = new PrintWriterProviderImpl();
    MetacardMarshaller metacardMarshaller = new MetacardMarshallerImpl(parser, printWriterProvider);
    mimeType = getMimeType();
    transformer =
        new XmlResponseQueueTransformer(parser, printWriterProvider, metacardMarshaller, mimeType);
  }

  /** @throws CatalogTransformerException */
  @Test
  public void testEmptySourceResponse()
      throws CatalogTransformerException, IOException, XpathException, SAXException {

    // given
    transformer.setThreshold(-1);

    SourceResponse response = new SourceResponseImpl(null, Collections.<Result>emptyList());

    // when
    BinaryContent binaryContent = transformer.transform(response, null);

    // then
    String output = new String(binaryContent.getByteArray());
    LOGGER.info(output);
    assertXpathEvaluatesTo("", "/mc:metacards", output);
  }

  /**
   * Should throw exception when given {@code null} input
   *
   * @throws CatalogTransformerException
   */
  @Test(expected = CatalogTransformerException.class)
  public void testNullSourceResponse() throws CatalogTransformerException {

    // given
    transformer.setThreshold(2);

    // when
    transformer.transform(null, null);

    // then
    // failure should occur

  }

  /**
   * No {@link MetacardType} name should use the default name.
   *
   * @throws CatalogTransformerException
   * @throws IOException
   * @throws SAXException
   * @throws XpathException
   */
  @Test
  public void testMetacardTypeNameNull()
      throws CatalogTransformerException, IOException, XpathException, SAXException {

    // given
    transformer.setThreshold(2);

    SourceResponse response = givenMetacardTypeName(null);

    // when
    BinaryContent binaryContent = transformer.transform(response, null);

    // then
    assertThat(binaryContent.getMimeType(), is(mimeType));

    byte[] bytes = binaryContent.getByteArray();

    String output = new String(bytes);

    print(output, verboseDebug);

    assertXpathEvaluatesTo(DEFAULT_TYPE_NAME, "/mc:metacards/mc:metacard/mc:type", output);
  }

  /**
   * No {@link MetacardType} name should use the default name.
   *
   * @throws CatalogTransformerException
   * @throws IOException
   * @throws SAXException
   * @throws XpathException
   */
  @Test
  public void testMetacardTypeNameEmpty()
      throws CatalogTransformerException, IOException, XpathException, SAXException {

    // given
    transformer.setThreshold(2);

    SourceResponse response = givenMetacardTypeName("");

    // when
    BinaryContent binaryContent = transformer.transform(response, null);

    // then
    assertThat(binaryContent.getMimeType(), is(mimeType));

    byte[] bytes = binaryContent.getByteArray();

    String output = new String(bytes);

    print(output, verboseDebug);

    assertXpathEvaluatesTo(DEFAULT_TYPE_NAME, "/mc:metacards/mc:metacard/mc:type", output);
  }

  @Test
  public void testNoIdNoSourceId()
      throws CatalogTransformerException, IOException, XpathException, SAXException {

    // given
    transformer.setThreshold(2);

    SourceResponse response = givenSourceResponse(null, null);

    // when
    BinaryContent binaryContent = transformer.transform(response, null);

    // then
    assertThat(binaryContent.getMimeType(), is(mimeType));

    byte[] bytes = binaryContent.getByteArray();

    String output = new String(bytes);

    print(output, verboseDebug);

    assertXpathNotExists("/mc:metacards/mc:metacard/mc:source", output);

    assertXpathNotExists("/mc:metacards/mc:metacard/@gml:id", output);

    verifyDefaults("1", output);
  }

  @Test
  public void testNoId()
      throws CatalogTransformerException, IOException, XpathException, SAXException {

    // given
    transformer.setThreshold(2);

    SourceResponse response = givenSourceResponse(DEFAULT_SOURCE_ID, null);

    // when
    BinaryContent binaryContent = transformer.transform(response, null);

    // then
    assertThat(binaryContent.getMimeType(), is(mimeType));

    byte[] bytes = binaryContent.getByteArray();

    String output = new String(bytes);

    print(output, verboseDebug);

    assertXpathEvaluatesTo(DEFAULT_SOURCE_ID, "/mc:metacards/mc:metacard/mc:source", output);

    assertXpathNotExists("/mc:metacards/mc:metacard/@gml:id", output);

    verifyDefaults("1", output);
  }

  @Test
  public void testStub()
      throws CatalogTransformerException, IOException, XpathException, SAXException {

    // given
    transformer.setThreshold(2);

    SourceResponse response = givenSourceResponse(DEFAULT_SOURCE_ID, DEFAULT_ID);

    // when
    BinaryContent binaryContent = transformer.transform(response, null);

    // then
    assertThat(binaryContent.getMimeType(), is(mimeType));

    byte[] bytes = binaryContent.getByteArray();

    String output = new String(bytes);

    print(output, verboseDebug);

    assertXpathEvaluatesTo(DEFAULT_SOURCE_ID, "/mc:metacards/mc:metacard/mc:source", output);

    assertXpathEvaluatesTo(DEFAULT_ID, "/mc:metacards/mc:metacard/@gml:id", output);

    verifyDefaults("1", output);
  }

  @Test
  public void testMultiple()
      throws CatalogTransformerException, IOException, XpathException, SAXException {
    // given
    transformer.setThreshold(2);

    SourceResponse response =
        givenSourceResponse(
            new MetacardStub("source1", "id1"),
            new MetacardStub("source2", "id2"),
            new MetacardStub("source3", "id3"));

    // when
    BinaryContent binaryContent = transformer.transform(response, null);

    // then
    assertThat(binaryContent.getMimeType(), is(mimeType));

    byte[] bytes = binaryContent.getByteArray();

    String output = new String(bytes);

    print(output, verboseDebug);

    assertXpathEvaluatesTo("source1", "/mc:metacards/mc:metacard[1]/mc:source", output);

    assertXpathEvaluatesTo("id1", "/mc:metacards/mc:metacard[1]/@gml:id", output);

    assertXpathEvaluatesTo("source2", "/mc:metacards/mc:metacard[2]/mc:source", output);

    assertXpathEvaluatesTo("id2", "/mc:metacards/mc:metacard[2]/@gml:id", output);

    assertXpathEvaluatesTo("source3", "/mc:metacards/mc:metacard[3]/mc:source", output);

    assertXpathEvaluatesTo("id3", "/mc:metacards/mc:metacard[3]/@gml:id", output);

    assertXpathEvaluatesTo("3", "count(/mc:metacards/mc:metacard)", output);

    verifyDefaults("1", output);
    verifyDefaults("2", output);
    verifyDefaults("3", output);
  }

  @Test
  public void testCompareSerialToFork()
      throws IOException, CatalogTransformerException, MimeTypeParseException {
    SourceResponse response =
        givenSourceResponse(
            new MetacardStub("source1", "id1"),
            new MetacardStub("source2", "id2"),
            new MetacardStub("source3", "id3"),
            new MetacardStub("source4", "id4"));

    PrintWriterProvider pwp = new PrintWriterProviderImpl();
    MetacardMarshaller mcm = new MetacardMarshallerImpl(parser, pwp);

    XmlResponseQueueTransformer serialXform =
        new XmlResponseQueueTransformer(parser, pwp, mcm, getMimeType());
    serialXform.setThreshold(2);

    XmlResponseQueueTransformer forkXForm =
        new XmlResponseQueueTransformer(parser, pwp, mcm, getMimeType());
    forkXForm.setThreshold(10);

    BinaryContent serialBc = serialXform.transform(response, null);
    BinaryContent forkBc = forkXForm.transform(response, null);

    String serialOutput = new String(serialBc.getByteArray());
    String forkOutput = new String(forkBc.getByteArray());

    // There are expected whitespace differences between the outputs.
    // This is an overly aggressive conversion; a better test would be to unmarshal the
    // xml metacards back into Metacard instances and compare equality.
    assertEquals(serialOutput.replaceAll("\\s", ""), forkOutput.replaceAll("\\s", ""));
  }

  @Test
  public void testXmlResponseQueueTransformer() throws Exception {

    MetacardImpl mc = new MetacardImpl();

    final String testId = "1234567890987654321";
    final String testSource = "FooBarSource";
    final String testTitle = "Title!";
    final Date testDate = new Date();
    final String testLocation =
        "POLYGON ((35 10, 10 20, 15 40, 45 45, 35 10),(20 30, 35 35, 30 20, 20 30))";
    final byte[] testThumbnail = {
      0, 0, 1, 0, 0, 1, 1, 1, 0, 1, 0, 0, 1, 1, 1, 0, 1, 0, 0, 1, 1, 1, 0, 1, 0, 0, 1, 1, 1, 0, 1,
      0, 0, 1, 1, 1, 0, 1, 0, 0, 1, 1, 1, 0, 1, 0, 0, 1, 1, 1
    };

    mc.setId(testId);
    mc.setSourceId(testSource);
    mc.setTitle(testTitle);
    mc.setExpirationDate(testDate);
    mc.setLocation(testLocation);
    mc.setThumbnail(testThumbnail);

    String metadata;
    try (FileInputStream stream =
        new FileInputStream(new File("src/test/resources/extensibleMetacard.xml"))) {
      FileChannel fc = stream.getChannel();
      MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
      /* Instead of using default, pass in a decoder. */
      metadata = Charset.defaultCharset().decode(bb).toString();
    }

    mc.setMetadata(metadata);

    Metacard mci = mc;

    transformer.setThreshold(2);

    SourceResponse response =
        new SourceResponseImpl(null, Arrays.asList((Result) new ResultImpl(mci)));
    BinaryContent bc = transformer.transform(response, null);

    if (bc == null) {
      fail("Binary Content is null.");
    }

    String outputXml = new String(bc.getByteArray());

    LOGGER.debug(outputXml);

    assertXpathEvaluatesTo(testId, "/mc:metacards/mc:metacard/@gml:id", outputXml);
    assertXpathEvaluatesTo(testSource, "/mc:metacards/mc:metacard/mc:source", outputXml);
    assertXpathEvaluatesTo(
        testTitle, "/mc:metacards/mc:metacard/mc:string[@name='title']/mc:value", outputXml);

    // TODO convert GML representation?
    // outputXml);
    assertXpathExists(
        "/mc:metacards/mc:metacard/mc:geometry[@name='location']/mc:value", outputXml);

    assertXpathExists(
        "/mc:metacards/mc:metacard/mc:base64Binary[@name='thumbnail']/mc:value", outputXml);

    // TODO XML Date representation?
    assertXpathExists(
        "/mc:metacards/mc:metacard/mc:dateTime[@name='expiration']/mc:value", outputXml);
  }

  @Test(expected = ExceptionInInitializerError.class)
  public void testMimeTypeInitException()
      throws IOException, CatalogTransformerException, XmlPullParserException,
          MimeTypeParseException {
    SourceResponse response = givenSourceResponse(new MetacardStub("source1", "id1"));

    PrintWriterProvider pwp = new PrintWriterProviderImpl();
    MetacardMarshaller mockMetacardMarshaller = mock(MetacardMarshaller.class);

    MimeType mockMimeType = mock(MimeType.class);

    doThrow(new MimeTypeParseException("")).when(mockMimeType).setSubType(anyString());

    XmlResponseQueueTransformer xrqt =
        new XmlResponseQueueTransformer(parser, pwp, mockMetacardMarshaller, mockMimeType);
    xrqt.setThreshold(2);

    xrqt.transform(response, null);

    // then exception
  }

  @Test(expected = CatalogTransformerException.class)
  public void testMetacardMarshallThrowsXmlPullParserException()
      throws IOException, CatalogTransformerException, XmlPullParserException,
          MimeTypeParseException {
    SourceResponse response = givenSourceResponse(new MetacardStub("source1", "id1"));

    PrintWriterProvider pwp = new PrintWriterProviderImpl();
    MetacardMarshaller mockMetacardMarshaller = mock(MetacardMarshaller.class);

    when(mockMetacardMarshaller.marshal(any(Metacard.class), any(Map.class)))
        .thenThrow(new XmlPullParserException(""));

    XmlResponseQueueTransformer xrqt =
        new XmlResponseQueueTransformer(parser, pwp, mockMetacardMarshaller, getMimeType());
    xrqt.setThreshold(2);

    xrqt.transform(response, null);

    // then exception
  }

  /** @return */
  private MetacardType getMetacardTypeStub(String name, Set<AttributeDescriptor> descriptors) {

    MetacardType type = mock(MetacardType.class);

    when(type.getName()).thenReturn(name);

    when(type.getAttributeDescriptors()).thenReturn(descriptors);

    return type;
  }

  /**
   * @param index TODO
   * @param output
   * @throws IOException
   * @throws SAXException
   * @throws XpathException
   */
  private void verifyDefaults(String index, String output)
      throws IOException, SAXException, XpathException {
    assertXpathEvaluatesTo(
        DEFAULT_TYPE_NAME, "/mc:metacards/mc:metacard[" + index + "]/mc:type", output);
    assertXpathExists(
        "/mc:metacards/mc:metacard[" + index + "]/mc:geometry[@name='location']//gml:Polygon",
        output);
    assertXpathExists(
        "/mc:metacards/mc:metacard[" + index + "]/mc:dateTime[@name='expiration']", output);
    assertXpathExists(
        "/mc:metacards/mc:metacard[" + index + "]/mc:stringxml[@name='metadata']", output);
    assertXpathEvaluatesTo(
        DEFAULT_TITLE,
        "/mc:metacards/mc:metacard[" + index + "]/mc:string[@name='title']/mc:value",
        output);
    assertXpathEvaluatesTo(
        DEFAULT_BASE64,
        "/mc:metacards/mc:metacard[" + index + "]/mc:base64Binary[@name='thumbnail']/mc:value",
        output);
  }

  /** @return */
  private SourceResponseImpl givenSourceResponse(String sourceId, String id) {
    return new SourceResponseImpl(
        null, Arrays.asList((Result) new ResultImpl(new MetacardStub(sourceId, id))));
  }

  private SourceResponseImpl givenSourceResponse(Metacard... metacards) {

    List<Result> results = new ArrayList<Result>();
    for (Metacard m : metacards) {
      results.add(new ResultImpl(m));
    }

    return new SourceResponseImpl(null, results);
  }

  private SourceResponse givenMetacardTypeName(String metacardTypeName) {
    MetacardType type = getMetacardTypeStub(metacardTypeName, new HashSet<AttributeDescriptor>());

    Metacard metacard = new MetacardImpl(type);

    SourceResponse response =
        new SourceResponseImpl(null, Arrays.asList((Result) new ResultImpl(metacard)));
    return response;
  }

  private void print(String output, boolean inFull) {
    if (inFull) {
      LOGGER.debug(output);
    }
  }

  private MimeType getMimeType() throws MimeTypeParseException {
    MimeType mimeType = new MimeType();
    mimeType.setPrimaryType("text");
    mimeType.setSubType("xml");
    return mimeType;
  }

  class MetacardStub extends MetacardImpl {

    public MetacardStub(String sourceId, String id) {

      super(new MetacardImpl());
      setSourceId(sourceId);
      setId(id);

      setTitle(DEFAULT_TITLE);
      setExpirationDate(DEFAULT_EXPIRATION_DATE);
      setLocation(DEFAULT_GEO);
      setMetadata(DEFAULT_METADATA);
      setThumbnail(DEFAULT_BYTES);
    }
  }
}
