/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.catalog.transform;

import ddf.catalog.operation.QueryResponse;

/**
 * The exception thrown when problems transforming a {@link QueryResponse} are detected.
 * 
 * @author ddf.isgs@lmco.com
 * 
 */
public class CatalogTransformerException extends Exception {

    /** The constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new {@code CatalogTransformerException}.
     */
    public CatalogTransformerException() {
        super();
    }

    /**
     * Instantiates a new {@code CatalogTransformerException} with the provided message.
     * 
     * @param message
     *            the message
     */
    public CatalogTransformerException(String message) {
        super(message);
    }

    /**
     * Instantiates a new {@code CatalogTransformerException} with the provided message and
     * throwable.
     * 
     * @param message
     *            the message
     * @param throwable
     *            the throwable
     */
    public CatalogTransformerException(String message, Throwable throwable) {
        super(message, throwable);
    }

    /**
     * Instantiates a new {@code CatalogTransformerException} with the provided throwable.
     * 
     * @param throwable
     *            the throwable
     */
    public CatalogTransformerException(Throwable throwable) {
        super(throwable);
    }
}
