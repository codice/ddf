/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.catalog.validation;

import java.util.List;

/**
 * Thrown to indicate that a validation operation could not be completed.
 * Provides information in the form of a summary message, a list of error
 * messages, and a list of warnings.
 * 
 * @author Michael Menousek, Lockheed Martin
 * @author Shaun Morris, Lockheed Martin
 * @author Ashraf Barakat, Lockheed Martin
 * @author ddf.isgs@lmco.com
 */
public abstract class ValidationException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a {@code ValidationException} with no detailed message.
     */
    public ValidationException() {
        super();
    }

    /**
     * Constructs a {@code ValidationException} with a specified summary message
     * of the failure.
     * 
     * @param summaryMessage
     *            summarizes why the validation operation failed
     */
    public ValidationException(String summaryMessage) {
        super(summaryMessage);
    }

    /**
     * @return a list of all error messages that has caused validation to fail.
     *         The error message should be human-readable plain text.
     */
    public abstract List<String> getErrors();

    /**
     * @return a list of warning messages of possible issues that arose during
     *         validation that did not cause validation to fail. The warning
     *         message should be human-readable plain text.
     */
    public abstract List<String> getWarnings();
}
