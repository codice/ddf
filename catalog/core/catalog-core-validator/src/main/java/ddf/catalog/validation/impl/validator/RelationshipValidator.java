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
package ddf.catalog.validation.impl.validator;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.validation.MetacardValidator;
import ddf.catalog.validation.ReportingMetacardValidator;
import ddf.catalog.validation.ValidationException;
import ddf.catalog.validation.impl.ValidationExceptionImpl;
import ddf.catalog.validation.impl.report.MetacardValidationReportImpl;
import ddf.catalog.validation.impl.violation.ValidationViolationImpl;
import ddf.catalog.validation.report.MetacardValidationReport;
import ddf.catalog.validation.violation.ValidationViolation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;

public class RelationshipValidator implements ReportingMetacardValidator, MetacardValidator {
  private final Map<String, Function<Attribute, Optional<ValidationViolation>>> relationships =
      new HashMap<>();
  private final String sourceAttribute;
  private final String sourceValue;
  private final String relationship;
  private final String targetAttribute;
  private final List<String> targetValues;

  public RelationshipValidator(
      String sourceAttribute,
      String sourceValue,
      String relationship,
      String targetAttribute,
      String... targetValues) {
    relationships.put("mustHave", this::mustHave);
    relationships.put("cannotHave", this::cannotHave);
    relationships.put("canOnlyHave", this::canOnlyHave);
    this.sourceAttribute = sourceAttribute;
    this.sourceValue = sourceValue;
    this.relationship = relationship;
    this.targetAttribute = targetAttribute;
    if (targetValues != null) {
      this.targetValues = Arrays.asList(targetValues);
    } else {
      this.targetValues = Collections.emptyList();
    }
    if (!relationships.keySet().contains(relationship)) {
      throw new IllegalArgumentException(
          "Unrecognized relationship " + relationship + " for validator " + this.toString());
    }
  }

  @Override
  public Optional<MetacardValidationReport> validateMetacard(Metacard metacard) {
    Attribute attribute = metacard.getAttribute(sourceAttribute);
    if (attribute != null
        && attribute.getValues().stream().anyMatch(Objects::nonNull)
        && (StringUtils.isEmpty(sourceValue) || attribute.getValues().contains(sourceValue))) {
      MetacardValidationReportImpl report = new MetacardValidationReportImpl();
      Optional<ValidationViolation> violation =
          relationships.get(relationship).apply(metacard.getAttribute(targetAttribute));
      violation.ifPresent(report::addAttributeViolation);
      if (violation.isPresent()) {
        return Optional.of(report);
      } else {
        return Optional.empty();
      }
    } else {
      return Optional.empty();
    }
  }

  private Optional<ValidationViolation> mustHave(Attribute attribute) {
    return getValidationViolation(
        targetAttribute
            + " must have "
            + (targetValues.stream().anyMatch(StringUtils::isNotEmpty)
                ? "a value of " + getValuesAsString()
                : "a value")
            + ".",
        attribute == null
            || !attribute
                .getValues()
                .stream()
                .map(Objects::toString)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toCollection(ArrayList::new))
                .containsAll(targetValues));
  }

  private Optional<ValidationViolation> cannotHave(Attribute attribute) {
    return getValidationViolation(
        targetAttribute + " cannot be one of " + getValuesAsString(),
        attribute != null
            && attribute
                .getValues()
                .stream()
                .map(Objects::toString)
                .filter(StringUtils::isNotEmpty)
                .anyMatch(targetValues::contains));
  }

  private Optional<ValidationViolation> canOnlyHave(Attribute attribute) {
    return getValidationViolation(
        targetAttribute + " must be one of " + getValuesAsString(),
        attribute == null
            || !attribute
                .getValues()
                .stream()
                .map(Objects::toString)
                .filter(StringUtils::isNotEmpty)
                .allMatch(targetValues::contains));
  }

  private String getValuesAsString() {
    return targetValues.stream().map(Object::toString).collect(Collectors.joining(", "));
  }

  private Optional<ValidationViolation> getValidationViolation(String message, boolean isViolated) {
    if (isViolated) {
      return Optional.of(
          new ValidationViolationImpl(
              Collections.singleton(targetAttribute),
              "If "
                  + sourceAttribute
                  + (StringUtils.isNotEmpty(sourceValue) ? " is set to " + sourceValue : " is set")
                  + ", "
                  + message,
              ValidationViolation.Severity.ERROR));
    } else {
      return Optional.empty();
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RelationshipValidator that = (RelationshipValidator) o;
    return Objects.equals(relationship, that.relationship)
        && Objects.equals(targetAttribute, that.targetAttribute)
        && Objects.equals(targetValues, that.targetValues)
        && Objects.equals(sourceAttribute, that.sourceAttribute)
        && Objects.equals(sourceValue, that.sourceValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(relationship, targetAttribute, targetValues, sourceAttribute, sourceValue);
  }

  @Override
  public String toString() {
    return "RelationshipValidator{"
        + "sourceAttribute='"
        + sourceAttribute
        + '\''
        + ", sourceValue='"
        + sourceValue
        + '\''
        + ", relationship='"
        + relationship
        + '\''
        + ", targetAttribute='"
        + targetAttribute
        + '\''
        + ", targetValues="
        + targetValues
        + '\''
        + '}';
  }

  @Override
  public void validate(Metacard metacard) throws ValidationException {
    Optional<MetacardValidationReport> report = validateMetacard(metacard);
    if (report.isPresent() && !report.get().getAttributeValidationViolations().isEmpty()) {
      List<String> errors =
          report
              .get()
              .getAttributeValidationViolations()
              .stream()
              .filter(v -> v.getSeverity() == ValidationViolation.Severity.ERROR)
              .map(ValidationViolation::getMessage)
              .collect(Collectors.toList());
      errors.addAll(
          report
              .get()
              .getMetacardValidationViolations()
              .stream()
              .filter(v -> v.getSeverity() == ValidationViolation.Severity.ERROR)
              .map(ValidationViolation::getMessage)
              .collect(Collectors.toList()));
      List<String> warnings =
          report
              .get()
              .getAttributeValidationViolations()
              .stream()
              .filter(v -> v.getSeverity() == ValidationViolation.Severity.WARNING)
              .map(ValidationViolation::getMessage)
              .collect(Collectors.toList());
      errors.addAll(
          report
              .get()
              .getMetacardValidationViolations()
              .stream()
              .filter(v -> v.getSeverity() == ValidationViolation.Severity.WARNING)
              .map(ValidationViolation::getMessage)
              .collect(Collectors.toList()));
      throw new ValidationExceptionImpl(report.toString(), errors, warnings);
    }
  }
}
