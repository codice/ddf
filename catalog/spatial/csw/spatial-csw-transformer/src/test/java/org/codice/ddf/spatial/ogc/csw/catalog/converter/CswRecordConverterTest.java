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
package org.codice.ddf.spatial.ogc.csw.catalog.converter;

import static net.opengis.cat.csw.v_2_0_2.ElementSetType.BRIEF;
import static net.opengis.cat.csw.v_2_0_2.ElementSetType.FULL;
import static net.opengis.cat.csw.v_2_0_2.ElementSetType.SUMMARY;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.DataHolder;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.core.TreeMarshaller;
import com.thoughtworks.xstream.core.TreeUnmarshaller;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.xml.DomReader;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import com.thoughtworks.xstream.io.xml.WstxDriver;
import com.thoughtworks.xstream.io.xml.XppDriver;
import com.thoughtworks.xstream.io.xml.XppReader;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.data.impl.types.AssociationsAttributes;
import ddf.catalog.data.impl.types.ContactAttributes;
import ddf.catalog.data.impl.types.LocationAttributes;
import ddf.catalog.data.impl.types.MediaAttributes;
import ddf.catalog.data.impl.types.TopicAttributes;
import ddf.catalog.data.types.Contact;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.Media;
import ddf.catalog.data.types.Topic;
import ddf.catalog.transform.CatalogTransformerException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.activation.MimeTypeParseException;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import net.opengis.cat.csw.v_2_0_2.ElementSetType;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class CswRecordConverterTest {

  private static final transient Logger LOGGER =
      LoggerFactory.getLogger(CswRecordConverterTest.class);

  private static final DatatypeFactory XSD_FACTORY;

  private static final GregorianCalendar CREATED_DATE = new GregorianCalendar(2014, 10, 1);

  private static final GregorianCalendar MODIFIED_DATE = new GregorianCalendar(2016, 10, 1);

  private static final GregorianCalendar EFFECTIVE_DATE = new GregorianCalendar(2015, 10, 1);

  private static final String THUMBNAIL_URL = "THUMBNAIL_URL";

  private static final String ACTION_URL = "http://example.com/source/id?transform=resource";

  private static String modified;

  private static String effective;

  private static String created;

  private static CswRecordConverter converter;

  private static String cswRecordXml;

  static {
    DatatypeFactory factory = null;
    try {
      factory = DatatypeFactory.newInstance();
    } catch (DatatypeConfigurationException e) {
      LOGGER.error("Failed to create xsdFactory: {}", e.getMessage());
    }
    XSD_FACTORY = factory;
  }

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    modified = XSD_FACTORY.newXMLGregorianCalendar(MODIFIED_DATE).toXMLFormat();
    effective = XSD_FACTORY.newXMLGregorianCalendar(EFFECTIVE_DATE).toXMLFormat();
    created = XSD_FACTORY.newXMLGregorianCalendar(CREATED_DATE).toXMLFormat();

    converter = new CswRecordConverter(getCswMetacardType());

    cswRecordXml =
        IOUtils.toString(
            CswRecordConverterTest.class.getResourceAsStream("/Csw_Record_Text.xml"),
            StandardCharsets.UTF_8);
  }

  @Test
  public void testMimeType() throws MimeTypeParseException {
    assertThat(
        converter.XML_MIME_TYPE.match(
            com.google.common.net.MediaType.APPLICATION_XML_UTF_8.toString()),
        is(true));
  }

  @Test
  public void testMarshalNullInput() throws CatalogTransformerException {
    Metacard expectedMetacard = getTestMetacard();
    BinaryContent bc = converter.transform(expectedMetacard, null);
    assertThat(bc, notNullValue());
  }

  @Test
  public void testMarshalEmptyInput() throws CatalogTransformerException {
    Metacard expectedMetacard = getTestMetacard();
    BinaryContent bc = converter.transform(expectedMetacard, new HashMap<>());
    assertThat(bc, notNullValue());
  }

  @Test
  public void testUnmarshalNoNamespaceDeclaration() throws IOException, SAXException {
    XStream xstream = new XStream(new XppDriver());

    xstream.registerConverter(converter);

    xstream.alias("Record", MetacardImpl.class);
    xstream.alias("csw:Record", MetacardImpl.class);
    InputStream is = IOUtils.toInputStream(getRecordNoNamespaceDeclaration());
    Metacard mc = (Metacard) xstream.fromXML(is);

    Metacard expectedMetacard = getTestMetacard();
    assertThat(mc, notNullValue());
    assertThat(mc.getContentTypeName(), is(expectedMetacard.getContentTypeName()));
    assertThat(mc.getCreatedDate(), is(expectedMetacard.getCreatedDate()));
    assertThat(mc.getEffectiveDate(), is(expectedMetacard.getEffectiveDate()));
    assertThat(mc.getId(), is(expectedMetacard.getId()));
    assertThat(mc.getModifiedDate(), is(expectedMetacard.getModifiedDate()));
    assertThat(mc.getTitle(), is(expectedMetacard.getTitle()));
    assertThat(mc.getResourceURI(), is(expectedMetacard.getResourceURI()));
  }

  @Test
  public void testUnmarshalWriteNamespaces()
      throws IOException, SAXException, XmlPullParserException {
    XStream xstream = new XStream(new XppDriver());

    xstream.registerConverter(converter);

    xstream.alias("Record", MetacardImpl.class);
    xstream.alias("csw:Record", MetacardImpl.class);
    InputStream is = IOUtils.toInputStream(getRecordNoNamespaceDeclaration());

    HierarchicalStreamReader reader =
        new XppReader(
            new InputStreamReader(is), XmlPullParserFactory.newInstance().newPullParser());
    DataHolder args = xstream.newDataHolder();
    Map<String, String> namespaces = new HashMap<>();
    namespaces.put(
        CswConstants.XMLNS + CswConstants.NAMESPACE_DELIMITER + CswConstants.CSW_NAMESPACE_PREFIX,
        CswConstants.CSW_OUTPUT_SCHEMA);
    namespaces.put(
        CswConstants.XMLNS
            + CswConstants.NAMESPACE_DELIMITER
            + CswConstants.DUBLIN_CORE_NAMESPACE_PREFIX,
        CswConstants.DUBLIN_CORE_SCHEMA);
    namespaces.put(
        CswConstants.XMLNS
            + CswConstants.NAMESPACE_DELIMITER
            + CswConstants.DUBLIN_CORE_TERMS_NAMESPACE_PREFIX,
        CswConstants.DUBLIN_CORE_TERMS_SCHEMA);
    namespaces.put(
        CswConstants.XMLNS + CswConstants.NAMESPACE_DELIMITER + CswConstants.OWS_NAMESPACE_PREFIX,
        CswConstants.OWS_NAMESPACE);
    args.put(CswConstants.NAMESPACE_DECLARATIONS, namespaces);
    Metacard mc = (Metacard) xstream.unmarshal(reader, null, args);

    Metacard expectedMetacard = getTestMetacard();
    assertThat(mc, notNullValue());
    assertThat(mc.getContentTypeName(), is(mc.getContentTypeName()));
    assertThat(mc.getCreatedDate(), is(expectedMetacard.getCreatedDate()));
    assertThat(mc.getEffectiveDate(), is(expectedMetacard.getEffectiveDate()));
    assertThat(mc.getId(), is(expectedMetacard.getId()));
    assertThat(mc.getModifiedDate(), is(expectedMetacard.getModifiedDate()));
    assertThat(mc.getTitle(), is(expectedMetacard.getTitle()));
    assertThat(mc.getResourceURI(), is(expectedMetacard.getResourceURI()));
    XMLUnit.setIgnoreWhitespace(true);
    assertXMLEqual(mc.getMetadata(), getControlRecord());
  }

  @Test
  public void testUnmarshalSingleCswRecordToMetacardContentTypeMapsToFormat()
      throws ParserConfigurationException, IOException, SAXException {
    XStream xstream = new XStream(new WstxDriver());
    xstream.registerConverter(converter);
    xstream.alias("csw:Record", MetacardImpl.class);
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
    Document doc =
        docBuilder.parse(CswRecordConverterTest.class.getResource("/Csw_Record.xml").getPath());
    HierarchicalStreamReader reader = new DomReader(doc);
    DataHolder holder = xstream.newDataHolder();

    Metacard mc = (Metacard) xstream.unmarshal(reader, null, holder);

    assertThat(mc, notNullValue());
    assertThat(mc.getContentTypeName(), is("IMAGE-PRODUCT"));
    assertThat(mc.getAttribute(Media.FORMAT).getValue(), is("PDF"));
  }

  @Test
  public void testUnmarshalCswRecordGeometryToMetacard() {
    XStream xstream = new XStream(new WstxDriver());

    xstream.registerConverter(converter);

    xstream.alias("Record", MetacardImpl.class);
    InputStream is =
        CswRecordConverterTest.class.getResourceAsStream("/Csw_Record_with_Geometry.xml");
    Metacard mc = (Metacard) xstream.fromXML(is);

    assertThat(mc, notNullValue());
    assertThat(mc.getLocation(), notNullValue());
  }

  /**
   * CSW Record has multiple elements of same name and it is an element that had to be uniquely
   * qualified between CSW and basic Metacard, e.g., "title" vs. "csw.title"
   */
  @Test
  public void testUnmarshalCswRecordMultipleTitles() {
    XStream xstream = new XStream(new WstxDriver());

    xstream.registerConverter(converter);

    xstream.alias("Record", MetacardImpl.class);
    InputStream is =
        CswRecordConverterTest.class.getResourceAsStream("/Csw_Record_MultiValueFields.xml");
    Metacard mc = (Metacard) xstream.fromXML(is);

    assertThat(mc, notNullValue());
    LOGGER.debug("Metacard title = {}", mc.getAttribute(Core.TITLE).getValue());
    assertThat(mc.getTitle(), is("Second title"));
  }

  @Test
  public void testUnmarshalCswRecordMultipleResourceUri() {
    XStream xstream = new XStream(new WstxDriver());

    xstream.registerConverter(converter);

    xstream.alias("Record", MetacardImpl.class);
    InputStream is =
        CswRecordConverterTest.class.getResourceAsStream("/Csw_Record_MultiValueFields.xml");
    Metacard mc = (Metacard) xstream.fromXML(is);
    assertThat(mc.getResourceURI().toString(), is("http://example.com/product_supplement.pdf"));
  }

  /** Verifies that Zulu time zone is valid in ISO 8601 date. */
  @Test
  public void testConvertISODateMetacardAttribute() {
    String dateStr = "2013-05-03T17:25:04Z";
    Serializable ser =
        CswUnmarshallHelper.convertStringValueToMetacardValue(AttributeFormat.DATE, dateStr);
    assertThat(ser, notNullValue());
    assertThat(Date.class.isAssignableFrom(ser.getClass()), is(true));
    Date date = (Date) ser;
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    cal.setTime(date);
    assertThat(cal.get(Calendar.MONTH), is(Calendar.MAY));
    assertThat(cal.get(Calendar.YEAR), is(2013));
    assertThat(cal.get(Calendar.DAY_OF_MONTH), is(3));
  }

  /**
   * Verifies that if metacard's date has an invalid timezone in the ISO 8601 format that the
   * current time is returned.
   */
  @Test
  public void testConvertInvalidTimeZoneInDateMetacardAttribute() {
    String dateStr = "2013-05-13T10:56:39EDT";
    Serializable ser =
        CswUnmarshallHelper.convertStringValueToMetacardValue(AttributeFormat.DATE, dateStr);

    assertDateConversion(ser, Calendar.getInstance());
  }

  /**
   * Verifies that if the metacard's date is not in ISO 8601 format the current time is returned.
   */
  @Test
  public void testConvertInvalidDateMetacardAttribute() {
    String dateStr = "26021000ZFEB11";
    Serializable ser =
        CswUnmarshallHelper.convertStringValueToMetacardValue(AttributeFormat.DATE, dateStr);

    assertDateConversion(ser, Calendar.getInstance());
  }

  /** Test to verify that a metacard's content type is set to the CSW Record's type field. */
  @Test
  public void testSetMetacardContentTypeToCswRecordType()
      throws ParserConfigurationException, SAXException, IOException {
    // Setup
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
    Document doc =
        docBuilder.parse(CswRecordConverterTest.class.getResource("/Csw_Record.xml").getPath());
    HierarchicalStreamReader reader = new DomReader(doc);
    UnmarshallingContext context = new TreeUnmarshaller(null, null, null, null);

    // Perform test
    Metacard metacard = (Metacard) converter.unmarshal(reader, context);

    // Verify
    LOGGER.debug("metacard id: {}", metacard.getId());
    LOGGER.debug("metacard content type: {}", metacard.getContentTypeName());
    assertThat(metacard.getContentTypeName(), is("IMAGE-PRODUCT"));
  }

  @Test
  public void testMarshalRecord() throws IOException, JAXBException, SAXException, XpathException {
    Metacard metacard = getTestMetacard();

    StringWriter stringWriter = new StringWriter();
    PrettyPrintWriter writer = new PrettyPrintWriter(stringWriter);
    MarshallingContext context = new TreeMarshaller(writer, null, null);

    converter.marshal(metacard, writer, context);

    String xml = stringWriter.toString();
    assertThat(xml, containsString(CswConstants.CSW_RECORD));
    assertRecordXml(xml, metacard, FULL);
  }

  @Test
  public void testMarshalBriefRecord()
      throws IOException, JAXBException, SAXException, XpathException {
    Metacard metacard = getTestMetacard();

    StringWriter stringWriter = new StringWriter();
    PrettyPrintWriter writer = new PrettyPrintWriter(stringWriter);
    MarshallingContext context = new TreeMarshaller(writer, null, null);
    context.put(CswConstants.ELEMENT_SET_TYPE, BRIEF);

    converter.marshal(metacard, writer, context);

    String xml = stringWriter.toString();
    assertThat(xml, containsString(CswConstants.CSW_BRIEF_RECORD));
    assertRecordXml(xml, metacard, BRIEF);
  }

  @Test
  public void testMarshalSummaryRecord()
      throws IOException, JAXBException, SAXException, XpathException {
    Metacard metacard = getTestMetacard();

    StringWriter stringWriter = new StringWriter();
    PrettyPrintWriter writer = new PrettyPrintWriter(stringWriter);
    MarshallingContext context = new TreeMarshaller(writer, null, null);
    context.put(CswConstants.ELEMENT_SET_TYPE, SUMMARY);

    converter.marshal(metacard, writer, context);

    String xml = stringWriter.toString();
    assertThat(xml, containsString(CswConstants.CSW_SUMMARY_RECORD));
    assertRecordXml(xml, metacard, SUMMARY);
  }

  @Test
  public void testMarshalRecordWithNamespaces()
      throws IOException, JAXBException, SAXException, XpathException {
    Metacard metacard = getTestMetacard();

    StringWriter stringWriter = new StringWriter();
    PrettyPrintWriter writer = new PrettyPrintWriter(stringWriter);
    MarshallingContext context = new TreeMarshaller(writer, null, null);
    context.put(CswConstants.WRITE_NAMESPACES, true);

    converter.marshal(metacard, writer, context);

    String xml = stringWriter.toString();
    XMLUnit.setIgnoreWhitespace(true);

    assertXMLEqual(getControlRecord(), xml);
  }

  @Test
  public void testMetacardTransform()
      throws IOException, JAXBException, SAXException, XpathException, CatalogTransformerException {
    Metacard metacard = getTestMetacard();

    Map<String, Serializable> args = new HashMap<>();
    args.put(CswConstants.WRITE_NAMESPACES, true);

    BinaryContent content = converter.transform(metacard, args);

    String xml = IOUtils.toString(content.getInputStream(), StandardCharsets.UTF_8);
    assertThat(
        xml, containsString("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"));
    XMLUnit.setIgnoreWhitespace(true);
    assertXMLEqual(getControlRecord(), xml);
  }

  @Test
  public void testMetacardTransformOmitXmlDeclaration()
      throws IOException, JAXBException, SAXException, XpathException, CatalogTransformerException {
    Metacard metacard = getTestMetacard();

    Map<String, Serializable> args = new HashMap<>();
    args.put(CswConstants.WRITE_NAMESPACES, true);
    args.put(CswConstants.OMIT_XML_DECLARATION, true);

    BinaryContent content = converter.transform(metacard, args);

    String xml = IOUtils.toString(content.getInputStream(), StandardCharsets.UTF_8);
    assertThat(
        xml, not(containsString("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")));
    XMLUnit.setIgnoreWhitespace(true);
    assertXMLEqual(getControlRecord(), xml);
  }

  @Test
  public void testMetacardTransformOmitNamespaces()
      throws IOException, JAXBException, SAXException, XpathException, CatalogTransformerException {
    Metacard metacard = getTestMetacard();

    Map<String, Serializable> args = new HashMap<>();
    args.put(CswConstants.WRITE_NAMESPACES, false);

    BinaryContent content = converter.transform(metacard, args);

    String xml = IOUtils.toString(content.getInputStream(), StandardCharsets.UTF_8);
    assertThat(xml, containsString("<csw:Record>"));
  }

  @Test
  public void testMetacardTransformWithCswRecordMetadata()
      throws IOException, JAXBException, SAXException, XpathException, CatalogTransformerException {
    Metacard metacard = getCswRecordMetacard();

    Map<String, Serializable> args = new HashMap<>();
    args.put(CswConstants.WRITE_NAMESPACES, true);

    BinaryContent content = converter.transform(metacard, args);

    String xml = IOUtils.toString(content.getInputStream(), StandardCharsets.UTF_8);
    assertThat(xml, containsString("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
    XMLUnit.setIgnoreWhitespace(true);
    assertXMLEqual(cswRecordXml, xml);
  }

  @Test
  public void testInputTransformWithNoNamespaceDeclaration()
      throws IOException, CatalogTransformerException {
    InputStream is = IOUtils.toInputStream(getRecordNoNamespaceDeclaration());
    Metacard mc = converter.transform(is);

    Metacard expectedMetacard = getTestMetacard();
    assertThat(mc, notNullValue());
    assertThat(mc.getContentTypeName(), is(expectedMetacard.getContentTypeName()));
    assertThat(mc.getCreatedDate(), is(expectedMetacard.getCreatedDate()));
    assertThat(mc.getEffectiveDate(), is(expectedMetacard.getEffectiveDate()));
    assertThat(mc.getId(), is(expectedMetacard.getId()));
    assertThat(mc.getModifiedDate(), is(expectedMetacard.getModifiedDate()));
    assertThat(mc.getTitle(), is(expectedMetacard.getTitle()));
    assertThat(mc.getResourceURI(), is(expectedMetacard.getResourceURI()));
  }

  @Test
  public void testInputTransform() throws IOException, CatalogTransformerException {
    InputStream is = CswRecordConverterTest.class.getResourceAsStream("/Csw_Record.xml");
    Metacard mc = converter.transform(is);
    assertThat(mc, notNullValue());
  }

  @Test
  public void testUnmarshalCswRecordWithProductAndThumbnail()
      throws URISyntaxException, IOException, JAXBException, ParserConfigurationException,
          SAXException {
    XStream xstream = new XStream(new WstxDriver());

    xstream.registerConverter(converter);

    InputStream is = CswRecordConverterTest.class.getResourceAsStream("/Csw_Record.xml");

    // get the URL to the thumbnail image and stick it in the xml string
    // this makes the test filesystem independent
    URL thumbnail = CswRecordConverterTest.class.getResource("/ddf_globe.png");
    String xml = null;
    if (thumbnail != null) {
      StringWriter writer = new StringWriter();
      IOUtils.copy(is, writer);
      xml = writer.toString();
      xml = xml.replace(THUMBNAIL_URL, thumbnail.toString());
    }

    xstream.alias("csw:Record", MetacardImpl.class);
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
    Document doc = docBuilder.parse(IOUtils.toInputStream(xml));
    HierarchicalStreamReader reader = new DomReader(doc);
    DataHolder holder = xstream.newDataHolder();

    Metacard mc = (Metacard) xstream.unmarshal(reader, null, holder);

    assertThat(mc, notNullValue());
    String productUrl = "http://example.com/product.pdf";
    assertThat(mc.getAttribute(Core.RESOURCE_URI).getValue(), is(productUrl));
    assertThat(mc.getThumbnail(), is(getThumbnailByteArray(thumbnail)));
  }

  private MetacardImpl getTestMetacard() {
    MetacardImpl metacard = new MetacardImpl(getCswMetacardType());
    metacard.setContentTypeName("I have some content type");
    metacard.setAttribute(new AttributeImpl(Media.FORMAT, "I have some format type"));
    metacard.setContentTypeVersion("1.0.0");
    metacard.setCreatedDate(CREATED_DATE.getTime());
    metacard.setEffectiveDate(EFFECTIVE_DATE.getTime());
    metacard.setId("ID");
    metacard.setLocation("POLYGON ((30 10, 10 20, 20 40, 40 40, 30 10))");
    metacard.setMetadata("<xml>metadata a whole bunch of metadata</xml>");
    metacard.setModifiedDate(MODIFIED_DATE.getTime());
    metacard.setResourceSize("123TB");
    metacard.setSourceId("sourceID");
    metacard.setTitle("This is my title");
    metacard.setAttribute(new AttributeImpl(Core.LANGUAGE, "english"));
    metacard.setAttribute(new AttributeImpl(Contact.PUBLISHER_NAME, "bob"));
    metacard.setAttribute(new AttributeImpl(Contact.CREATOR_NAME, "steve"));
    metacard.setAttribute(new AttributeImpl(Contact.CONTRIBUTOR_NAME, "rick"));
    metacard.setAttribute(new AttributeImpl(Topic.CATEGORY, Arrays.asList("topic1", "topic2")));
    metacard.setAttribute(new AttributeImpl(Core.DESCRIPTION, "This is a description"));

    try {
      metacard.setResourceURI(new URI(ACTION_URL));
    } catch (URISyntaxException e) {
      LOGGER.debug("URISyntaxException", e);
    }

    return metacard;
  }

  private MetacardImpl getCswRecordMetacard() throws IOException {
    MetacardImpl metacard = getTestMetacard();
    metacard.setMetadata(cswRecordXml);
    return metacard;
  }

  private void assertDateConversion(Serializable ser, Calendar expectedDate) {
    assertThat(ser, notNullValue());
    assertThat(Date.class.isAssignableFrom(ser.getClass()), is(true));
    Date date = (Date) ser;
    Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    assertThat(cal.get(Calendar.MONTH), is(expectedDate.get(Calendar.MONTH)));
    assertThat(cal.get(Calendar.YEAR), is(expectedDate.get(Calendar.YEAR)));
    assertThat(cal.get(Calendar.DAY_OF_MONTH), is(expectedDate.get(Calendar.DAY_OF_MONTH)));
  }

  private byte[] getThumbnailByteArray(URL url) throws IOException {
    InputStream is = url.openStream();
    byte[] thumbnail = IOUtils.toByteArray(is);
    IOUtils.closeQuietly(is);
    return thumbnail;
  }

  private String getControlRecord() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        + "<csw:Record xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\" "
        + "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" "
        + "xmlns:dct=\"http://purl.org/dc/terms/\" "
        + "xmlns:ows=\"http://www.opengis.net/ows\">\n"
        + "  <dct:created>"
        + created
        + "</dct:created>\n"
        + "  <dc:date>"
        + modified
        + "</dc:date>\n"
        + "  <dct:modified>"
        + modified
        + "</dct:modified>\n"
        + "  <dct:dateSubmitted>"
        + modified
        + "</dct:dateSubmitted>\n"
        + "  <dct:issued>"
        + modified
        + "</dct:issued>\n"
        + "  <dc:identifier>ID</dc:identifier>\n"
        + "  <dct:bibliographicCitation>ID</dct:bibliographicCitation>\n"
        + "  <dc:source>"
        + ACTION_URL
        + "</dc:source>\n"
        + "  <dc:title>This is my title</dc:title>\n"
        + "  <dct:alternative>This is my title</dct:alternative>\n"
        + "  <dc:format>I have some format type</dc:format>\n"
        + "  <dc:type>I have some content type</dc:type> "
        + "  <dct:dateAccepted>"
        + effective
        + "</dct:dateAccepted>\n"
        + "  <dct:dateCopyrighted>"
        + effective
        + "</dct:dateCopyrighted>\n"
        + "  <dc:creator>steve</dc:creator>\n"
        + "  <dc:publisher>bob</dc:publisher>\n"
        + "  <dc:contributor>rick</dc:contributor>\n"
        + "  <dc:language>english</dc:language>\n"
        + "  <dc:subject>topic1</dc:subject>\n"
        + "  <dc:subject>topic2</dc:subject>\n"
        + "  <dc:csw.description>This is a description</dc:csw.description>\n"
        + "  <dct:abstract>This is a description</dct:abstract>\n"
        + "  <dct:tableOfContents>This is a description</dct:tableOfContents>\n"
        + "  <ows:BoundingBox crs=\"urn:x-ogc:def:crs:EPSG:6.11:4326\">\n"
        + "    <ows:LowerCorner>10.0 10.0</ows:LowerCorner>\n"
        + "    <ows:UpperCorner>40.0 40.0</ows:UpperCorner>\n"
        + "  </ows:BoundingBox>\n"
        + "</csw:Record>\n";
  }

  private String getRecordNoNamespaceDeclaration() {
    return "<csw:Record>\n"
        + "  <dct:created>"
        + created
        + "</dct:created>\n"
        + "  <dc:date>"
        + modified
        + "</dc:date>\n"
        + "  <dct:modified>"
        + modified
        + "</dct:modified>\n"
        + "  <dct:dateSubmitted>"
        + modified
        + "</dct:dateSubmitted>\n"
        + "  <dct:issued>"
        + modified
        + "</dct:issued>\n"
        + "  <dc:identifier>ID</dc:identifier>\n"
        + "  <dct:bibliographicCitation>ID</dct:bibliographicCitation>\n"
        + "  <dc:language>english</dc:language>\n"
        + "  <dc:source>"
        + ACTION_URL
        + "</dc:source>\n"
        + "  <dc:title>This is my title</dc:title>\n"
        + "  <dct:alternative>This is my title</dct:alternative>\n"
        + "  <dc:type>I have some content type</dc:type> "
        + "  <dc:format>I have some format type</dc:format>"
        + "  <dct:dateAccepted>"
        + effective
        + "</dct:dateAccepted>\n"
        + "  <dct:dateCopyrighted>"
        + effective
        + "</dct:dateCopyrighted>\n"
        + "  <dc:creator>steve</dc:creator>\n"
        + "  <dc:publisher>bob</dc:publisher>\n"
        + "  <dc:contributor>rick</dc:contributor>\n"
        + "  <dc:subject>topic1</dc:subject>\n"
        + "  <dc:subject>topic2</dc:subject>\n"
        + "  <dc:csw.description>This is a description</dc:csw.description>\n"
        + "  <dct:abstract>This is a description</dct:abstract>\n"
        + "  <dct:tableOfContents>This is a description</dct:tableOfContents>\n"
        + "  <ows:BoundingBox crs=\"urn:x-ogc:def:crs:EPSG:6.11:4326\">\n"
        + "    <ows:LowerCorner>10.0 10.0</ows:LowerCorner>\n"
        + "    <ows:UpperCorner>40.0 40.0</ows:UpperCorner>\n"
        + "  </ows:BoundingBox>\n"
        + "</csw:Record>\n";
  }

  private void assertRecordXml(String xml, Metacard metacard, ElementSetType elemntSetType) {
    switch (elemntSetType) {
      case FULL:
        assertThat(
            xml,
            containsString(
                "<dct:bibliographicCitation>" + metacard.getId() + "</dct:bibliographicCitation>"));
        assertThat(
            xml, containsString("<dct:alternative>" + metacard.getTitle() + "</dct:alternative>"));
        assertThat(xml, containsString("<dc:date>" + modified + "</dc:date>"));
        assertThat(xml, containsString("<dct:modified>" + modified + "</dct:modified>"));
        assertThat(xml, containsString("<dct:created>" + created + "</dct:created>"));
        assertThat(xml, containsString("<dct:dateAccepted>" + effective + "</dct:dateAccepted>"));
        assertThat(
            xml, containsString("<dct:dateCopyrighted>" + effective + "</dct:dateCopyrighted>"));
        assertThat(xml, containsString("<dct:dateSubmitted>" + modified + "</dct:dateSubmitted>"));
        assertThat(xml, containsString("<dct:issued>" + modified + "</dct:issued>"));
        assertThat(xml, containsString("<dc:source>" + metacard.getResourceURI() + "</dc:source>"));
        assertThat(
            xml,
            containsString(
                "<dc:publisher>"
                    + metacard.getAttribute(Contact.PUBLISHER_NAME).getValue()
                    + "</dc:publisher>"));
        break;

      case SUMMARY:
        assertThat(xml, containsString("<dct:modified>" + modified + "</dct:modified>"));
        assertSubjectList(xml, metacard);
        assertThat(
            xml,
            containsString(
                "<dc:format>" + metacard.getAttribute(Media.FORMAT).getValue() + "</dc:format>"));
        assertThat(
            xml,
            containsString(
                "<dct:abstract>"
                    + metacard.getAttribute(Core.DESCRIPTION).getValue()
                    + "</dct:abstract>"));
        break;
      case BRIEF:
        assertThat(xml, containsString("<dc:identifier>" + metacard.getId() + "</dc:identifier>"));
        assertThat(xml, containsString("<dc:title>" + metacard.getTitle() + "</dc:title>"));
        break;
    }
    // TODO - assert the reverse - if brief then it shouldn't have the others
  }

  private void assertSubjectList(String xml, Metacard metacard) {
    Attribute attribute = metacard.getAttribute(Topic.CATEGORY);
    if (attribute != null) {
      List<Serializable> serializables = attribute.getValues();
      for (Serializable serializable : serializables) {
        assertThat(xml, containsString("<dc:subject>" + serializable + "</dc:subject>"));
      }
    }
  }

  public static MetacardType getCswMetacardType() {
    return new MetacardTypeImpl(
        CswConstants.CSW_METACARD_TYPE_NAME,
        Arrays.asList(
            new ContactAttributes(),
            new LocationAttributes(),
            new MediaAttributes(),
            new TopicAttributes(),
            new AssociationsAttributes()));
  }
}
