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
package org.codice.ddf.catalog.plugin.metacard.backup.storage.internal;

import java.io.IOException;

import org.codice.ddf.platform.services.common.Describable;

/**
 * <p>
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 * </p>
 * <p>
 * The {@code MetacardBackupStorageProvider} interface is used by the Metacard Backup Plugin
 * to represent storage providers for the transformed metacard data.
 */

public interface MetacardBackupStorageProvider extends Describable {
    /**
     * Deletes backed up metacard with the provided metacard ID
     *
     * @param id - Identifier of the metacard to remove
     * @throws IOException
     * @throws MetacardBackupException
     */
    void delete(String id) throws IOException, MetacardBackupException;

    /**
     * Stores data for the provided metacard ID
     *
     * @param id   - Identifier of the metacard to remove
     * @param data - Bytes of the metacard to store
     * @throws IOException
     * @throws MetacardBackupException
     */
    void store(String id, byte[] data) throws IOException, MetacardBackupException;
}
