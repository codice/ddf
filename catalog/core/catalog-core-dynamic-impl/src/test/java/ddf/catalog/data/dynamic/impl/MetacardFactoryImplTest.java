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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import junit.framework.TestCase;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.dynamic.api.DynamicMetacard;
import ddf.catalog.data.dynamic.api.MetacardFactory;
import ddf.catalog.data.dynamic.api.MetacardPropertyDescriptor;

public class MetacardFactoryImplTest {
    MetacardFactory factory = new MetacardFactoryImpl();
    private static HashMap<String, List<MetacardPropertyDescriptor>> descriptors = new HashMap<>();

    @BeforeClass
    public static void setup() {
        List<MetacardPropertyDescriptor> list = new ArrayList<>();
        list.add(new MetacardPropertyDescriptorImpl("attribute1", String.class));
        list.add(new MetacardPropertyDescriptorImpl("attribute2", Boolean.class));
        list.add(new MetacardPropertyDescriptorImpl("attribute3", Short.class));
        descriptors.put("list1", list);

        list = new ArrayList<>();
        list.add(new MetacardPropertyDescriptorImpl("attribute4", Long.class));
        list.add(new MetacardPropertyDescriptorImpl("attribute5", Float.class));
        list.add(new MetacardPropertyDescriptorImpl("attribute6", Double.class));
        descriptors.put("list2", list);
    }

    @Test
    public void testRegisterDynamicMetacardType() throws Exception {
        factory.registerDynamicMetacardType("test1", descriptors.get("list1"));
        factory.registerDynamicMetacardType("test2", descriptors.get("list2"));
        DynamicMetacard mc = factory.newInstance("test1");
        assertNotNull(mc);
        mc = factory.newInstance("test2");
        assertNotNull(mc);
        //don't blow up with nulls
        factory.registerDynamicMetacardType(null, descriptors.get("list1"));
        factory.registerDynamicMetacardType("test3", null);
        mc = factory.newInstance("test3");
        assertNull(mc);
    }

    @Test
    public void testNewInstance() throws Exception {
        DynamicMetacard mc = factory.newInstance();
        assertEquals(DynamicMetacard.DYNAMIC,mc.getMetacardType().getName());
        Set<AttributeDescriptor> descriptorSet = mc.getAttributeDescriptors();
        assertTrue(descriptorSet.size() >= factory.getBaseMetacardPropertyDescriptors().length);
    }

    @Test
    public void testNewInstanceByName() throws Exception {
        DynamicMetacard mc = factory.newInstance(DynamicMetacard.DYNAMIC);
        assertEquals(DynamicMetacard.DYNAMIC,
                mc.getMetacardType()
                        .getName());
        Set<AttributeDescriptor> descriptorSet = mc.getAttributeDescriptors();
        assertTrue(descriptorSet.size() >= factory.getBaseMetacardPropertyDescriptors().length);

        mc = factory.newInstance("blah");
        assertNull(mc);

        mc = factory.newInstance("test1");
        assertEquals("test1", mc.getMetacardType().getName());
        descriptorSet = mc.getAttributeDescriptors();
        assertTrue(descriptorSet.size() == descriptors.get("list1")
                .size());
    }

    @Test
    public void testAddAttributesForType() throws Exception {
        factory.registerDynamicMetacardType("test1", descriptors.get("list1"));
        factory.registerDynamicMetacardType("test2", descriptors.get("list2"));
        DynamicMetacard mc = factory.newInstance();
        assertEquals(mc.getAttributeDescriptors().size(), factory.getBaseMetacardPropertyDescriptors().length);

        mc = factory.addAttributesForType(mc, "test1");
        assertEquals(DynamicMetacard.DYNAMIC + ".test1", mc.getName());
        assertEquals(mc.getAttributeDescriptors()
                        .size(),
                factory.getBaseMetacardPropertyDescriptors().length + descriptors.get("list1")
                        .size());
        for (MetacardPropertyDescriptor prop : factory.getBaseMetacardPropertyDescriptors()) {
            assertTrue(metacardHasAttribute(mc, prop.getName()));
        }
        for (MetacardPropertyDescriptor prop : descriptors.get("list1")) {
            assertTrue(metacardHasAttribute(mc, prop.getName()));
        }

        String name = mc.getName();
        mc = factory.addAttributesForType(mc, "test2");
        assertEquals(name + ".test2", mc.getName());
        assertEquals(mc.getAttributeDescriptors()
                        .size(), factory.getBaseMetacardPropertyDescriptors().length +
                        descriptors.get("list1")
                                .size() +
                        descriptors.get("list2")
                                .size());
        for (MetacardPropertyDescriptor prop : factory.getBaseMetacardPropertyDescriptors()) {
            assertTrue(metacardHasAttribute(mc, prop.getName()));
        }
        for (MetacardPropertyDescriptor prop : descriptors.get("list1")) {
            assertTrue(metacardHasAttribute(mc, prop.getName()));
        }
        for (MetacardPropertyDescriptor prop : descriptors.get("list2")) {
            assertTrue(metacardHasAttribute(mc, prop.getName()));
        }

    }

