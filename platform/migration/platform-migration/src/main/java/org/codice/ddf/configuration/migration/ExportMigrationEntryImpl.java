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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.codice.ddf.migration.ExportMigrationEntry;
import org.codice.ddf.migration.ExportMigrationException;
import org.codice.ddf.migration.ExportPathMigrationException;
import org.codice.ddf.migration.ExportPathMigrationWarning;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationExporter;
import org.codice.ddf.migration.MigrationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides an implementation of the {@link ExportMigrationEntry}.
 */
public class ExportMigrationEntryImpl extends MigrationEntryImpl<ExportMigrationContextImpl>
        implements ExportMigrationEntry {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExportMigrationEntryImpl.class);

    private final Map<String, ExportMigrationJavaPropertyReferencedEntryImpl> properties =
            new HashMap<>(8);

    private final File file;

    private final AtomicReference<OutputStream> outputStream = new AtomicReference<>();

    protected ExportMigrationEntryImpl(ExportMigrationContextImpl context, Path path) {
        super(context, path);
        this.file = getAbsolutePath().toFile();
    }

    @Override
    public ExportMigrationReportImpl getReport() {
        return context.getReport();
    }

    @Override
    public long getLastModifiedTime() {
        return file.lastModified();
    }

    @Override
    public long getSize() {
        return file.length();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        try {
            return outputStream.updateAndGet(os -> (os != null) ?
                    os :
                    context.getOutputStreamFor(Paths.get(getId())
                            .resolve(path)));
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    @Override
    public void store() {
        store((r, os) -> {
            LOGGER.debug("Exporting file [{}] to [{}]...", getAbsolutePath(), path);
            if (isMigratable()) {
                FileUtils.copyFile(getAbsolutePath().toFile(), os);
            }
        });
    }

    @Override
    public void store(MigrationExporter exporter) {
        Validate.notNull(exporter, "invalid null exporter");
        if (!stored) {
            super.stored = true;
            try {
                exporter.apply(getReport(), getOutputStream());
            } catch (ExportIOException e) { // special case indicating the I/O error occurred while writing to the zip which would invalidate the zip so we are forced to abort
                throw newError("failed to copy", e.getCause());
            } catch (IOException e) { // here it means the error came out of reading/processing the input file/stream where it is safe to continue with the next entry, so don't abort
                recordError("failed to copy", e);
            } catch (MigrationException e) {
                throw e;
            }
        }
    }

    @Override
    public Optional<ExportMigrationEntry> getPropertyReferencedEntry(String name,
            BiPredicate<MigrationReport, String> validator) {
        Validate.notNull(name, "invalid null system property name");
        final ExportMigrationJavaPropertyReferencedEntryImpl me = properties.get(name);

        if (me != null) {
            return Optional.of(me);
        }
        try {
            final String val = getJavaPropertyValue(name);

            if (!validator.test(getReport(), val)) {
                return Optional.empty();
            } else if (val == null) {
                getReport().record(new ExportMigrationException(String.format(
                        "Java property [%s] from [%s] is not defined",
                        name,
                        path)));
                return Optional.empty();
            } else if (val.isEmpty()) {
                getReport().record(new ExportMigrationException(String.format(
                        "Java property [%s] from [%s] is empty",
                        name,
                        path)));
                return Optional.empty();
            }
            final ExportMigrationJavaPropertyReferencedEntryImpl prop =
                    new ExportMigrationJavaPropertyReferencedEntryImpl(context, path, name, val);

            properties.put(name, prop);
            return Optional.of(prop);
        } catch (IOException e) {
            getReport().record(new ExportMigrationException(String.format(
                    "unable to retrieve Java property [%s] from [%s]; failed to load property file",
                    name,
                    path), e));
            return Optional.empty();
        }
    }

    protected void recordWarning(String reason) {
        getReport().record(new ExportPathMigrationWarning(path, reason));
    }

    protected void recordError(String reason, Throwable cause) {
        getReport().record(newError(reason, cause));
    }

    protected ExportPathMigrationException newError(String reason, Throwable cause) {
        return new ExportPathMigrationException(path, reason, cause);
    }

    private boolean isMigratable() {
        final ExportMigrationReportImpl report = getReport();
        final Path apath = getAbsolutePath();

        if (path.isAbsolute()) {
            report.recordExternal(this, false);
            recordWarning("is absolute");
            return false;
        } else if (Files.isSymbolicLink(apath)) {
            report.recordExternal(this, true);
            recordWarning("contains a symbolic link");
            return false;
        } else {
            try {
                if (!apath.toRealPath()
                        .startsWith(MigrationContextImpl.DDF_HOME.toRealPath())) {
                    report.recordExternal(this, false);
                    recordWarning(String.format("is outside [%s]", MigrationContextImpl.DDF_HOME));
                    return false;
                }
            } catch (IOException e) {
                // test for existence after testing if it is under DDF_HOME as we want to report
                // that before
                if (!apath.toFile()
                        .exists()) {
                    report.recordExternal(this, false);
                    recordWarning("does not exist");
                } else {
                    recordError("cannot be read", e);
                }
                return false;
            }
        }
        return true;
    }

    private String getJavaPropertyValue(String name) throws IOException {
        final Properties props = new Properties();
        InputStream is = null;

        try {
            is = new BufferedInputStream(new FileInputStream(MigrationContextImpl.resolve(
                    path)
                    .toFile()));
            props.load(is);
        } finally {
            IOUtils.closeQuietly(is);
        }
        return props.getProperty(name);
    }
}
