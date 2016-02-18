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
package ddf.catalog.data.dynamic;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import junit.framework.TestCase;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.dynamic.api.DynamicMetacard;
import ddf.catalog.data.dynamic.api.MetacardFactory;
import ddf.catalog.data.dynamic.impl.MetacardFactoryImpl;
import ddf.catalog.data.impl.AttributeImpl;

/**
 * This test mimics the MetacardTest in the catalog-core-api-impl module. It tests the Metacard
 * interface as implemented by Dynamic Metacards. Method calls used that were not part of the
 * Metacard interface are commented out and the behavior is accomplished using Dynamic Metacard
 * methods.
 */
public class DynamicMetacardTest extends TestCase {
    private static final String DEFAULT_SERIALIZATION_FILE_LOCATION = "target/metacard1.ser";

    private MetacardFactory metacardFactory = new MetacardFactoryImpl();

    private DynamicMetacard mc;

    private Date createdDate;

    private Date effectiveDate;

    private Date expireDate;

    private Date modDate;

    private String mcId;

    private String locWkt;

    private URI resourceUri;

    private URI nsUri;

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
        mc = metacardFactory.newInstance(DynamicMetacard.DYNAMIC);
        mc.setAttribute(Metacard.CONTENT_TYPE, "testContentType");
        mc.setAttribute(Metacard.CONTENT_TYPE_VERSION, "testContentTypeVersion");
        mc.setAttribute("testAtt", "testAttValue");
        mc.setAttribute(Metacard.CREATED, createdDate);
        mc.setAttribute(Metacard.EFFECTIVE, effectiveDate);
        mc.setAttribute(Metacard.EXPIRATION, expireDate);
        mc.setAttribute(Metacard.MODIFIED, modDate);
        mc.setAttribute(Metacard.ID, mcId);
        mc.setAttribute(Metacard.GEOGRAPHY, locWkt);
        mc.setAttribute(Metacard.METADATA, "testMetadata");
        mc.setAttribute(Metacard.RESOURCE_URI, resourceUri);
        mc.setAttribute(Metacard.SOURCE_ID, "testSourceId");
        mc.setAttribute(Metacard.TARGET_NAMESPACE, nsUri);
        mc.setAttribute(Metacard.TITLE, "testTitle");
        mc.setAttribute(Metacard.THUMBNAIL, mcId.getBytes());
        mc.setAttribute(Metacard.DESCRIPTION, "testDescription");
        mc.setAttribute(Metacard.POINT_OF_CONTACT, "pointOfContact");
    }

    @Test
    public void testMetacardTypeAsNull() throws IllegalAccessException, InstantiationException {
        DynamicMetacard dmc = metacardFactory.newInstance((String)null);
        Assert.assertNull(dmc);
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

        DynamicMetacard
                metacard = metacardFactory.newInstance(DynamicMetacard.DYNAMIC);
        String xml = "<xml/>";
        metacard.setAttribute(Metacard.METADATA, xml);
        metacard.setAttribute(Metacard.ID, "id1");
    }

    /**
     * make sure type conversion works in MetacardImpl
     */
    @Test
    public void testMetacardImplAttributes() {
        DynamicMetacard
                metacard = metacardFactory.newInstance(DynamicMetacard.DYNAMIC);
        Date now = new Date();
        metacard.setAttribute(Metacard.EFFECTIVE, now);
        assertEquals(now, metacard.getEffectiveDate());
    }

    /**
     * make sure type conversion works in MetacardImpl
     */
    @Test
    public void testMetacardAttributes() {
        DynamicMetacard
                metacard = metacardFactory.newInstance(DynamicMetacard.DYNAMIC);
        Date now = new Date();
        metacard.setAttribute(new AttributeImpl(Metacard.EFFECTIVE, now));
        assertEquals(now, metacard.getEffectiveDate());
    }

    @Test
    public void testMetacardImpl() {
        DynamicMetacard metacard = metacardFactory.newInstance();
        assertEquals(null, metacard.getContentTypeName());
        assertEquals(null, metacard.getContentTypeNamespace());
        assertEquals(null, metacard.getContentTypeVersion());
        assertEquals(null, metacard.getCreatedDate());
        assertEquals(null, metacard.getEffectiveDate());
        assertEquals(null, metacard.getExpirationDate());
        assertEquals(null, metacard.getId());
        assertEquals(null, metacard.getLocation());
        assertEquals(null, metacard.getMetadata());
        assertEquals(null, metacard.getModifiedDate());
        assertEquals(null, metacard.getResourceSize());
        assertEquals(null, metacard.getResourceURI());
        assertEquals(null, metacard.getSourceId());
        assertEquals(null, metacard.getThumbnail());
        assertEquals(null, metacard.getTitle());
        assertEquals(null, metacard.getAttribute(Metacard.DESCRIPTION));
        assertEquals(null, metacard.getAttribute(Metacard.POINT_OF_CONTACT));

        metacard = metacardFactory.newInstance(DynamicMetacard.DYNAMIC);
        assertEquals(null, metacard.getContentTypeName());
        assertEquals(null, metacard.getContentTypeNamespace());
        assertEquals(null, metacard.getContentTypeVersion());
        assertEquals(null, metacard.getCreatedDate());
        assertEquals(null, metacard.getEffectiveDate());
        assertEquals(null, metacard.getExpirationDate());
        assertEquals(null, metacard.getId());
        assertEquals(null, metacard.getLocation());
        assertEquals(null, metacard.getMetadata());
        assertEquals(null, metacard.getModifiedDate());
        assertEquals(null, metacard.getResourceSize());
        assertEquals(null, metacard.getResourceURI());
        assertEquals(null, metacard.getSourceId());
        assertEquals(null, metacard.getThumbnail());
        assertEquals(null, metacard.getTitle());
        assertEquals(null, metacard.getAttribute(Metacard.DESCRIPTION));
        assertEquals(null, metacard.getAttribute(Metacard.POINT_OF_CONTACT));
    }

    @Test
    public void testSetNullFields() {
        mc.setAttribute(Metacard.CONTENT_TYPE, null);
        mc.setAttribute(Metacard.CONTENT_TYPE_VERSION, null);
        mc.setAttribute(null);
        mc.setAttribute(new AttributeImpl("testNullValueAtt1", (Serializable) null));
        mc.setAttribute("testNullValueAtt2", null);
        mc.setAttribute(Metacard.CREATED, null);
        mc.setAttribute(Metacard.EFFECTIVE, null);
        mc.setAttribute(Metacard.EXPIRATION, null);
        mc.setAttribute(Metacard.MODIFIED, null);
        mc.setAttribute(Metacard.ID, null);
        mc.setAttribute(Metacard.GEOGRAPHY, null);
        mc.setAttribute(Metacard.METADATA, null);
        mc.setAttribute(Metacard.RESOURCE_URI, null);
        mc.setSourceId(null);
        mc.setAttribute(Metacard.TITLE, null);
        mc.setAttribute(Metacard.THUMBNAIL, null);
        mc.setAttribute(Metacard.DESCRIPTION, null);
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

        assertEquals(null, mc.getAttribute(Metacard.DESCRIPTION));
        assertEquals(null, mc.getAttribute(Metacard.POINT_OF_CONTACT));

    }

    @Test
    public void testEmptyStringFields() {

    }

    @Test
    public void testSerializationSingle()
            throws IOException, ClassNotFoundException, URISyntaxException {

        DynamicMetacard metacard = metacardFactory.newInstance();

        Date now = new Date();
        metacard.setAttribute(Metacard.TITLE, "Flagstaff");
        metacard.setAttribute(Metacard.CONTENT_TYPE, "nitf");
        metacard.setAttribute(Metacard.CONTENT_TYPE_VERSION, "DDF_20");
        metacard.setAttribute(Metacard.GEOGRAPHY, "POINT (1 0)");
        metacard.setAttribute(Metacard.METADATA, "<something/>");
        metacard.setAttribute(Metacard.CREATED, now);
        metacard.setAttribute(Metacard.RESOURCE_URI, new URI("http://ddf.com"));
        byte[] buffer = {-86};
        metacard.setAttribute(Metacard.THUMBNAIL, buffer);
        metacard.setSourceId("mySourceId");
        metacard.setAttribute(Metacard.DESCRIPTION, "Northern Arizona City");
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
        assertEquals(metacard.getAttribute(Metacard.DESCRIPTION).getValue(), readMetacard.getAttribute("description").getValue());
        assertEquals(metacard.getAttribute(Metacard.POINT_OF_CONTACT).getValue(), readMetacard.getAttribute("point-of-contact").getValue());

        MetacardType metacardType = metacard.getMetacardType();
        MetacardType readMetacardType = readMetacard.getMetacardType();

        assertEquals(metacardType.getName(), readMetacardType.getName());

    }

    @Test
    public void testSerializingEmptyMetacard() throws IOException, ClassNotFoundException {

        DynamicMetacard metacard = metacardFactory.newInstance();

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

    /*
     * Test when an attribute exists but is not defined in the Metacard type
     */
    @Test()
    public void testDeserializingHiddenAttribute() throws IOException, ClassNotFoundException {
        DynamicMetacard metacard = metacardFactory.newInstance();

        metacard.setAttribute(new AttributeImpl("newName", "newNameValue"));

        Serializer<Metacard> serializer = new Serializer<Metacard>();

        serializer.serialize(metacard, DEFAULT_SERIALIZATION_FILE_LOCATION);

        Metacard readMetacard = serializer.deserialize(DEFAULT_SERIALIZATION_FILE_LOCATION);

        assertEquals("newNameValue", readMetacard.getAttribute("newName").getValue());

    }

}