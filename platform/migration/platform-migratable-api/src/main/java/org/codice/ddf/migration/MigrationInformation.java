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

import org.apache.commons.lang.Validate;

/**
 * Class that provides information statuses for migration operations.
 * <p>
 * <b>
 * This code is experimental. While this interface is functional
 * and tested, it may change or be removed in a future version of the
 * library.
 * </b>
 * </p>
 */
public class MigrationInformation implements MigrationMessage {
    private final String message;

    /**
     * Constructor
     *
     * @param message message regarding migration
     * @throws IllegalArgumentException if <code>message</code> is <code>null</code>
     */
    public MigrationInformation(String message) {
        Validate.notNull(message, "message cannot be null");
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public Optional<MigrationWarning> downgradeToWarning() {
        return Optional.empty();
    }

    @Override
    public String toString() {
        return message;
    }
}
