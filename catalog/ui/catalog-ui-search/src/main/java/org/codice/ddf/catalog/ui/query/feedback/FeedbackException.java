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
package org.codice.ddf.catalog.ui.query.feedback;

public class FeedbackException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs new exception.
     */
    public FeedbackException() {
        super();
    }

    /**
     * Constructs new exception with given message.
     *
     * @param message the detail message.
     */
    public FeedbackException(String message) {
        super(message);
    }

    /**
     * Constructs new exception with given message and cause.
     *
     * @param message the detail message.
     * @param cause   the cause.
     */
    public FeedbackException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs new exception with the given cause.
     *
     * @param cause the cause.
     */
    public FeedbackException(Throwable cause) {
        super(cause);
    }
}
