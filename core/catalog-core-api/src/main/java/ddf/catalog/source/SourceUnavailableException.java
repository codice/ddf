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
package ddf.catalog.source;

/**
 * This exception should be thrown on a {@link Source}-related operation when the source is
 * unavailable.
 * 
 */
public class SourceUnavailableException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new source unavailable exception.
     */
    public SourceUnavailableException() {
        super();
    }

    /**
     * Instantiates a new source unavailable exception with the provided message.
     * 
     * @param message
     *            the message
     */
    public SourceUnavailableException(String message) {
        super(message);
    }

    /**
     * Instantiates a new source unavailable exception with the provided message and
     * {@link Throwable}.
     * 
     * @param message
     *            the message
     * @param throwable
     *            the throwable
     */
    public SourceUnavailableException(String message, Throwable throwable) {
        super(message, throwable);
    }

    /**
     * Instantiates a new source unavailable exception with the provided {@link Throwable}.
     * 
     * @param throwable
     *            the throwable
     */
    public SourceUnavailableException(Throwable throwable) {
        super(throwable);
    }
}
