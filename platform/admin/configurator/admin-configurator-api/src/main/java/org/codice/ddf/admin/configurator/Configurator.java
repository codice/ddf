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

import java.util.UUID;

/**
 * Provides pseudo-transactional semantics to system configuration tasks.
 * <p>
 * In order to use, invoke the various {@code start}, {@code stop}, {@code create}, {@code delete},
 * {@code update}, methods in the intended order of operation, then invoke one of the {@code commit}
 * methods to complete the transaction.
 * <p>
 * <b> This code is experimental. While this class is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
public interface Configurator {
    /**
     * Sequentially invokes all the transaction's operations, committing their changes. If a failure
     * occurs during the processing, a rollback is attempted of those handlers that had already been
     * committed.
     * <p>
     * After commit is called, this {@code Configurator} should not be used again.
     * <p>
     * In the case of a successful commit, changes should be logged. {@code auditParams} are
     * interpolated into the {@code auditMessage} using the Log4J interpolation style.
     *
     * @param auditMessage In the case of a successful commit, the message to pass to be audited
     * @param auditParams  In the case of a successful commit, optional parameters to pass to be
     *                     interpolated into the message.
     * @return report of the commit status, whether successful, successfully rolled back, or partially
     * rolled back with errors
     */
    OperationReport commit(String auditMessage, String... auditParams);

    /**
     * Adds an {@code Operation} step to the transaction.
     *
     * @param operation the step to be added
     * @return a unique identifier for the step
     */
    UUID add(Operation operation);
}
