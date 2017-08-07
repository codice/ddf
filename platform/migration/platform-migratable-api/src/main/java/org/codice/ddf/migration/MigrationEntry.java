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
package org.codice.ddf.migration;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The <code>MigrationEntry</code> interfaces provides support for artifacts that are being exported
 * or imported during migration.
 * <p>
 * <b>
 * This code is experimental. While this interface is functional
 * and tested, it may change or be removed in a future version of the
 * library.
 * </b>
 * </p>
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
     * Gets name for this entry.
     * <p>
     * <i>Note:</i> Absolute path names that are under ${ddf.home} are automatically relativized.
     *
     * @return the name for this entry
     */
    public String getName();

    /**
     * Gets a {@link Path} for this entry.
     * <p>
     * <i>Note:</i> Absolute paths that are under ${ddf.home} are automatically relativized.
     *
     * @return a path for this entry
     */
    public default Path getPath() {
        return Paths.get(getName());
    }

    /**
     * Gets the last modification time of the entry.
     *
     * @return the last modification time of the entry in milliseconds since the epoch,
     * or -1 if not specified
     */
    public long getLastModifiedTime();

    /**
     * Gets the size (a.k.a length) of the entry.
     *
     * @return the size, in bytes, of the entry or -1 if unknown
     */
    public long getSize();

    /**
     * Stores this entry's content in the export during an export migration operation or underneath
     * the distribution root directory during an import migration operation based on this entry's
     * path which can include sub-directories.
     * <p>
     * During an import migration operation, this entry's sub-directories (if any) will be created if
     * they don't already exist. The destination file will be overwritten if it already exists.
     * <p>
     * All errors and warnings are automatically recorded with the associated migration report.
     *
     * @throws MigrationException if a failure that prevents the operation from continue occurred
     */
    public void store();
}
