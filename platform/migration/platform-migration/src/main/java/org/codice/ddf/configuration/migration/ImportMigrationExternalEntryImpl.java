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
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.migration.MigrationWarning;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides an implementation of the {@link
 * org.codice.ddf.migration.ImportMigrationEntry} representing an external file that was exported.
 */
public class ImportMigrationExternalEntryImpl extends ImportMigrationEntryImpl {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ImportMigrationExternalEntryImpl.class);

  private final String checksum;

  private final boolean softlink;

  ImportMigrationExternalEntryImpl(
      ImportMigrationContextImpl context, Map<String, Object> metadata) {
    super(context, JsonUtils.getStringFrom(metadata, MigrationEntryImpl.METADATA_NAME, true));
    this.checksum = JsonUtils.getStringFrom(metadata, MigrationEntryImpl.METADATA_CHECKSUM, false);
    this.softlink = JsonUtils.getBooleanFrom(metadata, MigrationEntryImpl.METADATA_SOFTLINK, false);
  }

  @Override
  public long getLastModifiedTime() {
    return -1L;
  }

  @Override
  public boolean restore(boolean required) {
    if (restored == null) {
      // until proven otherwise in case the next line throws an exception
      super.restored = false;

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Verifying {}{}...", (required ? "required " : ""), toDebugString());
      }
      super.restored = verifyRealFile(required);
    }
    return restored;
  }

  @Override
  protected Optional<InputStream> getInputStream(boolean checkAccess) throws IOException {
    return Optional.empty();
  }

  @Override
  protected String toDebugString() {
    return String.format("external file [%s] from [%s]", getAbsolutePath(), getPath());
  }

  @VisibleForTesting
  String getChecksum() {
    return checksum;
  }

  @VisibleForTesting
  boolean isSoftlink() {
    return softlink;
  }

  /**
   * Verifies the corresponding existing file to see if it matches the original one based on the
   * exported info.
   *
   * @param required <code>true</code> if the file was required to be exported; <code>false</code>
   *     if it was optional
   * @return <code>false</code> if an error was detected during verification; <code>true</code>
   *     otherwise
   */
  @SuppressWarnings(
      "squid:S3725" /* Files.isRegularFile() is used for consistency and to make sure that softlinks are not followed. not worried about performance here */)
  private boolean verifyRealFile(boolean required) {
    return AccessUtils.doPrivileged(
        () -> {
          final MigrationReport report = getReport();
          final Path apath = getAbsolutePath();
          final File file = getFile();

          if (!file.exists()) {
            if (required) {
              report.record(
                  new MigrationException(Messages.IMPORT_PATH_ERROR, apath, "does not exist"));
              return false;
            }
            return true;
          }
          if (softlink) {
            if (!Files.isSymbolicLink(apath)) {
              report.record(
                  new MigrationWarning(
                      Messages.IMPORT_PATH_WARNING, apath, "is not a symbolic link"));
              return false;
            }
          } else if (!Files.isRegularFile(apath, LinkOption.NOFOLLOW_LINKS)) {
            report.record(
                new MigrationWarning(Messages.IMPORT_PATH_WARNING, apath, "is not a regular file"));
          }
          return verifyChecksum();
        });
  }

  private boolean verifyChecksum() {
    if (checksum != null) {
      final MigrationReport report = getReport();
      final Path apath = getAbsolutePath();

      try {
        final String rchecksum = getContext().getPathUtils().getChecksumFor(apath);

        if (!rchecksum.equals(checksum)) {
          report.record(new MigrationWarning(Messages.IMPORT_CHECKSUM_MISMATCH_WARNING, apath));
          return false;
        }
      } catch (IOException e) {
        LOGGER.info("failed to compute MD5 checksum for '" + getName() + "': ", e);
        report.record(new MigrationWarning(Messages.IMPORT_CHECKSUM_COMPUTE_WARNING, apath, e));
        return false;
      }
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
