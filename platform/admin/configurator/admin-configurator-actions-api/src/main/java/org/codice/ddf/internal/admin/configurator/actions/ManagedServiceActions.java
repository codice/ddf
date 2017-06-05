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

import java.util.Map;

import org.codice.ddf.admin.configurator.ConfiguratorException;
import org.codice.ddf.admin.configurator.Operation;

/**
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
public interface ManagedServiceActions {
    /**
     * Creates a handler that will create a managed service as part of a transaction.
     *
     * @param factoryPid the PID of the service factory
     * @param configs    the configuration properties to apply to the service
     * @return a service operation object
     * @throws ConfiguratorException if an error occurs creating the operator
     */
    Operation<String> create(String factoryPid, Map<String, Object> configs)
            throws ConfiguratorException;

    /**
     * Creates a handler that will delete a managed service as part of a transaction.
     *
     * @param pid the PID of the instance to be deleted
     * @return a service operation object
     * @throws ConfiguratorException if an error occurs creating the operator
     */
    Operation<String> delete(String pid) throws ConfiguratorException;

    /**
     * For the given managed service factory, retrieves the full complement of configuration properties.
     * <p>
     * This will get all the key:value pairs for each available configuration.
     *
     * @param factoryPid the factoryPid of the service to query
     * @return the the current sets of key:value pairs, in a map keyed on {@code configId}
     * @throws ConfiguratorException if any errors occur reading the state
     */
    Map<String, Map<String, Object>> read(String factoryPid) throws ConfiguratorException;
}
