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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.codice.ddf.migration.ImportMigrationEntry;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.migration.MigrationWarning;
import org.codice.ddf.util.function.BiThrowingConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class provides an implementation of the {@link ImportMigrationEntry} interface. */
// squid:S2160 - the base class equals() is sufficient for our needs. entries are unique based on
// their paths
@SuppressWarnings("squid:S2160")
public class ImportMigrationEntryImpl extends MigrationEntryImpl implements ImportMigrationEntry {
  private static final Logger LOGGER = LoggerFactory.getLogger(ImportMigrationEntryImpl.class);

  private final Map<String, ImportMigrationJavaPropertyReferencedEntryImpl> properties =
      new HashMap<>(8);

  private final ImportMigrationContextImpl context;

  private final Path absolutePath;

  private final Path path;

  private final File file;

  private final String name;

  private final ZipEntry entry;

  /**
   * Will track if restore was attempted along with its result. Will be <code>null</code> until
   * restore() is attempted, at which point it will start tracking the first restore() result.
   */
  protected Boolean restored = null;

  /**
   * Instantiates a new migration entry by parsing the provided zip entry's name for a migratable
   * identifier and an entry relative name.
   *
   * @param contextProvider a provider for migration contexts given a migratable id
   * @param ze the zip entry for which we are creating an entry
   */
  ImportMigrationEntryImpl(
      Function<String, ImportMigrationContextImpl> contextProvider, ZipEntry ze) {
    // we still must sanitize because there could be a mix of / and \ and Paths.get() doesn't
    // support that
    final Path fqn = Paths.get(FilenameUtils.separatorsToSystem(ze.getName()));
    final int count = fqn.getNameCount();

    if (count > 1) {
      this.context = contextProvider.apply(fqn.getName(0).toString());
      this.path = fqn.subpath(1, count);
    } else { // system entry
      this.context = contextProvider.apply(null);
      this.path = fqn;
    }
    this.absolutePath = context.getPathUtils().resolveAgainstDDFHome(path);
    this.file = absolutePath.toFile();
    this.name = FilenameUtils.separatorsToUnix(path.toString());
    this.entry = ze;
  }

  /**
   * Instantiates a new migration entry with the given name.
   *
   * @param context the migration context associated with this entry
   * @param name the entry's relative name
   */
  protected ImportMigrationEntryImpl(ImportMigrationContextImpl context, String name) {
    this.context = context;
    this.path = Paths.get(name);
    this.name = FilenameUtils.separatorsToUnix(name);
    this.absolutePath = context.getPathUtils().resolveAgainstDDFHome(path);
    this.file = absolutePath.toFile();
    this.entry = null;
  }

  /**
   * Instantiates a new migration entry with the given path.
   *
   * @param context the migration context associated with this entry
   * @param path the entry's relative path
   */
  protected ImportMigrationEntryImpl(ImportMigrationContextImpl context, Path path) {
    this.context = context;
    this.path = path;
    this.name = FilenameUtils.separatorsToUnix(path.toString());
    this.absolutePath = context.getPathUtils().resolveAgainstDDFHome(path);
    this.file = absolutePath.toFile();
    this.entry = null;
  }

