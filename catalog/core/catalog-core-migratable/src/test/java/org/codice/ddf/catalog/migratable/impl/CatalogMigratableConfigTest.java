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
package org.codice.ddf.catalog.migratable.impl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CatalogMigratableConfigTest {
    private static final int ZERO = 0;

    private static final int VALID_THREAD_COUNT = 1;

    private static final int TOO_LARGE_THREAD_COUNT = 129;

    private static final int VALID_QUERY_PAGE_SIZE = 40000;

    private static final int TOO_LARGE_QUERY_PAGE_SIZE = 100001;

    private static final int VALID_CARDS_PER_FILE = 5000;

    private static final int TOO_LARGE_CARDS_PER_FILE = 50001;

    private static final int BIGGER_THAN_QUERY_CARDS_PER_FILE = 40001;

    private CatalogMigratableConfig config;

    @Before
    public void setup() throws Exception {
        config = new CatalogMigratableConfig();
    }

    @Test
    public void testExportQueryPageSizeValid() throws Exception {
        config.setExportQueryPageSize(VALID_QUERY_PAGE_SIZE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExportQueryPageSizeTooLarge() throws Exception {
        config.setExportQueryPageSize(TOO_LARGE_QUERY_PAGE_SIZE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExportQueryPageSizeTooSmall() throws Exception {
        config.setExportQueryPageSize(ZERO);
    }

    @Test
    public void testCardsPerFileSizeValid() throws Exception {
        config.setExportQueryPageSize(VALID_QUERY_PAGE_SIZE);
        config.setExportCardsPerFile(VALID_CARDS_PER_FILE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCardsPerFileTooLarge() throws Exception {
        config.setExportCardsPerFile(TOO_LARGE_CARDS_PER_FILE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCardsPerFileTooSmall() throws Exception {
        config.setExportCardsPerFile(ZERO);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCardsPerFileBiggerThenQueryPageSize() throws Exception {
        config.setExportQueryPageSize(VALID_QUERY_PAGE_SIZE);
        config.setExportCardsPerFile(BIGGER_THAN_QUERY_CARDS_PER_FILE);
    }

    @Test
    public void testThreadCountValid() throws Exception {
        config.setExportThreadCount(VALID_THREAD_COUNT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThreadCountTooSmall() throws Exception {
        config.setExportThreadCount(ZERO);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThreadCountTooLarge() throws Exception {
        config.setExportThreadCount(TOO_LARGE_THREAD_COUNT);
    }
}
