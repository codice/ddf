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
import java.util.Dictionary;

import org.codice.ddf.admin.configurator.ConfiguratorException;
import org.codice.ddf.admin.configurator.Operation;

/**
 * Config operations adapted for the Configurator API.
 * <p>
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
public interface ConfigActions {
    /**
     * Creates a handler for persisting config file changes to a new config file.
     *
     * @param configFile the config file to be created
     * @param configs    dictionary of key:value pairs to be written to the config file
     * @return instance of this class
     * @throws ConfiguratorException if an error occurs creating the operator
     */
    Operation<Void> create(Path configFile, Dictionary<String, Object> configs)
            throws ConfiguratorException;

    /**
     * Creates a handler for deleting a config file.
     *
     * @param configFile the config file to be deleted
     * @return instance of this class
     * @throws ConfiguratorException if an error occurs creating the operator
     */
    Operation<Void> delete(Path configFile) throws ConfiguratorException;

    /**
     * Creates a handler for persisting config file changes to an existing config file.
     *
     * @param configFile       the config file to be updated
     * @param configs          dictionary of key:value pairs to be written to the config file
     * @param keepIfNotPresent if true, any keys in the current config file that are not in the
     *                         {@code configs} map will be left with their initial values; if false, they
     *                         will be removed from the file
     * @return instance of this class
     * @throws ConfiguratorException if an error occurs creating the operator
     */
    Operation<Void> update(Path configFile, Dictionary<String, Object> configs,
            boolean keepIfNotPresent) throws ConfiguratorException;

    /**
     * Gets the current key:value pairs set in the given config file.
     *
     * @param propFile the config file to query
     * @return the current set of key:value pairs
     * @throws ConfiguratorException if there is an error reading the state
     */
    Dictionary<String, Object> getProperties(Path propFile) throws ConfiguratorException;
}
