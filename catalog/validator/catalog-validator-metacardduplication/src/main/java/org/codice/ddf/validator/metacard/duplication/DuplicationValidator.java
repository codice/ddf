/*
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
package org.codice.ddf.validator.metacard.duplication;

import com.google.common.base.Preconditions;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.types.Validation;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PreIngestPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.validation.ValidationException;
import ddf.catalog.validation.impl.ValidationExceptionImpl;
import ddf.catalog.validation.impl.report.MetacardValidationReportImpl;
import ddf.catalog.validation.impl.violation.ValidationViolationImpl;
import ddf.catalog.validation.report.MetacardValidationReport;
import ddf.catalog.validation.violation.ValidationViolation;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DuplicationValidator
    implements PreIngestPlugin,
        ddf.catalog.util.Describable,
        org.codice.ddf.platform.services.common.Describable {
  private static final Logger LOGGER = LoggerFactory.getLogger(DuplicationValidator.class);

  private static final String DESCRIBABLE_PROPERTIES_FILE = "/describable.properties";

  private static final String ORGANIZATION = "organization";

  private static final String VERSION = "version";

  private static final String INVALID_TAG = "INVALID";

  private static final String VALID_TAG = "VALID";

  private boolean enforceErrors = true;

  private boolean enforceWarnings = true;

  private List<String> enforcedMetacardValidators;

  private static Properties describableProperties = new Properties();

  static {
    try (InputStream properties =
        DuplicationValidator.class.getResourceAsStream(DESCRIBABLE_PROPERTIES_FILE)) {
      describableProperties.load(properties);
    } catch (IOException e) {
      LOGGER.info("Failed to load properties", e);
    }
  }

  private final CatalogFramework catalogFramework;

  private final FilterBuilder filterBuilder;

  private String[] errorOnDuplicateAttributes;

  private String[] warnOnDuplicateAttributes;

  private String[] rejectOnDuplicateAttributes;

  public DuplicationValidator(CatalogFramework catalogFramework, FilterBuilder filterBuilder) {
    this.catalogFramework = catalogFramework;
    this.filterBuilder = filterBuilder;
  }

  /**
   * Setter for the list of attributes to test for duplication in the local catalog. Resulting
   * attributes will cause the {@link ddf.catalog.data.types.Validation#VALIDATION_ERRORS} attribute
   * to be set on the metacard.
   *
   * @param attributeStrings
   */
  public void setErrorOnDuplicateAttributes(String[] attributeStrings) {
    if (attributeStrings != null) {
      this.errorOnDuplicateAttributes = Arrays.copyOf(attributeStrings, attributeStrings.length);
    }
  }

  /**
   * Setter for the list of attributes to test for duplication in the local catalog. Resulting
   * attributes will cause the {@link ddf.catalog.data.types.Validation#VALIDATION_WARNINGS}
   * attribute to be set on the metacard.
   *
   * @param attributeStrings
   */
  public void setWarnOnDuplicateAttributes(String[] attributeStrings) {
    if (attributeStrings != null) {
      this.warnOnDuplicateAttributes = Arrays.copyOf(attributeStrings, attributeStrings.length);
    }
  }

  /**
   * Setter for the list of attributes to test for duplication in the local catalog. Resulting
   * attributes will cause the metacard to be rejected.
   *
   * @param attributeStrings
   */
  public void setRejectOnDuplicateAttributes(String[] attributeStrings) {
    if (attributeStrings != null) {
      this.rejectOnDuplicateAttributes = Arrays.copyOf(attributeStrings, attributeStrings.length);
    }
  }

  public Optional<MetacardValidationReport> validateMetacard(Metacard metacard)
      throws StopProcessingException {
    Preconditions.checkArgument(metacard != null, "The metacard cannot be null.");

    return getReport(reportDuplicates(metacard));
  }

  public void validate(Metacard metacard) throws ValidationException, StopProcessingException {

    final Optional<MetacardValidationReport> report = validateMetacard(metacard);

    if (report.isPresent()) {
      final List<String> errors =
          report
              .get()
              .getMetacardValidationViolations()
              .stream()
              .filter(
                  validationViolation ->
                      validationViolation.getSeverity().equals(ValidationViolation.Severity.ERROR))
              .map(ValidationViolation::getMessage)
              .collect(Collectors.toList());
      final List<String> warnings =
          report
              .get()
              .getMetacardValidationViolations()
              .stream()
              .filter(
                  validationViolation ->
                      validationViolation
                          .getSeverity()
                          .equals(ValidationViolation.Severity.WARNING))
              .map(ValidationViolation::getMessage)
              .collect(Collectors.toList());

      String message =
          String.format("Duplicate data found in catalog for ID {%s}.", metacard.getId());
      final ValidationExceptionImpl exception = new ValidationExceptionImpl(message);
      exception.setErrors(errors);
      exception.setWarnings(warnings);
      throw exception;
    }
  }

  private Set<ValidationViolation> reportDuplicates(final Metacard metacard)
      throws StopProcessingException {

    Set<ValidationViolation> violations = new HashSet<>();

    if (ArrayUtils.isNotEmpty(rejectOnDuplicateAttributes)) {
      reportDuplicates(
          metacard, rejectOnDuplicateAttributes, ValidationViolation.Severity.ERROR, true);
    }
    if (ArrayUtils.isNotEmpty(warnOnDuplicateAttributes)) {
      ValidationViolation warnValidation =
          reportDuplicates(
              metacard, warnOnDuplicateAttributes, ValidationViolation.Severity.WARNING, false);
      if (warnValidation != null) {
        violations.add(warnValidation);
      }
    }
    if (ArrayUtils.isNotEmpty(errorOnDuplicateAttributes)) {
      ValidationViolation errorViolation =
          reportDuplicates(
              metacard, errorOnDuplicateAttributes, ValidationViolation.Severity.ERROR, false);
      if (errorViolation != null) {
        violations.add(errorViolation);
      }
    }

    return violations;
  }

  private ValidationViolation reportDuplicates(
      final Metacard metacard,
      String[] attributeNames,
      ValidationViolation.Severity severity,
      Boolean rejectMetacard)
      throws StopProcessingException {

    Set<String> duplicates = new HashSet<>();
    ValidationViolation violation = null;

    final Set<String> uniqueAttributeNames =
        Stream.of(attributeNames)
            .filter(attribute -> metacard.getAttribute(attribute) != null)
            .collect(Collectors.toSet());
    final Set<Attribute> uniqueAttributes =
        uniqueAttributeNames.stream().map(metacard::getAttribute).collect(Collectors.toSet());
    if (!uniqueAttributes.isEmpty()) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            "Checking for duplicates for id {} against attributes [{}]",
            metacard.getId(),
            collectionToString(uniqueAttributeNames));
      }

      SourceResponse response = query(uniqueAttributes);
      if (response != null) {
        response
            .getResults()
            .stream()
            .filter(result -> !result.getMetacard().getId().equals(metacard.getId()))
            .forEach(result -> duplicates.add(result.getMetacard().getId()));
      }
      if (!duplicates.isEmpty()) {
        if (rejectMetacard) {
          LOGGER.debug("Rejecting duplicate metacard.");
          throw new StopProcessingException("Rejecting duplicate metacard.");
        } else {
          violation = createViolation(uniqueAttributeNames, duplicates, severity);
          LOGGER.debug(violation.getMessage());
        }
      }
    }
    return violation;
  }

  private Filter[] buildFilters(Set<Attribute> attributes) {

    return attributes
        .stream()
        .flatMap(
            attribute ->
                attribute
                    .getValues()
                    .stream()
                    .map(
                        value ->
                            filterBuilder
                                .attribute(attribute.getName())
                                .equalTo()
                                .text(value.toString().trim())))
        .toArray(Filter[]::new);
  }

  private SourceResponse query(Set<Attribute> attributes) {

    final Filter filter = filterBuilder.allOf(filterBuilder.anyOf(buildFilters(attributes)));

    LOGGER.debug("filter {}", filter);

    QueryImpl query = new QueryImpl(filter);
    query.setRequestsTotalResultsCount(false);
    QueryRequest request = new QueryRequestImpl(query);

    SourceResponse response = null;
    try {
      response = catalogFramework.query(request);
    } catch (FederationException | SourceUnavailableException | UnsupportedQueryException e) {
      LOGGER.debug("Query failed ", e);
    }
    return response;
  }

  private ValidationViolation createViolation(
      final Set<String> attributes, Set<String> duplicates, ValidationViolation.Severity severity) {

    return new ValidationViolationImpl(
        attributes,
        String.format(
            "Duplicate data found in catalog: {%s}, based on attributes: {%s}.",
            collectionToString(duplicates), collectionToString(attributes)),
        severity);
  }

  private String collectionToString(final Collection collection) {

    return (String)
        collection.stream().map(Object::toString).sorted().collect(Collectors.joining(", "));
  }

  private Optional<MetacardValidationReport> getReport(final Set<ValidationViolation> violations) {
    if (CollectionUtils.isNotEmpty(violations)) {
      final MetacardValidationReportImpl report = new MetacardValidationReportImpl();
      violations.forEach(report::addMetacardViolation);
      return Optional.of(report);
    }

    return Optional.empty();
  }

  @Override
  public String getVersion() {
    return describableProperties.getProperty(VERSION);
  }

  @Override
  public String getId() {
    return this.getClass().getSimpleName();
  }

  @Override
  public String getTitle() {
    return this.getClass().getSimpleName();
  }

  @Override
  public String getDescription() {
    return "Checks metacard against the local catalog for duplicates based on configurable attributes.";
  }

  @Override
  public String getOrganization() {
    return describableProperties.getProperty(ORGANIZATION);
  }

  public void checkForDuplicates(Metacard metacard) throws StopProcessingException {
    Set<Serializable> newErrors = new HashSet<>();
    Set<Serializable> newWarnings = new HashSet<>();
    Set<Serializable> errorValidators = new HashSet<>();
    Set<Serializable> warningValidators = new HashSet<>();
    Map<String, Integer> counter = new HashMap<>();
    String validatorName = getClass().getCanonicalName();
    Set<String> tags = metacard.getTags();
    tags.remove(VALID_TAG);
    tags.remove(INVALID_TAG);
    String valid = VALID_TAG;

    try {
      validate(metacard);
    } catch (ValidationException e) {

      boolean validationErrorsExist = CollectionUtils.isNotEmpty(e.getErrors());
      boolean validationWarningsExist = CollectionUtils.isNotEmpty(e.getWarnings());

      if ((isValidatorEnforced(validatorName) && validationErrorsExist && enforceErrors)
          || isValidatorEnforced(validatorName) && validationWarningsExist && enforceWarnings) {
        LOGGER.debug(
            "The metacard with id={} is being removed from the operation because it failed the enforced validator [{}].",
            metacard.getId(),
            validatorName);
      } else {
        getValidationProblems(
            validatorName, e, newErrors, newWarnings, errorValidators, warningValidators, counter);
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
    metacard.setAttribute(new AttributeImpl(Metacard.TAGS, new ArrayList<String>(tags)));

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

  private boolean isValidatorEnforced(String validatorName) {
    return enforcedMetacardValidators != null && enforcedMetacardValidators.contains(validatorName);
  }

  public void setEnforcedMetacardValidators(List<String> enforcedMetacardValidators) {
    this.enforcedMetacardValidators = enforcedMetacardValidators;
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

  @Override
  public CreateRequest process(CreateRequest input)
      throws PluginExecutionException, StopProcessingException {

    List<Metacard> metacards = input.getMetacards();
    Iterator<Metacard> iter = metacards.iterator();

    while (iter.hasNext()) {
      Metacard currCard = iter.next();
      try {
        checkForDuplicates(currCard);
      } catch (StopProcessingException e) {
        iter.remove();
        LOGGER.debug("Rejecting duplicate metacard.", e);
        throw new StopProcessingException("Rejecting duplicate metacard.");
      }
    }

    return input;
  }

  @Override
  public UpdateRequest process(UpdateRequest input)
      throws PluginExecutionException, StopProcessingException {
    for (Entry<Serializable, Metacard> entry : input.getUpdates()) {
      checkForDuplicates(entry.getValue());
    }
    return input;
  }

  @Override
  public DeleteRequest process(DeleteRequest input)
      throws PluginExecutionException, StopProcessingException {
    // No operation for delete
    return input;
  }
}
