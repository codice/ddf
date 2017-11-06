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
package org.codice.ddf.migration;

import java.nio.file.Path;

/**
 * The <code>MigrationEntry</code> interface provides support for artifacts that are being exported
 * or imported during migration.
 *
 * <p><b> This code is experimental. While this interface is functional and tested, it may change or
 * be removed in a future version of the library. </b>
 */
public interface MigrationEntry extends Comparable<MigrationEntry> {

  /**
   * Gets the migration report associated with this entry. Warnings or errors can be recorded with
   * the report.
   *
   * @return the migration report associated with this entry
   */
  public MigrationReport getReport();

  /**
   * Gets the identifier for the {@link Migratable} service responsible for this entry.
   *
   * @return the responsible migratable service id
   */
  public String getId();

  /**
   * Gets name for this entry. The name is standardized with slashes (i.e /).
   *
   * @return the name for this entry
   */
  public String getName();

  /**
   * Gets a {@link Path} for this entry.
   *
   * <p><i>Note:</i> Absolute paths that are under ${ddf.home} are automatically relativized.
   *
   * @return a path for this entry
   */
  public Path getPath();

  /**
   * Tests whether this entry represents a directory.
   *
   * @return <code>true</code> if and only if the entry represents a directory; <code>false</code>
   *     otherwise
   */
  public boolean isDirectory();

  /**
   * Tests whether this entry represents a file.
   *
   * @return <code>true</code> if and only if the entry represents a file; <code>false</code>
   *     otherwise
   */
  public boolean isFile();

  /**
   * Gets the last modification time of the entry.
   *
   * @return the last modification time of the entry in milliseconds since the epoch, or -1 if not
   *     specified
   */
  public long getLastModifiedTime();
}
