/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */

package org.codice.ddf.sdk.validation.metacard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import ddf.catalog.data.Metacard;
import ddf.catalog.util.Describable;
import ddf.catalog.validation.MetacardValidator;
import ddf.catalog.validation.ValidationException;
import ddf.catalog.validation.impl.ValidationExceptionImpl;

public class SampleMetacardValidator implements MetacardValidator, Describable {

    private static String[] validWords = new String[] {"test", "default", "sample"};

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getId() {
        return "sample-validator";
    }

    @Override
    public String getTitle() {
        return "Sample Metacard Validator";
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
        if (!checkMetacard(metacard.getTitle(), new ArrayList<>(Arrays.asList(validWords)))) {
            ValidationExceptionImpl validationException = new ValidationExceptionImpl(
                    "Metacard title does not contain any of: " + Arrays.toString(validWords));
            validationException.setErrors(new ArrayList<String>(Arrays.asList("sampleError")));
            validationException.setWarnings(new ArrayList<String>(Arrays.asList("sampleWarnings")));
            throw validationException;
        }

    }

    private Boolean checkMetacard(String metacardAttribute, ArrayList<String> toCheck) {
        List<String> result = toCheck.stream()
                .filter(metacardAttribute::contains)
                .collect(Collectors.toList());
        return !result.isEmpty();
    }

    public void setValidWords(String... validWords) {
        SampleMetacardValidator.validWords = validWords;
    }

    public String[] getValidWords() {
        String[] resultValidWords = null;
        if (validWords != null) {
            resultValidWords = Arrays.copyOf(validWords, validWords.length);
        }
        return resultValidWords;
    }
}
