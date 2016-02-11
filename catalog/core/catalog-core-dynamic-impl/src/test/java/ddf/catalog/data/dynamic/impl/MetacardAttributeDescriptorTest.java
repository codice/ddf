package ddf.catalog.data.dynamic.impl;

import java.util.Date;

import org.apache.commons.beanutils.DynaProperty;
import org.junit.Test;

import junit.framework.TestCase;
import junit.framework.TestResult;

import ddf.catalog.data.AttributeType;
import ddf.catalog.data.dynamic.api.MetacardPropertyDescriptor;

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
public class MetacardAttributeDescriptorTest extends TestCase {

    @Test
    public void testGetAttributeFomat() {
        AttributeType.AttributeFormat format = null;

        DynaProperty dynaProperty = new DynaProperty("test", String.class);
        MetacardAttributeDescriptor descriptor = new MetacardAttributeDescriptor(dynaProperty);
        format = descriptor.getAttributeFormat();
        assertTrue(AttributeType.AttributeFormat.STRING.compareTo(format) == 0);

        dynaProperty = new DynaProperty("test", Boolean.class);
        descriptor = new MetacardAttributeDescriptor(dynaProperty);
        format = descriptor.getAttributeFormat();
        assertTrue(AttributeType.AttributeFormat.BOOLEAN.compareTo(format) == 0);

        dynaProperty = new DynaProperty("test", Date.class);
        descriptor = new MetacardAttributeDescriptor(dynaProperty);
        format = descriptor.getAttributeFormat();
        assertTrue(AttributeType.AttributeFormat.DATE.compareTo(format) == 0);

        dynaProperty = new DynaProperty("test", Short.class);
        descriptor = new MetacardAttributeDescriptor(dynaProperty);
        format = descriptor.getAttributeFormat();
        assertTrue(AttributeType.AttributeFormat.SHORT.compareTo(format) == 0);

        dynaProperty = new DynaProperty("test", Integer.class);
        descriptor = new MetacardAttributeDescriptor(dynaProperty);
        format = descriptor.getAttributeFormat();
        assertTrue(AttributeType.AttributeFormat.INTEGER.compareTo(format) == 0);

        dynaProperty = new DynaProperty("test", Long.class);
        descriptor = new MetacardAttributeDescriptor(dynaProperty);
        format = descriptor.getAttributeFormat();
        assertTrue(AttributeType.AttributeFormat.LONG.compareTo(format) == 0);

        dynaProperty = new DynaProperty("test", Float.class);
        descriptor = new MetacardAttributeDescriptor(dynaProperty);
        format = descriptor.getAttributeFormat();
        assertTrue(AttributeType.AttributeFormat.FLOAT.compareTo(format) == 0);

        dynaProperty = new DynaProperty("test", Double.class);
        descriptor = new MetacardAttributeDescriptor(dynaProperty);
        format = descriptor.getAttributeFormat();
        assertTrue(AttributeType.AttributeFormat.DOUBLE.compareTo(format) == 0);

        dynaProperty = new DynaProperty("test", Byte[].class);
        descriptor = new MetacardAttributeDescriptor(dynaProperty);
        format = descriptor.getAttributeFormat();
        assertTrue(AttributeType.AttributeFormat.BINARY.compareTo(format) == 0);

/*
        dynaProperty = new DynaProperty("test", String.class);
        descriptor = new MetacardAttributeDescriptor(dynaProperty);
        format = descriptor.getAttributeFormat();
        assertTrue(AttributeType.AttributeFormat.XML.compareTo(format) == 0);

        dynaProperty = new DynaProperty("test", String.class);
        descriptor = new MetacardAttributeDescriptor(dynaProperty);
        format = descriptor.getAttributeFormat();
        assertTrue(AttributeType.AttributeFormat.GEOMETRY.compareTo(format) == 0);
*/

        dynaProperty = new DynaProperty("test", Object.class);
        descriptor = new MetacardAttributeDescriptor(dynaProperty);
        format = descriptor.getAttributeFormat();
        assertTrue(AttributeType.AttributeFormat.OBJECT.compareTo(format) == 0);

        // Now test using MetacardPropertyDescriptors to initialize
        MetacardPropertyDescriptorImpl propertyDescriptor = new MetacardPropertyDescriptorImpl("test", String.class);
        descriptor = new MetacardAttributeDescriptor(propertyDescriptor);
        format = descriptor.getAttributeFormat();
        assertTrue(AttributeType.AttributeFormat.STRING.compareTo(format) == 0);

        propertyDescriptor = new MetacardPropertyDescriptorImpl("test", Boolean.class);
        descriptor = new MetacardAttributeDescriptor(propertyDescriptor);
        format = descriptor.getAttributeFormat();
        assertTrue(AttributeType.AttributeFormat.BOOLEAN.compareTo(format) == 0);

        propertyDescriptor = new MetacardPropertyDescriptorImpl("test", Date.class);
        descriptor = new MetacardAttributeDescriptor(propertyDescriptor);
        format = descriptor.getAttributeFormat();
        assertTrue(AttributeType.AttributeFormat.DATE.compareTo(format) == 0);

        propertyDescriptor = new MetacardPropertyDescriptorImpl("test", Short.class);
        descriptor = new MetacardAttributeDescriptor(propertyDescriptor);
        format = descriptor.getAttributeFormat();
        assertTrue(AttributeType.AttributeFormat.SHORT.compareTo(format) == 0);

        propertyDescriptor = new MetacardPropertyDescriptorImpl("test", Integer.class);
        descriptor = new MetacardAttributeDescriptor(propertyDescriptor);
        format = descriptor.getAttributeFormat();
        assertTrue(AttributeType.AttributeFormat.INTEGER.compareTo(format) == 0);

        propertyDescriptor = new MetacardPropertyDescriptorImpl("test", Long.class);
        descriptor = new MetacardAttributeDescriptor(propertyDescriptor);
        format = descriptor.getAttributeFormat();
        assertTrue(AttributeType.AttributeFormat.LONG.compareTo(format) == 0);

        propertyDescriptor = new MetacardPropertyDescriptorImpl("test", Float.class);
        descriptor = new MetacardAttributeDescriptor(propertyDescriptor);
        format = descriptor.getAttributeFormat();
        assertTrue(AttributeType.AttributeFormat.FLOAT.compareTo(format) == 0);

        propertyDescriptor = new MetacardPropertyDescriptorImpl("test", Double.class);
        descriptor = new MetacardAttributeDescriptor(propertyDescriptor);
        format = descriptor.getAttributeFormat();
        assertTrue(AttributeType.AttributeFormat.DOUBLE.compareTo(format) == 0);

        propertyDescriptor = new MetacardPropertyDescriptorImpl("test", Byte[].class);
        descriptor = new MetacardAttributeDescriptor(propertyDescriptor);
        format = descriptor.getAttributeFormat();
        assertTrue(AttributeType.AttributeFormat.BINARY.compareTo(format) == 0);

        propertyDescriptor = new MetacardPropertyDescriptorImpl("test", String.class);
        propertyDescriptor.setFormat(AttributeType.AttributeFormat.XML);
        descriptor = new MetacardAttributeDescriptor(propertyDescriptor);
        format = descriptor.getAttributeFormat();
        assertTrue(AttributeType.AttributeFormat.XML.compareTo(format) == 0);

        propertyDescriptor = new MetacardPropertyDescriptorImpl("test", String.class);
        propertyDescriptor.setFormat(AttributeType.AttributeFormat.GEOMETRY);
        descriptor = new MetacardAttributeDescriptor(propertyDescriptor);
        format = descriptor.getAttributeFormat();
        assertTrue(AttributeType.AttributeFormat.GEOMETRY.compareTo(format) == 0);

        propertyDescriptor = new MetacardPropertyDescriptorImpl("test", Object.class);
        descriptor = new MetacardAttributeDescriptor(propertyDescriptor);
        format = descriptor.getAttributeFormat();
        assertTrue(AttributeType.AttributeFormat.OBJECT.compareTo(format) == 0);
    }

