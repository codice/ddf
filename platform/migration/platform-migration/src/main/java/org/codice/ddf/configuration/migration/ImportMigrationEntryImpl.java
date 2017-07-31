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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.Validate;
import org.codice.ddf.migration.ImportMigrationEntry;
import org.codice.ddf.migration.ImportPathMigrationException;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationImporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides an implementation of the {@link ImportMigrationEntry} interface.
 */
public class ImportMigrationEntryImpl extends MigrationEntryImpl<ImportMigrationContextImpl>
        implements ImportMigrationEntry {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImportMigrationEntryImpl.class);

    private final Map<String, ImportMigrationJavaPropertyReferencedEntryImpl> properties =
            new HashMap<>(8);

    private final ZipFile zip;

    private final ZipEntry entry;

    private final AtomicReference<InputStream> inputStream = new AtomicReference<>();

    ImportMigrationEntryImpl(Function<String, ImportMigrationContextImpl> contextProvider,
            ZipFile zip, ZipEntry ze) {
        super(contextProvider, ze.getName());
        this.zip = zip;
        this.entry = ze;
    }

    protected ImportMigrationEntryImpl(ImportMigrationContextImpl context, Path path) {
        super(context, path);
        this.zip = null;
        this.entry = null;
    }

    protected ImportMigrationEntryImpl(ImportMigrationContextImpl context, String name) {
        super(context, name);
        this.zip = null;
        this.entry = null;
    }

    @Override
    public long getLastModifiedTime() {
        return entry.getTime();
    }

    @Override
    public long getSize() {
        return entry.getSize();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        try {
            return inputStream.updateAndGet(is -> (is != null) ?
                    is :
                    context.getInputStreamFor(entry));
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    @Override
    public void store() {
        store((r, in) -> FileUtils.copyInputStreamToFile(getInputStream(),
                getAbsolutePath().toFile()));
    }

    @Override
    public void store(MigrationImporter importer) {
        Validate.notNull(importer, "invalid null importer");
        if (!stored) {
            super.stored = true;
            LOGGER.debug("Importing [{}] to [{}]...", path, getAbsolutePath());
            try {
                importer.apply(getReport(), getInputStream());
            } catch (IOException e) {
                getReport().record(new ImportPathMigrationException(path,
                        String.format("failed to copy to [%s]", MigrationEntryImpl.DDF_HOME),
                        e));
            } catch (MigrationException e) {
                throw e;
            }
        }
    }

    @Override
    public Optional<ImportMigrationEntry> getPropertyReferencedEntry(String name) {
        Validate.notNull(name, "invalid null property name");
        return Optional.ofNullable(properties.get(name));
    }

    void addPropertyReferenceEntry(String name,
            ImportMigrationJavaPropertyReferencedEntryImpl entry) {
        properties.put(name, entry);
    }
}
