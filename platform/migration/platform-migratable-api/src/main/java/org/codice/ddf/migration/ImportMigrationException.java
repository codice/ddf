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

/**
 * Exception that indicates some problem when importing the system' configurations.
 * <p>
 * <b>
 * This code is experimental. While this interface is functional
 * and tested, it may change or be removed in a future version of the
 * library.
 * </b>
 * </p>
 */
public class ImportMigrationException extends MigrationException {
    private static final String MESSAGE_PREFIX = "Failed to import configurations";

    /**
     * Constructs a new exception with the specified detail message prefixed with a common import
     * configuration error.
     *
     * @param message the detail message. The detail message is saved for later retrieval by the
     *                {@link #getMessage()} method.
     */
    public ImportMigrationException(String message) {
        super(String.format("%s: %s", ImportMigrationException.MESSAGE_PREFIX, message));
    }

    /**
     * Constructs a new exception with a detail message that contains a common import
     * configuration error and the detail message associated with the {@code cause}.
     *
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()}
     *              method). Cannot be {@code null}.
     */
    public ImportMigrationException(Throwable cause) {
        super(String.format("%s: %s", ImportMigrationException.MESSAGE_PREFIX, cause.getMessage()),
                cause);
    }

    /**
     * Constructs a new exception with the specified detail message prefixed with a common import
     * configuration error and the detail message associated with the {@code cause}.
     *
     * @param message the detail message (which is saved for later retrieval by the
     *                {@link #getMessage()} method).
     * @param cause   the cause (which is saved for later retrieval by the {@link #getCause()}
     *                method). Cannot be {@code null}.
     */
    public ImportMigrationException(String message, Throwable cause) {
        super(String.format("%s: %s. %s",
                ImportMigrationException.MESSAGE_PREFIX,
                message,
                cause.getMessage()), cause);
    }
}
