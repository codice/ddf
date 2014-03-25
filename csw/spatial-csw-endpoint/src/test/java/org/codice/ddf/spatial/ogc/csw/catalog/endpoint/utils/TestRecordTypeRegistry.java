/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.xml.namespace.QName;

import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.junit.Test;

/**
 * Tests RecordTypeRegistry
 *
 */
public class TestRecordTypeRegistry {

    private static final QName TEST_TYPE = new QName(CswConstants.CSW_OUTPUT_SCHEMA, "Record", "csw");
    private static final String TEST_VERSION = "testVersion";
    private static final String TEST_RESOURCE_PATH = "testResourcePath";

    private static final QName BAD_TYPE = new QName(CswConstants.CSW_OUTPUT_SCHEMA, "badType", "csw");
    private static final String BAD_VERSION = "badVersion";

    @Test
    public void testContainsTypeSingleType() {
        RecordTypeRegistry registry = getRegistry();
        assertTrue(registry.containsType(TEST_TYPE, TEST_VERSION));
    }

    @Test
    public void testContainsTypeSingleTypeInvalidType() {
        RecordTypeRegistry registry = getRegistry();
        assertFalse(registry.containsType(BAD_TYPE, TEST_VERSION));
    }

    @Test
    public void testContainsTypeSingleTypeInvalidVersion() {
        RecordTypeRegistry registry = getRegistry();
        assertFalse(registry.containsType(TEST_TYPE, BAD_VERSION));
    }

    @Test
    public void testContainsTypeSingleTypeNullType() {
        RecordTypeRegistry registry = getRegistry();
        assertFalse(registry.containsType(null, TEST_VERSION));
    }

    @Test
    public void testGetEntryNominal() {
        RecordTypeRegistry registry = getRegistry();
        RecordTypeEntry entry1 = registry.getEntry(TEST_TYPE, TEST_VERSION);
        RecordTypeEntry entry2 = new RecordTypeEntry(TEST_TYPE, TEST_VERSION, TEST_RESOURCE_PATH, null);
        assertNotNull(entry1);
        assertNotNull(entry2);
        assertTrue(entry1.equals(entry2));
    }

    @Test
    public void testGetEntryNotPresentBadType() {
        RecordTypeRegistry registry = getRegistry();
        RecordTypeEntry entry1 = registry.getEntry(BAD_TYPE, TEST_VERSION);
        assertNull(entry1);
    }

    @Test
    public void testGetEntryNotPresentBadVersion() {
        RecordTypeRegistry registry = getRegistry();
        RecordTypeEntry entry1 = registry.getEntry(TEST_TYPE, BAD_VERSION);
        assertNull(entry1);
    }


    private RecordTypeRegistry getRegistry() {
        RecordTypeRegistry registry = new RecordTypeRegistry();
        List<RecordTypeEntry> recordTypes = registry.getRecordTypes();
        recordTypes.add(new RecordTypeEntry(TEST_TYPE, TEST_VERSION, TEST_RESOURCE_PATH, null));
        return registry;
    }



}


