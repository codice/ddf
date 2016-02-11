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

import static org.junit.Assert.assertThat;

import org.hamcrest.Matchers;
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
    public void testPropertyMaximaMinimaBounds() throws Exception {
        assertThat(CatalogMigratableConfig.MAX_CARDS_PER_FILE
                <= CatalogMigratableConfig.MAX_QUERY_PAGE_SIZE / 2, Matchers.is(true));
        assertThat(CatalogMigratableConfig.MIN_QUERY_PAGE_SIZE > 0, Matchers.is(true));
        assertThat(CatalogMigratableConfig.MAX_QUERY_PAGE_SIZE < 1000000, Matchers.is(true));
        assertThat(CatalogMigratableConfig.MAX_THREADS > 1, Matchers.is(true));
        assertThat(CatalogMigratableConfig.MAX_THREADS < 200, Matchers.is(true));
    }

    @Test
    public void testFilePrefixValid() throws Exception {
        config.setExportFilePrefix("validPrefix");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFilePrefixNumeric() throws Exception {
        config.setExportFilePrefix("numericPrefix8");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFilePrefixSymbolic() throws Exception {
        config.setExportFilePrefix("symbolic_prefix");
    }

    @Test
    public void testExportQueryPageSizeValid() throws Exception {
        config.setExportQueryPageSize(70000);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExportQueryPageSizeTooLarge() throws Exception {
        config.setExportQueryPageSize(200000);
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
        config.setExportCardsPerFile(50100);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCardsPerFileTooSmall() throws Exception {
        config.setExportCardsPerFile(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCardsPerFileBiggerThenQueryPageSize() throws Exception {
        config.setExportQueryPageSize(40000);
        config.setExportCardsPerFile(45000);
    }
}
