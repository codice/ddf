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
package org.codice.ddf.catalog.plugin.metacard.backup.internal;

public class MetacardBackupException  extends Exception {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new MetacardBackupException from a given string.
     *
     * @param string
     *            the string to use for the exception.
     */
    public MetacardBackupException(String string) {
        super(string);
    }

    /**
     * Instantiates a new MetacardBackupException.
     */
    public MetacardBackupException() {
        super();
    }

    /**
     * Instantiates a new MetacardBackupException with a message.
     *
     * @param message
     *            the message
     * @param throwable
     *            the throwable
     */
    public MetacardBackupException(String message, Throwable throwable) {
        super(message, throwable);
    }

    /**
     * Instantiates a new MetacardBackupException.
     *
     * @param throwable
     *            the throwable
     */
    public MetacardBackupException(Throwable throwable) {
        super(throwable);
    }

}