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
package ddf.catalog.validation.impl;

import ddf.catalog.validation.ValidationException;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public class ValidationExceptionImpl extends ValidationException {

    private static final long serialVersionUID = 1L;

    protected List<String> errors;

    protected List<String> warnings;

    public ValidationExceptionImpl() {
        super();
    }

    public ValidationExceptionImpl(String summaryMessage) {
        super(summaryMessage);
    }

    /**
     * Constructs a {@code ValidationException} with a specified summary message of the failure
     * and associated errors and warnings.
     *
     * @param summaryMessage
     *            summarizes why the validation operation failed
     * @param errors
     *            list of human readable error messages
     * @param warnings
     *            list of human readable warning messages
     */
    public ValidationExceptionImpl(String summaryMessage, List<String> errors, List<String> warnings) {
        super(summaryMessage);
        setErrors(errors);
        setWarnings(warnings);
    }

    public ValidationExceptionImpl(Throwable cause) { super(cause); }

    /**
     * Constructs a {@code ValidationException} with a specified summary message of the failure
     * and associated errors and warnings.
     *
     * @param cause
     *            the cause of why the validation operation failed
     * @param errors
     *            list of human readable error messages
     * @param warnings
     *            list of human readable warning messages
     */
    public ValidationExceptionImpl(Throwable cause, List<String> errors, List<String> warnings) {
        super(cause);
        setErrors(errors);
        setWarnings(warnings);
    }

    public ValidationExceptionImpl(String summaryMessage, Throwable cause) { super(summaryMessage, cause); }

    /**
     * Constructs a {@code ValidationException} with a specified summary message of the failure
     * and associated errors and warnings.
     *
     * @param summaryMessage
     *            summarizes why the validation operation failed
     * @param cause
     *            the cause of why the validation operation failed
     * @param errors
     *            list of human readable error messages
     * @param warnings
     *            list of human readable warning messages
     */
    public ValidationExceptionImpl(String summaryMessage, Throwable cause, List<String> errors, List<String> warnings) {
        super(summaryMessage, cause);
        setErrors(errors);
        setWarnings(warnings);
    }

    public List<String> getErrors() { return errors; }

    /**
     * @param errors Applies a list of all the error messages that have caused validation to fail.
     *               The error messages should be human-readable plain text.
     */
    public void setErrors(List<String> errors) {
        if (CollectionUtils.isNotEmpty(errors)) {
            this.errors = new ArrayList<>(errors);
        }
    }

    public List<String> getWarnings() { return warnings; }

    /**
     * @param warnings Applies a list of warnings messages to this exception.  Warning messages
     *                 are issues that arose during validation but did not cause validation to
     *                 fail. A warning message should be human-readable plain text.
     */
    public void setWarnings(List<String> warnings) {
        if (CollectionUtils.isNotEmpty(warnings)) {
            this.warnings = new ArrayList<>(warnings);
        }
    }
}
