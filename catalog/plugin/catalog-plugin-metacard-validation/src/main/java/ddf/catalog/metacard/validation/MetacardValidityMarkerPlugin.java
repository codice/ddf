/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.metacard.validation;

import ddf.catalog.Constants;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.types.Validation;
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

public class MetacardValidityMarkerPlugin implements PreIngestPlugin {
  private static final Logger LOGGER = LoggerFactory.getLogger(MetacardValidityMarkerPlugin.class);

  private static final Logger INGEST_LOGGER = LoggerFactory.getLogger(Constants.INGEST_LOGGER_NAME);

  private final Predicate<Object> didNotFailEnforcedValidator = Objects::nonNull;

  private static final String INVALID_TAG = "INVALID";

  private static final String VALID_TAG = "VALID";

  private List<String> enforcedMetacardValidators;

  private List<MetacardValidator> metacardValidators;

  private boolean enforceErrors = true;

  private boolean enforceWarnings = true;

  @Override
  public CreateRequest process(CreateRequest input)
      throws PluginExecutionException, StopProcessingException {
    List<Metacard> validatedMetacards = validateList(input.getMetacards(), Function.identity());
    return new CreateRequestImpl(validatedMetacards, input.getProperties(), input.getStoreIds());
  }

  @Override
  public UpdateRequest process(UpdateRequest input)
      throws PluginExecutionException, StopProcessingException {
    List<Map.Entry<Serializable, Metacard>> validatedUpdates =
        validateList(input.getUpdates(), Map.Entry::getValue);
    return new UpdateRequestImpl(
        validatedUpdates, input.getAttributeName(), input.getProperties(), input.getStoreIds());
  }

  private <T> List<T> validateList(List<T> requestItems, Function<T, Metacard> itemToMetacard) {
    Map<String, Integer> counter = new HashMap<>();

    List<T> validated =
        requestItems
            .stream()
            .map(item -> validate(item, itemToMetacard, counter))
            .filter(didNotFailEnforcedValidator)
            .collect(Collectors.toList());

    return validated;
  }

  private <T> T validate(
      T item, Function<T, Metacard> itemToMetacard, Map<String, Integer> counter) {
    Set<Serializable> newErrors = new HashSet<>();
    Set<Serializable> newWarnings = new HashSet<>();
    Set<Serializable> errorValidators = new HashSet<>();
    Set<Serializable> warningValidators = new HashSet<>();

    Metacard metacard = itemToMetacard.apply(item);
    Set<String> tags = metacard.getTags();
    tags.remove(VALID_TAG);
    tags.remove(INVALID_TAG);

    String valid = VALID_TAG;

    for (MetacardValidator validator : metacardValidators) {
      try {
        validator.validate(metacard);
      } catch (ValidationException e) {
        String validatorName = getValidatorName(validator);
        boolean validationErrorsExist = CollectionUtils.isNotEmpty(e.getErrors());
        boolean validationWarningsExist = CollectionUtils.isNotEmpty(e.getWarnings());

        if ((isValidatorEnforced(validatorName) && validationErrorsExist && enforceErrors)
            || isValidatorEnforced(validatorName) && validationWarningsExist && enforceWarnings) {
          INGEST_LOGGER.debug(
              "The metacard with title='{}' and id={} is being removed from the operation because it failed the enforced validator [{}].",
              metacard.getTitle(),
              metacard.getId(),
              validatorName,
              e.getMessage());
          return null;
        } else {
          if (validationErrorsExist) {
            INGEST_LOGGER.debug(
                "The metacard with title='{}' and id={} had an unenforced validation error [{}] and error message='{}'.",
                metacard.getTitle(),
                metacard.getId(),
                validatorName,
                e.getMessage());
          }
          if (validationWarningsExist) {
            INGEST_LOGGER.debug(
                "The metacard with title='{}' and id={} had an unenforced validation warning [{}] and warning message ='{}'.",
                metacard.getTitle(),
                metacard.getId(),
                validatorName,
                e.getMessage());
          }
          if (validationErrorsExist || validationWarningsExist) {
            INGEST_LOGGER.info(
                "The metacard with title='{}' and id={} had {} unenforced validation warnings and {} unenforced validation errors.",
                metacard.getTitle(),
                metacard.getId(),
                e.getWarnings() != null ? e.getWarnings().size() : 0,
                e.getErrors() != null ? e.getErrors().size() : 0);
          }

          getValidationProblems(
              validatorName,
              e,
              newErrors,
              newWarnings,
              errorValidators,
              warningValidators,
              counter);
        }
      }
    }

    Attribute existingErrors = metacard.getAttribute(Validation.VALIDATION_ERRORS);
    Attribute existingWarnings = metacard.getAttribute(Validation.VALIDATION_WARNINGS);

    if (existingErrors != null) {
      newErrors.addAll(existingErrors.getValues());
    }

    if (existingWarnings != null) {
      newWarnings.addAll(existingWarnings.getValues());
    }

    if (!newErrors.isEmpty() || !newWarnings.isEmpty()) {
      valid = INVALID_TAG;
    }

    tags.add(valid);
    metacard.setAttribute(new AttributeImpl(Metacard.TAGS, (List<String>) new ArrayList<>(tags)));

    metacard.setAttribute(
        new AttributeImpl(
            Validation.VALIDATION_ERRORS, (List<Serializable>) new ArrayList<>(newErrors)));
    metacard.setAttribute(
        new AttributeImpl(
            Validation.VALIDATION_WARNINGS, (List<Serializable>) new ArrayList<>(newWarnings)));
    metacard.setAttribute(
        new AttributeImpl(
            Validation.FAILED_VALIDATORS_WARNINGS,
            (List<Serializable>) new ArrayList<>(warningValidators)));
    metacard.setAttribute(
        new AttributeImpl(
            Validation.FAILED_VALIDATORS_ERRORS,
            (List<Serializable>) new ArrayList<>(errorValidators)));

    return item;
  }

