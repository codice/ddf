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
import static org.junit.Assert.assertTrue;

import javax.xml.namespace.QName;

import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.junit.Test;

/**
 * Tests RecordTypeEntry
 */
public class TestRecordTypeEntry {

    private static final QName TEST_TYPE = new QName(CswConstants.CSW_OUTPUT_SCHEMA, "Record", "csw");
    private static final String TEST_VERSION = "testVersion";
    private static final String TEST_RESOURCE_PATH = "testResourcePath";

    @Test
    public void testEqualsBothNull() {
        RecordTypeEntry entry1 = new RecordTypeEntry(null, null, null, null);
        RecordTypeEntry entry2 = new RecordTypeEntry(null, null, null, null);
        assertTrue(entry1.equals(entry2));
    }

    @Test
    public void testEqualsOneTypeNull() {
        RecordTypeEntry entry1 = new RecordTypeEntry(null, TEST_VERSION, TEST_RESOURCE_PATH, null);
        RecordTypeEntry entry2 = new RecordTypeEntry(TEST_TYPE, TEST_VERSION, TEST_RESOURCE_PATH, null);
        assertFalse(entry1.equals(entry2));
    }

    @Test
    public void testEqualsOneVersionNull() {
        RecordTypeEntry entry1 = new RecordTypeEntry(TEST_TYPE, null, TEST_RESOURCE_PATH, null);
        RecordTypeEntry entry2 = new RecordTypeEntry(TEST_TYPE, TEST_VERSION, TEST_RESOURCE_PATH, null);
        assertFalse(entry1.equals(entry2));
    }

    @Test
    public void testEqualsBothEqual() {
        RecordTypeEntry entry1 = new RecordTypeEntry(TEST_TYPE, TEST_VERSION, TEST_RESOURCE_PATH, null);
        RecordTypeEntry entry2 = new RecordTypeEntry(TEST_TYPE, TEST_VERSION, TEST_RESOURCE_PATH, null);
        assertTrue(entry1.equals(entry2));
    }


}
