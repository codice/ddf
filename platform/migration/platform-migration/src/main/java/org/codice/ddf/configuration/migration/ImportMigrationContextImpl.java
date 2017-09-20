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
import com.google.common.base.Stopwatch;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.codice.ddf.migration.ImportMigrationContext;
import org.codice.ddf.migration.ImportMigrationEntry;
import org.codice.ddf.migration.Migratable;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.migration.MigrationWarning;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The import migration context keeps track of exported migration entries for a given migratable
 * while processing an import migration operation.
 */
// squid:S2160 - the base class equals() is sufficient for our needs.
@SuppressWarnings("squid:S2160")
public class ImportMigrationContextImpl extends MigrationContextImpl<MigrationReport>
    implements ImportMigrationContext {
  private static final Logger LOGGER = LoggerFactory.getLogger(ImportMigrationContextImpl.class);

  private static final String INVALID_NULL_ZIP = "invalid null zip";

  private static final String INVALID_NULL_PATH = "invalid null path";

  /** Holds exported migration entries keyed by the exported path. */
  private final Map<Path, ImportMigrationEntryImpl> entries = new TreeMap<>();

  /** Holds migration entries referenced from system properties keyed by the property name. */
  private final Map<String, ImportMigrationSystemPropertyReferencedEntryImpl> systemProperties =
      new TreeMap<>();

  private final ZipFile zip;

  private final List<InputStream> inputStreams = new ArrayList<>();

  /**
   * Creates a new migration context for an import operation representing a system context.
   *
   * @param report the migration report where warnings and errors can be recorded
   * @param zip the zip file associated with the import
   * @throws IllegalArgumentException if <code>report</code> or <code>zip</code> is <code>null
   *     </code>
   * @throws java.io.IOError if unable to determine ${ddf.home}
   */
  public ImportMigrationContextImpl(MigrationReport report, ZipFile zip) {
    super(report);
    Validate.notNull(zip, ImportMigrationContextImpl.INVALID_NULL_ZIP);
    this.zip = zip;
  }

  /**
   * Creates a new migration context for an import operation.
   *
   * @param report the migration report where warnings and errors can be recorded
   * @param zip the zip file associated with the import
   * @param id the migratable id
   * @throws IllegalArgumentException if <code>report</code>, <code>zip</code>, or <code>id</code>
   *     is <code>null</code>
   * @throws java.io.IOError if unable to determine ${ddf.home}
   */
  public ImportMigrationContextImpl(MigrationReport report, ZipFile zip, String id) {
    super(report, id);
    Validate.notNull(zip, ImportMigrationContextImpl.INVALID_NULL_ZIP);
    this.zip = zip;
  }

  /**
   * Creates a new migration context for an import operation.
   *
   * @param report the migration report where warnings and errors can be recorded
   * @param zip the zip file associated with the import
   * @param migratable the migratable this context is for
   * @throws IllegalArgumentException if <code>report</code>, <code>zip</code> or <code>migratable
   *     </code> is <code>null</code>
   * @throws java.io.IOError if unable to determine ${ddf.home}
   */
  public ImportMigrationContextImpl(MigrationReport report, ZipFile zip, Migratable migratable) {
    super(report, migratable);
    Validate.notNull(zip, ImportMigrationContextImpl.INVALID_NULL_ZIP);
    this.zip = zip;
  }

  @Override
  public Optional<ImportMigrationEntry> getSystemPropertyReferencedEntry(String name) {
    Validate.notNull(name, "invalid null system property name");
    return Optional.ofNullable(systemProperties.get(name));
  }

  @Override
  public ImportMigrationEntry getEntry(Path path) {
    Validate.notNull(path, ImportMigrationContextImpl.INVALID_NULL_PATH);
    return entries.computeIfAbsent(path, p -> new ImportMigrationEmptyEntryImpl(this, p));
  }

  @Override
  public Stream<ImportMigrationEntry> entries() {
    return entries.values().stream().map(ImportMigrationEntry.class::cast);
  }

  @Override
  public Stream<ImportMigrationEntry> entries(Path path) {
    Validate.notNull(path, ImportMigrationContextImpl.INVALID_NULL_PATH);
    return entries().filter(me -> me.getPath().startsWith(path));
  }

  @Override
  public Stream<ImportMigrationEntry> entries(Path path, PathMatcher filter) {
    Validate.notNull(filter, "invalid null filter");

    return entries(path).filter(e -> filter.matches(e.getPath()));
  }

  @Override
  public boolean cleanDirectory(Path path) {
    Validate.notNull(path, ImportMigrationContextImpl.INVALID_NULL_PATH);
    final Path rpath = getPathUtils().resolveAgainstDDFHome(path);
    final File fdir = rpath.toFile();

    LOGGER.debug("Cleaning up directory [{}]...", fdir);
    try {
      if (!getPathUtils().isRelativeToDDFHome(rpath.toRealPath(LinkOption.NOFOLLOW_LINKS))) {
        LOGGER.info("Failed to clean directory [{}]", fdir);
        getReport()
            .record(
                new MigrationWarning(
                    Messages.IMPORT_PATH_CLEAN_WARNING,
                    path,
                    String.format("not relative to [%s]", getPathUtils().getDDFHome())));
        return false;
      }
    } catch (NoSuchFileException e) {
      return true;
    } catch (IOException e) {
      LOGGER.info("Failed to clean directory [" + fdir + "]: ", e);
      getReport().record(new MigrationWarning(Messages.IMPORT_PATH_CLEAN_WARNING, path, e));
      return false;
    }
    if (!fdir.exists()) {
      return true;
    }
    if (!fdir.isDirectory()) {
      LOGGER.info("Failed to clean directory [{}]", fdir);
      getReport()
          .record(
              new MigrationWarning(Messages.IMPORT_PATH_CLEAN_WARNING, path, "not a directory"));
      return false;
    }
    try {
      FileUtils.cleanDirectory(fdir);
    } catch (IOException e) {
      LOGGER.info("Failed to clean directory [" + fdir + "]: ", e);
      getReport().record(new MigrationWarning(Messages.IMPORT_PATH_CLEAN_WARNING, path, e));
      return false;
    }
    return true;
  }

  // PMD.DefaultPackage - designed to be called from ImportMigrationPropertyReferencedEntryImpl
  // within this package
  @SuppressWarnings("PMD.DefaultPackage")
  @VisibleForTesting
  Optional<ImportMigrationEntry> getOptionalEntry(Path path) {
    return Optional.ofNullable(entries.get(path));
  }

  /**
   * Performs an import using the context's migratable.
   *
   * @throws org.codice.ddf.migration.MigrationException to stop the import operation
   */
  // PMD.DefaultPackage - designed to be called from ImportMigrationManagerImpl within this package
  @SuppressWarnings("PMD.DefaultPackage")
  void doImport() {
    if (migratable != null) {
      LOGGER.debug("Importing migratable [{}] from version [{}]...", id, getVersion());
      Stopwatch stopwatch = null;

      if (LOGGER.isDebugEnabled()) {
        stopwatch = Stopwatch.createStarted();
      }
      try {
        final String version = getVersion().orElse(null);

        if (version == null) {
          migratable.doMissingImport(this);
        } else if (version.equals(migratable.getVersion())) {
          migratable.doImport(this);
        } else {
          migratable.doIncompatibleImport(this, version);
        }
      } finally {
        inputStreams.forEach(IOUtils::closeQuietly); // we do not care if we failed to close them
      }
      if (LOGGER.isDebugEnabled() && (stopwatch != null)) {
        LOGGER.debug("Imported time for {}: {}", id, stopwatch.stop());
      }
    } else if (id != null) { // not a system context
      LOGGER.warn(
          "unable to import migration data for migratable [{}]; migratable is no longer available",
          id);
      report.record(new MigrationException(Messages.IMPORT_UNKNOWN_DATA_FOUND_ERROR));
    } // else - no errors and nothing to do for the system context
  }

  // PMD.DefaultPackage - designed to be called from ImportMigrationManagerImpl within this package
  @SuppressWarnings("PMD.DefaultPackage")
  void addEntry(ImportMigrationEntryImpl entry) {
    entries.put(entry.getPath(), entry);
  }

  // PMD.DefaultPackage - designed to be called from ImportMigrationEntryImpl within this package
  @SuppressWarnings("PMD.DefaultPackage")
  @VisibleForTesting
  InputStream getInputStreamFor(ZipEntry entry) throws IOException {
    final InputStream is = zip.getInputStream(entry);

    inputStreams.add(is);
    return is;
  }

  @VisibleForTesting
  ZipFile getZip() {
    return zip;
  }

  @VisibleForTesting
  Map<Path, ImportMigrationEntryImpl> getEntries() {
    return entries;
  }

  @VisibleForTesting
  Map<String, ImportMigrationSystemPropertyReferencedEntryImpl>
      getSystemPropertiesReferencedEntries() {
    return systemProperties;
  }

  @Override
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
        .forEach(
            me ->
                entries.compute(
                    me.getPropertiesPath(),
                    (p, mpe) -> {
                      if (mpe == null) {
                        // create a new empty migration entry as it was not exported out (at least
                        // not by this migratable)!!!!
                        mpe = new ImportMigrationEmptyEntryImpl(this, p);
                      }
                      mpe.addPropertyReferenceEntry(me.getProperty(), me);
                      return mpe;
                    }));
  }
}
