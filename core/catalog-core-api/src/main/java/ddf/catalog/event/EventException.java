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
package ddf.catalog.event;

/**
 * The exception thrown to capture problems during the PubSub operations create, update, and delete
 * of subscriptions.
 * 
 * @author ddf.isgs@lmco.com
 */
public class EventException extends Exception {

    /** The constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new {@code EventException}.
     */
    public EventException() {
        super();
    }

    /**
     * Instantiates a new {@code EventException} with the provided message.
     * 
     * @param message
     *            the message
     */
    public EventException(String message) {
        super(message);
    }

    /**
     * Instantiates a new {@code EventException} with the provided message and throwable.
     * 
     * @param message
     *            the message
     * @param throwable
     *            the throwable
     */
    public EventException(String message, Throwable throwable) {
        super(message, throwable);
    }

    /**
     * Instantiates a new {@code EventException} with the provided throwable.
     * 
     * @param throwable
     *            the throwable
     */
    public EventException(Throwable throwable) {
        super(throwable);
    }

}
