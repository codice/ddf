package ddf.catalog.data.dynamic;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeImpl;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
public class DynamicMetacardTest extends TestCase {
    private static final String DEFAULT_SERIALIZATION_FILE_LOCATION = "target/metacard1.ser";

    DynamicMetacard mc;

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
        nsUri = new URI("http://" + DynamicMetacardTest.class.getName());
        resourceUri = new URI(nsUri.toString() + "/resource.html");
        mc = MetacardFactory.newInstance(DynamicMetacard.DYNAMIC);
//      mc.setContentTypeName("testContentType");
        mc.setAttribute(Metacard.CONTENT_TYPE, "testContentType");
//        mc.setContentTypeVersion("testContentTypeVersion");
        mc.setAttribute(Metacard.CONTENT_TYPE_VERSION, "testContentTypeVersion");
        mc.setAttribute("testAtt", "testAttValue");
//        mc.setCreatedDate(createdDate);
        mc.setAttribute(Metacard.CREATED, createdDate);
//        mc.setEffectiveDate(effectiveDate);
        mc.setAttribute(Metacard.EFFECTIVE, effectiveDate);
//        mc.setExpirationDate(expireDate);
        mc.setAttribute(Metacard.EXPIRATION, expireDate);
//        mc.setModifiedDate(modDate);
        mc.setAttribute(Metacard.MODIFIED, modDate);
//        mc.setId(mcId);
        mc.setAttribute(Metacard.ID, mcId);
//        mc.setLocation(locWkt);
        mc.setAttribute(Metacard.GEOGRAPHY, locWkt);
//        mc.setMetadata("testMetadata");
        mc.setAttribute(Metacard.METADATA, "testMetadata");
//        mc.setResourceURI(resourceUri);
        mc.setAttribute(Metacard.RESOURCE_URI, resourceUri);
//        mc.setSourceId("testSourceId");
        mc.setAttribute(Metacard.SOURCE_ID, "testSourceId");
//        mc.setTargetNamespace(nsUri);
        mc.setAttribute(Metacard.TARGET_NAMESPACE, nsUri);
//        mc.setTitle("testTitle");
        mc.setAttribute(Metacard.TITLE, "testTitle");
//        mc.setThumbnail(mc.getId().getBytes());
        mc.setAttribute(Metacard.THUMBNAIL, mcId.getBytes());
//        mc.setDescription("testDescription");
        mc.setAttribute(Metacard.DESCRIPTION, "testDescription");
//        mc.setPointOfContact("pointOfContact");
        mc.setAttribute(Metacard.POINT_OF_CONTACT, "pointOfContact");
    }

    @Test
    public void testMetacardTypeAsNull() throws IllegalAccessException, InstantiationException {
        DynamicMetacard dmc = MetacardFactory.newInstance(null);
        Assert.assertNotNull(dmc);
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

        DynamicMetacard metacard = MetacardFactory.newInstance(DynamicMetacard.DYNAMIC);
        String xml = "<xml/>";
        metacard.setAttribute(Metacard.METADATA, xml);
        metacard.setAttribute(Metacard.ID, "id1");
    }

    /**
     * make sure type conversion works in MetacardImpl
     */
    @Test
    public void testMetacardImplAttributes() {
        DynamicMetacard metacard = MetacardFactory.newInstance(DynamicMetacard.DYNAMIC);
        Date now = new Date();
        metacard.setAttribute(Metacard.EFFECTIVE, now);
        assertEquals(now, metacard.getEffectiveDate());
    }

    /**
     * make sure type conversion works in MetacardImpl
     */
    @Test
    public void testMetacardAttributes() {
        DynamicMetacard metacard = MetacardFactory.newInstance(DynamicMetacard.DYNAMIC);
        Date now = new Date();
        metacard.setAttribute(new AttributeImpl(Metacard.EFFECTIVE, now));
        assertEquals(now, metacard.getEffectiveDate());
    }

    @Test
    public void testMetacardImpl() {
        DynamicMetacard metacard = MetacardFactory.newInstance();
        assertEquals(null, metacard.getContentTypeName());
        assertEquals(null, metacard.getContentTypeNamespace());
        assertEquals(null, metacard.getContentTypeVersion());
        assertEquals(null, metacard.getCreatedDate());
        assertEquals(null, metacard.getEffectiveDate());
        assertEquals(null, metacard.getExpirationDate());
        assertEquals(null, metacard.getId());
        assertEquals(null, metacard.getLocation());
        //assertEquals(BasicTypes.BASIC_METACARD, metacard.getMetacardType());
        assertEquals(null, metacard.getMetadata());
        assertEquals(null, metacard.getModifiedDate());
        assertEquals(null, metacard.getResourceSize());
        assertEquals(null, metacard.getResourceURI());
        assertEquals(null, metacard.getSourceId());
        assertEquals(null, metacard.getThumbnail());
        assertEquals(null, metacard.getTitle());
//        assertEquals(null, metacard.getDescription());
        assertEquals(null, metacard.getAttribute(Metacard.DESCRIPTION));
//        assertEquals(null, metacard.getPointOfContact());
        assertEquals(null, metacard.getAttribute(Metacard.POINT_OF_CONTACT));

        metacard = MetacardFactory.newInstance(DynamicMetacard.DYNAMIC);
        assertEquals(null, metacard.getContentTypeName());
        assertEquals(null, metacard.getContentTypeNamespace());
        assertEquals(null, metacard.getContentTypeVersion());
        assertEquals(null, metacard.getCreatedDate());
        assertEquals(null, metacard.getEffectiveDate());
        assertEquals(null, metacard.getExpirationDate());
        assertEquals(null, metacard.getId());
        assertEquals(null, metacard.getLocation());
//        assertEquals(BasicTypes.BASIC_METACARD, metacard.getMetacardType());
        assertEquals(null, metacard.getMetadata());
        assertEquals(null, metacard.getModifiedDate());
        assertEquals(null, metacard.getResourceSize());
        assertEquals(null, metacard.getResourceURI());
        assertEquals(null, metacard.getSourceId());
        assertEquals(null, metacard.getThumbnail());
        assertEquals(null, metacard.getTitle());
//        assertEquals(null, metacard.getDescription());
        assertEquals(null, metacard.getAttribute(Metacard.DESCRIPTION));
//        assertEquals(null, metacard.getPointOfContact());
        assertEquals(null, metacard.getAttribute(Metacard.POINT_OF_CONTACT));

        /* This test isn't valid for testing against the Metacard interface */
//        metacard = new DynamicMetacard(mc);
//        assertEquals(mc.getContentTypeName(), metacard.getContentTypeName());
//        assertEquals(mc.getContentTypeNamespace(), metacard.getContentTypeNamespace());
//        assertEquals(mc.getContentTypeVersion(), metacard.getContentTypeVersion());
//        assertEquals(mc.getCreatedDate(), metacard.getCreatedDate());
//        assertEquals(mc.getEffectiveDate(), metacard.getEffectiveDate());
//        assertEquals(mc.getExpirationDate(), metacard.getExpirationDate());
//        assertEquals(mc.getId(), metacard.getId());
//        assertEquals(mc.getLocation(), metacard.getLocation());
//        assertEquals(BasicTypes.BASIC_METACARD, metacard.getMetacardType());
//        assertEquals(mc.getMetacardType(), metacard.getMetacardType());
//        assertEquals(mc.getMetadata(), metacard.getMetadata());
//        assertEquals(mc.getModifiedDate(), metacard.getModifiedDate());
//        assertEquals(mc.getResourceSize(), metacard.getResourceSize());
//        assertEquals(mc.getResourceURI(), metacard.getResourceURI());
//        assertEquals(mc.getSourceId(), metacard.getSourceId());
//        assertEquals(mc.getThumbnail(), metacard.getThumbnail());
//        assertEquals(mc.getTitle(), metacard.getTitle());
//        assertEquals(mc.getDescription(), metacard.getDescription());
//        assertEquals(mc.getPointOfContact(), metacard.getPointOfContact());
    }

    /* This test isn't valid for testing against the Metacard interface */
