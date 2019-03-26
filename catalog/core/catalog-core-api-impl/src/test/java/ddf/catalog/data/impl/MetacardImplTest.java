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
package ddf.catalog.data.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.types.Core;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class MetacardImplTest {
  private static final String DEFAULT_SERIALIZATION_FILE_LOCATION = "target/metacard1.ser";

  MetacardImpl mc;

  Date createdDate;

  Date effectiveDate;

  Date expireDate;

  Date modDate;

  String mcId;

  String locWkt;

  URI resourceUri;

  URI nsUri;

  @Before
  public void setUp() throws Exception {
    Calendar c = Calendar.getInstance();
    createdDate = c.getTime();
    c.add(Calendar.HOUR, 1);
    effectiveDate = c.getTime();
    c.add(Calendar.DAY_OF_YEAR, 1);
    modDate = c.getTime();
    c.add(Calendar.YEAR, 1);
    expireDate = c.getTime();
    mcId = UUID.randomUUID().toString();
    locWkt = "POLYGON ((30 10, 10 20, 20 40, 40 40, 30 10))";
    nsUri = new URI("http://" + MetacardImplTest.class.getName());
    resourceUri = new URI(nsUri.toString() + "/resource.html");
    mc = new MetacardImpl();
    mc.setContentTypeName("testContentType");
    mc.setContentTypeVersion("testContentTypeVersion");
    mc.setAttribute("testAtt", "testAttValue");
    mc.setCreatedDate(createdDate);
    mc.setEffectiveDate(effectiveDate);
    mc.setExpirationDate(expireDate);
    mc.setModifiedDate(modDate);
    mc.setId(mcId);
    mc.setLocation(locWkt);
    mc.setMetadata("testMetadata");
    mc.setResourceURI(resourceUri);
    mc.setSourceId("testSourceId");
    mc.setTargetNamespace(nsUri);
    mc.setTitle("testTitle");
    mc.setThumbnail(mc.getId().getBytes());
    mc.setDescription("testDescription");
    mc.setPointOfContact("pointOfContact");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMetacardTypeAsNull() {
    new MetacardImpl((MetacardType) null);
  }

  /**
   * Test XML support
   *
   * @throws ParserConfigurationException
   */
  @Test
  public void metacardObjectTest() throws ParserConfigurationException {
    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
    Document document = documentBuilder.newDocument();
    Element rootElement = document.createElement("test");
    document.appendChild(rootElement);

    MetacardImpl metacard = new MetacardImpl();
    String xml = "<xml/>";
    metacard.setMetadata(xml);
    metacard.setId("id1");
  }

  /** make sure type conversion works in MetacardImpl */
  @Test
  public void testMetacardImplAttributes() {
    MetacardImpl metacard = new MetacardImpl();
    Date now = new Date();
    metacard.setEffectiveDate(now);
    assertEquals(now, metacard.getEffectiveDate());
  }

  /** make sure type conversion works in MetacardImpl */
  @Test
  public void testMetacardAttributes() {
    Metacard metacard = new MetacardImpl();
    Date now = new Date();
    metacard.setAttribute(new AttributeImpl(Metacard.EFFECTIVE, now));
    assertEquals(now, metacard.getEffectiveDate());
  }

  @Test
  public void testMetacardImpl() {
    MetacardImpl mi = new MetacardImpl();
    assertEquals(null, mi.getContentTypeName());
    assertEquals(null, mi.getContentTypeNamespace());
    assertEquals(null, mi.getContentTypeVersion());
    assertEquals(null, mi.getAttribute(Core.CREATED).getValue());
    assertEquals(null, mi.getEffectiveDate());
    assertEquals(null, mi.getExpirationDate());
    assertEquals(null, mi.getId());
    assertEquals(null, mi.getLocation());
    assertEquals(MetacardImpl.BASIC_METACARD, mi.getMetacardType());
    assertEquals(null, mi.getMetadata());
    assertEquals(null, mi.getAttribute(Core.MODIFIED).getValue());
    assertEquals(null, mi.getResourceSize());
    assertEquals(null, mi.getResourceURI());
    assertEquals(null, mi.getSourceId());
    assertEquals(null, mi.getThumbnail());
    assertEquals(null, mi.getTitle());
    assertEquals(null, mi.getDescription());
    assertEquals(null, mi.getPointOfContact());

    mi = new MetacardImpl();
    assertEquals(null, mi.getContentTypeName());
    assertEquals(null, mi.getContentTypeNamespace());
    assertEquals(null, mi.getContentTypeVersion());
    assertEquals(null, mi.getAttribute(Core.CREATED).getValue());
    assertEquals(null, mi.getEffectiveDate());
    assertEquals(null, mi.getExpirationDate());
    assertEquals(null, mi.getId());
    assertEquals(null, mi.getLocation());
    assertEquals(MetacardImpl.BASIC_METACARD, mi.getMetacardType());
    assertEquals(null, mi.getMetadata());
    assertEquals(null, mi.getAttribute(Core.MODIFIED).getValue());
    assertEquals(null, mi.getResourceSize());
    assertEquals(null, mi.getResourceURI());
    assertEquals(null, mi.getSourceId());
    assertEquals(null, mi.getThumbnail());
    assertEquals(null, mi.getTitle());
    assertEquals(null, mi.getDescription());
    assertEquals(null, mi.getPointOfContact());

    mi = new MetacardImpl(mc);
    assertEquals(mc.getContentTypeName(), mi.getContentTypeName());
    assertEquals(mc.getContentTypeNamespace(), mi.getContentTypeNamespace());
    assertEquals(mc.getContentTypeVersion(), mi.getContentTypeVersion());
    assertEquals(
        mc.getAttribute(Core.CREATED).getValue(), mi.getAttribute(Core.CREATED).getValue());
    assertEquals(mc.getEffectiveDate(), mi.getEffectiveDate());
    assertEquals(mc.getExpirationDate(), mi.getExpirationDate());
    assertEquals(mc.getId(), mi.getId());
    assertEquals(mc.getLocation(), mi.getLocation());
    assertEquals(MetacardImpl.BASIC_METACARD, mi.getMetacardType());
    assertEquals(mc.getMetacardType(), mi.getMetacardType());
    assertEquals(mc.getMetadata(), mi.getMetadata());
    assertEquals(
        mc.getAttribute(Core.MODIFIED).getValue(), mi.getAttribute(Core.MODIFIED).getValue());
    assertEquals(mc.getResourceSize(), mi.getResourceSize());
    assertEquals(mc.getResourceURI(), mi.getResourceURI());
    assertEquals(mc.getSourceId(), mi.getSourceId());
    assertEquals(mc.getThumbnail(), mi.getThumbnail());
    assertEquals(mc.getTitle(), mi.getTitle());
    assertEquals(mc.getDescription(), mi.getDescription());
    assertEquals(mc.getPointOfContact(), mi.getPointOfContact());
  }

  @Test
  public void testUpdatingWrappedMetacardFields() {
    HashSet<AttributeDescriptor> descriptors = new HashSet<AttributeDescriptor>();
    descriptors.add(
        new AttributeDescriptorImpl(
            "test-string",
            true /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.STRING_TYPE));
    MetacardType testType = new MetacardTypeImpl("test.type", descriptors);

    MetacardImpl mi = new MetacardImpl(mc);
    mi.setSourceId("testSource");
    mi.setType(testType);

    assertEquals("testSource", mi.getSourceId());
    assertEquals(testType, mi.getMetacardType());
  }

  @Test
  public void testSetNullFields() {
    mc.setContentTypeName(null);
    mc.setContentTypeVersion(null);
    mc.setAttribute(null);
    mc.setAttribute(new AttributeImpl("testNullValueAtt1", (Serializable) null));
    mc.setAttribute("testNullValueAtt2", null);
    mc.setCreatedDate(null);
    mc.setEffectiveDate(null);
    mc.setExpirationDate(null);
    mc.setModifiedDate(null);
    mc.setId(null);
    mc.setLocation(null);
    mc.setMetadata(null);
    mc.setResourceURI(null);
    mc.setSourceId(null);
    mc.setTitle(null);
    mc.setThumbnail(null);
    mc.setDescription(null);
    mc.setPointOfContact(null);

    assertEquals(null, mc.getAttribute("testNullValueAtt1"));
    assertEquals(null, mc.getAttribute("testNullValueAtt2"));
    assertEquals(null, mc.getContentTypeName());
    assertEquals(null, mc.getContentTypeVersion());
    assertEquals(null, mc.getAttribute(Core.CREATED).getValue());
    assertEquals(null, mc.getEffectiveDate());
    assertEquals(null, mc.getExpirationDate());
    assertEquals(null, mc.getId());
    assertEquals(null, mc.getLocation());
    assertEquals(null, mc.getMetadata());
    assertEquals(null, mc.getAttribute(Core.MODIFIED).getValue());
    assertEquals(null, mc.getResourceSize());
    assertNotNull(mc.getResourceURI());
    assertEquals(null, mc.getSourceId());
    assertEquals(null, mc.getThumbnail());
    assertEquals(null, mc.getTitle());
    assertEquals(null, mc.getDescription());
    assertEquals(null, mc.getPointOfContact());
  }

  @Test
  public void testEmptyStringFields() {}

  @Test
  public void testSerializationSingle()
      throws IOException, ClassNotFoundException, URISyntaxException {

    MetacardImpl metacard = new MetacardImpl();

    Date now = new Date();
    metacard.setTitle("Flagstaff");
    metacard.setContentTypeName("nitf");
    metacard.setContentTypeVersion("DDF_20");
    metacard.setLocation("POINT (1 0)");
    metacard.setMetadata("<something/>");
    metacard.setCreatedDate(now);
    metacard.setResourceURI(new URI("http://ddf.com"));
    byte[] buffer = {-86};
    metacard.setThumbnail(buffer);
    metacard.setSourceId("mySourceId");
    metacard.setDescription("Northern Arizona City");
    metacard.setPointOfContact("poc");
    Serializer<Metacard> serializer = new Serializer<Metacard>();

    /* WRITE */
    serializer.serialize(metacard, DEFAULT_SERIALIZATION_FILE_LOCATION);

    /* READ */
    Metacard readMetacard = serializer.deserialize(DEFAULT_SERIALIZATION_FILE_LOCATION);

    assertEquals(metacard.getTitle(), readMetacard.getTitle());
    assertEquals(metacard.getContentTypeName(), readMetacard.getContentTypeName());
    assertEquals(metacard.getContentTypeVersion(), readMetacard.getContentTypeVersion());
    assertEquals(metacard.getLocation(), readMetacard.getLocation());
    assertEquals(metacard.getMetadata(), readMetacard.getMetadata());
    assertEquals(
        metacard.getAttribute(Core.CREATED).getValue(),
        readMetacard.getAttribute(Core.CREATED).getValue());
    assertEquals(metacard.getExpirationDate(), readMetacard.getExpirationDate());
    assertEquals(metacard.getResourceURI(), readMetacard.getResourceURI());
    assertEquals(metacard.getResourceSize(), readMetacard.getResourceSize());
    assertTrue(Arrays.equals(metacard.getThumbnail(), readMetacard.getThumbnail()));
    assertEquals(metacard.getSourceId(), readMetacard.getSourceId());
    assertEquals(metacard.getDescription(), readMetacard.getAttribute("description").getValue());
    assertEquals(
        metacard.getPointOfContact(), readMetacard.getAttribute("point-of-contact").getValue());

    MetacardType metacardType = metacard.getMetacardType();
    MetacardType readMetacardType = readMetacard.getMetacardType();

    assertEquals(metacardType.getName(), readMetacardType.getName());

    Set<AttributeDescriptor> oldAd = metacardType.getAttributeDescriptors();
    Set<AttributeDescriptor> newAd = readMetacardType.getAttributeDescriptors();

    assertEquals(oldAd.size(), newAd.size());

    for (int i = 0; i < oldAd.size(); i++) {

      AttributeDescriptor oldDescriptor = oldAd.iterator().next();

      boolean match = false;

      for (AttributeDescriptor newDescriptor : newAd) {

        if (oldDescriptor.equals(newDescriptor)) {
          match = true;
          break;
        }
      }
      assertTrue(match);
    }
  }

  @Test
  public void testSerializationSingleWrapped()
      throws IOException, ClassNotFoundException, URISyntaxException {

    MetacardImpl innerMetacard = new MetacardImpl();

    Date now = new Date();
    innerMetacard.setTitle("Flagstaff");
    innerMetacard.setContentTypeName("nitf");
    innerMetacard.setContentTypeVersion("DDF_20");
    innerMetacard.setLocation("POINT (1 0)");
    innerMetacard.setMetadata("<something/>");
    innerMetacard.setCreatedDate(now);
    innerMetacard.setResourceURI(new URI("http://ddf.com"));
    byte[] buffer = {-86};
    innerMetacard.setThumbnail(buffer);
    innerMetacard.setDescription("Northern Arizona City");
    innerMetacard.setPointOfContact("poc");

    Metacard metacard = new MetacardImpl(innerMetacard);

    Serializer<Metacard> serializer = new Serializer<>();

    serializer.serialize(metacard, DEFAULT_SERIALIZATION_FILE_LOCATION);

    Metacard readMetacard = serializer.deserialize(DEFAULT_SERIALIZATION_FILE_LOCATION);

    assertEquals(metacard.getTitle(), readMetacard.getTitle());
    assertEquals(metacard.getContentTypeName(), readMetacard.getContentTypeName());
    assertEquals(metacard.getContentTypeVersion(), readMetacard.getContentTypeVersion());
    assertEquals(metacard.getLocation(), readMetacard.getLocation());
    assertEquals(metacard.getMetadata(), readMetacard.getMetadata());
    assertEquals(
        metacard.getAttribute(Core.CREATED).getValue(),
        readMetacard.getAttribute(Core.CREATED).getValue());
    assertEquals(metacard.getExpirationDate(), readMetacard.getExpirationDate());
    assertEquals(metacard.getResourceURI(), readMetacard.getResourceURI());
    assertEquals(metacard.getResourceSize(), readMetacard.getResourceSize());
    assertEquals(
        metacard.getAttribute("description").getValue(),
        readMetacard.getAttribute("description").getValue());
    assertEquals(
        metacard.getAttribute("point-of-contact").getValue(),
        readMetacard.getAttribute("point-of-contact").getValue());

    assertTrue(Arrays.equals(metacard.getThumbnail(), readMetacard.getThumbnail()));

    MetacardType metacardType = metacard.getMetacardType();
    MetacardType readMetacardType = readMetacard.getMetacardType();

    assertEquals(metacardType.getName(), readMetacardType.getName());

    Set<AttributeDescriptor> oldAd = metacardType.getAttributeDescriptors();
    Set<AttributeDescriptor> newAd = readMetacardType.getAttributeDescriptors();

    assertEquals(oldAd.size(), newAd.size());

    assertEquals(oldAd.size(), newAd.size());

    for (int i = 0; i < oldAd.size(); i++) {

      AttributeDescriptor oldDescriptor = oldAd.iterator().next();

      boolean match = false;

      for (AttributeDescriptor newDescriptor : newAd) {

        if (oldDescriptor.equals(newDescriptor)) {
          match = true;
          break;
        }
      }
      assertTrue(match);
    }
  }

  @Test
  public void testSerializingEmptyMetacard() throws IOException, ClassNotFoundException {

    MetacardImpl metacard = new MetacardImpl();

    Serializer<Metacard> serializer = new Serializer<Metacard>();

    serializer.serialize(metacard, DEFAULT_SERIALIZATION_FILE_LOCATION);

    Metacard readMetacard = serializer.deserialize(DEFAULT_SERIALIZATION_FILE_LOCATION);

    MetacardType metacardType = metacard.getMetacardType();
    MetacardType readMetacardType = readMetacard.getMetacardType();

    assertEquals(metacardType.getName(), readMetacardType.getName());

    for (AttributeDescriptor ad : readMetacardType.getAttributeDescriptors()) {
      assertNull(readMetacard.getAttribute(ad.getName()));
    }
  }

  @Test
  public void testSerializingWithEmptyMetacardType() throws IOException, ClassNotFoundException {

    MetacardImpl metacard = new MetacardImpl(new EmptyMetacardType());

    Serializer<Metacard> serializer = new Serializer<Metacard>();

    serializer.serialize(metacard, DEFAULT_SERIALIZATION_FILE_LOCATION);

    Metacard readMetacard = serializer.deserialize(DEFAULT_SERIALIZATION_FILE_LOCATION);

    MetacardType metacardType = metacard.getMetacardType();
    MetacardType readMetacardType = readMetacard.getMetacardType();

    assertEquals(metacardType.getName(), readMetacardType.getName());
    assertTrue(readMetacardType.getAttributeDescriptors().isEmpty());
    assertEquals(
        metacardType.getAttributeDescriptor(null), readMetacardType.getAttributeDescriptor(null));

    assertTrue(readMetacardType.getName() == null);
    assertTrue(readMetacardType.getAttributeDescriptor(null) == null);
  }

  @Test
  public void testSerializingEmptyWrappedMetacardType() throws IOException, ClassNotFoundException {

    MetacardImpl metacard = new MetacardImpl(new MetacardImpl(new EmptyMetacardType()));

    Serializer<Metacard> serializer = new Serializer<Metacard>();

    serializer.serialize(metacard, DEFAULT_SERIALIZATION_FILE_LOCATION);

    Metacard readMetacard = serializer.deserialize(DEFAULT_SERIALIZATION_FILE_LOCATION);
    MetacardType metacardType = metacard.getMetacardType();
    MetacardType readMetacardType = readMetacard.getMetacardType();

    assertNotNull(readMetacardType);

    assertTrue(readMetacardType.getName() == null);
    assertTrue(readMetacardType.getAttributeDescriptor(null) == null);
    assertTrue(readMetacardType.getAttributeDescriptors().isEmpty());

    assertEquals(metacardType.getName(), readMetacardType.getName());
    assertEquals(
        metacardType.getAttributeDescriptor(null), readMetacardType.getAttributeDescriptor(null));
  }

  /*
   * A test where a metacardType is not defined but there are attributes in the metacard
   */
  @Test()
  public void testDeserializingUndefinedMetacardType() throws IOException, ClassNotFoundException {
    MetacardImpl metacard = new MetacardImpl(new EmptyMetacardType());

    metacard.setTitle("someTitle");

    Serializer<Metacard> serializer = new Serializer<Metacard>();

    serializer.serialize(metacard, DEFAULT_SERIALIZATION_FILE_LOCATION);

    Metacard readMetacard = serializer.deserialize(DEFAULT_SERIALIZATION_FILE_LOCATION);

    // the expected return is an empty list because it is documented that
    // the MetacardImpl will not use the original MetacardType
    // implementation.
    // It will use ddf.catalog.data.MetacardTypeImpl
    // which does not allow null for attribute descriptors list
    assertEquals(0, readMetacard.getMetacardType().getAttributeDescriptors().size());

    assertEquals(null, readMetacard.getMetacardType().getName());

    assertEquals("someTitle", readMetacard.getTitle());

    assertEquals(null, readMetacard.getSourceId());
  }

  /*
   * Test when an attribute exists but is not defined in the Metacard type
   */
  @Test()
  public void testDeserializingHiddenAttribute() throws IOException, ClassNotFoundException {
    MetacardImpl metacard = new MetacardImpl();

    metacard.setAttribute(new AttributeImpl("newName", "newNameValue"));

    Serializer<Metacard> serializer = new Serializer<Metacard>();

    serializer.serialize(metacard, DEFAULT_SERIALIZATION_FILE_LOCATION);

    Metacard readMetacard = serializer.deserialize(DEFAULT_SERIALIZATION_FILE_LOCATION);

    assertEquals("newNameValue", readMetacard.getAttribute("newName").getValue());
  }
}
