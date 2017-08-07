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
import java.nio.file.PathMatcher;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nullable;

/**
 * The import migration context keeps track of exported migration entries for a given migratable
 * while processing an import migration operation.
 * <p>
 * <b>
 * This code is experimental. While this interface is functional
 * and tested, it may change or be removed in a future version of the
 * library.
 * </b>
 * </p>
 */
public interface ImportMigrationContext extends MigrationContext {
    /**
     * Gets the exported version for the corresponding migratable.
     *
     * @return the exported version for the corresponding migratable or <code>null</code> if the
     * corresponding migratable was not exported
     */
    @Nullable
    public String getVersion();

    /**
     * Retrieves a migration entry referenced from a given system property that was exported by the
     * corresponding migratable.
     *
     * @param name the name of the system property referencing a migration entry to retrieve
     * @return the corresponding migration entry or empty if it was not migrated by the corresponding
     * migratable
     * @throws IllegalArgumentException if <code>name</code> is <code>null</code>
     */
    public Optional<ImportMigrationEntry> getSystemPropertyReferencedEntry(String name);

    /**
     * Retrieves a migration entry that was exported by the corresponding migratable with the given
     * path.
     *
     * @param path the path of the file that was exported (should be relative to ${ddf.home})
     * @return the corresponding migration entry or empty it it was not migrated by the corresponding
     * migratable
     * @throws IllegalArgumentException if <code>path</code> is <code>null</code>
     */
    public Optional<ImportMigrationEntry> getEntry(Path path);

    /**
     * Retrieves all exported migration entries.
     *
     * @return a stream of all migration entries
     */
    public Stream<ImportMigrationEntry> entries();

    /**
     * Retrieves all exported migration entries located underneath the provided relative path.
     *
     * @param path the path to the directory for which to retrieve all exported migration entries
     *             (should be relative to ${ddf.home})
     * @return a stream of all migration entries located under <code>path</code>
     * @throws IllegalArgumentException if <code>path</code> is <code>null</code>
     */
    public Stream<ImportMigrationEntry> entries(Path path);

    /**
     * Retrieves all exported migration entries located underneath the provided relative path that
     * matches the provided path filter.
     *
     * @param path   the path to the directory for which to retrieve all exported migration entries that
     *               matched the given filter (should be relative to ${ddf.home})
     * @param filter the path filter to use
     * @return a stream of all migration entries located under <code>path</code> that matches the given
     * filter
     * @throws IllegalArgumentException if <code>path</code> or <code>filter</code> is <code>null</code>
     */
    public Stream<ImportMigrationEntry> entries(Path path, PathMatcher filter);

    /**
     * Cleans the specified directory path recursively.
     * <p>
     * Errors and/or warnings will automatically be recorded with the migration report.
     *
     * @param path the directory to recursively clean (can be relative to ${ddf.home})
     * @return <code>true</code> if the directory was cleaned; <code>false</code> otherwise
     * @throws IllegalArgumentException if <code>path</code> is <code>null</code>
     */
    public boolean cleanDirectory(Path path);
}