  @Override
  public MigrationReport getReport() {
    return context.getReport();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Path getPath() {
    return path;
  }

  @Override
  public long getLastModifiedTime() {
    return entry.getTime();
  }

  @Override
  public Optional<InputStream> getInputStream() throws IOException {
    return Optional.ofNullable(context.getInputStreamFor(entry));
  }

  @Override
  public boolean restore(boolean required) {
    return restore(
        (r, is) -> {
          final String debugString =
              LOGGER.isDebugEnabled() ? ("required " + toDebugString()) : null;
          final InputStream fis = is.orElse(null);

          if (fis == null) { // no entry stored!!!!
            handleRestoreWhenNoEntryWasExported(required, debugString);
          } else {
            handleRestoreWhenAnEntryWasExported(fis, debugString);
          }
        });
  }

  @Override
  public boolean restore(
      BiThrowingConsumer<MigrationReport, Optional<InputStream>, IOException> consumer) {
    Validate.notNull(consumer, "invalid null consumer");
    if (restored == null) {
      this.restored = false; // until proven otherwise
      Optional<InputStream> is = Optional.empty();

      try {
        is = getInputStream();
        final Optional<InputStream> fis = is;

        this.restored = getReport().wasIOSuccessful(() -> consumer.accept(getReport(), fis));
      } catch (IOException e) {
        getReport()
            .record(
                new MigrationException(
                    Messages.IMPORT_PATH_COPY_ERROR, path, context.getPathUtils().getDDFHome(), e));
      } finally {
        is.ifPresent(IOUtils::closeQuietly); // we do not care if we cannot close it
      }
    }
    return restored;
  }

  @Override
  public Optional<ImportMigrationEntry> getPropertyReferencedEntry(String pname) {
    Validate.notNull(pname, "invalid null property name");
    return Optional.ofNullable(properties.get(pname));
  }

  @Override
  protected ImportMigrationContextImpl getContext() {
    return context;
  }

  protected Path getAbsolutePath() {
    return absolutePath;
  }

  protected File getFile() {
    return file;
  }

  /**
   * Gets a debug string to represent this entry.
   *
   * @return a debug string for this entry
   */
  protected String toDebugString() {
    return String.format("file [%s] from [%s]", absolutePath, path);
  }

  protected boolean isMigratable() {
    final MigrationReport report = getContext().getReport();

    if (path.isAbsolute()) {
      report.record(
          new MigrationWarning(
              Messages.IMPORT_PATH_DELETE_WARNING,
              path,
              String.format("is outside [%s]", getContext().getPathUtils().getDDFHome())));
      return false;
    } else if (Files.isSymbolicLink(getAbsolutePath())) {
      report.record(
          new MigrationWarning(Messages.IMPORT_PATH_DELETE_WARNING, path, "is a symbolic link"));
      return false;
    }
    return true;
  }

  // PMD.DefaultPackage - designed to be called from ImportMigrationContextImpl within this package
  @SuppressWarnings("PMD.DefaultPackage")
  @VisibleForTesting
  void addPropertyReferenceEntry(
      String pname, ImportMigrationJavaPropertyReferencedEntryImpl entry) {
    properties.put(pname, entry);
  }

  @VisibleForTesting
  Map<String, ImportMigrationJavaPropertyReferencedEntryImpl> getJavaPropertyReferencedEntries() {
    return properties;
  }

  private void handleRestoreWhenNoEntryWasExported(boolean required, @Nullable String debugString) {
    if (required) {
      LOGGER.debug("Importing {}...", debugString);
      getReport().record(new MigrationException(Messages.IMPORT_PATH_NOT_EXPORTED_ERROR, path));
    } else {
      // it is optional so delete it as it was optional when we exported and wasn't on
      // disk so we want to make sure we end up without the file on disk after import
      LOGGER.debug("Deleting {}...", debugString);
      // but only if it is migratable to start with
      if (isMigratable() && !getFile().delete()) {
        getReport().record(new MigrationException(Messages.IMPORT_PATH_DELETE_ERROR, path));
      }
    }
  }

  private void handleRestoreWhenAnEntryWasExported(InputStream is, @Nullable String debugString)
      throws IOException {
    LOGGER.debug("Importing {}...", debugString);
    try {
      FileUtils.copyInputStreamToFile(is, file);
    } catch (IOException e) {
      if (!file.canWrite()) { // make it writable and try again
        InputStream ris = context.getInputStreamFor(entry);

        try {
          LOGGER.debug("temporarily overriding write privileges for {}", file);
          if (!file.setWritable(true)) { // cannot set it writable so bail
            throw e;
          }
          FileUtils.copyInputStreamToFile(ris, file);
        } finally {
          IOUtils.closeQuietly(ris); // we do not care if we cannot close it
          // reset the permissions properly
          file.setWritable(false);
        }
      }
    } finally {
      IOUtils.closeQuietly(is); // we do not care if we cannot close it
    }
  }
}
