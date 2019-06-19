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
import ddf.security.common.audit.SecurityLogger;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang.Validate;
import org.codice.ddf.configuration.migration.util.AccessUtils;
import org.codice.ddf.migration.ImportMigrationEntry;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides an implementation of the {@link
 * org.codice.ddf.migration.ImportMigrationEntry} representing a directory that was exported.
 */
public class ImportMigrationDirectoryEntryImpl extends ImportMigrationEntryImpl {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ImportMigrationDirectoryEntryImpl.class);

  private final boolean filtered;

  private final Set<ImportMigrationEntry> fileEntries;

  private final long lastModified;

  ImportMigrationDirectoryEntryImpl(
      ImportMigrationContextImpl context, Map<String, Object> metadata) {
    super(
        context, JsonUtils.getStringFrom(metadata, MigrationEntryImpl.METADATA_NAME, true), false);
    this.filtered = JsonUtils.getBooleanFrom(metadata, MigrationEntryImpl.METADATA_FILTERED, false);
    this.fileEntries =
        JsonUtils.getListFrom(metadata, MigrationEntryImpl.METADATA_FILES)
            .stream()
            .map(Object::toString)
            .map(FilenameUtils::separatorsToSystem)
            .map(Paths::get)
            .map(context::getEntry)
            .collect(Collectors.toSet());
    final Long modified =
        JsonUtils.getLongFrom(metadata, MigrationEntryImpl.METADATA_LAST_MODIFIED, false);

    this.lastModified = (modified != null) ? modified : -1;
  }

  @Override
  public long getLastModifiedTime() {
    return lastModified;
  }

  @Override
  public boolean restore(boolean required) {
    if (restored == null) {
      super.restored = false; // until proven otherwise
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Importing {}...", toDebugString());
      }
      // a directory is always exported by the framework, as such we can safely extend our
      // privileges
      AccessUtils.doPrivileged(
          () -> {
            final PathUtils pathUtils = getContext().getPathUtils();
            final Path apath = getAbsolutePath();
            // find all existing files and keep track of it relative from ddf.home to absolute path
            final Map<Path, Path> existingFiles =
                FileUtils.listFiles(
                        apath.toFile(), TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE)
                    .stream()
                    .map(File::toPath)
                    .collect(Collectors.toMap(pathUtils::relativizeFromDDFHome, p -> p));

            // it is safe to ignore the 'required' parameter since if we get here, we have a
            // directory
            // exported to start with and all files underneath are always optional so pass false to
            // restore()
            if (fileEntries
                .stream()
                .peek(me -> existingFiles.remove(me.getPath()))
                .map(me -> me.restore(false))
                .reduce(true, Boolean::logicalAnd)) {
              if (!filtered) {
                // all files from the original system were exported under this directory, so remove
                // all files that were not on the original system but are found on the current one
                final MigrationReport report = getReport();

                existingFiles.forEach((p, ap) -> PathUtils.cleanQuietly(ap, report));
                // cleanup all empty directories left underneath this entry's path
                PathUtils.cleanQuietly(apath, report);
              }
              SecurityLogger.audit("Imported directory {}", apath);
              super.restored = true;
            } else {
              SecurityLogger.audit("Error importing directory {}", apath);
              getReport()
                  .record(
                      new MigrationException(
                          Messages.IMPORT_PATH_COPY_ERROR,
                          getPath(),
                          pathUtils.getDDFHome(),
                          "some directory entries failed"));
            }
          });
    }
    return restored;
  }

  @Override
  public boolean restore(boolean required, PathMatcher filter) {
    Validate.notNull(filter, "invalid null path filter");
    if (restored == null) {
      super.restored = false; // until proven otherwise
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Importing {} with path filter...", toDebugString());
      }
      // a directory is always exported by the framework, as such we can safely extend our
      // privileges
      AccessUtils.doPrivileged(
          () -> {
            // it is safe to ignore the 'required' parameter since if we get here, we have a
            // directory
            // exported to start with and all files underneath are always optional so pass false to
            // restore()
            super.restored =
                fileEntries
                    .stream()
                    .filter(me -> filter.matches(me.getPath()))
                    .map(me -> me.restore(false))
                    .reduce(true, Boolean::logicalAnd);
            final Path apath = getAbsolutePath();

            if (restored) {
              SecurityLogger.audit("Imported directory {}", apath);
            } else {
              SecurityLogger.audit("Error importing directory {}", apath);
              getReport()
                  .record(
                      new MigrationException(
                          Messages.IMPORT_PATH_COPY_ERROR,
                          getPath(),
                          getContext().getPathUtils().getDDFHome(),
                          "some directory entries failed"));
            }
          });
    }
    return restored;
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

  @VisibleForTesting
  boolean isFiltered() {
    return filtered;
  }

  @VisibleForTesting
  Set<ImportMigrationEntry> getFileEntries() {
    return fileEntries;
  }

  @Override
  protected Optional<InputStream> getInputStream(boolean checkAccess) throws IOException {
    return Optional.empty();
  }

  @Override
  protected String toDebugString() {
    return String.format("directory [%s] from [%s]", getAbsolutePath(), getPath());
  }
}
