/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.plugin;

import ddf.catalog.data.Metacard;

/**
 * Plugin Interface for the catalog framework that populates a {@link Metacard} with default values prior to a {@Link CreateRequest} or {@Link UpdateRequest} being processed by the {@link PolicyPlugin},
 *
 */
public interface DefaultMetacardAttributePlugin {

    /**
     * Processes a {@link Metacard}, prior to ddf.catalog.source.CatalogProvider#update(ddf.catalog.operation.UpdateRequest) and ddf.catalog.source.CatalogProvider#create(ddf.catalog.operation.CreateRequest), to add default values to the metacard
     *
     * @param metacard the new {@link Metacard} to process
     */
    Metacard addDefaults(Metacard metacard);
}
