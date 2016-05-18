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

package org.codice.ddf.sdk.validation.metacard;

import java.util.Collections;
import java.util.Set;

import com.google.common.collect.Sets;

import ddf.catalog.data.Metacard;
import ddf.catalog.util.Describable;
import ddf.catalog.validation.MetacardValidator;
import ddf.catalog.validation.ValidationException;
import ddf.catalog.validation.impl.ValidationExceptionImpl;

public class SampleMetacardValidator implements MetacardValidator, Describable {
    private Set<String> validWords = Sets.newHashSet("test", "default", "sample");

    private String id = "sample-validator";

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getTitle() {
        return "Sample Metacard Validator";
    }

    public void setId(String newId) {
        id = newId;
    }

    @Override
    public String getDescription() {
        return "A sample metacard validator used for development and testing.";
    }

    @Override
    public String getOrganization() {
        return "Codice";
    }

    @Override
    public void validate(Metacard metacard) throws ValidationException {
        if (!checkMetacard(metacard.getTitle())) {
            ValidationExceptionImpl validationException = new ValidationExceptionImpl(
                    "Metacard title does not contain any of: " + validWords);
            validationException.setErrors(Collections.singletonList("sampleError"));
            validationException.setWarnings(Collections.singletonList("sampleWarnings"));
            throw validationException;
        }
    }

    private boolean checkMetacard(String title) {
        return validWords.stream()
                .anyMatch(title::contains);
    }

    public void setValidWords(Set<String> validWords) {
        if (validWords != null) {
            this.validWords = validWords;
        }
    }

    public Set<String> getValidWords() {
        return validWords;
    }
}
