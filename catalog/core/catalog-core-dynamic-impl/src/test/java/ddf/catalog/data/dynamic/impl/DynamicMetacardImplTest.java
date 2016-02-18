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
package ddf.catalog.data.dynamic.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.beanutils.DynaProperty;
import org.apache.commons.beanutils.LazyDynaBean;
import org.apache.commons.beanutils.LazyDynaClass;
import org.junit.Before;
import org.junit.Test;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeImpl;

public class DynamicMetacardImplTest {
    protected LazyDynaBean baseBean;
    protected LazyDynaClass baseClass;
    protected DynaProperty[] properties;
    protected DynamicMetacardImpl metacard;

    private static final String STRING = "string";
    private static final String BOOLEAN = "boolean";
    private static final String DATE = "date";
    private static final String SHORT = "short";
    private static final String INTEGER = "integer";
    private static final String LONG = "long";
    private static final String FLOAT = "float";
    private static final String DOUBLE = "double";
    private static final String BYTE = "byte";
    private static final String BINARY = "binary";
    private static final String XML = "xml";
    private static final String OBJECT = "object";
    private static final String STRING_LIST = "stringList";
    private static final String XML_STRING = "<?xml version=\"1.0\"?><tag></tag>";

    @Before
    public void setUp() throws Exception {
        properties = new DynaProperty[] {
                new DynaProperty(STRING, String.class),
                new DynaProperty(BOOLEAN, Boolean.class),
                new DynaProperty(DATE, Date.class),
                new DynaProperty(SHORT, Short.class),
                new DynaProperty(INTEGER, Integer.class),
                new DynaProperty(LONG, Long.class),
                new DynaProperty(FLOAT, Float.class),
                new DynaProperty(DOUBLE, Double.class),
                new DynaProperty(BINARY, Byte[].class, Byte.class),
                new DynaProperty(XML, String.class),
                new DynaProperty(OBJECT, Object.class),
                new DynaProperty(STRING_LIST, List.class, String.class)
        };

        baseClass = new LazyDynaClass("test", properties);
        baseBean = new LazyDynaBean(baseClass);
        metacard = new DynamicMetacardImpl(baseBean);
    }

    @Test
    public void testSetAttribute() throws Exception {
        Attribute attribute = new AttributeImpl(STRING, "abc");
        metacard.setAttribute(attribute);
        assertEquals("abc", baseBean.get(STRING));

        attribute = new AttributeImpl(BOOLEAN, true);
        metacard.setAttribute(attribute);
        assertEquals(true, baseBean.get(BOOLEAN));

        Date d = new Date(System.currentTimeMillis());
        attribute = new AttributeImpl(DATE, d);
        metacard.setAttribute(attribute);
        assertEquals(d, baseBean.get(DATE));

        attribute = new AttributeImpl(SHORT, Short.MIN_VALUE);
        metacard.setAttribute(attribute);
        assertEquals(Short.MIN_VALUE, baseBean.get(SHORT));

        attribute = new AttributeImpl(INTEGER, Integer.MAX_VALUE);
        metacard.setAttribute(attribute);
        assertEquals(Integer.MAX_VALUE, baseBean.get(INTEGER));

        attribute = new AttributeImpl(LONG, Long.MAX_VALUE);
        metacard.setAttribute(attribute);
        assertEquals(Long.MAX_VALUE, baseBean.get(LONG));

        attribute = new AttributeImpl(FLOAT, Float.MAX_VALUE);
        metacard.setAttribute(attribute);
        assertEquals(Float.MAX_VALUE, baseBean.get(FLOAT));

        attribute = new AttributeImpl(DOUBLE, Double.MAX_VALUE);
        metacard.setAttribute(attribute);
        assertEquals(Double.MAX_VALUE, baseBean.get(DOUBLE));

        Byte[] bytes = new Byte[] {0x00, 0x01, 0x02, 0x03};
        attribute = new AttributeImpl(BINARY, bytes);
        metacard.setAttribute(attribute);
        assertEquals(bytes, baseBean.get(BINARY));

        attribute = new AttributeImpl(XML, XML_STRING);
        metacard.setAttribute(attribute);
        assertEquals(XML_STRING, baseBean.get(XML));

        attribute = new AttributeImpl(OBJECT, XML_STRING);
        metacard.setAttribute(attribute);
        assertEquals(XML_STRING, baseBean.get(OBJECT));

        List<Serializable> list = new ArrayList<>();
        list.add("123");
        list.add("234");
        list.add("345");
        attribute = new AttributeImpl(STRING_LIST, list);
        metacard.setAttribute(attribute);
        assertEquals(list, baseBean.get(STRING_LIST));

    }

