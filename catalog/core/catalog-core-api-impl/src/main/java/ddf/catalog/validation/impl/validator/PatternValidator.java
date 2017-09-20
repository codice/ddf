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
import ddf.catalog.validation.AttributeValidator;
import ddf.catalog.validation.impl.report.AttributeValidationReportImpl;
import ddf.catalog.validation.impl.violation.ValidationViolationImpl;
import ddf.catalog.validation.report.AttributeValidationReport;
import ddf.catalog.validation.violation.ValidationViolation.Severity;
import java.io.Serializable;
import java.util.Collections;
import java.util.Optional;
import java.util.regex.Pattern;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Validates an attribute's value(s) against a regular expression.
 *
 * <p>Is capable of validating {@link CharSequence}s.
 */
public class PatternValidator implements AttributeValidator {
  private final Pattern pattern;

  /**
   * Constructs a {@code PatternValidator} with the given regular expression.
   *
   * @param regex the regular expression
   * @throws IllegalArgumentException if {@code regex} is null
   * @throws java.util.regex.PatternSyntaxException if {@code regex} is not a valid regular
   *     expression
   */
  public PatternValidator(final String regex) {
    Preconditions.checkArgument(regex != null, "The regular expression cannot be null.");

    pattern = Pattern.compile(regex);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Validates only the values of {@code attribute} that are {@link CharSequence}s.
   */
  @Override
  public Optional<AttributeValidationReport> validate(final Attribute attribute) {
    Preconditions.checkArgument(attribute != null, "The attribute cannot be null.");

    final String name = attribute.getName();
    for (final Serializable value : attribute.getValues()) {
      if (value instanceof CharSequence && !(pattern.matcher((CharSequence) value)).matches()) {
        final AttributeValidationReportImpl report = new AttributeValidationReportImpl();
        report.addViolation(
            new ValidationViolationImpl(
                Collections.singleton(name),
                name + " does not follow the pattern " + pattern.pattern(),
                Severity.ERROR));
        return Optional.of(report);
      }
    }

    return Optional.empty();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    PatternValidator validator = (PatternValidator) o;

    return new EqualsBuilder().append(pattern.pattern(), validator.pattern.pattern()).isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 29).append(pattern.pattern()).toHashCode();
  }
}
