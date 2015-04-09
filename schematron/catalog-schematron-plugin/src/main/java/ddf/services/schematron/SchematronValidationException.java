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

import java.util.List;

import ddf.catalog.validation.impl.ValidationExceptionImpl;

/**
 * @author Shaun Morris, Lockheed Martin
 * @author ddf.isgs@lmco.com
 */
public class SchematronValidationException extends ValidationExceptionImpl {

    private static final long serialVersionUID = 1L;

    public SchematronValidationException() { super(); }

    public SchematronValidationException(String message, List<String> errors, List<String> warnings) {
        super(message, errors, warnings);
    }

    public SchematronValidationException(String message) {
        super(message);
    }

    public SchematronValidationException(Throwable cause, List<String> errors, List<String> warnings) {
        super(cause, errors, warnings);
    }

    public SchematronValidationException(Throwable cause) {
        super(cause);
    }

    public SchematronValidationException(String message, Throwable cause, List<String> errors, List<String> warnings) {
        super(message, cause, errors, warnings);
    }

    public SchematronValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
