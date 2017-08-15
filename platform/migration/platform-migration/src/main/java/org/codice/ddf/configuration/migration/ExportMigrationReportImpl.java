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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.commons.lang.Validate;
import org.codice.ddf.migration.Migratable;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationMessage;
import org.codice.ddf.migration.MigrationOperation;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.util.function.ERunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

/**
 * The export migration report provides additional functionality for tracking metadata required during
 * export.
 */
public class ExportMigrationReportImpl implements MigrationReport {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExportMigrationReportImpl.class);

    private final MigrationReport report;

    private final List<Map<String, Object>> externals = new ArrayList<>(8);

    private final List<Map<String, Object>> systemProperties = new ArrayList<>(8);

    private final List<Map<String, Object>> javaProperties = new ArrayList<>(8);

    private final Map<String, Object> metadata;

    // use for unit testing
    ExportMigrationReportImpl() {
        this.report = new MigrationReportImpl(MigrationOperation.EXPORT, Optional.empty());
        this.metadata = Collections.emptyMap();
    }

    public ExportMigrationReportImpl(MigrationReport report, Migratable migratable) {
        Validate.notNull(report, "invalid null report");
        Validate.notNull(migratable, "invalid null migratable");
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
    public long getStartTime() {
        return report.getStartTime();
    }

    @Override
    public long getEndTime() {
        return report.getEndTime();
    }

    @Override
    public ExportMigrationReportImpl record(String msg) {
        report.record(msg);
        return this;
    }

    @Override
    public ExportMigrationReportImpl record(MigrationMessage msg) {
        report.record(msg);
        return this;
    }

    @Override
    public ExportMigrationReportImpl doAfterCompletion(Consumer<MigrationReport> code) {
        report.doAfterCompletion(code);
        return this;
    }

    @Override
    public Stream<MigrationMessage> messages() {
        return report.messages();
    }

    @Override
    public boolean wasSuccessful() {
        return report.wasSuccessful();
    }

    @Override
    public boolean wasSuccessful(Runnable code) {
        return report.wasSuccessful(code);
    }

    @Override
    public boolean wasIOSuccessful(ERunnable<IOException> code) throws IOException {
        return report.wasIOSuccessful(code);
    }

    @Override
    public boolean hasInfos() {
        return report.hasInfos();
    }

    @Override
    public boolean hasWarnings() {
        return report.hasWarnings();
    }

    @Override
    public boolean hasErrors() {
        return report.hasErrors();
    }

    @Override
    public void verifyCompletion() throws MigrationException {
        report.verifyCompletion();
    }

    public MigrationReport getReport() {
        return report;
    }

    ExportMigrationReportImpl recordExternal(ExportMigrationEntryImpl entry, boolean softlink) {
        final Map<String, Object> metadata = new HashMap<>(8);

        metadata.put(MigrationEntryImpl.METADATA_NAME, entry.getName());
        try {
            metadata.put(MigrationEntryImpl.METADATA_CHECKSUM,
                    entry.getContext()
                            .getPathUtils()
                            .getChecksumFor(entry.getAbsolutePath()));
        } catch (IOException e) {
            LOGGER.info("failed to compute MD5 checksum for '" + entry.getName() + "': ", e);
        }
        metadata.put(MigrationEntryImpl.METADATA_SOFTLINK, softlink);
        externals.add(metadata);
        return this;
    }

    ExportMigrationReportImpl recordSystemProperty(
            ExportMigrationSystemPropertyReferencedEntryImpl entry) {
        systemProperties.add(ImmutableMap.of( //
                MigrationEntryImpl.METADATA_PROPERTY,
                entry.getProperty(),
                MigrationEntryImpl.METADATA_REFERENCE,
                entry.getPath()
                        .toString()));
        return this;
    }

    ExportMigrationReportImpl recordJavaProperty(
            ExportMigrationJavaPropertyReferencedEntryImpl entry) {
        javaProperties.add(ImmutableMap.of( //
                MigrationEntryImpl.METADATA_PROPERTY,
                entry.getProperty(),
                MigrationEntryImpl.METADATA_REFERENCE,
                entry.getPath()
                        .toString(),
                MigrationEntryImpl.METADATA_NAME,
                entry.getPropertiesPath()
                        .toString()));
        return this;
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
}
