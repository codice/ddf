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

import com.google.common.base.Preconditions;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.validation.MetacardValidator;
import ddf.catalog.validation.ReportingMetacardValidator;
import ddf.catalog.validation.ValidationException;
import ddf.catalog.validation.impl.ValidationExceptionImpl;
import ddf.catalog.validation.impl.report.MetacardValidationReportImpl;
import ddf.catalog.validation.impl.violation.ValidationViolationImpl;
import ddf.catalog.validation.report.MetacardValidationReport;
import ddf.catalog.validation.violation.ValidationViolation;
import ddf.catalog.validation.violation.ValidationViolation.Severity;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;

/** Validates that a {@link Metacard} contains certain {@link Attribute}s. */
public class RequiredAttributesMetacardValidator
    implements MetacardValidator, ReportingMetacardValidator {
  private final String metacardTypeName;

  private final Set<String> requiredAttributes;

  /**
   * Creates a {@code RequiredAttributesMetacardValidator} with the given metacard type name and set
   * of attribute names representing the required attributes.
   *
   * <p>This validator will only validate {@link Metacard}s that have the type name specified by
   * {@code metacardTypeName} (case-sensitive).
   *
   * <p>Any missing required attributes will be flagged as metacard-level validation errors.
   *
   * @param metacardTypeName the name of the metacard type this validator can validate, cannot be
   *     null
   * @param requiredAttributes the names of the attributes this validator will check for, cannot be
   *     null or empty
   * @throws IllegalArgumentException if {@code metacardTypeName} is null or if {@code
   *     requiredAttributes} is null or empty
   */
  public RequiredAttributesMetacardValidator(
      final String metacardTypeName, final Set<String> requiredAttributes) {
    Preconditions.checkArgument(metacardTypeName != null, "The metacard type name cannot be null.");
    Preconditions.checkArgument(
        CollectionUtils.isNotEmpty(requiredAttributes),
        "Must specify at least one required attribute.");

    this.metacardTypeName = metacardTypeName;
    this.requiredAttributes =
        requiredAttributes.stream().filter(Objects::nonNull).collect(Collectors.toSet());
  }

  @Override
  public void validate(final Metacard metacard) throws ValidationException {
    final Optional<MetacardValidationReport> reportOptional = validateMetacard(metacard);

    if (reportOptional.isPresent()) {
      final List<String> errors =
          reportOptional
              .get()
              .getMetacardValidationViolations()
              .stream()
              .map(ValidationViolation::getMessage)
              .collect(Collectors.toList());

      final ValidationExceptionImpl exception = new ValidationExceptionImpl();
      exception.setErrors(errors);
      throw exception;
    }
  }

  @Override
  public Optional<MetacardValidationReport> validateMetacard(final Metacard metacard) {
    Preconditions.checkArgument(metacard != null, "The metacard cannot be null.");

    final MetacardType metacardType = metacard.getMetacardType();

    if (metacardTypeName.equals(metacardType.getName())) {
      final Set<ValidationViolation> violations = new HashSet<>();

      for (final String attributeName : requiredAttributes) {
        final Attribute attribute = metacard.getAttribute(attributeName);
        if (attribute != null) {
          final AttributeDescriptor descriptor = metacardType.getAttributeDescriptor(attributeName);
          if (descriptor.isMultiValued()) {
            if (attribute.getValues().size() == 0) {
              addRequiredAttributeViolation(attributeName, violations);
            }
          } else if (attribute.getValue() == null) {
            addRequiredAttributeViolation(attributeName, violations);
          }
        } else {
          addRequiredAttributeViolation(attributeName, violations);
        }
      }

      if (!violations.isEmpty()) {
        return getReport(violations);
      }
    }

    return Optional.empty();
  }

  private void addRequiredAttributeViolation(
      final String attributeName, final Set<ValidationViolation> violations) {
    violations.add(
        new ValidationViolationImpl(
            Collections.singleton(attributeName), attributeName + " is required", Severity.ERROR));
  }

  private Optional<MetacardValidationReport> getReport(final Set<ValidationViolation> violations) {
    final MetacardValidationReportImpl report = new MetacardValidationReportImpl();
    violations.forEach(report::addMetacardViolation);
    return Optional.of(report);
  }
}
