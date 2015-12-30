/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.configuration.status;

import static org.apache.commons.lang.Validate.notNull;

import javax.validation.constraints.NotNull;

/**
 * Class that provides statuses for migration operations.
 */
public class MigrationWarning {

    private final String message;

    /**
     * Constructor
     *
     * @param message message regarding migration
     */
    public MigrationWarning(@NotNull String message) {
        notNull(message, "message cannot be null");
        this.message = message;
    }

    public String getMessage() {
        return this.message;
    }
}
