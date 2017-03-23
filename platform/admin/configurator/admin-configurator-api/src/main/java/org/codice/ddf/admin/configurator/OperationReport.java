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

import java.util.List;
import java.util.UUID;

/**
 * Summary report for a {@link Configurator#commit()}.
 * <p>
 * <b> This code is experimental. While this class is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
public interface OperationReport {
    /**
     * Indicates if the transaction completed successfully with all tasks committing their changes.
     *
     * @return true if the transaction completed; else, false
     */
    boolean hasTransactionSucceeded();

    /**
     * Method to retrieve a specific task result from the report.
     *
     * @param key the UUID key returned from an initial setup call for a {@code Configurator} task
     * @return the result of the task
     */
    Result getResult(UUID key);

    /**
     * An immutable list of Results that failed.
     *
     * @return an immutable list of failed results.
     * It is an implementor's responsibility to ensure that the collection is unmodifiable.
     */
    List<Result> getFailedResults();

    /**
     * Returns true if any tasks failed, causing failure results.
     *
     * @return true if any failures present; else, false
     */
    boolean containsFailedResults();

    /**
     * Used internally to populate the report. Clients should not call this method directly.
     *
     * @param key    the unique key of an operation
     * @param result the result of the operation
     */
    void putResult(UUID key, Result result);
}
