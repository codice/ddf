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

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
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
import javax.crypto.CipherOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.output.ClosedOutputStream;
import org.apache.commons.io.output.ProxyOutputStream;
import org.apache.commons.lang.Validate;
import org.codice.ddf.configuration.migration.util.AccessUtils;
import org.codice.ddf.migration.ExportMigrationContext;
import org.codice.ddf.migration.ExportMigrationEntry;
import org.codice.ddf.migration.Migratable;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The export migration context keeps track of exported migration entries for a given migratable
 * while processing an export migration operation.
 */
public class ExportMigrationContextImpl extends MigrationContextImpl<ExportMigrationReportImpl>
    implements ExportMigrationContext, Closeable {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExportMigrationContextImpl.class);

  /** Holds exported migration entries keyed by the exported path. */
  private final Map<Path, ExportMigrationEntryImpl> entries = new TreeMap<>();

  /** Holds migration entries referenced from system properties keyed by the property name. */
  private final Map<String, ExportMigrationEntry> systemPropertiesEntries = new TreeMap<>();

  private final ZipOutputStream zipOutputStream;

  private final CipherUtils cipherUtils;

  private volatile OutputStream currentOutputStream;

  /**
   * Creates a new migration context for an export operation.
   *
   * @param report the migration report where to record warnings and errors
   * @param migratable the migratable this context is for
   * @param zos the output stream for the zip file being generated
   * @throws IllegalArgumentException if <code>report</code>, <code>migratable</code>, <code>zos
   * </code> is <code>null</code>
   * @throws java.io.IOError if unable to determine ${ddf.home}
   */
  ExportMigrationContextImpl(
      MigrationReport report, Migratable migratable, ZipOutputStream zos, CipherUtils cipherUtils) {
    super(
        new ExportMigrationReportImpl(report, migratable),
        migratable,
        ExportMigrationContextImpl.validateNotNull(migratable, "invalid null migratable")
            .getVersion());
    Validate.notNull(zos, "invalid null zip output stream");
    Validate.notNull(cipherUtils, "invalid null cipher utils");
    this.zipOutputStream = zos;
    this.cipherUtils = cipherUtils;
  }

  private static <T> T validateNotNull(T t, String msg) {
    Validate.notNull(t, msg);
    return t;
  }

  @Override
  public Optional<ExportMigrationEntry> getSystemPropertyReferencedEntry(String name) {
    return AccessUtils.doPrivileged(() -> getSystemPropertyReferencedEntry(name, (r, v) -> true));
  }

  @Override
  public Optional<ExportMigrationEntry> getSystemPropertyReferencedEntry(
      String name, BiPredicate<MigrationReport, String> validator) {
    Validate.notNull(name, "invalid null system property name");
    Validate.notNull(validator, "invalid null validator");
    final ExportMigrationEntry me = systemPropertiesEntries.get(name);

    if (me != null) {
      return Optional.of(me);
    }
    final String val = System.getProperty(name);

    // must first let the validator deal with it before we check for the value
    // do not check for null first!!!
    if (!validator.test(report, val)) {
      return Optional.empty();
    } else if (val == null) {
      report.record(
          new MigrationException(Messages.EXPORT_SYSTEM_PROPERTY_NOT_DEFINED_ERROR, name));
      return Optional.empty();
    } else if (val.isEmpty()) {
      report.record(new MigrationException(Messages.EXPORT_SYSTEM_PROPERTY_IS_EMPTY_ERROR, name));
      return Optional.empty();
    }
    final ExportMigrationSystemPropertyReferencedEntryImpl sprop =
        new ExportMigrationSystemPropertyReferencedEntryImpl(this, name, val);

    systemPropertiesEntries.put(name, sprop);
    return Optional.of(sprop);
  }

  @Override
  public ExportMigrationEntry getEntry(Path path) {
    final ExportMigrationEntryImpl e = new ExportMigrationEntryImpl(this, path);

    return entries.computeIfAbsent(e.getPath(), p -> e);
  }

  @Override
  public Stream<ExportMigrationEntry> entries(Path path, boolean recurse) {
    final ExportMigrationEntryImpl entry = new ExportMigrationEntryImpl(this, path);

    if (!isDirectory(entry)) {
      return Stream.empty();
    }
    return FileUtils.listFiles(
            entry.getFile(), TrueFileFilter.INSTANCE, recurse ? TrueFileFilter.INSTANCE : null)
        .stream()
        .map(File::toPath)
        .map(this::getEntry);
  }

  @Override
  public Stream<ExportMigrationEntry> entries(Path path, boolean recurse, PathMatcher filter) {
    Validate.notNull(filter, "invalid null path filter");
    final ExportMigrationEntryImpl entry = new ExportMigrationEntryImpl(this, path);

    if (!isDirectory(entry)) {
      return Stream.empty();
    }
    return FileUtils.listFiles(
            entry.getFile(), TrueFileFilter.INSTANCE, recurse ? TrueFileFilter.INSTANCE : null)
        .stream()
        .map(File::toPath)
        .map(p -> new ExportMigrationEntryImpl(this, p))
        .filter(e -> filter.matches(e.getPath()))
        .map(e -> entries.computeIfAbsent(e.getPath(), p -> e));
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
   * @throws org.codice.ddf.migration.MigrationException to stop the export operation
   */
  @SuppressWarnings(
      "PMD.DefaultPackage" /* designed to be called from ExportMigrationManagerImpl within this package */)
  Map<String, Map<String, Object>> doExport() {
    LOGGER.debug(
        "Exporting [{}] with version [{}]...",
        id,
        getMigratableVersion()); // version will never be empty
    Stopwatch stopwatch = null;

    if (LOGGER.isDebugEnabled()) {
      stopwatch = Stopwatch.createStarted();
    }
    migratable.doExport(this);
    if (LOGGER.isDebugEnabled() && (stopwatch != null)) {
      LOGGER.debug("Exported time for {}: {}", id, stopwatch.stop());
    }
    final Map<String, Map<String, Object>> metadata = ImmutableMap.of(id, report.getMetadata());

    LOGGER.debug("Exported metadata for {}: {}", id, metadata);
    return metadata;
  }

  @SuppressWarnings(
      "PMD.DefaultPackage" /* designed to be called from ExportMigrationEntryImpl within this package */)
  OutputStream getOutputStreamFor(ExportMigrationEntryImpl entry) {
    try {
      close();
      // zip entries are always Unix style based on our convention
      final ZipEntry ze = new ZipEntry(id + '/' + entry.getName());

      ze.setTime(entry.getLastModifiedTime()); // save the current modified time
      zipOutputStream.putNextEntry(ze);
      final OutputStream oos =
          new ProxyOutputStream(zipOutputStream) {
            @Override
            public void close() throws IOException {
              if (!(super.out instanceof ClosedOutputStream)) {
                super.out = ClosedOutputStream.CLOSED_OUTPUT_STREAM;
                zipOutputStream.closeEntry();
              }
            }

            @Override
            protected void handleIOException(IOException e) throws IOException {
              super.handleIOException(new ExportIOException(e));
            }
          };

      CipherOutputStream cos = cipherUtils.getCipherOutputStream(oos);
      this.currentOutputStream = cos;
      return cos;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private boolean isDirectory(ExportMigrationEntryImpl entry) {
    final File file = entry.getFile();

    if (!file.exists()) {
      report.record(
          new MigrationException(Messages.EXPORT_PATH_DOES_NOT_EXIST_ERROR, entry.getPath()));
      return false;
    } else if (!file.isDirectory()) {
      report.record(
          new MigrationException(Messages.EXPORT_PATH_NOT_A_DIRECTORY_ERROR, entry.getPath()));
      return false;
    }
    return true;
  }

  /**
   * The superclass implementation is sufficient for our needs.
   *
   * @param o the object to check
   * @return true if equal
   */
  @Override
  public boolean equals(Object o) {
    return super.equals(o);
  }

  /**
   * The superclass implementation is sufficient for our needs.
   *
   * @return the hashcode
   */
  @Override
  public int hashCode() {
    return super.hashCode();
  }
}

/**
 * Special wrapper I/O exception used to internally determine if an I/O error occurred from the
 * export output stream processing versus from reading processed entries during export. This allows
 * us to determine if we can safely continue the export in order to gather as many errors as
 * possible or if we are forced to stop the export.
 *
 * <p>Any attempts to continue exporting to a zip file when an I/O exception occurs while writing to
 * it will simply result in another exception being generated thus loosing its value. In such case,
 * we shall simply stop processing the export operation.
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
