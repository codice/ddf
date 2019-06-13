/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.configuration.migration;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.nio.file.PathMatcher;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.lang.Validate;
import org.codice.ddf.configuration.migration.util.AccessUtils;
import org.codice.ddf.migration.Migratable;
import org.codice.ddf.migration.MigrationMessage;
import org.codice.ddf.migration.MigrationOperation;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.migration.MigrationWarning;
import org.codice.ddf.util.function.ThrowingRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The export migration report provides additional functionality for tracking metadata required
 * during export.
 */
public class ExportMigrationReportImpl implements MigrationReport {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExportMigrationReportImpl.class);

  private final MigrationReport report;

  private final List<Map<String, Object>> files = new ArrayList<>();

  private final List<Map<String, Object>> folders = new ArrayList<>();

  private final List<Map<String, Object>> externals = new ArrayList<>(8);

  private final List<Map<String, Object>> systemProperties = new ArrayList<>(8);

  private final List<Map<String, Object>> javaProperties = new ArrayList<>(8);

  private final Map<String, Object> metadata;

  @VisibleForTesting
  ExportMigrationReportImpl() {
    this.report = new MigrationReportImpl(MigrationOperation.EXPORT, Optional.empty());
    this.metadata = Collections.emptyMap();
  }

  public ExportMigrationReportImpl(MigrationReport report, Migratable migratable) {
    Validate.notNull(report, "invalid null report");
    Validate.notNull(migratable, "invalid null migratable");
    this.report = report;
    this.metadata =
        ImmutableMap.of( //
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
  public Instant getStartTime() {
    return report.getStartTime();
  }

  @Override
  public Optional<Instant> getEndTime() {
    return report.getEndTime();
  }

  @Override
  public ExportMigrationReportImpl record(String msg) {
    report.record(msg);
    return this;
  }

  @Override
  public ExportMigrationReportImpl record(String format, @Nullable Object... args) {
    report.record(format, args);
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
  public boolean wasIOSuccessful(ThrowingRunnable<IOException> code) throws IOException {
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
  public void verifyCompletion() {
    report.verifyCompletion();
  }

  public MigrationReport getReport() {
    return report;
  }

  @SuppressWarnings(
      "PMD.DefaultPackage" /* designed to be called from ExportMigrationEntryImpl within this package */)
  ExportMigrationReportImpl recordFile(ExportMigrationEntryImpl entry) {
    files.add(ImmutableMap.of(MigrationEntryImpl.METADATA_NAME, entry.getName()));
    return this;
  }

  @SuppressWarnings(
      "PMD.DefaultPackage" /* designed to be called from ExportMigrationEntryImpl within this package */)
  ExportMigrationReportImpl recordDirectory(
      ExportMigrationEntryImpl entry, @Nullable PathMatcher filter, Set<String> files) {
    folders.add(
        ImmutableMap.of( //
            MigrationEntryImpl.METADATA_NAME,
            entry.getName(),
            MigrationEntryImpl.METADATA_FILTERED,
            (filter != null),
            MigrationEntryImpl.METADATA_FILES,
            files,
            MigrationEntryImpl.METADATA_LAST_MODIFIED,
            entry.getLastModifiedTime()));
    return this;
  }

  @SuppressWarnings(
      "PMD.DefaultPackage" /* designed to be called from ExportMigrationEntryImpl within this package */)
  ExportMigrationReportImpl recordExternal(ExportMigrationEntryImpl entry, boolean softlink) {
    AccessUtils.doPrivileged(
        () -> {
          final Map<String, Object> emetadata = new HashMap<>(8);

          emetadata.put(MigrationEntryImpl.METADATA_NAME, entry.getName());
          emetadata.put(MigrationEntryImpl.METADATA_FOLDER, entry.isDirectory());
          if (entry.isFile()) {
            try {
              emetadata.put(
                  MigrationEntryImpl.METADATA_CHECKSUM,
                  entry.getContext().getPathUtils().getChecksumFor(entry.getAbsolutePath()));
            } catch (SecurityException | IOException e) {
              LOGGER.info("failed to compute MD5 checksum for '" + entry.getName() + "': ", e);
              report.record(
                  new MigrationWarning(
                      Messages.EXPORT_CHECKSUM_COMPUTE_WARNING, entry.getPath(), e));
            }
          } // else - cannot compute a checksum for directories
          emetadata.put(MigrationEntryImpl.METADATA_SOFTLINK, softlink);
          externals.add(emetadata);
        });
    return this;
  }

  @SuppressWarnings(
      "PMD.DefaultPackage" /* designed to be called from ExportMigrationSystemPropertyReferencedEntryImpl within this package */)
  ExportMigrationReportImpl recordSystemProperty(
      ExportMigrationSystemPropertyReferencedEntryImpl entry) {
    AccessUtils.doPrivileged(
        () ->
            systemProperties.add(
                ImmutableMap.of( //
                    MigrationEntryImpl.METADATA_PROPERTY,
                    entry.getProperty(),
                    MigrationEntryImpl.METADATA_REFERENCE,
                    entry.getPath().toString())));
    return this;
  }

  @SuppressWarnings(
      "PMD.DefaultPackage" /* designed to be called from ExportMigrationJavaPropertyReferencedEntryImpl within this package */)
  ExportMigrationReportImpl recordJavaProperty(
      ExportMigrationJavaPropertyReferencedEntryImpl entry) {
    AccessUtils.doPrivileged(
        () ->
            javaProperties.add(
                ImmutableMap.of( //
                    MigrationEntryImpl.METADATA_PROPERTY,
                    entry.getProperty(),
                    MigrationEntryImpl.METADATA_REFERENCE,
                    entry.getPath().toString(),
                    MigrationEntryImpl.METADATA_NAME,
                    entry.getPropertiesPath().toString())));
    return this;
  }

  /**
   * Retrieves the recorded metadata so far.
   *
   * @return metadata recorded with this report
   */
  @SuppressWarnings(
      "PMD.DefaultPackage" /* designed to be called from ExportMigrationContextImpl within this package */)
  Map<String, Object> getMetadata() {
    final Map<String, Object> mmetadata = new LinkedHashMap<>(16);

    mmetadata.putAll(this.metadata);
    if (!files.isEmpty()) {
      mmetadata.put(MigrationContextImpl.METADATA_FILES, files);
    }
    if (!folders.isEmpty()) {
      mmetadata.put(MigrationContextImpl.METADATA_FOLDERS, folders);
    }
    if (!externals.isEmpty()) {
      mmetadata.put(MigrationContextImpl.METADATA_EXTERNALS, externals);
    }
    if (!systemProperties.isEmpty()) {
      mmetadata.put(MigrationContextImpl.METADATA_SYSTEM_PROPERTIES, systemProperties);
    }
    if (!javaProperties.isEmpty()) {
      mmetadata.put(MigrationContextImpl.METADATA_JAVA_PROPERTIES, javaProperties);
    }
    return mmetadata;
  }
}