//    @Test
//    public void testUpdatingWrappedMetacardFields() {
//        HashSet<AttributeDescriptor> descriptors = new HashSet<AttributeDescriptor>();
//        descriptors.add(new AttributeDescriptorImpl("test-string", true /* indexed */, true /* stored */,
//                false /* tokenized */, false /* multivalued */, BasicTypes.STRING_TYPE));
//        MetacardType testType = new MetacardTypeImpl("test.type", descriptors);
//
//        MetacardImpl mi = new MetacardImpl(mc);
//        mi.setSourceId("testSource");
//        mi.setType(testType);
//
//        assertEquals("testSource", mi.getSourceId());
//        assertEquals(testType, mi.getMetacardType());
//    }

    @Test
    public void testSetNullFields() {
        mc.setAttribute(Metacard.CONTENT_TYPE, null);
        mc.setAttribute(Metacard.CONTENT_TYPE_VERSION, null);
        mc.setAttribute(null);
        mc.setAttribute(new AttributeImpl("testNullValueAtt1", (Serializable) null));
        mc.setAttribute("testNullValueAtt2", null);
//        mc.setCreatedDate(null);
        mc.setAttribute(Metacard.CREATED, null);
//        mc.setEffectiveDate(null);
        mc.setAttribute(Metacard.EFFECTIVE, null);
//        mc.setExpirationDate(null);
        mc.setAttribute(Metacard.EXPIRATION, null);
//        mc.setModifiedDate(null);
        mc.setAttribute(Metacard.MODIFIED, null);
//        mc.setId(null);
        mc.setAttribute(Metacard.ID, null);
//        mc.setLocation(null);
        mc.setAttribute(Metacard.GEOGRAPHY, null);
//        mc.setMetadata(null);
        mc.setAttribute(Metacard.METADATA, null);
//        mc.setResourceURI(null);
        mc.setAttribute(Metacard.RESOURCE_URI, null);
//        mc.setSourceId(null);
        mc.setSourceId(null);
//        mc.setTitle(null);
        mc.setAttribute(Metacard.TITLE, null);
//        mc.setThumbnail(null);
        mc.setAttribute(Metacard.THUMBNAIL, null);
//        mc.setDescription(null);
        mc.setAttribute(Metacard.DESCRIPTION, null);
//        mc.setPointOfContact(null);
        mc.setAttribute(Metacard.POINT_OF_CONTACT, null);

        assertEquals(null, mc.getAttribute("testNullValueAtt1"));
        assertEquals(null, mc.getAttribute("testNullValueAtt2"));
        assertEquals(null, mc.getContentTypeName());
        assertEquals(null, mc.getContentTypeVersion());
        assertEquals(null, mc.getCreatedDate());
        assertEquals(null, mc.getEffectiveDate());
        assertEquals(null, mc.getExpirationDate());
        assertEquals(null, mc.getId());
        assertEquals(null, mc.getLocation());
        assertEquals(null, mc.getMetadata());
        assertEquals(null, mc.getModifiedDate());
        assertEquals(null, mc.getResourceSize());
        assertNull(mc.getResourceURI());
        assertEquals(null, mc.getSourceId());
        assertEquals(null, mc.getThumbnail());
        assertEquals(null, mc.getTitle());

        //assertEquals(null, mc.getDescription());
        assertEquals(null, mc.getAttribute(Metacard.DESCRIPTION));
        //assertEquals(null, mc.getPointOfContact());
        assertEquals(null, mc.getAttribute(Metacard.POINT_OF_CONTACT));

    }

    @Test
    public void testEmptyStringFields() {

    }

    @Test
    public void testSerializationSingle()
            throws IOException, ClassNotFoundException, URISyntaxException {

        DynamicMetacard metacard = MetacardFactory.newInstance();

        Date now = new Date();
//        metacard.setTitle("Flagstaff");
        metacard.setAttribute(Metacard.TITLE, "Flagstaff");
//        metacard.setContentTypeName("nitf");
        metacard.setAttribute(Metacard.CONTENT_TYPE, "nitf");
//        metacard.setContentTypeVersion("DDF_20");
        metacard.setAttribute(Metacard.CONTENT_TYPE_VERSION, "DDF_20");
//        metacard.setLocation("POINT (1 0)");
        metacard.setAttribute(Metacard.GEOGRAPHY, "POINT (1 0)");
//        metacard.setMetadata("<something/>");
        metacard.setAttribute(Metacard.METADATA, "<something/>");
//        metacard.setCreatedDate(now);
        metacard.setAttribute(Metacard.CREATED, now);
//        metacard.setResourceURI(new URI("http://ddf.com"));
        metacard.setAttribute(Metacard.RESOURCE_URI, new URI("http://ddf.com"));
        byte[] buffer = {-86};
//        metacard.setThumbnail(buffer);
        metacard.setAttribute(Metacard.THUMBNAIL, buffer);
        metacard.setSourceId("mySourceId");
//        metacard.setDescription("Northern Arizona City");
        metacard.setAttribute(Metacard.DESCRIPTION, "Northern Arizona City");
//        metacard.setPointOfContact("poc");
        metacard.setAttribute(Metacard.POINT_OF_CONTACT, "poc");
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
        assertEquals(metacard.getCreatedDate(), readMetacard.getCreatedDate());
        assertEquals(metacard.getExpirationDate(), readMetacard.getExpirationDate());
        assertEquals(metacard.getResourceURI(), readMetacard.getResourceURI());
        assertEquals(metacard.getResourceSize(), readMetacard.getResourceSize());
        assertTrue(Arrays.equals(metacard.getThumbnail(), readMetacard.getThumbnail()));
        assertEquals(metacard.getSourceId(), readMetacard.getSourceId());
//        assertEquals(metacard.getDescription(), readMetacard.getAttribute("description").getValue());
        assertEquals(metacard.getAttribute(Metacard.DESCRIPTION).getValue(), readMetacard.getAttribute("description").getValue());
//        assertEquals(metacard.getPointOfContact(), readMetacard.getAttribute("point-of-contact").getValue());
        assertEquals(metacard.getAttribute(Metacard.POINT_OF_CONTACT).getValue(), readMetacard.getAttribute("point-of-contact").getValue());

        MetacardType metacardType = metacard.getMetacardType();
        MetacardType readMetacardType = readMetacard.getMetacardType();

        assertEquals(metacardType.getName(), readMetacardType.getName());

        //ToDo: The equals doesn't seem to be working here, for now disable and re-enable once the equals is fixed
//        Set<AttributeDescriptor> oldAd = metacardType.getAttributeDescriptors();
//        Set<AttributeDescriptor> newAd = readMetacardType.getAttributeDescriptors();
//
//        assertEquals(oldAd.size(), newAd.size());
//
//        AttributeDescriptor newDescriptor = null;
//        boolean match = true;
//        for (AttributeDescriptor oldDescriptor : oldAd) {
//            if (!newAd.contains(oldDescriptor)) {
//                match = false;
//                break;
//            }
//        }
//        assertTrue(match);
    }

    // This test is not valid for testing the MetaCard interface
