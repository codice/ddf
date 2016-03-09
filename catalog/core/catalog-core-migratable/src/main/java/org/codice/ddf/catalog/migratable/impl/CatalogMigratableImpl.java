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

import static org.apache.commons.lang.Validate.notNull;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.codice.ddf.migration.AbstractMigratable;
import org.codice.ddf.migration.ExportMigrationException;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationMetadata;
import org.codice.ddf.migration.MigrationWarning;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;

/**
 * Implementation of the {@link org.codice.ddf.migration.Migratable} interface used to migrate
 * the current catalog framework's {@link Metacard}s.
 */
public class CatalogMigratableImpl extends AbstractMigratable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogMigratableImpl.class);

    private static final String DEFAULT_FAILURE_MESSAGE = "Catalog could not export metacards";

    private static final String CATALOG_EXPORT_DIRECTORY = "org.codice.ddf.catalog";

    private final CatalogFramework framework;

    private final FilterBuilder filterBuilder;

    private final MigrationFileWriter fileWriter;

    private final CatalogMigratableConfig config;

    private final MigrationTaskManager taskManager;

    /**
     * Basic constructor with minimum required components.
     *
     * @param description   description of this migratable
     * @param framework     framework used to retrieve the metacards to export
     * @param filterBuilder builder used to create query filters
     * @param fileWriter    object used to write metacards to disk
     * @param config        export configuration information
     */
    public CatalogMigratableImpl(@NotNull String description, @NotNull CatalogFramework framework,
            @NotNull FilterBuilder filterBuilder, @NotNull MigrationFileWriter fileWriter,
            @NotNull CatalogMigratableConfig config) {
        this(description,
                framework,
                filterBuilder,
                fileWriter,
                config,
                new MigrationTaskManager(config, fileWriter));
    }

    /**
     * Constructor that allows the caller choice of {@link MigrationTaskManager} to be injected.
     *
     * @param description   description of this migratable
     * @param framework     framework used to retrieve the metacards to export
     * @param filterBuilder builder used to create query filters
     * @param fileWriter    object used to write metacards to disk
     * @param config        export configuration information
     * @param taskManager   the manager responsible for submitting file writing jobs during export
     */
    public CatalogMigratableImpl(@NotNull String description, @NotNull CatalogFramework framework,
            @NotNull FilterBuilder filterBuilder, @NotNull MigrationFileWriter fileWriter,
            @NotNull CatalogMigratableConfig config, @NotNull MigrationTaskManager taskManager) {
        super(description, true);

        notNull(framework, "CatalogFramework cannot be null");
        notNull(filterBuilder, "FilterBuilder cannot be null");
        notNull(fileWriter, "FileWriter cannot be null");
        notNull(config, "Configuration object cannot be null");
        notNull(taskManager, "Task manager cannot be null");

        this.framework = framework;
        this.filterBuilder = filterBuilder;
        this.fileWriter = fileWriter;
        this.config = config;

        this.taskManager = taskManager;
    }

    /**
     * Exports all the metacards currently stored in the catalog framework.
     * <p>
     * {@inheritDoc}
     */
    public MigrationMetadata export(Path exportPath) throws MigrationException {
        config.setExportPath(exportPath.resolve(CATALOG_EXPORT_DIRECTORY));
        fileWriter.createExportDirectory(config.getExportPath());

        Collection<MigrationWarning> warnings = new ArrayList<>();
        Map<String, Serializable> props = createMapWithNativeQueryMode();
        Filter dumpFilter = filterBuilder.attribute(Metacard.ANY_TEXT)
                .is()
                .like()
                .text("*");

        QueryImpl exportQuery = new QueryImpl(dumpFilter);
        exportQuery.setPageSize(config.getExportQueryPageSize());
        exportQuery.setRequestsTotalResultsCount(false);

        QueryRequest exportQueryRequest = new QueryRequestImpl(exportQuery, props);

        try {
            executeQueryLoop(exportQuery, exportQueryRequest);
        } catch (Exception e) {
            LOGGER.error("Internal error occurred when exporting catalog: {}", e);
            throw new ExportMigrationException(DEFAULT_FAILURE_MESSAGE);
        } finally {
            cleanup();
        }

        return new MigrationMetadata(warnings);
    }

    private void cleanup() throws MigrationException {
        try {
            taskManager.close();
        } catch (MigrationException e) {
            LOGGER.error("Migration exception when closing the task manager: {}", e);
            throw e;
        } catch (Exception e) {
            LOGGER.error("Internal error when closing the task manager: {}", e);
            throw new MigrationException("Error closing task manager: {}", e);
        }
    }

    private Map<String, Serializable> createMapWithNativeQueryMode() {
        Map<String, Serializable> props = new HashMap<>();
        // Prevent the catalog framework from caching results. Be efficient.
        props.put("mode", "native");
        return props;
    }

    private void executeQueryLoop(QueryImpl exportQuery, QueryRequest exportQueryRequest)
            throws UnsupportedQueryException, SourceUnavailableException, FederationException {

        long exportGroupCount = 1;
        SourceResponse response;
        List<Result> results;

        do {
            response = framework.query(exportQueryRequest);
            if (response == null) {
                LOGGER.error("Response came back null from the query");
                throw new ExportMigrationException(DEFAULT_FAILURE_MESSAGE);
            }

            results = response.getResults();
            if (results == null) {
                LOGGER.error("Results came back null from the response");
                throw new ExportMigrationException(DEFAULT_FAILURE_MESSAGE);
            }

            if (!results.isEmpty()) {
                taskManager.exportMetacardQuery(results, exportGroupCount);
                exportQuery.setStartIndex(
                        exportQuery.getStartIndex() + config.getExportQueryPageSize());
                exportGroupCount++;
            }
        } while (results.size() >= config.getExportQueryPageSize());
    }
}
