/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.data.impl;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.base.Charsets;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;

public class MetacardImplTest {

    private static final String DEFAULT_SERIALIZATION_FILE_LOCATION = "target/metacard1.ser";

    private static final String TEST_GENERIC_STRING = "string";

    private static final String TEST_ATTRIBUTE_NAME = "TestAttributeName";

    private static final String TEST_ALT_ATTRIB_NAME = "testattributename";

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
        mcId = UUID.randomUUID()
                .toString();
        locWkt = "POLYGON ((30 10, 10 20, 20 40, 40 40, 30 10))";
        nsUri = new URI("http://" + MetacardImplTest.class.getName());
        resourceUri = new URI(nsUri.toString() + "/resource.html");
        mc = new MetacardImpl(BasicTypes.BASIC_METACARD);
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
        mc.setThumbnail(mc.getId()
                .getBytes());
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

    /**
     * make sure type conversion works in MetacardImpl
     */
    @Test
    public void testMetacardImplAttributes() {
        MetacardImpl metacard = new MetacardImpl();
        Date now = new Date();
        metacard.setEffectiveDate(now);
        assertEquals(now, metacard.getEffectiveDate());
    }

    /**
     * make sure type conversion works in MetacardImpl
     */
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
        assertEquals(null, mi.getCreatedDate());
        assertEquals(null, mi.getEffectiveDate());
        assertEquals(null, mi.getExpirationDate());
        assertEquals(null, mi.getId());
        assertEquals(null, mi.getLocation());
        assertEquals(BasicTypes.BASIC_METACARD, mi.getMetacardType());
        assertEquals(null, mi.getMetadata());
        assertEquals(null, mi.getModifiedDate());
        assertEquals(null, mi.getResourceSize());
        assertEquals(null, mi.getResourceURI());
        assertEquals(null, mi.getSourceId());
        assertEquals(null, mi.getThumbnail());
        assertEquals(null, mi.getTitle());
        assertEquals(null, mi.getDescription());
        assertEquals(null, mi.getPointOfContact());

