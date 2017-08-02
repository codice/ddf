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

import org.codice.ddf.migration.DataMigratable;
import org.codice.ddf.migration.ExportMigrationContext;
import org.codice.ddf.migration.ImportMigrationContext;
import org.codice.ddf.migration.MigrationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.filter.FilterBuilder;

/**
 * Implementation of the {@link org.codice.ddf.migration.DataMigratable} interface used to migrate
 * the current catalog framework's {@link Metacard}s.
 */
public class MetacardsMigratable implements DataMigratable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetacardsMigratable.class);

    /**
     * Holds the current export version.
     * <p>
     * 1.0 - initial version
     */
    private static final String VERSION = "1.0";

    private static final String DEFAULT_FAILURE_MESSAGE = "Catalog could not export metacards";

    private final CatalogFramework framework;

    private final FilterBuilder filterBuilder;

    private final MigrationFileWriter fileWriter;

    private final CatalogMigratableConfig config;

    private final MigrationTaskManager taskManager;

    /**
     * Basic constructor with minimum required components.
     *
     * @param framework     framework used to retrieve the metacards to export
     * @param filterBuilder builder used to create query filters
     * @param config        export configuration information
     */
    public MetacardsMigratable(CatalogFramework framework, FilterBuilder filterBuilder,
            MigrationFileWriter fileWriter, CatalogMigratableConfig config) {

        this(framework,
                filterBuilder,
                fileWriter,
                config,
                new MigrationTaskManager(config, fileWriter));

    }

    /**
     * Constructor that allows the caller choice of {@link MigrationTaskManager} to be injected.
     *
     * @param framework     framework used to retrieve the metacards to export
     * @param filterBuilder builder used to create query filters
     * @param config        export configuration information
     * @param taskManager   the manager responsible for submitting file writing jobs during export
     */
    public MetacardsMigratable(CatalogFramework framework, FilterBuilder filterBuilder,
            MigrationFileWriter fileWriter, CatalogMigratableConfig config,
            MigrationTaskManager taskManager) {
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

    @Override
    public String getVersion() {
        return MetacardsMigratable.VERSION;
    }

    @Override
    public String getId() {
        return "ddf.metacards";
    }

    @Override
    public String getTitle() {
        return "Metacard Migration";
    }

    @Override
    public String getDescription() {
        return "Exports Catalog metacards";
    }

    @Override
    public String getOrganization() {
        return "Codice";
    }

    /**
     * Exports all the metacards currently stored in the catalog framework.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void doExport(ExportMigrationContext context) {
        // When re-implementing this, make sure to address paging using the paging iterator
        // to export all metacards
        //        config.setExportPath(exportPath.resolve(this.getId()));
        //        fileWriter.createExportDirectory(config.getExportPath());
        //
        //        Collection<MigrationWarning> warnings = new ArrayList<>();
        //        Map<String, Serializable> props = createMapWithNativeQueryMode();
        //        Filter dumpFilter = filterBuilder.attribute(Metacard.ANY_TEXT)
        //                .is()
        //                .like()
        //                .text("*");
        //
        //        QueryImpl exportQuery = new QueryImpl(dumpFilter);
        //        exportQuery.setPageSize(config.getExportQueryPageSize());
        //        exportQuery.setRequestsTotalResultsCount(false);
        //
        //        QueryRequest exportQueryRequest = new QueryRequestImpl(exportQuery, props);
        //
        //        try {
        //            executeQueryLoop(exportQuery, exportQueryRequest);
        //        } catch (Exception e) {
        //            LOGGER.info("Internal error occurred when exporting catalog: {}", e);
        //            throw new ExportMigrationException(DEFAULT_FAILURE_MESSAGE);
        //        } finally {
        //            cleanup();
        //        }
        //
        //        return new MigrationMetadata(warnings);
        throw new MigrationException("not implemented yet");
    }

    @Override
    public void doImport(ImportMigrationContext context) {
        throw new MigrationException("not implemented yet");
    }

    //    private void cleanup() throws MigrationException {
    //        try {
    //            taskManager.close();
    //        } catch (MigrationException e) {
    //            LOGGER.info("Migration exception when closing the task manager: {}", e);
    //            throw e;
    //        } catch (Exception e) {
    //            LOGGER.info("Internal error when closing the task manager: {}", e);
    //            throw new MigrationException("Error closing task manager: {}", e);
    //        }
    //    }
    //
    //    private Map<String, Serializable> createMapWithNativeQueryMode() {
    //        Map<String, Serializable> props = new HashMap<>();
    //        // Prevent the catalog framework from caching results. Be efficient.
    //        props.put("mode", "native");
    //        return props;
    //    }
    //
    //    private void executeQueryLoop(QueryImpl exportQuery, QueryRequest exportQueryRequest)
    //            throws UnsupportedQueryException, SourceUnavailableException, FederationException {
    //
    //        long exportGroupCount = 1;
    //        SourceResponse response;
    //        List<Result> results;
    //
    //        do {
    //            response = framework.query(exportQueryRequest);
    //            if (response == null) {
    //                LOGGER.info("Response came back null from the query");
    //                throw new ExportMigrationException(DEFAULT_FAILURE_MESSAGE);
    //            }
    //
    //            results = response.getResults();
    //            if (results == null) {
    //                LOGGER.info("Results came back null from the response");
    //                throw new ExportMigrationException(DEFAULT_FAILURE_MESSAGE);
    //            }
    //
    //            if (!results.isEmpty()) {
    //                taskManager.exportMetacardQuery(results, exportGroupCount);
    //                exportQuery.setStartIndex(
    //                        exportQuery.getStartIndex() + config.getExportQueryPageSize());
    //                exportGroupCount++;
    //            }
    //        } while (results.size() >= config.getExportQueryPageSize());
    //    }
}
