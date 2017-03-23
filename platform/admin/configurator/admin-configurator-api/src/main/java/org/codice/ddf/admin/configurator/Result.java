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
package org.codice.ddf.admin.configurator;

import java.util.Optional;

/**
 * Provides result information for a specific {@link Configurator} operation.
 * <p>
 * <b> This code is experimental. While this class is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
public interface Result {
    /**
     * Returns the success or failure of a particular operation.
     *
     * @return true if the operation committed successfully; else, false.
     */
    boolean isOperationSucceeded();

    /**
     * The final disposition of the operation.
     *
     * @return status indicating if the operation committed, rolled back, was skipped, or failed to process correctly
     */
    Status getStatus();

    /**
     * If an exception occurred during processing, this {@code Optional} will contain the exception thrown.
     *
     * @return Optional failure exception
     */
    Optional<Throwable> getBadOutcome();

    /**
     * For operations against Managed Services, this field will be populated with the associated {@code configId}.
     *
     * @return the configId of a specific managed service instance or null for other operations
     */
    String getConfigId();
}
