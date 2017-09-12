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
 */
package org.codice.ddf.migration;

import java.util.Optional;

/**
 * Interface for describing migration messages that can be recorded during an import or export
 * migration operation.
 * <p>
 * <i>Note:</i> Messages are displayed to the administrator on the console during a migration
 * operation.
 * <p>
 * <b>
 * This code is experimental. While this interface is functional
 * and tested, it may change or be removed in a future version of the
 * library.
 * </b>
 * </p>
 */
public interface MigrationMessage {
    /**
     * Gets the string representation for this migration message.
     *
     * @return the message string representation
     */
    public String getMessage();

    /**
     * Downgrades the specified message to a warning if possible.
     *
     * @return a warning that correspond to this message or empty if the message cannot be downgraded
     * (e.g. it is an informational message)
     */
    public Optional<MigrationWarning> downgradeToWarning();
}
