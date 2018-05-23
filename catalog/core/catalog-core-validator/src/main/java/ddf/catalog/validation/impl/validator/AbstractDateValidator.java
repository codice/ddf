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
import ddf.catalog.validation.impl.report.AttributeValidationReportImpl;
import ddf.catalog.validation.impl.violation.ValidationViolationImpl;
import ddf.catalog.validation.report.AttributeValidationReport;
import ddf.catalog.validation.violation.ValidationViolation.Severity;
import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.function.Function;

/**
 * Provides support for {@link ddf.catalog.validation.AttributeValidator}s that validate {@link
 * Date}s.
 */
public abstract class AbstractDateValidator {
  /**
   * Validates the values of {@code attribute} that are {@link Date}s.
   *
   * <p>If {@code validator} returns false for a value, this method will return an {@link Optional}
   * containing an {@link AttributeValidationReport} with a message of {@code attribute.getName() +
   * " " + message} and a severity of {@link Severity#ERROR}. Otherwise, an empty {@link Optional}
   * is returned.
   *
   * @param attribute the {@link Attribute} to validate
   * @param validator the test to apply to the values of {@code attribute}
   * @param message the message to include in the report in the case of a validation violation
   * @return an {@link Optional} containing an {@link AttributeValidationReport} if there are
   *     violations, or an empty {@link Optional} if there are no violations
   */
  protected final Optional<AttributeValidationReport> validate(
      final Attribute attribute, final Function<Date, Boolean> validator, final String message) {
    final String name = attribute.getName();

    for (final Serializable value : attribute.getValues()) {
      if (value instanceof Date && !validator.apply((Date) value)) {
        final AttributeValidationReportImpl report = new AttributeValidationReportImpl();
        report.addViolation(
            new ValidationViolationImpl(
                Collections.singleton(name), name + " " + message, Severity.ERROR));
        return Optional.of(report);
      }
    }

    return Optional.empty();
  }
}
