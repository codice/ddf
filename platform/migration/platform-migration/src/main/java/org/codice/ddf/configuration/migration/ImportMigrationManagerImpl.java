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
package org.codice.ddf.configuration.migration;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.codice.ddf.migration.ImportMigrationEntry;
import org.codice.ddf.migration.ImportMigrationException;
import org.codice.ddf.migration.Migratable;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationOperation;
import org.codice.ddf.migration.MigrationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The import migration manager process an exported file and manages the import migration operation.
 */
public class ImportMigrationManagerImpl implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImportMigrationManagerImpl.class);

    private final MigrationReport report;

    /**
     * Holds import migration contexts for all migratables registered in the system keyed by the
     * migratable id. <code>null</code> key is used to represent the system context.
     */
    private final Map<String, ImportMigrationContextImpl> contexts;

    /**
     * Holds the exported metadata. The data is retrieved from the export.json file.
     */
    private final Map<String, Object> metadata;

    private final ZipFile zip;

    private final String version;

    private final String productVersion;

    private final Path exportFile;

    /**
     * Creates a new migration manager for an import operation.
     *
     * @param report      the migration report where to record warnings and errors
     * @param exportFile  the exported zip file
     * @param migratables a stream of all migratables in the system
     * @throws MigrationException       if a failure occurs while processing the zip file (the error
     *                                  will not be recorded with the report)
     * @throws IllegalArgumentException if <code>report</code> is <code>null</code> or if it is not
     *                                  for an import migration operation or if <code>exportFile</code>
     *                                  or <code>migratables</code> is <code>null</code>
     */
    public ImportMigrationManagerImpl(MigrationReport report, Path exportFile,
            Stream<? extends Migratable> migratables) {
        this(report, exportFile, migratables, ImportMigrationManagerImpl.newZipFileFor(exportFile));
    }

    ImportMigrationManagerImpl(MigrationReport report, Path exportFile,
            Stream<? extends Migratable> migratables, ZipFile zip) {
        Validate.notNull(report, "invalid null report");
        Validate.isTrue(report.getOperation() == MigrationOperation.IMPORT,
                "invalid migration operation");
        Validate.notNull(migratables, "invalid null migratables");
        this.report = report;
        this.exportFile = exportFile;
        this.zip = zip;
        try {
            // pre-create contexts for all registered migratables
            this.contexts = migratables.collect(Collectors.toMap(Migratable::getId,
                    m -> new ImportMigrationContextImpl(report, zip, m),
                    ConfigurationMigrationManager.throwingMerger(),
                    LinkedHashMap::new)); // to preserved ranking order and remove duplicates
            // add a system contexts
            contexts.put(null, new ImportMigrationContextImpl(report, zip));
            zip.stream()
                    .map(ze -> new ImportMigrationEntryImpl(this::getContextFor, ze))
                    .forEach(me -> me.getContext()
                            .addEntry(me));
            this.metadata = retrieveMetadata();
        } catch (IOException e) {
            throw new ImportMigrationException(String.format("failed importing from file [%s]",
                    exportFile), e);
        }
        this.version = JsonUtils.getStringFrom(metadata,
                MigrationContextImpl.METADATA_VERSION,
                true);
        if (!MigrationContextImpl.VERSION.equals(version)) {
            throw new ImportMigrationException(String.format(
                    "unsupported exported migrated version [%s]; currently supporting [%s]",
                    version,
                    MigrationContextImpl.VERSION));
        }
        this.productVersion = JsonUtils.getStringFrom(metadata,
                MigrationContextImpl.METADATA_PRODUCT_VERSION,
                true);
        // process migratables' metadata
        JsonUtils.getMapFrom(metadata, MigrationContextImpl.METADATA_MIGRATABLES)
                .forEach((id, o) -> getContextFor(id).processMetadata(JsonUtils.convertToMap(o)));
    }

    private static ZipFile newZipFileFor(Path exportFile) {
        Validate.notNull(exportFile, "invalid null export file");
        try {
            return new ZipFile(exportFile.toFile());
        } catch (FileNotFoundException e) {
            throw new ImportMigrationException(String.format("missing export file [%s]",
                    exportFile), e);
        } catch (IOException e) {
            throw new ImportMigrationException(String.format("failed to open export file [%s]",
                    exportFile), e);
        }
    }

    /**
     * Proceed with the import migration operation.
     *
     * @param productVersion the product version to compare against
     * @throws IllegalArgumentException if <code>productVersion</code> is <code>null</code>
     * @throws MigrationException       if the versions don't match or if a failure occurred that required
     *                                  interrupting the operation right away
     */
    public void doImport(String productVersion) {
        Validate.notNull(productVersion, "invalid null product version");
        if (!productVersion.equals(this.productVersion)) {
            throw new ImportMigrationException(String.format(
                    "mismatched exported product version [%s]; expecting [%s]",
                    this.productVersion,
                    productVersion));
        }
        LOGGER.debug("Importing product [{}] from version [{}]...", productVersion, version);
        contexts.values()
                .forEach(ImportMigrationContextImpl::doImport);
    }

    @Override
    public void close() throws IOException {
        zip.close();
    }

    public MigrationReport getReport() {
        return report;
    }

    public Path getExportFile() {
        return exportFile;
    }

    // used for testing
    Collection<ImportMigrationContextImpl> getContexts() {
        return contexts.values();
    }

    private ImportMigrationContextImpl getContextFor(String id) {
        return contexts.computeIfAbsent(id, mid -> {
            // create a dummy context with no migratable and bind it to its migratable id
            return new ImportMigrationContextImpl(report, zip, mid);
        });
    }

    private Map<String, Object> retrieveMetadata() throws IOException {
        final ImportMigrationEntry me =
                contexts.get(null) // metadata entries have no migratable id and will always exist see ctor
                        .getEntry(MigrationContextImpl.METADATA_FILENAME);
        InputStream is = null;

        try {
            is = me.getInputStream()
                    .orElseThrow(() -> new ImportMigrationException(String.format(
                            "missing metadata file [%s] from exported data",
                            MigrationContextImpl.METADATA_FILENAME)));
            return JsonUtils.MAPPER.parser()
                    .parseMap(IOUtils.toString(is, Charset.defaultCharset()));
        } finally {
            IOUtils.closeQuietly(is);
        }
    }
}
