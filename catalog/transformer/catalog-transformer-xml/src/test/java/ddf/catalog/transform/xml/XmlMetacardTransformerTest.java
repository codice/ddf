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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.transformer.api.MetacardMarshaller;
import ddf.catalog.transformer.xml.MetacardMarshallerImpl;
import ddf.catalog.transformer.xml.PrintWriterProviderImpl;
import ddf.catalog.transformer.xml.XmlMetacardTransformer;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.xml.XmlParser;
import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Test;

public class XmlMetacardTransformerTest {

  private XmlMetacardTransformer transformer;

  private Map<String, Serializable> emptyArgs = Collections.EMPTY_MAP;

  @Before
  public void setup() {
    Parser parser = new XmlParser();
    MetacardMarshaller metacardMarshaller =
        new MetacardMarshallerImpl(parser, new PrintWriterProviderImpl());
    transformer = new XmlMetacardTransformer(metacardMarshaller);
  }

  @Before
  public void setupXpath() {
    Map<String, String> m = new HashMap<String, String>();
    m.put("m", "urn:catalog:metacard");
    m.put("gml", "http://www.opengis.net/gml");
    NamespaceContext ctx = new SimpleNamespaceContext(m);
    XMLUnit.setXpathNamespaceContext(ctx);
  }

  private String transform(Metacard mc) throws Exception {
    BinaryContent bc = transformer.transform(mc, emptyArgs);
    return new String(bc.getByteArray());
  }

  @Test
  public void testMetacardTypeNameEmpty() throws Exception {
    Metacard mc = mock(Metacard.class);

    MetacardType mct = mock(MetacardType.class);
    when(mct.getName()).thenReturn("");
    when(mct.getAttributeDescriptors()).thenReturn(Collections.<AttributeDescriptor>emptySet());

    when(mc.getMetacardType()).thenReturn(mct);
    when(mc.getId()).thenReturn(null);
    when(mc.getSourceId()).thenReturn(null);

    String outputXml = transform(mc);

    assertXpathEvaluatesTo(
        MetacardType.DEFAULT_METACARD_TYPE_NAME, "/m:metacard/m:type", outputXml);
  }

  @Test
  public void testMetacardTypeNameNull() throws Exception {
    Metacard mc = mock(Metacard.class);

    MetacardType mct = mock(MetacardType.class);
    when(mct.getName()).thenReturn(null);
    when(mct.getAttributeDescriptors()).thenReturn(Collections.<AttributeDescriptor>emptySet());

    when(mc.getMetacardType()).thenReturn(mct);
    when(mc.getId()).thenReturn(null);
    when(mc.getSourceId()).thenReturn(null);

    String outputXml = transform(mc);

    assertXpathEvaluatesTo(
        MetacardType.DEFAULT_METACARD_TYPE_NAME, "/m:metacard/m:type", outputXml);
  }

  @Test
  public void testXmlMetacardTransformerSparse() throws Exception {

    MetacardImpl mc = new MetacardImpl();

    mc.setId("1234567890987654321");
    mc.setSourceId("FooBarSource");
    mc.setTitle("Title!");
    mc.setExpirationDate(new Date());
    mc.setLocation("POLYGON ((35 10, 10 20, 15 40, 45 45, 35 10),(20 30, 35 35, 30 20, 20 30))");
    mc.setMetadata(
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><foo><bar/></foo>");
    byte[] bytes = {
      0, 0, 1, 0, 0, 1, 1, 1, 0, 1, 0, 0, 1, 1, 1, 0, 1, 0, 0, 1, 1, 1, 0, 1, 0, 0, 1, 1, 1, 0, 1,
      0, 0, 1, 1, 1, 0, 1, 0, 0, 1, 1, 1, 0, 1, 0, 0, 1, 1, 1
    };
    mc.setThumbnail(bytes);

    transform(mc);

    // TODO add assertions. Use XMLunit?
  }

  @Test
  public void testXmlMetacardTransformer() throws Exception {

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

    InputStream input = getClass().getResourceAsStream("/extensibleMetacard.xml");
    String metadata = IOUtils.toString(input, StandardCharsets.UTF_8);
    mc.setMetadata(metadata);

    String outputXml = transform(mc);

    assertXpathEvaluatesTo(testId, "/m:metacard/@gml:id", outputXml);
    assertXpathEvaluatesTo(testSource, "/m:metacard/m:source", outputXml);
    assertXpathEvaluatesTo(testTitle, "/m:metacard/m:string[@name='title']/m:value", outputXml);

    // TODO convert GML representation?
    // assertXpathEvaluatesTo(testLocation,"/m:metacard/m:geometry[@name='location']/m:value",
    // outputXml);
    assertXpathExists("/m:metacard/m:geometry[@name='location']/m:value", outputXml);

    // TODO Base64 check?
    // assertXpathEvaluatesTo(testThumbnail,
    // "/metacard/base64Binary[@id='thumbnail']", outputXml);
    assertXpathExists("/m:metacard/m:base64Binary[@name='thumbnail']/m:value", outputXml);

    // TODO XML Date representation?
    assertXpathExists("/m:metacard/m:dateTime[@name='expiration']/m:value", outputXml);
  }

  @Test
  public void testMultivalueAttribute() throws Exception {
    MetacardImpl mc = new MetacardImpl();
    Set<String> tags = new HashSet<>(Arrays.asList("basic-tag", "another-tag"));
    mc.setTags(tags);

    String outputXml = transform(mc);

    // only one element gets produced for a multivalued attribute
    assertXpathEvaluatesTo("1", "count(/m:metacard/m:string[@name='metacard-tags'])", outputXml);

    // the element contains every value for the multivalued attribute
    assertXpathExists(
        "/m:metacard/m:string[@name='metacard-tags']/m:value[text()='basic-tag']", outputXml);
    assertXpathExists(
        "/m:metacard/m:string[@name='metacard-tags']/m:value[text()='another-tag']", outputXml);
  }
}