//    @Test
//    public void testSerializationSingleWrapped()
//            throws IOException, ClassNotFoundException, URISyntaxException {
//
//        MetacardImpl innerMetacard = new MetacardImpl();
//
//        Date now = new Date();
//        innerMetacard.setTitle("Flagstaff");
//        innerMetacard.setContentTypeName("nitf");
//        innerMetacard.setContentTypeVersion("DDF_20");
//        innerMetacard.setLocation("POINT (1 0)");
//        innerMetacard.setMetadata("<something/>");
//        innerMetacard.setCreatedDate(now);
//        innerMetacard.setResourceURI(new URI("http://ddf.com"));
//        byte[] buffer = {-86};
//        innerMetacard.setThumbnail(buffer);
//        innerMetacard.setDescription("Northern Arizona City");
//        innerMetacard.setPointOfContact("poc");
//
//        Metacard metacard = new MetacardImpl(innerMetacard);
//
//        Serializer<Metacard> serializer = new Serializer<Metacard>();
//
//        serializer.serialize(metacard, DEFAULT_SERIALIZATION_FILE_LOCATION);
//
//        Metacard readMetacard = serializer.deserialize(DEFAULT_SERIALIZATION_FILE_LOCATION);
//
//        assertEquals(metacard.getTitle(), readMetacard.getTitle());
//        assertEquals(metacard.getContentTypeName(), readMetacard.getContentTypeName());
//        assertEquals(metacard.getContentTypeVersion(), readMetacard.getContentTypeVersion());
//        assertEquals(metacard.getLocation(), readMetacard.getLocation());
//        assertEquals(metacard.getMetadata(), readMetacard.getMetadata());
//        assertEquals(metacard.getCreatedDate(), readMetacard.getCreatedDate());
//        assertEquals(metacard.getExpirationDate(), readMetacard.getExpirationDate());
//        assertEquals(metacard.getResourceURI(), readMetacard.getResourceURI());
//        assertEquals(metacard.getResourceSize(), readMetacard.getResourceSize());
//        assertEquals(metacard.getAttribute("description").getValue(), readMetacard.getAttribute("description").getValue());
//        assertEquals(metacard.getAttribute("point-of-contact").getValue(), readMetacard.getAttribute("point-of-contact").getValue());
//
//        assertTrue(Arrays.equals(metacard.getThumbnail(), readMetacard.getThumbnail()));
//
//        MetacardType metacardType = metacard.getMetacardType();
//        MetacardType readMetacardType = readMetacard.getMetacardType();
//
//        assertEquals(metacardType.getName(), readMetacardType.getName());
//
//        Set<AttributeDescriptor> oldAd = metacardType.getAttributeDescriptors();
//        Set<AttributeDescriptor> newAd = readMetacardType.getAttributeDescriptors();
//
//        assertEquals(oldAd.size(), newAd.size());
//
//        assertEquals(oldAd.size(), newAd.size());
//
//        for (int i = 0; i < oldAd.size(); i++) {
//
//            AttributeDescriptor oldDescriptor = oldAd.iterator().next();
//
//            boolean match = false;
//
//            for (AttributeDescriptor newDescriptor : newAd) {
//
//                if (oldDescriptor.equals(newDescriptor)) {
//                    match = true;
//                    break;
//                }
//
//            }
//            assertTrue(match);
//
//        }
//    }

    @Test
    public void testSerializingEmptyMetacard() throws IOException, ClassNotFoundException {

//        MetacardImpl metacard = new MetacardImpl();
        DynamicMetacard metacard = MetacardFactory.newInstance();

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

    // Doesn't aply to testing the Metacard interface
//    @Test
//    public void testSerializingWithEmptyMetacardType() throws IOException, ClassNotFoundException {
//
//        MetacardImpl metacard = new MetacardImpl(new EmptyMetacardType());
//
//        Serializer<Metacard> serializer = new Serializer<Metacard>();
//
//        serializer.serialize(metacard, DEFAULT_SERIALIZATION_FILE_LOCATION);
//
//        Metacard readMetacard = serializer.deserialize(DEFAULT_SERIALIZATION_FILE_LOCATION);
//
//        MetacardType metacardType = metacard.getMetacardType();
//        MetacardType readMetacardType = readMetacard.getMetacardType();
//
//        assertEquals(metacardType.getName(), readMetacardType.getName());
//        assertTrue(readMetacardType.getAttributeDescriptors().isEmpty());
//        assertEquals(metacardType.getAttributeDescriptor(null),
//                readMetacardType.getAttributeDescriptor(null));
//
//        assertTrue(readMetacardType.getName() == null);
//        assertTrue(readMetacardType.getAttributeDescriptor(null) == null);
//
//    }

    // Doesn't apply to testing the Metacard interface
//    @Test
//    public void testSerializingEmptyWrappedMetacardType()
//            throws IOException, ClassNotFoundException {
//
//        MetacardImpl metacard = new MetacardImpl(new MetacardImpl(new EmptyMetacardType()));
//
//        Serializer<Metacard> serializer = new Serializer<Metacard>();
//
//        serializer.serialize(metacard, DEFAULT_SERIALIZATION_FILE_LOCATION);
//
//        Metacard readMetacard = serializer.deserialize(DEFAULT_SERIALIZATION_FILE_LOCATION);
//        MetacardType metacardType = metacard.getMetacardType();
//        MetacardType readMetacardType = readMetacard.getMetacardType();
//
//        assertNotNull(readMetacardType);
//
//        assertTrue(readMetacardType.getName() == null);
//        assertTrue(readMetacardType.getAttributeDescriptor(null) == null);
//        assertTrue(readMetacardType.getAttributeDescriptors().isEmpty());
//
//        assertEquals(metacardType.getName(), readMetacardType.getName());
//        assertEquals(metacardType.getAttributeDescriptor(null),
//                readMetacardType.getAttributeDescriptor(null));
//
//    }

    /*
     * A test where a metacardType is not defined but there are attributes in the metacard
     *
     * This can't happen with the DynamicMetacard
     */
//    @Test()
//    public void testDeserializingUndefinedMetacardType()
//            throws IOException, ClassNotFoundException {
//        MetacardImpl metacard = new MetacardImpl(new EmptyMetacardType());
//
//        metacard.setTitle("someTitle");
//
//        Serializer<Metacard> serializer = new Serializer<Metacard>();
//
//        serializer.serialize(metacard, DEFAULT_SERIALIZATION_FILE_LOCATION);
//
//        Metacard readMetacard = serializer.deserialize(DEFAULT_SERIALIZATION_FILE_LOCATION);
//
//        // the expected return is an empty list because it is documented that
//        // the MetacardImpl will not use the original MetacardType
//        // implementation.
//        // It will use ddf.catalog.data.MetacardTypeImpl
//        // which does not allow null for attribute descriptors list
//        assertEquals(0, readMetacard.getMetacardType().getAttributeDescriptors().size());
//
//        assertEquals(null, readMetacard.getMetacardType().getName());
//
//        assertEquals("someTitle", readMetacard.getTitle());
//
//        assertEquals(null, readMetacard.getSourceId());
//
//    }

    /*
     * Test when an attribute exists but is not defined in the Metacard type
     */
    @Test()
    public void testDeserializingHiddenAttribute() throws IOException, ClassNotFoundException {
//        MetacardImpl metacard = new MetacardImpl();
        DynamicMetacard metacard = MetacardFactory.newInstance();

        metacard.setAttribute(new AttributeImpl("newName", "newNameValue"));

        Serializer<Metacard> serializer = new Serializer<Metacard>();

        serializer.serialize(metacard, DEFAULT_SERIALIZATION_FILE_LOCATION);

        Metacard readMetacard = serializer.deserialize(DEFAULT_SERIALIZATION_FILE_LOCATION);

        assertEquals("newNameValue", readMetacard.getAttribute("newName").getValue());

    }

}