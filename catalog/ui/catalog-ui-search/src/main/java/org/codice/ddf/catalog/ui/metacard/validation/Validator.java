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
package org.codice.ddf.catalog.ui.metacard.validation;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.validation.AttributeValidator;
import ddf.catalog.validation.AttributeValidatorRegistry;
import ddf.catalog.validation.ReportingMetacardValidator;
import ddf.catalog.validation.report.AttributeValidationReport;
import ddf.catalog.validation.report.MetacardValidationReport;
import ddf.catalog.validation.violation.ValidationViolation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Validator {
  private final List<ReportingMetacardValidator> validators;

  private final List<AttributeValidatorRegistry> attributeValidatorRegistry;

  public Validator(
      List<ReportingMetacardValidator> validators,
      List<AttributeValidatorRegistry> attributeValidatorRegistry) {
    this.validators = validators;
    this.attributeValidatorRegistry = attributeValidatorRegistry;
  }

  public List<ViolationResult> getValidation(Metacard metacard)
      throws SourceUnavailableException, UnsupportedQueryException, FederationException {
    Set<ValidationViolation> attributeValidationViolations =
        validators
            .stream()
            .map(v -> v.validateMetacard(metacard))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(MetacardValidationReport::getAttributeValidationViolations)
            .reduce(
                (left, right) -> {
                  HashSet<ValidationViolation> res = new HashSet<>();
                  res.addAll(left);
                  res.addAll(right);
                  return res;
                })
            .orElse(new HashSet<>());
    Map<String, ViolationResult> violationsResult =
        getViolationsResult(attributeValidationViolations);

    return violationsResult
        .entrySet()
        .stream()
        .map(Map.Entry::getValue)
        .collect(Collectors.toList());
  }

  public List<ValidationViolation> getFullValidation(Metacard metacard) {
    Set<ValidationViolation> attributeValidationViolations =
        validators
            .stream()
            .map(v -> v.validateMetacard(metacard))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(MetacardValidationReport::getMetacardValidationViolations)
            .reduce(
                (left, right) -> {
                  HashSet<ValidationViolation> res = new HashSet<>();
                  res.addAll(left);
                  res.addAll(right);
                  return res;
                })
            .orElse(new HashSet<>());
    return new ArrayList<>(attributeValidationViolations);
  }

  public AttributeValidationResponse validateAttribute(String attribute, String value) {
    Set<AttributeValidator> attributeValidators =
        attributeValidatorRegistry
            .stream()
            .map(avr -> avr.getValidators(attribute))
            .reduce(
                (left, right) -> {
                  left.addAll(right);
                  return left;
                })
            .orElse(new HashSet<>());

    Set<Map<String, String>> suggestedValues = new HashSet<>();
    Set<ValidationViolation> violations = new HashSet<>();
    for (AttributeValidator validator : attributeValidators) {
      Optional<AttributeValidationReport> validationReport =
          validator.validate(new AttributeImpl(attribute, value));
      if (validationReport.isPresent()) {
        AttributeValidationReport report = validationReport.get();
        if (!report.getSuggestedValues().isEmpty()) {
          suggestedValues = report.getSuggestedValues();
        }
        violations.addAll(report.getAttributeValidationViolations());
      }
    }

    return new AttributeValidationResponse(violations, suggestedValues);
  }

  private Map<String, ViolationResult> getViolationsResult(
      Set<ValidationViolation> attributeValidationViolations) {
    Map<String, ViolationResult> violationsResult = new HashMap<>();
    for (ValidationViolation violation : attributeValidationViolations) {
      for (String attribute : violation.getAttributes()) {
        if (!violationsResult.containsKey(attribute)) {
          violationsResult.put(attribute, new ViolationResult());
        }
        ViolationResult violationResponse = violationsResult.get(attribute);
        violationResponse.setAttribute(attribute);

        if (ValidationViolation.Severity.ERROR.equals(violation.getSeverity())) {
          violationResponse.getErrors().add(violation.getMessage());
        } else if (ValidationViolation.Severity.WARNING.equals(violation.getSeverity())) {
          violationResponse.getWarnings().add(violation.getMessage());
        } else {
          throw new IllegalArgumentException("Unexpected Severity Level");
        }
      }
    }
    return violationsResult;
  }
}