    @Test
    public void testSetAttributeWithValues() throws Exception {
        metacard.setAttribute(STRING, "abc");
        assertEquals("abc", baseBean.get(STRING));

        metacard.setAttribute(BOOLEAN, true);
        assertEquals(true, baseBean.get(BOOLEAN));

        Date d = new Date(System.currentTimeMillis());
        metacard.setAttribute(DATE, d);
        assertEquals(d, baseBean.get(DATE));

        metacard.setAttribute(SHORT, Short.MAX_VALUE);
        assertEquals(Short.MAX_VALUE, baseBean.get(SHORT));

        metacard.setAttribute(INTEGER, Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, baseBean.get(INTEGER));

        metacard.setAttribute(LONG, Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, baseBean.get(LONG));

        metacard.setAttribute(FLOAT, Float.MAX_VALUE);
        assertEquals(Float.MAX_VALUE, baseBean.get(FLOAT));

        metacard.setAttribute(DOUBLE, Double.MAX_VALUE);
        assertEquals(Double.MAX_VALUE, baseBean.get(DOUBLE));

        Byte[] bytes = new Byte[] {0x00, 0x01, 0x02, 0x03};
        metacard.setAttribute(BINARY, bytes);
        assertEquals(bytes, baseBean.get(BINARY));

        metacard.setAttribute(XML, XML_STRING);
        assertEquals(XML_STRING, baseBean.get(XML));

        metacard.setAttribute(OBJECT, XML_STRING);
        assertEquals(XML_STRING, baseBean.get(OBJECT));

        List<Serializable> list = new ArrayList<>();
        list.add("123");
        list.add("234");
        list.add("345");
        metacard.setAttribute(STRING_LIST, list);
        assertEquals(list, baseBean.get(STRING_LIST));

        metacard.setAttribute(STRING_LIST, "456");
        Collection c = (Collection) baseBean.get(STRING_LIST);
        assertEquals(4, c.size());
        assertTrue(c.contains("456"));

        list.remove("123");
        metacard.setAttribute(STRING_LIST, list);
        c = (Collection) baseBean.get(STRING_LIST);
        assertEquals(3, c.size());
        assertTrue(c.contains("456"));
        assertFalse(c.contains("123"));

        metacard.setAttribute(SHORT, Short.MIN_VALUE);
        // this should take no action since auto-conversion fails - handled ConversionException
        metacard.setAttribute(SHORT, Long.MAX_VALUE);
        assertEquals(Short.MIN_VALUE, baseBean.get(SHORT));
    }

    @Test
    public void testAddAttribute() throws Exception {
        Attribute attribute = null;
        metacard.addAttribute(STRING, "123");
        assertEquals("123", baseBean.get(STRING));

        // replace the existing value for single-valued attributes
        metacard.addAttribute(STRING, "234");
        assertEquals("234", baseBean.get(STRING));

        // add a single value to an empty list
        metacard.addAttribute(STRING_LIST, "123");
        List<Serializable> list = new ArrayList<>();
        list.add("123");
        assertEquals(list, baseBean.get(STRING_LIST));

        // adds a value to the existing list
        metacard.addAttribute(STRING_LIST, "234");
        Collection c = (Collection) baseBean.get(STRING_LIST);
        assertEquals(2, c.size());
        assertTrue(c.contains("123"));
        assertTrue(c.contains("234"));

        // adding a list adds all values to the existing list
        list.clear();
        list.add("345");
        list.add("456");
        metacard.addAttribute(STRING_LIST, list);
        c = (Collection) baseBean.get(STRING_LIST);
        assertEquals(4, c.size());
        assertTrue(c.contains("123"));
        assertTrue(c.contains("234"));
        assertTrue(c.contains("345"));
        assertTrue(c.contains("456"));
    }

