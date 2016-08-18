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

import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains configuration constants used to change the behavior of catalog migratable operations.
 */
public class CatalogMigratableConfig {

    private static final int MAX_CARDS_PER_FILE = 50000;

    private static final int MAX_QUERY_PAGE_SIZE = 100000;

    private static final int MIN_QUERY_PAGE_SIZE = 1;

    private static final int MAX_THREADS = 128;

    private static final String FILE_PREFIX = "catalogExport";

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogMigratableConfig.class);

    private int exportCardsPerFile;

    private Path exportPath;

    private int exportQueryPageSize;

    private int exportThreadCount;

    public CatalogMigratableConfig() {
        exportPath = null;
        this.exportCardsPerFile = 1;
        this.exportQueryPageSize = 5000;
        this.exportThreadCount = 8;
    }

    public Path getExportPath() {
        return exportPath;
    }

    public void setExportPath(Path exportPath) {
        this.exportPath = exportPath;
    }

    public int getExportCardsPerFile() {
        return exportCardsPerFile;
    }

    public void setExportCardsPerFile(int exportCardsPerFile) {
        if (exportCardsPerFile < 1 || exportCardsPerFile > MAX_CARDS_PER_FILE) {
            String errorMsgBounds = String.format(
                    "%d cards per file is invalid; must be between %d and %d",
                    exportCardsPerFile,
                    1,
                    MAX_CARDS_PER_FILE);

            LOGGER.info(errorMsgBounds);
            throw new IllegalArgumentException(errorMsgBounds);
        }

        if (exportCardsPerFile > this.exportQueryPageSize) {
            String errorMsgLessThanQuery = String.format(
                    "%d cards per file is not less than or equal to query page size of %d",
                    exportCardsPerFile,
                    this.exportQueryPageSize);

            LOGGER.info(errorMsgLessThanQuery);
            throw new IllegalArgumentException(errorMsgLessThanQuery);
        }

        this.exportCardsPerFile = exportCardsPerFile;
    }

    public int getExportQueryPageSize() {
        return exportQueryPageSize;
    }

    public void setExportQueryPageSize(int exportQueryPageSize) {
        if (exportQueryPageSize < MIN_QUERY_PAGE_SIZE
                || exportQueryPageSize > MAX_QUERY_PAGE_SIZE) {
            String errorMsgBounds = String.format(
                    "A query page size of %d is invalid; must be between %d and %d",
                    exportQueryPageSize,
                    MIN_QUERY_PAGE_SIZE,
                    MAX_QUERY_PAGE_SIZE);

            LOGGER.info(errorMsgBounds);
            throw new IllegalArgumentException(errorMsgBounds);
        }

        this.exportQueryPageSize = exportQueryPageSize;
    }

    public int getExportThreadCount() {
        return exportThreadCount;
    }

    public void setExportThreadCount(int exportThreadCount) {
        if (exportThreadCount < 1 || exportThreadCount > MAX_THREADS) {
            String errorMsgBounds = String.format(
                    "A thread count of %d is invalid; must be between %d and %d",
                    exportThreadCount,
                    1,
                    MAX_THREADS);

            LOGGER.info(errorMsgBounds);
            throw new IllegalArgumentException(errorMsgBounds);
        }

        this.exportThreadCount = exportThreadCount;
    }

    public String getExportFilePrefix() {
        return FILE_PREFIX;
    }
}
