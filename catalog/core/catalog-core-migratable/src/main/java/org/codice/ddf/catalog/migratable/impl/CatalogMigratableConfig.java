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
import java.nio.file.Paths;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CatalogMigratableConfig {

    //  Pkg pvt bounds on the property values - to ensure relationships are preserved in test

    static final int MAX_CARDS_PER_FILE = 50000;

    static final int MAX_QUERY_PAGE_SIZE = 100000;

    static final int MIN_QUERY_PAGE_SIZE = 1000;

    static final int MAX_THREADS = 20;

    private static final Pattern RULE_FILE_NAME = Pattern.compile("[a-zA-Z]+");

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogMigratableConfig.class);

    private String exportFilePrefix;

    private int exportCardsPerFile;

    private Path exportPath;

    private int exportQueryPageSize;

    private int exportThreadCount;

    // Needed to bypass setter restrictions when unit testing
    CatalogMigratableConfig(int queryPageSize) {
        this();
        this.exportQueryPageSize = queryPageSize;
    }

    public CatalogMigratableConfig() {
        exportPath = Paths.get("migration");
        this.exportFilePrefix = "catalogExport";
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
        if (exportCardsPerFile >= 1 && exportCardsPerFile <= MAX_CARDS_PER_FILE) {
            if (exportCardsPerFile <= this.exportQueryPageSize) {
                this.exportCardsPerFile = exportCardsPerFile;
            } else {
                LOGGER.error("Cards per file must be less than or equal to query page size");
                throw new IllegalArgumentException(
                        "Cards per file must be less than or equal to query page size");
            }
        } else {
            LOGGER.error("Cards per file is invalid; must be between {} and {}",
                    1,
                    MAX_CARDS_PER_FILE);
            throw new IllegalArgumentException(String.format(
                    "Cards per file is invalid; must be between %d and %d",
                    1,
                    MAX_CARDS_PER_FILE));
        }
    }

    public int getExportQueryPageSize() {
        return exportQueryPageSize;
    }

    public void setExportQueryPageSize(int exportQueryPageSize) {
        if (exportQueryPageSize >= MIN_QUERY_PAGE_SIZE
                && exportQueryPageSize <= MAX_QUERY_PAGE_SIZE) {
            this.exportQueryPageSize = exportQueryPageSize;
        } else {
            LOGGER.error("Query page size is invalid; must be between {} and {}",
                    MIN_QUERY_PAGE_SIZE,
                    MAX_QUERY_PAGE_SIZE);
            throw new IllegalArgumentException(String.format(
                    "Query page size is invalid; must be between %d and %d",
                    MIN_QUERY_PAGE_SIZE,
                    MAX_QUERY_PAGE_SIZE));
        }
    }

    public int getExportThreadCount() {
        return exportThreadCount;
    }

    public void setExportThreadCount(int exportThreadCount) {
        if (exportThreadCount >= 1 && exportThreadCount <= MAX_THREADS) {
            this.exportThreadCount = exportThreadCount;
        } else {
            LOGGER.error("Thread count is invalid; must be between 1 and {}", MAX_THREADS);
            throw new IllegalArgumentException(String.format(
                    "Thread count is invalid; must be between 1 and %s",
                    MAX_THREADS));
        }
    }

    public String getExportFilePrefix() {
        return exportFilePrefix;
    }

    public void setExportFilePrefix(String prefix) {
        if (RULE_FILE_NAME.matcher(prefix)
                .matches()) {
            this.exportFilePrefix = prefix;
        } else {
            LOGGER.error("File prefix is invalid - must be of the form: {}",
                    RULE_FILE_NAME.toString());
            throw new IllegalArgumentException(String.format(
                    "File prefix is invalid - must be of the form: %s",
                    RULE_FILE_NAME.toString()));
        }
    }
}
