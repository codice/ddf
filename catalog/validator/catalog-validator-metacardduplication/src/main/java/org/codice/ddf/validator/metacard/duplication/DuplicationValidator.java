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
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.validation.MetacardValidator;
import ddf.catalog.validation.ReportingMetacardValidator;
import ddf.catalog.validation.ValidationException;
import ddf.catalog.validation.impl.ValidationExceptionImpl;
import ddf.catalog.validation.impl.report.MetacardValidationReportImpl;
import ddf.catalog.validation.impl.violation.ValidationViolationImpl;
import ddf.catalog.validation.report.MetacardValidationReport;
import ddf.catalog.validation.violation.ValidationViolation;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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
    implements MetacardValidator,
        ReportingMetacardValidator,
        ddf.catalog.util.Describable,
        org.codice.ddf.platform.services.common.Describable {
  private static final Logger LOGGER = LoggerFactory.getLogger(DuplicationValidator.class);

  private static final String DESCRIBABLE_PROPERTIES_FILE = "/describable.properties";

  private static final String ORGANIZATION = "organization";

  private static final String VERSION = "version";

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

  @Override
  public Optional<MetacardValidationReport> validateMetacard(Metacard metacard) {
    Preconditions.checkArgument(metacard != null, "The metacard cannot be null.");

    return getReport(reportDuplicates(metacard));
  }

  @Override
  public void validate(Metacard metacard) throws ValidationException {

    final Optional<MetacardValidationReport> report = validateMetacard(metacard);

    if (report.isPresent()) {
      final List<String> errors =
          report.get().getMetacardValidationViolations().stream()
              .filter(
                  validationViolation ->
                      validationViolation.getSeverity().equals(ValidationViolation.Severity.ERROR))
              .map(ValidationViolation::getMessage)
              .collect(Collectors.toList());
      final List<String> warnings =
          report.get().getMetacardValidationViolations().stream()
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

  private Set<ValidationViolation> reportDuplicates(final Metacard metacard) {

    Set<ValidationViolation> violations = new HashSet<>();

    if (ArrayUtils.isNotEmpty(warnOnDuplicateAttributes)) {
      ValidationViolation warnValidation =
          reportDuplicates(
              metacard, warnOnDuplicateAttributes, ValidationViolation.Severity.WARNING);
      if (warnValidation != null) {
        violations.add(warnValidation);
      }
    }
    if (ArrayUtils.isNotEmpty(errorOnDuplicateAttributes)) {
      ValidationViolation errorViolation =
          reportDuplicates(
              metacard, errorOnDuplicateAttributes, ValidationViolation.Severity.ERROR);
      if (errorViolation != null) {
        violations.add(errorViolation);
      }
    }

    return violations;
  }

  private ValidationViolation reportDuplicates(
      final Metacard metacard, String[] attributeNames, ValidationViolation.Severity severity) {

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
        response.getResults().stream()
            .filter(result -> !result.getMetacard().getId().equals(metacard.getId()))
            .forEach(result -> duplicates.add(result.getMetacard().getId()));
      }
      if (!duplicates.isEmpty()) {
        violation = createViolation(uniqueAttributeNames, duplicates, severity);
        LOGGER.debug(violation.getMessage());
      }
    }
    return violation;
  }

  private Filter[] buildFilters(Set<Attribute> attributes) {

    return attributes.stream()
        .flatMap(
            attribute ->
                attribute.getValues().stream()
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
}
