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
package org.codice.ddf.admin.application.service.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ZipFileApplicationDetailsTest {

    private static final String TEST_NAME_1 = "TestName";

    private static final String TEST_NAME_2 = "TestName2";

    private static final String TEST_VERSION_1 = "0.0.0";

    private static final String TEST_VERSION_2 = "0.0.1";

    /**
     * Tests the {@link ZipFileApplicationDetails#ZipFileApplicationDetails(String, String)} constructor,
     * and the getters related to it
     */
    @Test
    public void testConstructor() {
        ZipFileApplicationDetails testZipFile = new ZipFileApplicationDetails(TEST_NAME_1,
                TEST_VERSION_1);

        assertEquals(TEST_NAME_1, testZipFile.getName());
        assertEquals(TEST_VERSION_1, testZipFile.getVersion());

        testZipFile.setName(TEST_NAME_2);
        testZipFile.setVersion(TEST_VERSION_2);

        assertEquals(TEST_NAME_2, testZipFile.getName());
        assertEquals(TEST_VERSION_2, testZipFile.getVersion());
    }
}
