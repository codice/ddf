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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;
import java.util.function.BiPredicate;

import org.codice.ddf.util.function.EBiConsumer;

/**
 * The <code>ExportMigrationEntry</code> interfaces provides support for artifacts that are being
 * exported during migration.
 * <p>
 * <b>
 * This code is experimental. While this interface is functional
 * and tested, it may change or be removed in a future version of the
 * library.
 * </b>
 * </p>
 */
public interface ExportMigrationEntry extends MigrationEntry {
    /**
     * Creates or retrieves (if already created) a migration entry referenced from the specified property
     * in the properties file associated with this migration entry to be exported by the corresponding
     * migratable.
     * <p>
     * An error will be automatically recorded with the associated migration report if the property
     * is not defined in the property file referenced by this entry or its value is blank.
     * <p>
     * <i>Note:</i> The file referenced from the property is assumed to be relative to the current
     * working directory if not defined as absolute. All paths will automatically be relativized from
     * ${ddf.home} if located underneath.
     *
     * @param name the name of the property referencing a migration entry to create or retrieve
     * @return a new migration entry or the existing one if already created for the migration entry
     * referenced from the specified property value or empty if the property is not defined or
     * its value is blank
     * @throws IllegalArgumentException if <code>name</code> is <code>null</code>
     */
    public default Optional<ExportMigrationEntry> getPropertyReferencedEntry(String name) {
        return getPropertyReferencedEntry(name, (r, n) -> true);
    }

    /**
     * Creates or retrieves (if already created) a migration entry referenced from the specified property
     * in the properties file associated with this migration entry to be exported by the corresponding
     * migratable.
     * <p>
     * The provided predicate will be invoked with the associated migration report and the property
     * value (may be <code>null</code> if not defined). In case the property is not defined or is
     * blank, an error will be automatically recorded unless the predicate returns <code>false</code>.
     * Returning <code>true</code> in such case will still not create a corresponding migration entry.
     * In all other cases, no errors or warning will be generated if the predicate returns <code>false</code>
     * so it is up to the predicate to record one if required.
     * <p>
     * <i>Note:</i> The file referenced from the property is assumed to be relative to the current
     * working directory if not defined as absolute. All paths will automatically be relativized from
     * ${ddf.home} if located underneath.
     *
     * @param name      the name of the property referencing a migration entry to create or retrieve
     * @param validator a predicate to be invoked to validate the property value further which must
     *                  return <code>true</code> to have a migration entry created
     * @return a new migration entry or the existing one if already created for the referenced file
     * from the specified property value or empty if the property is not defined, is blank or is invalid
     * @throws IllegalArgumentException if <code>name</code> or <code>validator</code> is <code>null</code>
     */
    public Optional<ExportMigrationEntry> getPropertyReferencedEntry(String name,
            BiPredicate<MigrationReport, String> validator);

    /**
     * Stores this entry's content in the export using the specified consumer based on this entry's
     * path which can include sub-directories.
     * <p>
     * All errors and warnings are automatically recorded with the associated migration report including
     * those thrown by the consumer logic.
     *
     * @param consumer a consumer capable of exporting the content of this entry to a provided output stream
     * @return <code>true</code> if no errors were recorded as a result of processing this command;
     * <code>false</code> otherwise
     * @throws MigrationException       if a failure that prevents the operation from continue occurred
     * @throws IllegalArgumentException if <code>consumer</code> is <code>null</code>
     */
    public boolean store(EBiConsumer<MigrationReport, OutputStream, IOException> consumer);

    /**
     * Gets an output stream for this entry which provides a low-level way for the migratable to
     * store its own content in the export.
     * <p>
     * <i>Note:</i> The output stream will automatically be closed (if not closed already) when the
     * output stream for another entry is retrieved or when calling {@link #store} on another entry.
     *
     * @return an output stream for this entry
     * @throws IOException if an I/O error has occurred
     */
    public OutputStream getOutputStream() throws IOException;
}