    @Test
    public void testGetAttribute() throws Exception {
        Attribute attribute = null;
        baseBean.set(STRING, "abc");
        attribute = metacard.getAttribute(STRING);
        assertEquals("abc", attribute.getValue());

        baseBean.set(BOOLEAN, true);
        attribute = metacard.getAttribute(BOOLEAN);
        assertEquals(true, attribute.getValue());

        Date d = new Date(System.currentTimeMillis());
        baseBean.set(DATE, d);
        attribute = metacard.getAttribute(DATE);
        assertEquals(d, attribute.getValue());

        baseBean.set(SHORT, Short.MAX_VALUE);
        attribute = metacard.getAttribute(SHORT);
        assertEquals(Short.MAX_VALUE, attribute.getValue());

        baseBean.set(INTEGER, Integer.MAX_VALUE);
        attribute = metacard.getAttribute(INTEGER);
        assertEquals(Integer.MAX_VALUE, attribute.getValue());

        baseBean.set(LONG, Long.MAX_VALUE);
        attribute = metacard.getAttribute(LONG);
        assertEquals(Long.MAX_VALUE, attribute.getValue());

        baseBean.set(FLOAT, Float.MAX_VALUE);
        attribute = metacard.getAttribute(FLOAT);
        assertEquals(Float.MAX_VALUE, attribute.getValue());

        baseBean.set(DOUBLE, Double.MAX_VALUE);
        attribute = metacard.getAttribute(DOUBLE);
        assertEquals(Double.MAX_VALUE, attribute.getValue());

        Byte[] bytes = new Byte[] {0x00, 0x01, 0x02, 0x03};
        baseBean.set(BINARY, bytes);
        attribute = metacard.getAttribute(BINARY);
        assertEquals(bytes, attribute.getValue());

        baseBean.set(XML, XML_STRING);
        attribute = metacard.getAttribute(XML);
        assertEquals(XML_STRING, attribute.getValue());

        baseBean.set(OBJECT, XML_STRING);
        attribute = metacard.getAttribute(OBJECT);
        assertEquals(XML_STRING, attribute.getValue());
    }


    @Test
    public void testGetMetacardType() throws Exception {
        assertEquals("test",
                metacard.getMetacardType()
                        .getName());
        MetacardType mcType = metacard.getMetacardType();
        assertEquals(12, mcType.getAttributeDescriptors().size());
        assertEquals(STRING, mcType.getAttributeDescriptor(STRING).getName());
        assertEquals(BOOLEAN, mcType.getAttributeDescriptor(BOOLEAN).getName());
        assertEquals(DATE, mcType.getAttributeDescriptor(DATE).getName());
        assertEquals(SHORT, mcType.getAttributeDescriptor(SHORT).getName());
        assertEquals(INTEGER, mcType.getAttributeDescriptor(INTEGER).getName());
        assertEquals(LONG, mcType.getAttributeDescriptor(LONG).getName());
        assertEquals(FLOAT, mcType.getAttributeDescriptor(FLOAT).getName());
        assertEquals(DOUBLE, mcType.getAttributeDescriptor(DOUBLE).getName());
        assertEquals(BINARY, mcType.getAttributeDescriptor(BINARY).getName());
        assertEquals(XML, mcType.getAttributeDescriptor(XML).getName());
        assertEquals(OBJECT, mcType.getAttributeDescriptor(OBJECT).getName());
        assertEquals(STRING_LIST, mcType.getAttributeDescriptor(STRING_LIST).getName());
    }

    @Test
    public void testGetId() throws Exception {
        assertNull(metacard.getId());
        metacard.setAttribute(Metacard.ID, "12345");
        assertEquals("12345", metacard.getId());
    }

    @Test
    public void testGetMetadata() throws Exception {
        assertNull(metacard.getMetadata());
        metacard.setAttribute(Metacard.METADATA, "12345");
        assertEquals("12345", metacard.getMetadata());
    }

    @Test
    public void testGetCreatedDate() throws Exception {
        assertNull(metacard.getCreatedDate());
        Date date = new Date(System.currentTimeMillis());
        metacard.setAttribute(Metacard.CREATED, date);
        assertEquals(date, metacard.getCreatedDate());
    }

    @Test
    public void testGetModifiedDate() throws Exception {
        assertNull(metacard.getModifiedDate());
        Date date = new Date(System.currentTimeMillis());
        metacard.setAttribute(Metacard.MODIFIED, date);
        assertEquals(date, metacard.getModifiedDate());
    }

