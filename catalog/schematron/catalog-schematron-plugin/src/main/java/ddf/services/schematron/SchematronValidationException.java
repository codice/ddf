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

import java.util.ArrayList;
import java.util.List;

import ddf.catalog.validation.ValidationException;

/**
 * @author Shaun Morris, Lockheed Martin
 * @author ddf.isgs@lmco.com
 */
public class SchematronValidationException extends ValidationException {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private List<String> errors;

    private List<String> warnings;

    public SchematronValidationException(String message, List<String> errors, List<String> warnings) {
        super(message);
        if (errors != null) {
            this.errors = new ArrayList<String>(errors);
        } else {
            this.errors = new ArrayList<String>();
        }

        if (warnings != null) {
            this.warnings = new ArrayList<String>(warnings);
        } else {
            this.warnings = new ArrayList<String>();
        }
    }

    public SchematronValidationException(String message) {
        super(message);
        errors = new ArrayList<String>();
        warnings = new ArrayList<String>();
    }

    @Override
    public List<String> getErrors() {
        return errors;
    }

    @Override
    public List<String> getWarnings() {
        return warnings;
    }

    public void setErrors(List<String> errors) {
        if (errors != null && !errors.isEmpty()) {
            this.errors.addAll(errors);
        }
    }

    public void setWarnings(List<String> warnings) {
        if (warnings != null && !warnings.isEmpty()) {
            this.warnings.addAll(warnings);
        }
    }

}