        mi = new MetacardImpl(BasicTypes.BASIC_METACARD);
        assertEquals(null, mi.getContentTypeName());
        assertEquals(null, mi.getContentTypeNamespace());
        assertEquals(null, mi.getContentTypeVersion());
        assertEquals(null, mi.getCreatedDate());
        assertEquals(null, mi.getEffectiveDate());
        assertEquals(null, mi.getExpirationDate());
        assertEquals(null, mi.getId());
        assertEquals(null, mi.getLocation());
        assertEquals(BasicTypes.BASIC_METACARD, mi.getMetacardType());
        assertEquals(null, mi.getMetadata());
        assertEquals(null, mi.getModifiedDate());
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
        assertEquals(mc.getCreatedDate(), mi.getCreatedDate());
        assertEquals(mc.getEffectiveDate(), mi.getEffectiveDate());
        assertEquals(mc.getExpirationDate(), mi.getExpirationDate());
        assertEquals(mc.getId(), mi.getId());
        assertEquals(mc.getLocation(), mi.getLocation());
        assertEquals(BasicTypes.BASIC_METACARD, mi.getMetacardType());
        assertEquals(mc.getMetacardType(), mi.getMetacardType());
        assertEquals(mc.getMetadata(), mi.getMetadata());
        assertEquals(mc.getModifiedDate(), mi.getModifiedDate());
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
        descriptors.add(new AttributeDescriptorImpl("test-string",
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
        assertEquals(null, mc.getCreatedDate());
        assertEquals(null, mc.getEffectiveDate());
        assertEquals(null, mc.getExpirationDate());
        assertEquals(null, mc.getId());
        assertEquals(null, mc.getLocation());
        assertEquals(null, mc.getMetadata());
        assertEquals(null, mc.getModifiedDate());
        assertEquals(null, mc.getResourceSize());
        assertNotNull(mc.getResourceURI());
        assertEquals(null, mc.getSourceId());
        assertEquals(null, mc.getThumbnail());
        assertEquals(null, mc.getTitle());
        assertEquals(null, mc.getDescription());
        assertEquals(null, mc.getPointOfContact());

    }

    @Test
    public void testEmptyStringFields() {

    }

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
        assertEquals(metacard.getCreatedDate(), readMetacard.getCreatedDate());
        assertEquals(metacard.getExpirationDate(), readMetacard.getExpirationDate());
        assertEquals(metacard.getResourceURI(), readMetacard.getResourceURI());
        assertEquals(metacard.getResourceSize(), readMetacard.getResourceSize());
        assertTrue(Arrays.equals(metacard.getThumbnail(), readMetacard.getThumbnail()));
        assertEquals(metacard.getSourceId(), readMetacard.getSourceId());
        assertEquals(metacard.getDescription(),
                readMetacard.getAttribute("description")
                        .getValue());
        assertEquals(metacard.getPointOfContact(),
                readMetacard.getAttribute("point-of-contact")
                        .getValue());

        MetacardType metacardType = metacard.getMetacardType();
        MetacardType readMetacardType = readMetacard.getMetacardType();

        assertEquals(metacardType.getName(), readMetacardType.getName());

        Set<AttributeDescriptor> oldAd = metacardType.getAttributeDescriptors();
        Set<AttributeDescriptor> newAd = readMetacardType.getAttributeDescriptors();

        assertEquals(oldAd.size(), newAd.size());

        for (int i = 0; i < oldAd.size(); i++) {

            AttributeDescriptor oldDescriptor = oldAd.iterator()
                    .next();

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

        Serializer<Metacard> serializer = new Serializer<Metacard>();

        serializer.serialize(metacard, DEFAULT_SERIALIZATION_FILE_LOCATION);

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
        assertEquals(metacard.getAttribute("description")
                        .getValue(),
                readMetacard.getAttribute("description")
                        .getValue());
        assertEquals(metacard.getAttribute("point-of-contact")
                        .getValue(),
                readMetacard.getAttribute("point-of-contact")
                        .getValue());

        assertTrue(Arrays.equals(metacard.getThumbnail(), readMetacard.getThumbnail()));

        MetacardType metacardType = metacard.getMetacardType();
        MetacardType readMetacardType = readMetacard.getMetacardType();

        assertEquals(metacardType.getName(), readMetacardType.getName());

        Set<AttributeDescriptor> oldAd = metacardType.getAttributeDescriptors();
        Set<AttributeDescriptor> newAd = readMetacardType.getAttributeDescriptors();

        assertEquals(oldAd.size(), newAd.size());

        assertEquals(oldAd.size(), newAd.size());

        for (int i = 0; i < oldAd.size(); i++) {

            AttributeDescriptor oldDescriptor = oldAd.iterator()
                    .next();

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
        assertTrue(readMetacardType.getAttributeDescriptors()
                .isEmpty());
        assertEquals(metacardType.getAttributeDescriptor(null),
                readMetacardType.getAttributeDescriptor(null));

        assertTrue(readMetacardType.getName() == null);
        assertTrue(readMetacardType.getAttributeDescriptor(null) == null);

    }

    @Test
    public void testSerializingEmptyWrappedMetacardType()
            throws IOException, ClassNotFoundException {

        MetacardImpl metacard = new MetacardImpl(new MetacardImpl(new EmptyMetacardType()));

        Serializer<Metacard> serializer = new Serializer<Metacard>();

        serializer.serialize(metacard, DEFAULT_SERIALIZATION_FILE_LOCATION);

        Metacard readMetacard = serializer.deserialize(DEFAULT_SERIALIZATION_FILE_LOCATION);
        MetacardType metacardType = metacard.getMetacardType();
        MetacardType readMetacardType = readMetacard.getMetacardType();

        assertNotNull(readMetacardType);

        assertTrue(readMetacardType.getName() == null);
        assertTrue(readMetacardType.getAttributeDescriptor(null) == null);
        assertTrue(readMetacardType.getAttributeDescriptors()
                .isEmpty());

        assertEquals(metacardType.getName(), readMetacardType.getName());
        assertEquals(metacardType.getAttributeDescriptor(null),
                readMetacardType.getAttributeDescriptor(null));

    }

    /*
     * A test where a metacardType is not defined but there are attributes in the metacard
     */
    @Test()
    public void testDeserializingUndefinedMetacardType()
            throws IOException, ClassNotFoundException {
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
        assertEquals(0,
                readMetacard.getMetacardType()
                        .getAttributeDescriptors()
                        .size());

        assertEquals(null,
                readMetacard.getMetacardType()
                        .getName());

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

        assertEquals("newNameValue",
                readMetacard.getAttribute("newName")
                        .getValue());

    }

    /*
     * Tests for attribute type validation/enforcement
     */

    @Test(expected = IllegalArgumentException.class)
    public void testSetCreatedDateIncorrectAttributeType() {
        MetacardImpl metacard = new MetacardImpl();

        metacard.setAttribute(new AttributeImpl(Metacard.CREATED, TEST_GENERIC_STRING));
    }

    @Test
    public void testSetCreatedDateIncorrectAttributeTypeMetacardFormatting() {
        MetacardImpl metacard = new MetacardImpl();
        Date testDate = new Date();
        DateTime date = new DateTime(testDate);
        DateTimeFormatter metacardDateFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ");

        metacard.setAttribute(new AttributeImpl(Metacard.CREATED, metacardDateFormat.print(date)));

        assertThat("Should convert to (and return) valid date object.", metacard.getAttribute(
                Metacard.CREATED)
                .getValue()
                .toString(), is(testDate.toString()));
    }

    @Test
    public void testSetCreatedDateIncorrectAttributeTypeDefaultFormatting() {
        MetacardImpl metacard = new MetacardImpl();
        Date testDate = new Date();

        metacard.setAttribute(new AttributeImpl(Metacard.CREATED, testDate.toString()));

        assertThat("Should convert to (and return) valid date object.", metacard.getAttribute(
                Metacard.CREATED)
                .getValue()
                .toString(), is(testDate.toString()));
    }

    @Test
    public void testSetCreatedDateStringParameter() {
        MetacardImpl metacard = new MetacardImpl();

        String[] dateTimeStrings = {"2016-04-26T12:00:00.1-00:00", "2016-04-26T12:00:00.01",
                "2016-04-26T12:00:00.001Z", "2016-04-26T12:00:00-00:00", "2016-04-26T12:00:00",
                "2016-04-26T11:57:00Z", "2016-04-26T12:00-00:00", "2016-04-26T11:57",
                "2016-04-26T11:57Z", "2016-04-26T12:00:22-00:00", "2016-04-26T12:00:22",
                "2016-04-26T12:00:22Z", "2016-04-26T12:00:22.1-00:00", "2016-04-26T12:00:22.01",
                "2016-04-26T12:00:22.001Z", "2016-04-26Z", "2016-04-26", "2016-04-26-00:00",
                "2016-04Z", "2016-04", "2016-04-00:00", "2016Z", "2016", "2016-00:00"};
        ArrayList<Date> dates = new ArrayList<>();
        for (int i = 0; i < dateTimeStrings.length; i++) {
            metacard.setAttribute(Metacard.CREATED, dateTimeStrings[i]);
            assertThat("Should correctly convert " + dateTimeStrings[i] + " into a Date object.",
                    metacard.getAttribute(Metacard.CREATED)
                            .getValue(),
                    instanceOf(Date.class));
        }
    }

    @Test
    public void testSetXmlIncorrectAttributeType() {
        MetacardImpl metacard = new MetacardImpl();
        Date xmlDate = new Date();

        metacard.setAttribute(Metacard.METADATA, xmlDate);

        assertThat("Should convert to a string.",
                metacard.getAttribute(Metacard.METADATA)
                        .getValue(),
                is(xmlDate.toString()));
    }

    @Test
    public void testSetAttributeResourceUriWithUriParameterGracefulHandling()
            throws URISyntaxException {
        MetacardImpl metacard = new MetacardImpl();
        URI uri = new URI(nsUri.toString() + "/resource.html");

        metacard.setAttribute("resource-uri", uri);

        assertThat("Should convert the uri to a string.",
                metacard.getAttribute("resource-uri")
                        .getValue(),
                is(uri.toString()));
    }

    @Test
    public void testSetAttributeDoubleWithStringParameterGracefulHandling() {
        MetacardImpl metacard = new MetacardImpl(getMetacardType(BasicTypes.DOUBLE_TYPE));
        String doubleString = "1.001";
        Double expectedReturnValue = 1.001;

        metacard.setAttribute(TEST_ATTRIBUTE_NAME, doubleString);

        assertThat("Should convert the string to a double.",
                metacard.getAttribute(TEST_ATTRIBUTE_NAME)
                        .getValue(),
                is(expectedReturnValue));
    }

    @Test
    public void testSetAttributeIntegerWithStringParameterGracefulHandling() {
        MetacardImpl metacard = new MetacardImpl(getMetacardType(BasicTypes.INTEGER_TYPE));
        String integerString = "5005";
        Integer expectedReturnValue = 5005;

        metacard.setAttribute(TEST_ATTRIBUTE_NAME, integerString);

        assertThat("Should convert the string to an integer.",
                metacard.getAttribute(TEST_ATTRIBUTE_NAME)
                        .getValue(),
                is(expectedReturnValue));
    }

    @Test
    public void testSetAttributeFloatWithStringParameterGracefulHandling() {
        MetacardImpl metacard = new MetacardImpl(getMetacardType(BasicTypes.FLOAT_TYPE));
        String floatString = "1.1";
        Float expectedReturnValue = 1.1f;

        metacard.setAttribute(TEST_ATTRIBUTE_NAME, floatString);

        assertThat("Should convert the string to a float.",
                metacard.getAttribute(TEST_ATTRIBUTE_NAME)
                        .getValue(),
                is(expectedReturnValue));
    }

    @Test
    public void testSetAttributeLongWithStringParameterGracefulHandling() {
        MetacardImpl metacard = new MetacardImpl(getMetacardType(BasicTypes.LONG_TYPE));
        String longString = "1";
        Long expectedReturnValue = 1L;

        metacard.setAttribute(TEST_ATTRIBUTE_NAME, longString);

        assertThat("Should convert the string to a long.",
                metacard.getAttribute(TEST_ATTRIBUTE_NAME)
                        .getValue(),
                is(expectedReturnValue));
    }

    @Test
    public void testSetAttributeBooleanWithStringParameterGracefulHandling() {
        MetacardImpl metacard = new MetacardImpl(getMetacardType(BasicTypes.BOOLEAN_TYPE));
        String booleanString = "true";
        Boolean expectedReturnValue = true;

        metacard.setAttribute(TEST_ATTRIBUTE_NAME, booleanString);

        assertThat("Should convert the string to a boolean.",
                metacard.getAttribute(TEST_ATTRIBUTE_NAME)
                        .getValue(),
                is(expectedReturnValue));
    }

    @Test
    public void testSetAttributeBooleanWithStringListParameterGracefulHandling() {
        MetacardImpl metacard = new MetacardImpl(getMetacardType(BasicTypes.BOOLEAN_TYPE));
        ArrayList<String> booleanStrings = new ArrayList<>();
        booleanStrings.add("true");
        booleanStrings.add("false");
        ArrayList<Boolean> booleanList = new ArrayList<>();
        booleanList.add(true);
        booleanList.add(false);

        metacard.setAttribute(TEST_ATTRIBUTE_NAME, booleanStrings);

        assertThat("Should convert the list of strings to a list of booleans.",
                metacard.getAttribute(TEST_ATTRIBUTE_NAME)
                        .getValues(),
                is(booleanList));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetAttributeIntegerWithStringParameter() {
        MetacardImpl metacard = new MetacardImpl(getMetacardType(BasicTypes.INTEGER_TYPE));
        String invalidIntegerString = "notANumber";

        metacard.setAttribute(TEST_ATTRIBUTE_NAME, invalidIntegerString);
    }

    @Test
    public void testSetAttributeObjectWithStringParameter() {
        MetacardImpl metacard = new MetacardImpl(getMetacardType(BasicTypes.OBJECT_TYPE));
        String objectString = "objectString";

        metacard.setAttribute(TEST_ATTRIBUTE_NAME, objectString);

        assertThat("Should convert the string to an object.",
                metacard.getAttribute(TEST_ATTRIBUTE_NAME)
                        .getValue(),
                is(objectString));
    }

    @Test
    public void testSetAttributeBinaryWithStringParameter() throws UnsupportedEncodingException {
        MetacardImpl metacard = new MetacardImpl(getMetacardType(BasicTypes.BINARY_TYPE));
        String binaryString = "binaryString";

        metacard.setAttribute(TEST_ATTRIBUTE_NAME, binaryString);

        assertThat("Should convert the string to a binary object.",
                metacard.getAttribute(TEST_ATTRIBUTE_NAME)
                        .getValue(),
                is(binaryString.getBytes(Charsets.UTF_8)));
    }

    @Test
    public void testSetAttributeStringWithDateParameter() {
        MetacardImpl metacard = new MetacardImpl(getMetacardType(BasicTypes.STRING_TYPE));
        Date stringDate = new Date();

        metacard.setAttribute(TEST_ATTRIBUTE_NAME, stringDate);

        assertThat("Should convert the date to a string.",
                metacard.getAttribute(TEST_ATTRIBUTE_NAME)
                        .getValue(),
                is(stringDate.toString()));
    }

    @Test
    public void testSetAttributeShortWithStringParameter() {
        MetacardImpl metacard = new MetacardImpl(getMetacardType(BasicTypes.SHORT_TYPE));
        String shortString = "5";
        Short expectedReturnValue = new Short(shortString);

        metacard.setAttribute(TEST_ATTRIBUTE_NAME, shortString);

        assertThat("Should convert the string to a short.",
                metacard.getAttribute(TEST_ATTRIBUTE_NAME)
                        .getValue(),
                is(expectedReturnValue));
    }

    @Test
    public void testSetAttributeWithNoAttributeDescriptors() {
        MetacardImpl metacard = new MetacardImpl(getMetacardType(BasicTypes.DATE_TYPE));

        metacard.setAttribute(TEST_ALT_ATTRIB_NAME, TEST_GENERIC_STRING);

        assertThat("Should allow the attribute to be set without any changes.",
                metacard.getAttribute(TEST_ALT_ATTRIB_NAME)
                        .getValue(),
                is(TEST_GENERIC_STRING));
    }

    private MetacardTypeImpl getMetacardType(AttributeType type) {
        Set<AttributeDescriptor> attributeDescriptors = new HashSet<>();
        attributeDescriptors.add(new AttributeDescriptorImpl(TEST_ATTRIBUTE_NAME,
                true,
                true,
                true,
                true,
                type));
        return new MetacardTypeImpl("TestMetacardType", attributeDescriptors);
    }
}
