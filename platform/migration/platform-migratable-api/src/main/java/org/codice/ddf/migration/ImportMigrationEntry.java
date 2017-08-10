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
import java.io.InputStream;
import java.util.Optional;

import org.codice.ddf.util.function.EBiConsumer;

/**
 * The <code>ImportMigrationEntry</code> interfaces provides support for artifacts that are being
 * imported during migration.
 * <p>
 * <b>
 * This code is experimental. While this interface is functional
 * and tested, it may change or be removed in a future version of the
 * library.
 * </b>
 * </p>
 */
public interface ImportMigrationEntry extends MigrationEntry {
    /**
     * Retrieves a migration entry referenced from a property value defined in the properties file
     * associated with this migration entry.
     *
     * @param name the name of the property that contains the reference to a migration entry
     * @return the migration entry corresponding to the property value defined by <code>name</code>
     * in the property file referenced by this entry or empty if it was not exported
     * @throws IllegalArgumentException if <code>name</code> is <code>null</code>
     */
    public Optional<ImportMigrationEntry> getPropertyReferencedEntry(String name);

    /**
     * Stores this required entry's content appropriately based on this entry's path which can include
     * sub-directories using the specified consumer.
     * <p>
     * All errors and warnings are automatically recorded with the associated migration report including
     * those thrown by the exporter logic.
     *
     * @param consumer a consumer capable of importing the content of this entry from a provided input
     *                 stream which might be empty if the entry was not exported otherwise an error
     *                 will automatically be recorded)
     * @return <code>true</code> if no errors were recorded as a result of processing this command;
     * <code>false</code> otherwise
     * @throws MigrationException       if a failure that prevents the operation from continue occurred
     * @throws IllegalArgumentException if <code>consumer</code> is <code>null</code>
     */
    public boolean store(EBiConsumer<MigrationReport, Optional<InputStream>, IOException> consumer);

    /**
     * Gets an input stream for this entry.
     *
     * @return an input stream for this entry or empty if it was not exported
     * @throws IOException           if an I/O error has occurred
     */
    public Optional<InputStream> getInputStream() throws IOException;
}
