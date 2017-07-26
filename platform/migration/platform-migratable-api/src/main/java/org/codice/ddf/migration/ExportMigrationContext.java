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
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

/**
 * The export migration context provides functionality for creating new migration entries and
 * tracking exported migration entries for a given migratable while processing an export migration
 * operation.
 * <p>
 * <b>
 * This code is experimental. While this interface is functional
 * and tested, it may change or be removed in a future version of the
 * library.
 * </b>
 * </p>
 */
public interface ExportMigrationContext extends MigrationContext {
    /**
     * Creates or retrieves (if already created) a migration entry referenced from the specified system
     * property to be exported by the corresponding migratable.
     * <p>
     * An error will automatically be recorded with the associated migration report if the system
     * property is not defined or its value is blank.
     *
     * @param name the name of the system property referencing a migration entry to create or retrieve
     * @return a new migration entry or the existing one if already created for the migration entry
     * referenced from the specified system property value or empty if the property is not defined or
     * its value is blank
     */
    public default Optional<ExportMigrationEntry> getSystemPropertyReferencedEntry(String name) {
        return getSystemPropertyReferencedEntry(name, (r, n) -> true);
    }

    /**
     * Creates or retrieves (if already created) a migration entry referenced from the specified system
     * property to be exported by the corresponding migratable.
     * <p>
     * The provided predicate will be invoked with the property value (may be <code>null</code> if not
     * defined). In case the property is not defined or is blank, an error will be automatically
     * recorded unless the predicate returns <code>false</code>. Returning <code>true</code> in such
     * case will still not create a corresponding migration entry. In all other cases, no errors or
     * warning will be generated if the predicate returns <code>false</code> so it is up to the
     * predicate to record one if required.
     *
     * @param name      the name of the system property referencing a migration entry to create or retrieve
     * @param validator a predicate to be invoked to validate the property value further which must
     *                  return <code>true</code> to have a migration entry created and returned
     * @return a new migration entry or the existing one if already created for the migration entry
     * referenced from the specified system property value or empty if the property is not defined or
     * is invalid
     */
    public Optional<ExportMigrationEntry> getSystemPropertyReferencedEntry(String name,
            BiPredicate<MigrationReport, String> validator);

    /**
     * Creates or retrieves (if already created) a migration entry to be exported by the corresponding
     * migratable corresponding to the specied path.
     *
     * @param path the path of the file to be exported
     * @return a new migration entry for the corresponding migratable or the existing one if already
     * created
     * @throws IllegalAccessException if <code>path</code> is <code>null</code>
     */
    public ExportMigrationEntry getEntry(Path path);

    /**
     * Recursively walks the provided path's tree to create or retrieve (if already created) entries
     * for all files found and returns existing or new migration entries for each one of them.
     *
     * @param path the path to the directory to recursively walk
     * @return a stream of all created or retrieved entries corresponding to all files recursively
     * found under <code>path</code>
     * @throws IllegalAccessException if <code>path</code> is <code>null</code>
     */
    public Stream<ExportMigrationEntry> entries(Path path);
}
