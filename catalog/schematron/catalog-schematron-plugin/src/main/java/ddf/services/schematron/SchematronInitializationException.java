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

package ddf.services.schematron;

/**
 * Custom exception for handling situations where Schematron Validation Service cannot initialize
 * correctly. This can happen if an invalid sch file is specified during initialization.
 * 
 * @author rodgersh
 */
public class SchematronInitializationException extends Exception {
    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new SchematronInitializationException.
     */
    public SchematronInitializationException() {
        super();
    }

    /**
     * Instantiates a new SchematronInitializationException with specified message.
     * 
     * @param message
     *            the exception message
     */
    public SchematronInitializationException(String message) {
        super(message);
    }

    /**
     * Instantiates a new SchematronInitializationException with message and throwable.
     * 
     * @param message
     *            the message
     * @param throwable
     *            the throwable
     */
    public SchematronInitializationException(String message, Throwable throwable) {
        super(message, throwable);
    }

    /**
     * Instantiates a new SchematronInitializationException with throwable.
     * 
     * @param throwable
     *            the throwable
     */
    public SchematronInitializationException(Throwable throwable) {
        super(throwable);
    }

}
