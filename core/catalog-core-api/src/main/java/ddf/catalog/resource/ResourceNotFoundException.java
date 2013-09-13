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
package ddf.catalog.resource;

/**
 * ResourceNotFoundException is thrown when a {@link ResourceReader} is unable to find a
 * {@link Resource}.
 * 
 * @author michael.menousek@lmco.com
 */
public class ResourceNotFoundException extends Exception {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new ResourceNotFoundException.
     */
    public ResourceNotFoundException() {
        super();
    }

    /**
     * Instantiates a new resource not found exception with the provided message.
     * 
     * @param message
     *            the message
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Instantiates a new resource not found exception with the provided message and a throwable.
     * 
     * @param message
     *            the message
     * @param throwable
     *            the throwable
     */
    public ResourceNotFoundException(String message, Throwable throwable) {
        super(message, throwable);
    }

    /**
     * Instantiates a new resource not found exception with the provided throwable.
     * 
     * @param throwable
     *            the throwable
     */
    public ResourceNotFoundException(Throwable throwable) {
        super(throwable);
    }
}
