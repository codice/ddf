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
package org.codice.ddf.validator.metacard.wkt;

import com.google.common.collect.ImmutableSet;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.types.Validation;
import ddf.catalog.validation.MetacardValidator;
import ddf.catalog.validation.ReportingMetacardValidator;
import ddf.catalog.validation.ValidationException;
import ddf.catalog.validation.impl.ValidationExceptionImpl;
import ddf.catalog.validation.impl.report.MetacardValidationReportImpl;
import ddf.catalog.validation.impl.violation.ValidationViolationImpl;
import ddf.catalog.validation.report.MetacardValidationReport;
import ddf.catalog.validation.violation.ValidationViolation;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.validator.wkt.WktValidator;

public class MetacardWktValidator implements MetacardValidator, ReportingMetacardValidator {
  private String errorMsg;

  private String validatedAttributeKey;

  private final WktValidator wktValidator;

  public MetacardWktValidator(WktValidator wktValidator, String validatedAttributeKey) {
    this.wktValidator = wktValidator;
    this.validatedAttributeKey = validatedAttributeKey;
    this.errorMsg = validatedAttributeKey + " is invalid: ";
  }

  @Override
  public Optional<MetacardValidationReport> validateMetacard(Metacard metacard) {
    MetacardValidationReportImpl report = null;

    String attributeValue = getAttributeValue(metacard);

    if (StringUtils.isNotEmpty(attributeValue)) {
      if (!wktValidator.isValid(attributeValue)) {
        String message = errorMsg + metacard.getLocation();
        ValidationViolation violation =
            new ValidationViolationImpl(
                ImmutableSet.of(validatedAttributeKey),
                message,
                ValidationViolation.Severity.ERROR);
        report = new MetacardValidationReportImpl();
        report.addMetacardViolation(violation);
      }
    } else {
      // Put the validation error from the metacard into this validation report
      // Otherwise the validation error gets lost
      Attribute validationErrorAttr = metacard.getAttribute(Validation.VALIDATION_ERRORS);
      if (validationErrorAttr != null) {
        List<Serializable> errors = validationErrorAttr.getValues();
        for (Serializable error : errors) {
          String errorStr = String.valueOf(error);
          if (errorStr.startsWith(errorMsg)) {
            ValidationViolation violation =
                new ValidationViolationImpl(
                    ImmutableSet.of(validatedAttributeKey),
                    errorStr,
                    ValidationViolation.Severity.ERROR);
            report = new MetacardValidationReportImpl();
            report.addMetacardViolation(violation);
          }
        }
      }
    }

    return Optional.ofNullable(report);
  }

  @Override
  public void validate(Metacard metacard) throws ValidationException {
    Optional<MetacardValidationReport> validationReport = validateMetacard(metacard);
    if (validationReport.isPresent()) {
      MetacardValidationReport report = validationReport.get();
      Set<ValidationViolation> violations = report.getMetacardValidationViolations();
      if (!violations.isEmpty()) {
        metacard.setAttribute(new AttributeImpl(validatedAttributeKey, (Serializable) null));
        final ValidationExceptionImpl exception = new ValidationExceptionImpl();
        exception.setErrors(
            violations.stream().map(ValidationViolation::getMessage).collect(Collectors.toList()));
        throw exception;
      }
    }
  }

  private String getAttributeValue(Metacard metacard) {
    Attribute attribute = metacard.getAttribute(validatedAttributeKey);
    if (attribute == null) {
      return null;
    }

    Serializable attributeValue = attribute.getValue();

    return attributeValue == null ? null : attributeValue.toString();
  }
}
