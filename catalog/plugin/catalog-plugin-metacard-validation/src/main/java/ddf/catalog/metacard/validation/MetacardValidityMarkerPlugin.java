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

import static ddf.catalog.data.impl.BasicTypes.VALIDATION_ERRORS;
import static ddf.catalog.data.impl.BasicTypes.VALIDATION_WARNINGS;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.Constants;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PreIngestPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.util.Describable;
import ddf.catalog.validation.MetacardValidator;
import ddf.catalog.validation.ValidationException;

public class MetacardValidityMarkerPlugin implements PreIngestPlugin {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(MetacardValidityMarkerPlugin.class);

    private static final Logger INGEST_LOGGER =
            LoggerFactory.getLogger(Constants.INGEST_LOGGER_NAME);

    private final Predicate<Object> didNotFailEnforcedValidator = Objects::nonNull;

    private List<String> enforcedMetacardValidators;

    private List<MetacardValidator> metacardValidators;

    @Override
    public CreateRequest process(CreateRequest input)
            throws PluginExecutionException, StopProcessingException {
        List<Metacard> validatedMetacards = validateList(input.getMetacards(), Function.identity());
        return new CreateRequestImpl(validatedMetacards,
                input.getProperties(),
                input.getStoreIds());
    }

    @Override
    public UpdateRequest process(UpdateRequest input)
            throws PluginExecutionException, StopProcessingException {
        List<Map.Entry<Serializable, Metacard>> validatedUpdates = validateList(input.getUpdates(),
                Map.Entry::getValue);
        return new UpdateRequestImpl(validatedUpdates,
                input.getAttributeName(),
                input.getProperties(),
                input.getStoreIds());
    }

    private <T> List<T> validateList(List<T> requestItems, Function<T, Metacard> itemToMetacard) {
        Map<String, Integer> counter = new HashMap<>();

        List<T> validated = requestItems.stream()
                .map(item -> validate(item, itemToMetacard, counter))
                .filter(didNotFailEnforcedValidator)
                .collect(Collectors.toList());

        INGEST_LOGGER.info("Validation results: {} had warnings and {} had errors.",
                counter.getOrDefault(VALIDATION_WARNINGS, 0),
                counter.getOrDefault(VALIDATION_ERRORS, 0));

        return validated;
    }

    private <T> T validate(T item, Function<T, Metacard> itemToMetacard,
            Map<String, Integer> counter) {
        Set<String> errors = new HashSet<>();
        Set<String> warnings = new HashSet<>();

        Metacard metacard = itemToMetacard.apply(item);
        for (MetacardValidator validator : metacardValidators) {
            try {
                validator.validate(metacard);
            } catch (ValidationException e) {
                String validatorName = getValidatorName(validator);

                if (isValidatorEnforced(validatorName)) {
                    INGEST_LOGGER.info(
                            "The metacard with id={} is being removed from the operation because it failed the enforced validator [{}].",
                            metacard.getId(),
                            validatorName);
                    return null;
                } else {
                    getValidationProblems(validatorName, e, errors, warnings, counter);
                }
            }
        }

        metacard.setAttribute(new AttributeImpl(VALIDATION_ERRORS,
                (List<Serializable>) new ArrayList<Serializable>(errors)));
        metacard.setAttribute(new AttributeImpl(VALIDATION_WARNINGS,
                (List<Serializable>) new ArrayList<Serializable>(warnings)));

        return item;
    }

    private void getValidationProblems(String validatorName, ValidationException e,
            Set<String> errors, Set<String> warnings, Map<String, Integer> counter) {
        boolean validationErrorsExist = CollectionUtils.isNotEmpty(e.getErrors());
        boolean validationWarningsExist = CollectionUtils.isNotEmpty(e.getWarnings());
        if (validationErrorsExist || validationWarningsExist) {
            if (validationErrorsExist) {
                errors.addAll(e.getErrors());
                counter.merge(VALIDATION_ERRORS, 1, Integer::sum);
            }
            if (validationWarningsExist) {
                warnings.addAll(e.getWarnings());
                counter.merge(VALIDATION_WARNINGS, 1, Integer::sum);
            }
        } else {
            LOGGER.warn(
                    "Metacard validator {} did not have any warnings or errors but it threw a validation exception."
                            + " There is likely something wrong with your implementation. This will result in the metacard not"
                            + " being properly marked as invalid.",
                    validatorName);
        }
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
        return enforcedMetacardValidators;
    }

    public void setMetacardValidators(List<MetacardValidator> metacardValidators) {
        this.metacardValidators = metacardValidators;

        List<String> validatorsNoDescribable = metacardValidators.stream()
                .filter(validator -> !(validator instanceof Describable))
                .map(this::getValidatorName)
                .collect(Collectors.toList());

        if (validatorsNoDescribable.size() > 0) {
            LOGGER.warn("Metacard validators SHOULD implement Describable. Validators in error: {}",
                    validatorsNoDescribable);
        }
    }

    public List<MetacardValidator> getMetacardValidators() {
        return metacardValidators;
    }

    private boolean isValidatorEnforced(String validatorName) {
        return enforcedMetacardValidators != null && enforcedMetacardValidators.contains(
                validatorName);
    }

    private String getValidatorName(MetacardValidator metacardValidator) {
        if (metacardValidator instanceof Describable) {
            return ((Describable) metacardValidator).getId();
        } else {
            return metacardValidator.getClass()
                    .getCanonicalName();
        }
    }
}