  private void getValidationProblems(
      String validatorName,
      ValidationException e,
      Set<Serializable> errors,
      Set<Serializable> warnings,
      Set<Serializable> errorValidators,
      Set<Serializable> warningValidators,
      Map<String, Integer> counter) {
    boolean validationErrorsExist = CollectionUtils.isNotEmpty(e.getErrors());
    boolean validationWarningsExist = CollectionUtils.isNotEmpty(e.getWarnings());
    if (validationErrorsExist || validationWarningsExist) {
      if (validationErrorsExist) {
        errors.addAll(e.getErrors());
        errorValidators.add(validatorName);
        counter.merge(Validation.VALIDATION_ERRORS, 1, Integer::sum);
      }
      if (validationWarningsExist) {
        warnings.addAll(e.getWarnings());
        warningValidators.add(validatorName);
        counter.merge(Validation.VALIDATION_WARNINGS, 1, Integer::sum);
      }
    } else {
      LOGGER.debug(
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

  public List<String> getEnforcedMetacardValidators() {
    return enforcedMetacardValidators;
  }

  public void setEnforcedMetacardValidators(List<String> enforcedMetacardValidators) {
    this.enforcedMetacardValidators = enforcedMetacardValidators;
  }

  public List<MetacardValidator> getMetacardValidators() {
    return metacardValidators;
  }

  public void setMetacardValidators(List<MetacardValidator> metacardValidators) {
    this.metacardValidators = metacardValidators;

    List<String> validatorsNoDescribable =
        metacardValidators
            .stream()
            .filter(validator -> !(validator instanceof Describable))
            .map(this::getValidatorName)
            .collect(Collectors.toList());

    if (!validatorsNoDescribable.isEmpty()) {
      LOGGER.debug(
          "Metacard validators SHOULD implement Describable. Validators in error: {}",
          validatorsNoDescribable);
    }
  }

  private boolean isValidatorEnforced(String validatorName) {
    return enforcedMetacardValidators != null && enforcedMetacardValidators.contains(validatorName);
  }

  protected String getValidatorName(MetacardValidator metacardValidator) {
    if (metacardValidator instanceof Describable) {
      return ((Describable) metacardValidator).getId();
    } else {
      return metacardValidator.getClass().getCanonicalName();
    }
  }

  public void setEnforceErrors(boolean enforceErrors) {
    this.enforceErrors = enforceErrors;
  }

  public boolean getEnforceErrors() {
    return enforceErrors;
  }

  public void setEnforceWarnings(boolean enforceWarnings) {
    this.enforceWarnings = enforceWarnings;
  }

  public boolean getEnforceWarnings() {
    return enforceWarnings;
  }
}