    @Test
    public void testGetExpirationDate() throws Exception {
        assertNull(metacard.getExpirationDate());
        Date date = new Date(System.currentTimeMillis());
        metacard.setAttribute(Metacard.EXPIRATION, date);
        assertEquals(date, metacard.getExpirationDate());
    }

    @Test
    public void testGetEffectiveDate() throws Exception {
        assertNull(metacard.getEffectiveDate());
        Date date = new Date(System.currentTimeMillis());
        metacard.setAttribute(Metacard.EFFECTIVE, date);
        assertEquals(date, metacard.getEffectiveDate());
    }

    @Test
    public void testGetLocation() throws Exception {
        assertNull(metacard.getLocation());
        metacard.setAttribute(Metacard.GEOGRAPHY, "WKT");
        assertEquals("WKT", metacard.getLocation());
    }

    @Test
    public void testGetSourceId() throws Exception {
        assertNull(metacard.getSourceId());
        metacard.setAttribute(Metacard.SOURCE_ID, "blah");
        assertEquals("blah", metacard.getSourceId());
    }

    @Test
    public void testSetSourceId() throws Exception {
        assertNull(metacard.getSourceId());
        metacard.setSourceId("blah");
        assertEquals("blah", metacard.getSourceId());
    }

    @Test
    public void testGetTitle() throws Exception {
        assertNull(metacard.getTitle());
        metacard.setAttribute(Metacard.TITLE, "blah");
        assertEquals("blah", metacard.getTitle());
    }

    @Test
    public void testGetResourceURI() throws Exception {
        URI uri = new URI("http://test.com/search");
        assertNull(metacard.getResourceURI());
        metacard.setAttribute(Metacard.RESOURCE_URI, uri);
        assertEquals(uri, metacard.getResourceURI());
        metacard.setAttribute(Metacard.RESOURCE_URI, "http://test.com/search");
        assertEquals(uri, metacard.getResourceURI());
    }

    @Test
    public void testGetResourceSize() throws Exception {
        assertNull(metacard.getResourceSize());
        metacard.setAttribute(Metacard.RESOURCE_SIZE, "120");
        assertEquals("120", metacard.getResourceSize());
    }

    @Test
    public void testGetThumbnail() throws Exception {
        assertNull(metacard.getThumbnail());
        byte[] array = new byte[] {0x00, 0x01};
        metacard.setAttribute(Metacard.THUMBNAIL, array);
        assertEquals(array, metacard.getThumbnail());
    }

    @Test
    public void testGetContentTypeName() throws Exception {
        assertNull(metacard.getContentTypeName());
        metacard.setAttribute(Metacard.CONTENT_TYPE, "blah");
        assertEquals("blah", metacard.getContentTypeName());
    }

    @Test
    public void testGetContentTypeVersion() throws Exception {
        assertNull(metacard.getContentTypeVersion());
        metacard.setAttribute(Metacard.CONTENT_TYPE_VERSION, "0.1");
        assertEquals("0.1", metacard.getContentTypeVersion());
    }

    @Test
    public void testGetContentTypeNamespace() throws Exception {
        assertNull(metacard.getContentTypeNamespace());
        URI namespace = new URI("urn:somevalue");
        metacard.setAttribute(Metacard.TARGET_NAMESPACE, namespace);
        assertEquals(namespace, metacard.getContentTypeNamespace());
    }

    @Test
    public void testGetName() throws Exception {
        assertEquals("test", metacard.getName());
    }

    @Test
    public void testGetAttributeDescriptors() throws Exception {
        MetacardType mcType = metacard.getMetacardType();
        assertEquals(12, mcType.getAttributeDescriptors().size());
        assertEquals(12, metacard.getAttributeDescriptors().size());
    }

    @Test
    public void testGetAttributeDescriptor() throws Exception {
        assertEquals(SHORT, metacard.getAttributeDescriptor(SHORT).getName());
    }

    @Test
    public void testGetMetacardTypes() throws Exception {
        List<String> types = metacard.getMetacardTypes();
        assertEquals(1, types.size());
        assertEquals("test", types.get(0));
    }

    @Test
    public void testIsType() throws Exception {
        assertTrue(metacard.isType("test"));
    }
}