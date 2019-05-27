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

import ddf.security.common.audit.SecurityLogger;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.codice.ddf.configuration.migration.util.AccessUtils;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.migration.MigrationWarning;

public class PathUtils {

  private static final String INVALID_NULL_PATH = "invalid null path";

  /**
   * Quietly deletes the specified file or recursively deletes the specified directory and audit the
   * result. The provided directory is deleted.
   *
   * @param path the file or directory to be deleted
   * @param type a string representing the type of file or directory to delete
   * @return <code>true</code> if the file or directory was deleted, <code>false</code> otherwise
   * @throws IllegalArgumentException if <code>path</code> or <code>type</code> is <code>null</code>
   */
  public static boolean deleteQuietly(Path path, String type) {
    Validate.notNull(path, PathUtils.INVALID_NULL_PATH);
    Validate.notNull(type, "invalid null type");
    final File file = path.toFile();

    if (FileUtils.deleteQuietly(file)) {
      SecurityLogger.audit("{} file {} deleted", StringUtils.capitalize(type), file);
      return true;
    }
    SecurityLogger.audit("Failed to delete {} file {}", StringUtils.lowerCase(type), file);
    return false;
  }

  /**
   * Quietly deletes the specified file or recursively cleans up the specified directory by removing
   * all empty sub-directories. The provided directory is not deleted even if it becomes empty.
   *
   * @param path the file ot directory to be cleaned
   * @param report the report where to record warning messages if unable to delete the file or
   *     directories
   * @return <code>true</code> if the file was deleted or the directory is empty, <code>false</code>
   *     otherwise
   * @throws IllegalArgumentException if <code>path</code> or <code>report</code> is <code>null
   * </code>
   */
  public static boolean cleanQuietly(Path path, MigrationReport report) {
    Validate.notNull(path, PathUtils.INVALID_NULL_PATH);
    Validate.notNull(report, "invalid null report");
    final File file = path.toFile();

    if (file.isDirectory()) {
      return PathUtils.cleanQuietly(file, report);
    }
    if (FileUtils.deleteQuietly(file)) {
      SecurityLogger.audit("File {} deleted", file);
      return true;
    }
    SecurityLogger.audit("Failed to delete file {}", file);
    report.record(new MigrationWarning(Messages.IMPORT_PATH_DELETE_WARNING, file));
    return false;
  }

  /**
   * Recursively cleans up a given directory by removing all empty sub-directories. The provided
   * directory is not deleted even if it is or becomes empty.
   *
   * @param directory the directory to be cleaned
   * @param report the report where to record warnings for directories that could not be deleted
   * @return <code>true</code> if the directory is empty; <code>false</code> otherwise
   */
  private static boolean cleanQuietly(File directory, MigrationReport report) {
    if (!directory.isDirectory()) {
      return false;
    }
    final File[] files = directory.listFiles();

    if (ArrayUtils.isEmpty(files)) {
      return true;
    }
    boolean empty = true; // until proven otherwise

    for (final File f : files) {
      if (PathUtils.cleanQuietly(f, report)) {
        if (!f.delete()) {
          SecurityLogger.audit("Failed to delete directory {}", f);
          report.record(new MigrationWarning(Messages.IMPORT_PATH_DELETE_WARNING, f));
          empty = false;
        } else {
          SecurityLogger.audit("Directory {} deleted", f);
        }
      } else {
        empty = false;
      }
    }
    return empty;
  }

  // Forced to define it as non-static to simplify unit testing.
  private final Path ddfHome;

  /**
   * Creates a new path utility.
   *
   * @throws IOError if unable to determine ${ddf.home}
   */
  public PathUtils() {
    try {
      this.ddfHome =
          Paths.get(System.getProperty("ddf.home")).toRealPath(LinkOption.NOFOLLOW_LINKS);
    } catch (IOException e) {
      throw new IOError(e);
    }
  }

  /**
   * Gets the path to ${ddf.home}.
   *
   * @return the path to ${ddf.home}
   */
  public Path getDDFHome() {
    return ddfHome;
  }

  /**
   * Checks if the specified path is located under ${ddf.home}.
   *
   * @param path the path to check if it is relative from ${ddf.home}
   * @return <code>true</code> if the path it located under ${ddf.home}; <code>false</code>
   *     otherwise
   */
  public boolean isRelativeToDDFHome(Path path) {
    return path.startsWith(ddfHome);
  }

  /**
   * Relativizes the specified path from ${ddf.home}.
   *
   * @param path the path to relativize from ${ddf.home}
   * @return the corresponding relativize path or <code>path</code> if it is not located under
   *     ${ddf.home}
   */
  public Path relativizeFromDDFHome(Path path) {
    if (isRelativeToDDFHome(path)) {
      return ddfHome.relativize(path);
    }
    return path;
  }

  /**
   * Resolves the specified path against ${ddf.home}.
   *
   * @param path the path to resolve against ${ddf.home}
   * @return the corresponding path resolved against ${ddf.home} if it is relative; otherwise <code>
   * path</code>
   */
  public Path resolveAgainstDDFHome(Path path) {
    return ddfHome.resolve(path);
  }

  /**
   * Resolves the specified path against ${ddf.home}.
   *
   * @param path the path to resolve against ${ddf.home}
   * @return the corresponding path resolved against ${ddf.home} if it is relative; otherwise a path
   *     corresponding to <code>pathname</code>
   */
  public Path resolveAgainstDDFHome(String path) {
    return ddfHome.resolve(path);
  }

  /**
   * Calculate the checksum for the specified path.
   *
   * @param path the path to calculate the checksum for
   * @return the corresponding checksum
   * @throws IllegalArgumentException if <code>path</code> is <code>null</code>
   * @throws IOException if an I/O error occurred
   */
  @SuppressWarnings(
      "squid:S2093" /* try-with-resource will throw IOException with InputStream and we do not care to get that exception */)
  public String getChecksumFor(Path path) throws IOException {
    Validate.notNull(path, PathUtils.INVALID_NULL_PATH);
    InputStream is = null;

    try {
      is = AccessUtils.doPrivileged(() -> new FileInputStream(path.toFile()));
      return DigestUtils.md5Hex(is);
    } finally {
      IOUtils.closeQuietly(is); // don't care about errors when closing
    }
  }
}
