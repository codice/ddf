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
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.BiPredicate;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.output.ClosedOutputStream;
import org.apache.commons.io.output.ProxyOutputStream;
import org.apache.commons.lang.Validate;
import org.codice.ddf.migration.ExportMigrationContext;
import org.codice.ddf.migration.ExportMigrationEntry;
import org.codice.ddf.migration.ExportMigrationException;
import org.codice.ddf.migration.ExportPathMigrationException;
import org.codice.ddf.migration.Migratable;
import org.codice.ddf.migration.MigrationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;

/**
 * The export migration context keeps track of exported migration entries for a given migratable
 * while processing an export migration operation.
 */
public class ExportMigrationContextImpl extends MigrationContextImpl
        implements ExportMigrationContext, Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExportMigrationContextImpl.class);

    /**
     * Holds exported migration entries keyed by the exported path.
     */
    private final Map<Path, ExportMigrationEntryImpl> entries = new TreeMap<>();

    /**
     * Holds migration entries referenced from system properties keyed by the property name.
     */
    private final Map<String, ExportMigrationSystemPropertyReferencedEntryImpl> systemProperties =
            new TreeMap<>();

    private final ExportMigrationReportImpl report;

    private final ZipOutputStream zos;

    private volatile OutputStream currentOutputStream;

    /**
     * Creates a new migration context for an export operation.
     *
     * @param report     the migration report where to record warnings and errors
     * @param migratable the migratable this context is for
     * @param zos        the output stream for the zip file being generated
     * @throws IllegalArgumentException if <code>report</code>, <code>migratable</code>, <code>zos</code>
     *                                  is <code>null</code>
     */
    public ExportMigrationContextImpl(MigrationReport report, Migratable migratable,
            ZipOutputStream zos) {
        super(report, migratable);
        this.report = new ExportMigrationReportImpl(report);
        this.zos = zos;
    }

    @Override
    public ExportMigrationReportImpl getReport() {
        return report;
    }

    @Override
    public Optional<ExportMigrationEntry> getSystemPropertyReferencedEntry(String name,
            BiPredicate<MigrationReport, String> validator) {
        Validate.notNull(name, "invalid null system property name");
        final ExportMigrationSystemPropertyReferencedEntryImpl me = systemProperties.get(name);

        if (me != null) {
            return Optional.of(me);
        }
        final String val = System.getProperty(name);

        if (!validator.test(report, val)) {
            return Optional.empty();
        } else if (val == null) {
            report.record(new ExportMigrationException(String.format(
                    "System property [%s] is not defined",
                    name)));
            return Optional.empty();
        } else if (val == null) {
            report.record(new ExportMigrationException(String.format("System property [%s] is empty",
                    name)));
            return Optional.empty();
        }
        return Optional.of(new ExportMigrationSystemPropertyReferencedEntryImpl(this, name, val));
    }

    @Override
    public ExportMigrationEntry getEntry(Path path) {
        Validate.notNull(path, "invalid null path");
        return entries.computeIfAbsent(path, p -> new ExportMigrationEntryImpl(this, p));
    }

    @Override
    public Stream<ExportMigrationEntry> entries(Path path) {
        Validate.notNull(path, "invalid null path");
        final File file = path.toFile();

        if (!file.exists()) {
            report.record(new ExportPathMigrationException(path, "does not exist"));
            return Stream.empty();
        } else if (!file.isDirectory()) {
            report.record(new ExportPathMigrationException(path, "is not a directory"));
            return Stream.empty();
        }
        return FileUtils.listFiles(file, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE)
                .stream()
                .map(File::toPath)
                .map(this::getEntry);
    }

    @Override
    public Stream<ExportMigrationEntry> entries(Path path, PathMatcher filter) {
        Validate.notNull(path, "invalid null path");
        Validate.notNull(filter, "invalid null filter");
        final File file = path.toFile();

        if (!file.exists()) {
            report.record(new ExportPathMigrationException(path, "does not exist"));
            return Stream.empty();
        } else if (!file.isDirectory()) {
            report.record(new ExportPathMigrationException(path, "is not a directory"));
            return Stream.empty();
        }
        return FileUtils.listFiles(file, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE)
                .stream()
                .map(File::toPath)
                .filter(filter::matches)
                .map(this::getEntry);
    }

    @Override
    public void close() throws IOException {
        final OutputStream oos = this.currentOutputStream;

        if (oos != null) {
            this.currentOutputStream = null;
            oos.close();
        }
    }

    /**
     * Performs an export using the context's migratable.
     *
     * @return metadata to export for the corresponding migratable keyed by the migratable's id
     */
    Map<String, Object> doExport() {
        Stopwatch stopwatch = null;

        if (LOGGER.isDebugEnabled()) {
            stopwatch = Stopwatch.createStarted();
        }
        migratable.doExport(this);
        if (LOGGER.isDebugEnabled() && (stopwatch != null)) {
            LOGGER.debug("Export time for {}: {}", id, stopwatch.stop());
        }
        final Map<String, Object> metadata = ImmutableMap.of(id, report.getMetadata());

        LOGGER.debug("Exported metadata for {}: {}", id, metadata);
        return metadata;
    }

    OutputStream getOutputStreamFor(Path path) {
        try {
            close();
            zos.putNextEntry(new ZipEntry(path.toString()));
            return new ProxyOutputStream(null) {
                @Override
                public void close() throws IOException {
                    if (!(super.out instanceof ClosedOutputStream)) {
                        super.out = ClosedOutputStream.CLOSED_OUTPUT_STREAM;
                        zos.closeEntry();
                    }
                }
                @Override
                protected void handleIOException(IOException e) throws IOException {
                    throw new ExportIOException(e);
                }
            };
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

/**
 * Special wrapper I/O exception used to internally determine if an I/O error occurred from the
 * export output stream processing versus from reading processed entries during export. This allows
 * us to determine if we can safely continue the export in order to gather as many errors as possible
 * or if we are forced to stop the export.
 * <p>
 * Any attempts to continue exporting to a zip file when an I/O exception occurs while writing to it
 * will simply result in another exception being generated thus loosing its value. In such case, we
 * shall simply stop processing the export operation.
 */
class ExportIOException extends IOException {
    private final IOException cause;

    ExportIOException(IOException e) {
        super(e);
        this.cause = e;
    }

    public IOException getIOException() {
        return cause;
    }
}