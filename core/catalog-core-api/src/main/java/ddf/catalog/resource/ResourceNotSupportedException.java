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
 * 
 * The ResourceNotSupportedException is thrown by a {@link ResourceReader} or
 * {@link CatalogFramework} when they do not support a {@link Resource} or {@link ResourceRequest}.
 * 
 * Common uses of this exception include when the {@link ResourceRequest} has an attribute name or
 * value not supported by the {@link CatalogFramework} or {@link ResourceReader}. Also, when the
 * resource URI has a scheme that the {@link ResourceReader} or {@link CatalogFramework} doesn't
 * support.
 * 
 * 
 * @author michael.menousek@lmco.com
 */
public class ResourceNotSupportedException extends Exception {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a resource not supported exception.
     */
    public ResourceNotSupportedException() {
        super();
    }

    /**
     * Instantiates a new resource not supported exception with the provided message.
     * 
     * @param message
     *            the message
     */
    public ResourceNotSupportedException(String message) {
        super(message);
    }

    /**
     * Instantiates a new resource not supported exception with the provided message and throwable.
     * 
     * @param message
     *            the message
     * @param throwable
     *            the throwable
     */
    public ResourceNotSupportedException(String message, Throwable throwable) {
        super(message, throwable);
    }

    /**
     * Instantiates a new resource not supported exception with the provided throwable.
     * 
     * @param throwable
     *            the throwable
     */
    public ResourceNotSupportedException(Throwable throwable) {
        super(throwable);
    }
}
