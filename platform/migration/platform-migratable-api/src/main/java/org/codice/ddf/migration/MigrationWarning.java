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

import static org.apache.commons.lang.Validate.notNull;

import org.apache.commons.lang.Validate;

/**
 * Class that provides statuses for migration operations.
 * <p>
 * <b>
 * This code is experimental. While this interface is functional
 * and tested, it may change or be removed in a future version of the
 * library.
 * </b>
 * </p>
 */
public class MigrationWarning {

    private final String message;

    /**
     * Constructor
     *
     * @param message message regarding migration
     * @throws IllegalArgumentException if <code>message</code> is <code>null</code>
     */
    public MigrationWarning(String message) {
        notNull(message, "message cannot be null");
        this.message = message;
    }

    /**
     * Constructor
     *
     * @param error migration exception to convert into a warning
     * @throws IllegalArgumentException if <code>error</code> is <code>null</code>
     */
    public MigrationWarning(MigrationException error) {
        Validate.notNull(error, "invalid null error");
        this.message = error.getMessage();
    }

    public String getMessage() {
        return this.message;
    }

    @Override
    public String toString() {
        return message;
    }
}
