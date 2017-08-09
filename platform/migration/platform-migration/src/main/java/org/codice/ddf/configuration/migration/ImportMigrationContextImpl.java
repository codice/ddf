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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.Validate;
import org.codice.ddf.migration.ImportMigrationContext;
import org.codice.ddf.migration.ImportMigrationEntry;
import org.codice.ddf.migration.Migratable;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.migration.MigrationWarning;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

/**
 * The import migration context keeps track of exported migration entries for a given migratable
 * while processing an import migration operation.
 */
public class ImportMigrationContextImpl extends MigrationContextImpl
        implements ImportMigrationContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImportMigrationContextImpl.class);

    /**
     * Holds exported migration entries keyed by the exported path.
     */
    private final Map<Path, ImportMigrationEntryImpl> entries = new TreeMap<>();

    /**
     * Holds migration entries referenced from system properties keyed by the property name.
     */
    private final Map<String, ImportMigrationSystemPropertyReferencedEntryImpl> systemProperties =
            new TreeMap<>();

    private final ZipFile zip;

    /**
     * Creates a new migration context for an import operation representing a system context.
     *
     * @param report the migration report where to record warnings and errors
     * @param zip    the zip file associated with the import
     * @throws IllegalArgumentException if <code>report</code> or <code>zip</code> is <code>null</code>
     */
    public ImportMigrationContextImpl(MigrationReport report, ZipFile zip) {
        super(report);
        Validate.notNull(zip, "invalid null zip");
        this.zip = zip;
    }

    /**
     * Creates a new migration context for an import operation.
     *
     * @param report the migration report where to record warnings and errors
     * @param zip    the zip file associated with the import
     * @param id     the migratable id
     * @throws IllegalArgumentException if <code>report</code>, <code>zip</code>, or <code>id</code>
     *                                  is <code>null</code>
     */
    public ImportMigrationContextImpl(MigrationReport report, ZipFile zip, String id) {
        super(report, id, null); // no version yet
        Validate.notNull(zip, "invalid null zip");
        this.zip = zip;
    }

    /**
     * Creates a new migration context for an import operation.
     *
     * @param report     the migration report where to record warnings and errors
     * @param zip        the zip file associated with the import
     * @param migratable the migratable this context is for
     * @throws IllegalArgumentException if <code>report</code>, <code>zip</code> or <code>migratable</code>
     *                                  is <code>null</code>
     */
    public ImportMigrationContextImpl(MigrationReport report, ZipFile zip, Migratable migratable) {
        super(report, migratable, null); // no version yet
        Validate.notNull(zip, "invalid null zip");
        this.zip = zip;
    }

    @Override
    public Optional<ImportMigrationEntry> getSystemPropertyReferencedEntry(String name) {
        Validate.notNull(name, "invalid null system property name");
        return Optional.ofNullable(systemProperties.get(name));
    }

    @Override
    public Optional<ImportMigrationEntry> getEntry(Path path) {
        Validate.notNull(path, "invalid null path");
        return Optional.ofNullable(entries.get(path));
    }

    @Override
    public Stream<ImportMigrationEntry> entries() {
        return entries.values()
                .stream()
                .map(ImportMigrationEntry.class::cast);
    }

    @Override
    public Stream<ImportMigrationEntry> entries(Path path) {
        Validate.notNull(path, "invalid null path");
        return entries.values()
                .stream()
                .filter(me -> me.getPath()
                        .startsWith(path))
                .map(ImportMigrationEntry.class::cast);
    }

    @Override
    public Stream<ImportMigrationEntry> entries(Path path, PathMatcher filter) {
        Validate.notNull(path, "invalid null path");
        Validate.notNull(filter, "invalid null filter");

        return entries(path).filter(e -> filter.matches(e.getPath()));
    }

    @Override
    public boolean cleanDirectory(Path path) {
        Validate.notNull(path, "invalid null path");
        final File fdir = getPathUtils().resolveAgainstDDFHome(path).toFile();

        LOGGER.debug("Cleaning up directory [{}]...", fdir);
        if (!fdir.exists()) {
            return true;
        }
        if (!fdir.isDirectory()) {
            LOGGER.info("Failed to clean directory [{}]", fdir);
            getReport().record(new MigrationWarning(String.format(
                    "Unable to clean directory [%s]; it is not a directory.",
                    fdir)));
            return false;
        }
        try {
            FileUtils.cleanDirectory(fdir);
        } catch (IOException e) {
            LOGGER.info("Failed to clean directory [" + fdir + "]: ", e);
            getReport().record(new MigrationWarning(String.format(
                    "Unable to clean directory [%s]; %s.",
                    fdir,
                    e.getMessage())));
            return false;
        }
        return true;
    }

    void doImport() {
        if (migratable != null) {
            LOGGER.debug("Importing migratable [{}] from version [{}]...",
                    id,
                    ((getVersion() != null) ? getVersion() : "<not-exported>"));
            Stopwatch stopwatch = null;

            if (LOGGER.isDebugEnabled()) {
                stopwatch = Stopwatch.createStarted();
            }
            if (Objects.equals(getVersion(), migratable.getVersion())) {
                migratable.doImport(this);
            } else {
                migratable.doIncompatibleImport(this, getVersion());
            }
            if (LOGGER.isDebugEnabled() && (stopwatch != null)) {
                LOGGER.debug("Imported time for {}: {}", id, stopwatch.stop());
            }
        } else if (id != null) { // not a system context
            report.record(new MigrationException("Exported data for migratable [" + id
                    + "] cannot be imported; migratable was not installed."));
        } // else - no errors and nothing to do for the system context
    }

    void addEntry(ImportMigrationEntryImpl entry) {
        entries.put(entry.getPath(), entry);
    }

    InputStream getInputStreamFor(ZipEntry entry) throws IOException {
        return zip.getInputStream(entry);
    }

    protected void processMetadata(Map<String, Object> metadata) {
        LOGGER.debug("Imported metadata for {}: {}", id, metadata);
        super.processMetadata(metadata);
        // process external entries first so we have a complete set of migratable data entries that
        // were exported by a migratable before we start looking at the property references
        JsonUtils.getListFrom(metadata, MigrationContextImpl.METADATA_EXTERNALS)
                .stream()
                .map(JsonUtils::convertToMap)
                .map(m -> new ImportMigrationExternalEntryImpl(this, m))
                .forEach(me -> entries.put(me.getPath(), me));
        // process system property references
        JsonUtils.getListFrom(metadata, MigrationContextImpl.METADATA_SYSTEM_PROPERTIES)
                .stream()
                .map(JsonUtils::convertToMap)
                .map(m -> new ImportMigrationSystemPropertyReferencedEntryImpl(this, m))
                .forEach(me -> systemProperties.put(me.getProperty(), me));
        // process java property references
        JsonUtils.getListFrom(metadata, MigrationContextImpl.METADATA_JAVA_PROPERTIES)
                .stream()
                .map(JsonUtils::convertToMap)
                .map(m -> new ImportMigrationJavaPropertyReferencedEntryImpl(this, m))
                .forEach(me -> entries.compute(me.getPropertiesPath(), (p, mpe) -> {
                    if (mpe == null) {
                        // create a new empty migration entry as it was not exported up (at least not by this migratable)!!!!
                        mpe = new ImportMigrationEmptyEntryImpl(this, p);
                    }
                    mpe.addPropertyReferenceEntry(me.getProperty(), me);
                    return mpe;
                }));
    }
}
