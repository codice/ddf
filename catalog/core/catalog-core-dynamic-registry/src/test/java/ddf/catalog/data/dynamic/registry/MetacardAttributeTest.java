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
package ddf.catalog.data.dynamic.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ddf.catalog.data.dynamic.api.MetacardPropertyDescriptor;

public class MetacardAttributeTest {

    @Test
    public void testGetMetacardPropertyDescriptor() throws Exception {
        MetacardAttribute mca = new MetacardAttribute();

        mca.setIndexed(true);
        mca.setMultiValued(true);
        mca.setName("test");
        mca.setStored(false);
        mca.setTokenized(true);
        mca.setType("Double");

        MetacardPropertyDescriptor descriptor = mca.getMetacardPropertyDescriptor();

        assertEquals("test", descriptor.getName());
        assertEquals(java.util.ArrayList.class, descriptor.getType());
        assertEquals(double.class, descriptor.getContentType());
        assertTrue(descriptor.isIndexed());
        assertTrue(descriptor.isIndexed());
        assertFalse(descriptor.isStored());
        assertTrue(descriptor.isTokenized());

        mca = new MetacardAttribute();

        mca.setIndexed(false);
        mca.setMultiValued(false);
        mca.setName("test2");
        mca.setStored(true);
        mca.setTokenized(false);
        mca.setType("String");

        descriptor = mca.getMetacardPropertyDescriptor();

        assertEquals("test2", descriptor.getName());
        assertEquals(String.class, descriptor.getType());
        assertFalse(descriptor.isIndexed());
        assertFalse(descriptor.isIndexed());
        assertTrue(descriptor.isStored());
        assertFalse(descriptor.isTokenized());
    }
}