    @Test
    public void testCreateByFormat() {
        AttributeType.AttributeFormat format = null;

        MetacardPropertyDescriptor descriptor = MetacardPropertyDescriptorImpl.createByFormat("test",
                AttributeType.AttributeFormat.STRING, true, true, false);
        format = descriptor.getFormat();
        assertTrue(AttributeType.AttributeFormat.STRING.compareTo(format) == 0);

        descriptor = MetacardPropertyDescriptorImpl.createByFormat("test",
                AttributeType.AttributeFormat.BOOLEAN, true, true, false);
        format = descriptor.getFormat();
        assertTrue(AttributeType.AttributeFormat.BOOLEAN.compareTo(format) == 0);

        descriptor = MetacardPropertyDescriptorImpl.createByFormat("test",
                AttributeType.AttributeFormat.DATE, true, true, false);
        format = descriptor.getFormat();
        assertTrue(AttributeType.AttributeFormat.DATE.compareTo(format) == 0);

        descriptor = MetacardPropertyDescriptorImpl.createByFormat("test",
                AttributeType.AttributeFormat.SHORT, true, true, false);
        format = descriptor.getFormat();
        assertTrue(AttributeType.AttributeFormat.SHORT.compareTo(format) == 0);

        descriptor = MetacardPropertyDescriptorImpl.createByFormat("test",
                AttributeType.AttributeFormat.INTEGER, true, true, false);
        format = descriptor.getFormat();
        assertTrue(AttributeType.AttributeFormat.INTEGER.compareTo(format) == 0);

        descriptor = MetacardPropertyDescriptorImpl.createByFormat("test",
                AttributeType.AttributeFormat.LONG, true, true, false);
        format = descriptor.getFormat();
        assertTrue(AttributeType.AttributeFormat.LONG.compareTo(format) == 0);

        descriptor = MetacardPropertyDescriptorImpl.createByFormat("test",
                AttributeType.AttributeFormat.FLOAT, true, true, false);
        format = descriptor.getFormat();
        assertTrue(AttributeType.AttributeFormat.FLOAT.compareTo(format) == 0);

        descriptor = MetacardPropertyDescriptorImpl.createByFormat("test",
                AttributeType.AttributeFormat.DOUBLE, true, true, false);
        format = descriptor.getFormat();
        assertTrue(AttributeType.AttributeFormat.DOUBLE.compareTo(format) == 0);

        descriptor = MetacardPropertyDescriptorImpl.createByFormat("test",
                AttributeType.AttributeFormat.BINARY, true, true, false);
        format = descriptor.getFormat();
        assertTrue(AttributeType.AttributeFormat.BINARY.compareTo(format) == 0);

        descriptor = MetacardPropertyDescriptorImpl.createByFormat("test",
                AttributeType.AttributeFormat.XML, true, true, false);
        format = descriptor.getFormat();
        assertTrue(AttributeType.AttributeFormat.XML.compareTo(format) == 0);

        descriptor = MetacardPropertyDescriptorImpl.createByFormat("test",
                AttributeType.AttributeFormat.GEOMETRY, true, true, false);
        format = descriptor.getFormat();
        assertTrue(AttributeType.AttributeFormat.GEOMETRY.compareTo(format) == 0);

        descriptor = MetacardPropertyDescriptorImpl.createByFormat("test",
                AttributeType.AttributeFormat.OBJECT, true, true, false);
        format = descriptor.getFormat();
        assertTrue(AttributeType.AttributeFormat.OBJECT.compareTo(format) == 0);
    }
}