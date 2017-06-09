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
 **/
package org.codice.ddf.internal.admin.configurator.actions;

import java.nio.file.Path;
import java.util.Map;

import org.codice.ddf.admin.configurator.ConfiguratorException;
import org.codice.ddf.admin.configurator.Operation;

/**
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
public interface PropertyActions {
    /**
     * Creates a handler for persisting property file changes to a new property file.
     *
     * @param configFile the property file to be created
     * @param configs    map of key:value pairs to be written to the property file
     * @return instance of this class
     * @throws ConfiguratorException if an error occurs creating the operator
     */
    Operation<Void> create(Path configFile, Map<String, String> configs)
            throws ConfiguratorException;

    /**
     * Creates a handler for deleting a property file.
     *
     * @param configFile the property file to be deleted
     * @return instance of this class
     * @throws ConfiguratorException if an error occurs creating the operator
     */
    Operation<Void> delete(Path configFile) throws ConfiguratorException;

    /**
     * Creates a handler for persisting property file changes to an existing property file.
     *
     * @param configFile       the property file to be updated
     * @param configs          map of key:value pairs to be written to the property file
     * @param keepIfNotPresent if true, any keys in the current property file that are not in the
     *                         {@code configs} map will be left with their initial values; if false, they
     *                         will be removed from the file
     * @return instance of this class
     * @throws ConfiguratorException if an error occurs creating the operator
     */
    Operation<Void> update(Path configFile, Map<String, String> configs, boolean keepIfNotPresent)
            throws ConfiguratorException;

    /**
     * Gets the current key:value pairs set in the given property file.
     *
     * @param propFile the property file to query
     * @return the current set of key:value pairs
     * @throws ConfiguratorException if there is an error reading the state
     */
    Map<String, String> getProperties(Path propFile) throws ConfiguratorException;
}
