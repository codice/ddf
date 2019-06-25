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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transformer.xml.XmlInputTransformer;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.codice.ddf.parser.xml.XmlParser;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XmlInputTransformerTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(XmlInputTransformerTest.class);

  private static final String DESCRIPTION = "Description!";

  private static final String POINT_OF_CONTACT = "POC!";

  private XmlInputTransformer xit;

  @Before
  public void setup() {
    xit = new XmlInputTransformer(new XmlParser());
  }

  @Test
  public void testTransformWithInvalidMetacardType()
      throws IOException, CatalogTransformerException {
    Metacard metacard =
        xit.transform(new FileInputStream("src/test/resources/invalidExtensibleMetacard.xml"));

    LOGGER.info("ID: {}", metacard.getId());
    LOGGER.info("Type: {}", metacard.getMetacardType().getName());
    LOGGER.info("Source: {}", metacard.getSourceId());
    LOGGER.info("Attributes: ");
    for (AttributeDescriptor descriptor : metacard.getMetacardType().getAttributeDescriptors()) {
      Attribute attribute = metacard.getAttribute(descriptor.getName());
      LOGGER.info(
          "\t" + descriptor.getName() + ": " + ((attribute == null) ? null : attribute.getValue()));
    }

    assertThat(metacard.getMetacardType().getName(), is(MetacardImpl.BASIC_METACARD.getName()));
  }

  @Test
  public void testTransformWithExtensibleMetacardType()
      throws IOException, CatalogTransformerException {
    List<MetacardType> metacardTypes = new ArrayList<MetacardType>(1);
    MetacardType extensibleType =
        new MetacardTypeImpl(
            "extensible.metacard", MetacardImpl.BASIC_METACARD.getAttributeDescriptors());
    metacardTypes.add(extensibleType);
    xit.setMetacardTypes(metacardTypes);
    Metacard metacard =
        xit.transform(new FileInputStream("src/test/resources/extensibleMetacard.xml"));

    LOGGER.info("ID: {}", metacard.getId());
    LOGGER.info("Type: {}", metacard.getMetacardType().getName());
    LOGGER.info("Source: {}", metacard.getSourceId());
    LOGGER.info("Attributes: ");
    for (AttributeDescriptor descriptor : metacard.getMetacardType().getAttributeDescriptors()) {
      Attribute attribute = metacard.getAttribute(descriptor.getName());
      LOGGER.info(
          "\t" + descriptor.getName() + ": " + ((attribute == null) ? null : attribute.getValue()));
    }
  }

  @Test
  public void testSimpleMetadata() throws IOException, CatalogTransformerException, ParseException {
    Metacard metacard = xit.transform(new FileInputStream("src/test/resources/metacard1.xml"));

    LOGGER.info("Attributes: ");
    for (AttributeDescriptor descriptor : metacard.getMetacardType().getAttributeDescriptors()) {
      Attribute attribute = metacard.getAttribute(descriptor.getName());
      LOGGER.info(
          "\t" + descriptor.getName() + ": " + ((attribute == null) ? null : attribute.getValue()));
    }

    LOGGER.info("ID: {}", metacard.getId());
    LOGGER.info("Type: {}", metacard.getMetacardType().getName());
    LOGGER.info("Source: {}", metacard.getSourceId());

    assertEquals("1234567890987654321", metacard.getId());
    assertEquals("ddf.metacard", metacard.getMetacardType().getName());
    assertEquals("foobar", metacard.getSourceId());

    // TODO use JTS to check for equality, not string comparison.
    assertEquals(
        "POLYGON ((35 10, 10 20, 15 40, 45 45, 35 10), (20 30, 35 35, 30 20, 20 30))",
        metacard.getAttribute(Metacard.GEOGRAPHY).getValue());

    assertEquals("Title!", metacard.getAttribute(Metacard.TITLE).getValue());

    assertArrayEquals(
        Base64.getDecoder()
            .decode("AAABAAABAQEAAQAAAQEBAAEAAAEBAQABAAABAQEAAQAAAQEBAAEAAAEBAQABAAABAQE="),
        (byte[]) metacard.getAttribute(Metacard.THUMBNAIL).getValue());

    // TODO use XMLUnit to test equivalence
    assertThat(
        metacard.getAttribute(Metacard.METADATA).getValue().toString(),
        startsWith("<foo xmlns=\"http://foo.com\">"));

    assertEquals(
        (new SimpleDateFormat("MMM d, yyyy HH:mm:ss.SSS z")).parse("Dec 27, 2012 16:31:01.641 MST"),
        metacard.getAttribute(Metacard.EXPIRATION).getValue());

    assertEquals(DESCRIPTION, metacard.getAttribute("description").getValue());
    assertEquals(POINT_OF_CONTACT, metacard.getAttribute("point-of-contact").getValue());
  }

  @Test
  public void testFallbackToBasicMetacardForUnknowMetacardType()
      throws FileNotFoundException, IOException, CatalogTransformerException, ParseException {
    List<MetacardType> metacardTypes = new ArrayList<MetacardType>(1);
    metacardTypes.add(MetacardImpl.BASIC_METACARD);
    xit.setMetacardTypes(metacardTypes);

    Metacard metacard =
        xit.transform(new FileInputStream("src/test/resources/unknownMetacard1.xml"));

    LOGGER.info("ID: {}", metacard.getId());
    LOGGER.info("Type: {}", metacard.getMetacardType().getName());
    LOGGER.info("Source: {}", metacard.getSourceId());
    LOGGER.info("Attributes: ");
    for (AttributeDescriptor descriptor : metacard.getMetacardType().getAttributeDescriptors()) {
      Attribute attribute = metacard.getAttribute(descriptor.getName());
      LOGGER.info(
          "\t" + descriptor.getName() + ": " + ((attribute == null) ? null : attribute.getValue()));
    }

    assertThat(metacard.getMetacardType().getName(), is(MetacardImpl.BASIC_METACARD.getName()));

    assertThat("1234567890987654321", is(metacard.getId()));
    assertThat("foobar", is(metacard.getSourceId()));

    assertThat(
        "POLYGON ((35 10, 10 20, 15 40, 45 45, 35 10), (20 30, 35 35, 30 20, 20 30))",
        is(metacard.getAttribute(Metacard.GEOGRAPHY).getValue()));

    assertThat("Title!", is(metacard.getAttribute(Metacard.TITLE).getValue()));

    assertArrayEquals(
        Base64.getDecoder()
            .decode("AAABAAABAQEAAQAAAQEBAAEAAAEBAQABAAABAQEAAQAAAQEBAAEAAAEBAQABAAABAQE="),
        (byte[]) metacard.getAttribute(Metacard.THUMBNAIL).getValue());

    assertThat(
        metacard.getAttribute(Metacard.METADATA).getValue().toString(),
        startsWith("<foo xmlns=\"http://foo.com\">"));

    assertThat(
        (new SimpleDateFormat("MMM d, yyyy HH:mm:ss.SSS z")).parse("Dec 27, 2012 16:31:01.641 MST"),
        is(metacard.getAttribute(Metacard.EXPIRATION).getValue()));

    assertEquals(DESCRIPTION, metacard.getAttribute("description").getValue());
    assertEquals(POINT_OF_CONTACT, metacard.getAttribute("point-of-contact").getValue());
  }

  @Test
  public void testEmptyLineString()
      throws IOException, CatalogTransformerException, ParseException {
    Metacard metacard =
        xit.transform(new FileInputStream("src/test/resources/metacard-emptygeo1.xml"));

    assertThat(metacard.getAttribute(Metacard.GEOGRAPHY), nullValue());
  }

  @Test
  public void testEmptyPolygon() throws IOException, CatalogTransformerException, ParseException {
    Metacard metacard =
        xit.transform(new FileInputStream("src/test/resources/metacard-emptygeo2.xml"));

    assertThat(metacard.getAttribute(Metacard.GEOGRAPHY), nullValue());
  }
}
