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

package ddf.catalog.metacard.validation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PreIngestPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.util.Describable;
import ddf.catalog.validation.MetacardValidator;
import ddf.catalog.validation.ValidationException;

public class MetacardValidityMarkerPlugin implements PreIngestPlugin {

    private List<String> enforcedMetacardValidators;

    private List<MetacardValidator> metacardValidators;

    private static final Logger LOGGER = LoggerFactory
            .getLogger(MetacardValidityMarkerPlugin.class);

    public static final String VALIDATION_ERRORS = BasicTypes.VALIDATION_ERRORS;

    public static final String VALIDATION_WARNINGS = BasicTypes.VALIDATION_WARNINGS;

    public CreateRequest process(CreateRequest input)
            throws PluginExecutionException, StopProcessingException {

        // Initialize empty list of metacards to allow through
        List<Metacard> returnMetacards = new ArrayList<Metacard>();
        // Loop all metacards in request
        for (Metacard metacard : input.getMetacards()) {
            MetacardImpl newMetacard = new MetacardImpl(metacard);
            List<Serializable> validationWarnings = new LinkedList<>();
            List<Serializable> validationErrors = new LinkedList<>();
            // Default to allowing metacard through
            Boolean toReturn = true;
            // Run metacard through each validator
            for (MetacardValidator metacardValidator : metacardValidators) {
                try {
                    // Attempt validation
                    metacardValidator.validate(metacard);
                } catch (ValidationException e) {
                    // If validator is not explicitly turned on by admin, set invalid and allow through
                    if (checkEnforcedMetacardValidators(metacardValidator)) {
                        Boolean validationErrorsExist = !e.getErrors().isEmpty();
                        Boolean validationWarningsExist = !e.getWarnings().isEmpty();
                        if (validationErrorsExist || validationWarningsExist) {
                            // Check for warnings and errors
                            if (validationErrorsExist) {
                                validationErrors.add(getValidatorName(metacardValidator));
                            }
                            if (validationWarningsExist) {
                                validationWarnings.add(getValidatorName(metacardValidator));
                            }
                        } else {
                            LOGGER.error(
                                    "Metacard validator {} did not have any warnings or errors but it threw a validation exception."
                                            + " There is likely something wrong with your implementation. This will result in the metacard not"
                                            + " being properly marked as invalid.",
                                    getValidatorName(metacardValidator));
                        }

                    } else {
                        // If validator is explicitly turned on, break out and do not include in the
                        // list of metacards that will be allowed through.
                        toReturn = false;
                        break;
                    }

                }
            }
            if (toReturn) {
                newMetacard
                        .setAttribute(new AttributeImpl(VALIDATION_WARNINGS, validationWarnings));
                newMetacard.setAttribute(new AttributeImpl(VALIDATION_ERRORS, validationErrors));
                returnMetacards.add(newMetacard);
            }
        }
        return new CreateRequestImpl(returnMetacards, input.getProperties());
    }

    @Override
    public UpdateRequest process(UpdateRequest input)
            throws PluginExecutionException, StopProcessingException {
        return input;
    }

    @Override
    public DeleteRequest process(DeleteRequest input)
            throws PluginExecutionException, StopProcessingException {
        return input;
    }

    public void setEnforcedMetacardValidators(List<String> enforcedMetacardValidators) {
        this.enforcedMetacardValidators = enforcedMetacardValidators;
    }

    public List<String> getEnforcedMetacardValidators() {
        return this.enforcedMetacardValidators;
    }

    public void setMetacardValidators(List<MetacardValidator> metacardValidators) {
        this.metacardValidators = metacardValidators;
    }

    public List<MetacardValidator> getMetacardValidators() {
        return this.metacardValidators;
    }

    private Boolean checkEnforcedMetacardValidators(MetacardValidator metacardValidator) {
        return (null == enforcedMetacardValidators || !enforcedMetacardValidators
                .contains(getValidatorName(metacardValidator)));
    }

    private String getValidatorName(MetacardValidator metacardValidator) {
        if (metacardValidator instanceof Describable) {
            return ((Describable) metacardValidator).getId();
        } else {
            String canonicalName = metacardValidator.getClass().getCanonicalName();
            LOGGER.warn("Metacard validators SHOULD implement Describable. Validator in error: {}",
                    canonicalName);
            return canonicalName;
        }
    }
}
