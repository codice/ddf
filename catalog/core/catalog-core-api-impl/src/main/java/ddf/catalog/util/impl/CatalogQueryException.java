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
package ddf.catalog.util.impl;

/**
 * Exception used to wrap checked exceptions thrown by {@link ddf.catalog.CatalogFramework#query}
 * methods. The original exception can be accessed by calling {@link #getCause()}.
 */
public class CatalogQueryException extends RuntimeException {

    public CatalogQueryException(String message, Throwable cause) {
        super(message, cause);
    }

    public CatalogQueryException(Throwable cause) {
        super(cause);
    }
}
