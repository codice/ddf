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
package ddf.catalog.validation.impl;

import com.google.common.base.Preconditions;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.validation.AttributeValidator;
import ddf.catalog.validation.AttributeValidatorRegistry;
import ddf.catalog.validation.MetacardValidator;
import ddf.catalog.validation.ReportingMetacardValidator;
import ddf.catalog.validation.ValidationException;
import ddf.catalog.validation.impl.report.MetacardValidationReportImpl;
import ddf.catalog.validation.report.MetacardValidationReport;
import ddf.catalog.validation.violation.ValidationViolation;
import ddf.catalog.validation.violation.ValidationViolation.Severity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Default {@link Metacard} validator that validates all of a {@link Metacard}'s attributes using
 * the {@link AttributeValidator}s registered in the attribute validator registry.
 */
public class ReportingMetacardValidatorImpl
    implements MetacardValidator, ReportingMetacardValidator {
  private final AttributeValidatorRegistry validatorRegistry;

  public ReportingMetacardValidatorImpl(final AttributeValidatorRegistry validatorRegistry) {
    this.validatorRegistry = validatorRegistry;
  }

  private void getMessages(
      final Set<ValidationViolation> violations,
      final List<String> warnings,
      final List<String> errors) {
    for (final ValidationViolation violation : violations) {
      if (violation.getSeverity() == Severity.WARNING) {
        warnings.add(violation.getMessage());
      } else {
        errors.add(violation.getMessage());
      }
    }
  }

  @Override
  public void validate(final Metacard metacard) throws ValidationException {
    final Optional<MetacardValidationReport> reportOptional = validateMetacard(metacard);

    if (reportOptional.isPresent()) {
      final MetacardValidationReport report = reportOptional.get();
      final List<String> warnings = new ArrayList<>();
      final List<String> errors = new ArrayList<>();

      getMessages(report.getAttributeValidationViolations(), warnings, errors);
      getMessages(report.getMetacardValidationViolations(), warnings, errors);

      final ValidationExceptionImpl exception = new ValidationExceptionImpl();
      exception.setWarnings(warnings);
      exception.setErrors(errors);

      throw exception;
    }
  }

  @Override
  public Optional<MetacardValidationReport> validateMetacard(final Metacard metacard) {
    Preconditions.checkArgument(metacard != null, "The metacard cannot be null.");

    final Set<ValidationViolation> violations = new HashSet<>();

    for (final AttributeDescriptor descriptor :
        metacard.getMetacardType().getAttributeDescriptors()) {
      final String attributeName = descriptor.getName();
      final Attribute attribute = metacard.getAttribute(attributeName);
      if (attribute != null) {
        for (final AttributeValidator validator : validatorRegistry.getValidators(attributeName)) {
          validator
              .validate(attribute)
              .ifPresent(report -> violations.addAll(report.getAttributeValidationViolations()));
        }
      }
    }

    if (!violations.isEmpty()) {
      return getReport(violations);
    }

    return Optional.empty();
  }

  private Optional<MetacardValidationReport> getReport(final Set<ValidationViolation> violations) {
    final MetacardValidationReportImpl report = new MetacardValidationReportImpl();
    violations.forEach(report::addAttributeViolation);
    return Optional.of(report);
  }
}
