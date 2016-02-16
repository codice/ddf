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

    private CatalogMigratableConfig config;

    @Before
    public void setup() throws Exception {
        config = new CatalogMigratableConfig();
    }

    @Test
    public void testExportQueryPageSizeValid() throws Exception {
        config.setExportQueryPageSize(70000);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExportQueryPageSizeTooLarge() throws Exception {
        config.setExportQueryPageSize(100001);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExportQueryPageSizeTooSmall() throws Exception {
        config.setExportQueryPageSize(0);
    }

    @Test
    public void testCardsPerFileSizeValid() throws Exception {
        config.setExportQueryPageSize(40000);
        config.setExportCardsPerFile(5000);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCardsPerFileTooLarge() throws Exception {
        config.setExportCardsPerFile(50001);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCardsPerFileTooSmall() throws Exception {
        config.setExportCardsPerFile(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCardsPerFileBiggerThenQueryPageSize() throws Exception {
        config.setExportQueryPageSize(40000);
        config.setExportCardsPerFile(40001);
    }

    @Test
    public void testThreadCountValid() throws Exception {
        config.setExportThreadCount(1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThreadCountTooSmall() throws Exception {
        config.setExportThreadCount(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThreadCountTooLarge() throws Exception {
        config.setExportThreadCount(129);
    }
}
