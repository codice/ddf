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
package org.codice.ddf.configuration.store;

import java.util.Collection;
import java.util.Dictionary;

import javax.validation.constraints.NotNull;

/**
 * Interface implemented by classes used to manage and persist configuration files.
 */
public interface FileHandler {

    /**
     * Gets the list of configuration Persistent Identifiers (PID) for which configuration files
     * exist.
     *
     * @return collection of PIDs for which a configuration file exists
     * @throws ConfigurationFileException thrown if the list of PIDs couldn't be retrieved.
     */
    @NotNull
    Collection<String> getConfigurationPids() throws ConfigurationFileException;

    /**
     * Reads the properties associated with a specific PID.
     *
     * @param pid configuration PID of the properties to read
     * @return properties associated with the configuration PID provided
     * @throws ConfigurationFileException thrown if the properties couldn't be read
     * @throws IllegalArgumentException   thrown if no file exists for the configuration PID
     *                                    provided
     */
    @NotNull
    Dictionary<String, Object> read(@NotNull String pid)
            throws ConfigurationFileException, IllegalArgumentException;

    /**
     * Writes the properties associated with a configuration PID. The properties will only be
     * written if a file existed at startup.
     *
     * @param pid        configuration PID of the properties to write
     * @param properties properties to write
     * @throws ConfigurationFileException thrown if the properties couldn't be written
     */
    void write(@NotNull String pid, @NotNull Dictionary<String, Object> properties)
            throws ConfigurationFileException;

    /**
     * Deletes the file and properties associated with a configuration PID. Nothing will happen if
     * no file exists for the configuration PID provided.
     *
     * @param pid configuration PID of the properties to delete.
     * @throws ConfigurationFileException thrown if the properties couldn't be deleted
     */
    void delete(@NotNull String pid) throws ConfigurationFileException;

    /**
     * Registers a listener that will be called when any file associated with a configuration PID
     * changes. Only one listener can be registered at any given time, i.e., calling this method
     * will replace any listener previously registered.
     *
     * @param listener listener to call when a configuration file is created, changed or deleted
     */
    void registerForChanges(@NotNull ChangeListener listener);
}
