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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.NotNull;

import org.codice.ddf.migration.AbstractMigratable;
import org.codice.ddf.migration.ExportMigrationException;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationMetadata;
import org.codice.ddf.migration.MigrationWarning;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.UnsupportedQueryException;

/**
 * Implementation of the {@link org.codice.ddf.migration.Migratable} interface used to migrate
 * the current catalog provider's {@link Metacard}s.
 */
public class CatalogMigratableImpl extends AbstractMigratable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogMigratableImpl.class);

    private static final String CATALOG_EXPORT_DIRECTORY = "org.codice.ddf.catalog";

    private final CatalogProvider provider;

    private final FilterBuilder filterBuilder;

    private final MigrationFileWriter fileWriter;

    private final CatalogMigratableConfig config;

    /**
     * Constructor.
     *
     * @param description   description of this migratable
     * @param provider      provider used to retrieve the metacards to export
     * @param filterBuilder builder used to create query filters
     * @param fileWriter    object used to write metacards to disk
     * @param config        export configuration information
     */
    public CatalogMigratableImpl(@NotNull String description, @NotNull CatalogProvider provider,
            @NotNull FilterBuilder filterBuilder, @NotNull MigrationFileWriter fileWriter,
            @NotNull CatalogMigratableConfig config) {
        super(description, true);

        notNull(provider, "CatalogProvider cannot be null");
        notNull(filterBuilder, "FilterBuilder cannot be null");
        notNull(fileWriter, "FileWriter cannot be null");
        notNull(config, "Configuration object cannot be null");

        this.provider = provider;
        this.filterBuilder = filterBuilder;
        this.fileWriter = fileWriter;
        this.config = config;
    }

    /**
     * Exports all the metacards currently stored in the catalog provider.
     * <p>
     * {@inheritDoc}
     */
    public MigrationMetadata export(Path exportPath) throws MigrationException {
        config.setExportPath(exportPath.resolve(CATALOG_EXPORT_DIRECTORY));
        MigrationTaskManager taskManager = createTaskManager(config, createExecutorService(config));

        Collection<MigrationWarning> warnings = new ArrayList<>();
        Map<String, Serializable> props = new HashMap<>();
        Filter dumpFilter = filterBuilder.attribute(Metacard.ANY_TEXT)
                .is()
                .like()
                .text("*");

        useNativeQueryMode(props);

        QueryImpl exportQuery = createQuery(dumpFilter);
        QueryRequest exportQueryRequest = new QueryRequestImpl(exportQuery, props);

        exportQuery.setPageSize(config.getExportQueryPageSize());
        exportQuery.setRequestsTotalResultsCount(false);

        long exportGroupCount = 1;
        SourceResponse response;

        ExportMigrationException exportMigrationException = new ExportMigrationException(
                "Catalog could not export metacards");

        try {
            List<Result> results;

            do {
                response = provider.query(exportQueryRequest);
                if (response == null) {
                    LOGGER.error("Response came back null from the query: {}");
                    throw exportMigrationException;
                }

                results = response.getResults();
                if (results == null) {
                    LOGGER.error("Results came back null from the response: {}");
                    throw exportMigrationException;
                }

                if (results.size() > 0) {
                    taskManager.exportMetacardQuery(results, exportGroupCount);
                    exportQuery.setStartIndex(
                            exportQuery.getStartIndex() + config.getExportQueryPageSize());
                    exportGroupCount++;
                }
            } while (results.size() >= config.getExportQueryPageSize());
        } catch (UnsupportedQueryException e) {
            LOGGER.error("Query {} was invalid due to: {}", exportGroupCount, e.getMessage(), e);
            throw exportMigrationException;
        } catch (RuntimeException e) {
            LOGGER.error("Internal error occurred when exporting catalog: {}", e.getMessage(), e);
            throw exportMigrationException;
        } finally {
            try {
                taskManager.exportFinish();
            } catch (RuntimeException e) {
                throw new ExportMigrationException(e);
            }
        }

        return new MigrationMetadata(warnings);
    }

    // Factory method for creating QueryImpl objects so they can be mocked later.
    QueryImpl createQuery(Filter dumpFilter) {
        return new QueryImpl(dumpFilter);
    }

    // Factory method for creating MigrationTaskManager objects so they can be mocked later.
    MigrationTaskManager createTaskManager(CatalogMigratableConfig config,
            ExecutorService executorService) {
        return new MigrationTaskManager(config, fileWriter, executorService);
    }

    private ExecutorService createExecutorService(CatalogMigratableConfig config) {
        return new ThreadPoolExecutor(config.getExportThreadCount(),
                config.getExportThreadCount(),
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(config.getExportThreadCount()),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    private void useNativeQueryMode(Map<String, Serializable> props) {
        // Prevent the catalog provider from caching results.
        props.put("mode", "native");
    }
}
