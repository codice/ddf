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
public interface FeatureActions {
    /**
     * Creates a handler that will start a feature as part of a transaction.
     *
     * @param featureName the name of the feature to start
     * @return instance of this class
     * @throws ConfiguratorException if an error occurs creating the operator
     */
    Operation<Void> start(String featureName) throws ConfiguratorException;

    /**
     * Creates a handler that will stop a feature as part of a transaction.
     *
     * @param featureName the name of the feature to stop
     * @return instance of this class
     * @throws ConfiguratorException if an error occurs creating the operator
     */
    Operation<Void> stop(String featureName) throws ConfiguratorException;

    /**
     * Determines if the feature with the given name is started.
     *
     * @param featureName the name of the feature
     * @return true if started; else, false
     * @throws ConfiguratorException if an error occurs reading state
     */
    boolean isFeatureStarted(String featureName) throws ConfiguratorException;
}
