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

import org.codice.ddf.admin.configurator.ConfiguratorException;
import org.codice.ddf.admin.configurator.Operation;

/**
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
public interface BundleActions {
    /**
     * Creates a handler that will start a bundle as part of a transaction.
     *
     * @param bundleSymName the name of the bundle to start
     * @return instance of this class
     * @throws ConfiguratorException if an error occurs creating the operator
     */
    Operation<Void> start(String bundleSymName) throws ConfiguratorException;

    /**
     * Creates a handler that will stop a bundle as part of a transaction.
     *
     * @param bundleSymName the name of the bundle to stop
     * @return instance of this class
     * @throws ConfiguratorException if an error occurs creating the operator
     */
    Operation<Void> stop(String bundleSymName) throws ConfiguratorException;

    /**
     * Determines if the bundle with the given name is started.
     *
     * @param bundleSymName the symbolic name of the bundle
     * @return true if started; else, false
     * @throws ConfiguratorException if an error occurs reading state
     */
    boolean isStarted(String bundleSymName) throws ConfiguratorException;
}
