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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.codice.ddf.migration.Migratable;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationOperation;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.migration.MigrationWarning;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

/**
 * The export migration report provides additional functionnality for tracking metadata required during
 * export.
 */
public class ExportMigrationReportImpl implements MigrationReport {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExportMigrationReportImpl.class);

    private final MigrationReport report;

    private final List<Map<String, Object>> externals = new ArrayList<>(8);

    private final List<Map<String, Object>> systemProperties = new ArrayList<>(8);

    private final List<Map<String, Object>> javaProperties = new ArrayList<>(8);

    private final Map<String, Object> metadata;

    public ExportMigrationReportImpl(MigrationReport report, Migratable migratable) {
        Validate.notNull(report, "invalid null report");
        Validate.notNull(report, "invalid null migratable");
        this.report = report;
        this.metadata = ImmutableMap.of( //
                MigrationContextImpl.METADATA_VERSION,
                migratable.getVersion(),
                MigrationContextImpl.METADATA_TITLE,
                migratable.getTitle(),
                MigrationContextImpl.METADATA_DESCRIPTION,
                migratable.getDescription(),
                MigrationContextImpl.METADATA_ORGANIZATION,
                migratable.getOrganization());
    }

    @Override
    public MigrationOperation getOperation() {
        return report.getOperation();
    }

    @Override
    public ExportMigrationReportImpl record(MigrationWarning w) {
        report.record(w);
        return this;
    }

    @Override
    public ExportMigrationReportImpl record(MigrationException e) {
        report.record(e);
        return this;
    }

    @Override
    public ExportMigrationReportImpl doAfterCompletion(Consumer<MigrationReport> code) {
        report.doAfterCompletion(code);
        return this;
    }

    @Override
    public Stream<MigrationException> errors() {
        return report.errors();
    }

    @Override
    public Stream<MigrationWarning> warnings() {
        return report.warnings();
    }

    @Override
    public Collection<MigrationWarning> getWarnings() {
        return report.getWarnings();
    }

    @Override
    public boolean wasSuccessful() {
        return report.wasSuccessful();
    }

    public boolean hasWarnings() {
        return report.hasWarnings();
    }

    public boolean hasErrors() {
        return report.hasErrors();
    }

    @Override
    public void verifyCompletion() throws MigrationException {
        report.verifyCompletion();
    }

    /**
     * Retrieves the recorded metadata so far.
     *
     * @return metadata recorded with this report
     */
    Map<String, Object> getMetadata() {
        final Map<String, Object> metadata = new LinkedHashMap<>(16);

        metadata.putAll(this.metadata);
        if (!externals.isEmpty()) {
            metadata.put(MigrationContextImpl.METADATA_EXTERNALS, externals);
        }
        if (!systemProperties.isEmpty()) {
            metadata.put(MigrationContextImpl.METADATA_SYSTEM_PROPERTIES, systemProperties);
        }
        if (!javaProperties.isEmpty()) {
            metadata.put(MigrationContextImpl.METADATA_JAVA_PROPERTIES, javaProperties);
        }
        return metadata;
    }

    ExportMigrationReportImpl recordExternal(ExportMigrationEntryImpl entry, boolean softlink) {
        Validate.notNull(entry, "invalid null migration entry");
        final Map<String, Object> metadata = new HashMap<>(8);
        final File file = entry.getAbsolutePath()
                .toFile();

        metadata.put(MigrationEntryImpl.METADATA_NAME, entry.getName());
        if (!file.exists()) {
            metadata.put(MigrationEntryImpl.METADATA_SIZE, 0L);
        } else {
            InputStream is = null;

            try {
                is = new FileInputStream(file);
                metadata.put(MigrationEntryImpl.METADATA_CHECKSUM, DigestUtils.md5Hex(is));
            } catch (IOException e) {
                LOGGER.info("failed to compute MD5 checksum for '" + entry.getName() + "': ", e);
            } finally {
                IOUtils.closeQuietly(is); // don't care about errors when closing
            }
            metadata.put(MigrationEntryImpl.METADATA_SOFTLINK, softlink);
            final long size = file.length();

            metadata.put(MigrationEntryImpl.METADATA_SIZE, (size != 0L) ? size : -1L);
        }
        externals.add(metadata);
        return this;
    }

    ExportMigrationReportImpl recordSystemProperty(
            ExportMigrationSystemPropertyReferencedEntryImpl entry) {
        systemProperties.add(ImmutableMap.of(MigrationEntryImpl.METADATA_PROPERTY,
                entry.getProperty(),
                MigrationEntryImpl.METADATA_REFERENCE,
                entry.getPath()
                        .toString()));
        return this;
    }

    ExportMigrationReportImpl recordJavaProperty(
            ExportMigrationJavaPropertyReferencedEntryImpl entry) {
        javaProperties.add(ImmutableMap.of(MigrationEntryImpl.METADATA_PROPERTY,
                entry.getProperty(),
                MigrationEntryImpl.METADATA_REFERENCE,
                entry.getPath()
                        .toString(),
                MigrationEntryImpl.METADATA_NAME,
                entry.getPropertiesPath()
                        .toString()));
        return this;
    }
}