    @Test
    public void testNewInstanceMultipleNames() throws Exception {
        factory.registerDynamicMetacardType("test1", descriptors.get("list1"));
        factory.registerDynamicMetacardType("test2", descriptors.get("list2"));
        ArrayList<String> names = new ArrayList<>();
        DynamicMetacard mc = factory.newInstance((List<String>) null);
        assertEquals(DynamicMetacard.DYNAMIC,
                mc.getMetacardType()
                        .getName());
        Set<AttributeDescriptor> descriptorSet = mc.getAttributeDescriptors();
        assertTrue(descriptorSet.size() >= factory.getBaseMetacardPropertyDescriptors().length);

        names.add(DynamicMetacard.DYNAMIC);
        assertEquals(DynamicMetacard.DYNAMIC,
                mc.getMetacardType()
                        .getName());
        descriptorSet = mc.getAttributeDescriptors();
        assertTrue(descriptorSet.size() >= factory.getBaseMetacardPropertyDescriptors().length);

        names.add("test1");
        mc = factory.newInstance(names);
        assertEquals(DynamicMetacard.DYNAMIC + ".test1", mc.getName());
        assertEquals(mc.getAttributeDescriptors().size(),
                factory.getBaseMetacardPropertyDescriptors().length +
                        descriptors.get("list1").size());
        for (MetacardPropertyDescriptor prop : factory.getBaseMetacardPropertyDescriptors()) {
            assertTrue(metacardHasAttribute(mc, prop.getName()));
        }
        for (MetacardPropertyDescriptor prop : descriptors.get("list1")) {
            assertTrue(metacardHasAttribute(mc, prop.getName()));
        }

        names.add("test2");
        mc = factory.newInstance(names);
        assertEquals("ddf.test1.test2", mc.getName());
        assertEquals(mc.getAttributeDescriptors().size(),
                factory.getBaseMetacardPropertyDescriptors().length +
                        descriptors.get("list1").size() +
                        descriptors.get("list2").size());
        for (MetacardPropertyDescriptor prop : factory.getBaseMetacardPropertyDescriptors()) {
            assertTrue(metacardHasAttribute(mc, prop.getName()));
        }
        for (MetacardPropertyDescriptor prop : descriptors.get("list1")) {
            assertTrue(metacardHasAttribute(mc, prop.getName()));
        }
        for (MetacardPropertyDescriptor prop : descriptors.get("list2")) {
            assertTrue(metacardHasAttribute(mc, prop.getName()));
        }

    }

    @Test
    public void testGetBaseMetacardPropertyDescriptors() throws Exception {
        factory.registerDynamicMetacardType("test1", descriptors.get("list1"));
        factory.registerDynamicMetacardType("test2", descriptors.get("list2"));

        MetacardPropertyDescriptor[] propertyDescriptors = factory.getBaseMetacardPropertyDescriptors();
        MetacardPropertyDescriptor[] dynamicDescriptors = factory.getMetacardPropertyDescriptors(DynamicMetacard.DYNAMIC);
        for (MetacardPropertyDescriptor prop : propertyDescriptors) {
            assertTrue(mcpdIsInArray(prop, dynamicDescriptors));
        }
    }

    @Test
    public void testGetMetacardPropertyDescriptors() throws Exception {
        factory.registerDynamicMetacardType("test1", descriptors.get("list1"));
        factory.registerDynamicMetacardType("test2", descriptors.get("list2"));

        MetacardPropertyDescriptor[] propertyDescriptors = factory.getMetacardPropertyDescriptors(
                DynamicMetacard.DYNAMIC);
        assertEquals(propertyDescriptors.length, factory.getBaseMetacardPropertyDescriptors().length);
        propertyDescriptors = factory.getMetacardPropertyDescriptors("test1");
        assertEquals(propertyDescriptors.length, descriptors.get("list1").size());
        List<MetacardPropertyDescriptor> originalList = descriptors.get("list1");
        for (MetacardPropertyDescriptor prop : propertyDescriptors) {
            assertTrue(originalList.contains(prop));
        }

        propertyDescriptors = factory.getMetacardPropertyDescriptors("test2");
        assertEquals(propertyDescriptors.length, descriptors.get("list1").size());
        originalList = descriptors.get("list2");
        for (MetacardPropertyDescriptor prop : propertyDescriptors) {
            assertTrue(originalList.contains(prop));
        }
    }

    private List<MetacardPropertyDescriptor> getDescriptors(String name) {
        ArrayList<MetacardPropertyDescriptor> list = new ArrayList<>();

        return list;
    }

    private boolean metacardHasAttribute(DynamicMetacard mc, String name) {
        boolean retValue = false;
        for (AttributeDescriptor prop : mc.getAttributeDescriptors()) {
            if (name.equals(prop.getName())) {
                retValue = true;
                break;
            }
        }
        return retValue;
    }

    private boolean mcpdIsInArray(MetacardPropertyDescriptor p, MetacardPropertyDescriptor[] array) {
        boolean retValue = false;
        for (MetacardPropertyDescriptor prop : array) {
            if (prop.getName().equals(p.getName())) {
                retValue = true;
                break;
            }
        }
        return retValue;
    }
}