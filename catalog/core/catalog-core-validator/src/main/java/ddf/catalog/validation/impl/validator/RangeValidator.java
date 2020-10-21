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
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Validates an attribute's value(s) against an <strong>inclusive</strong> numeric range.
 *
 * <p>Is capable of validating {@link Number}s.
 */
public class RangeValidator implements AttributeValidator {
  private static final BigDecimal DEFAULT_EPSILON = new BigDecimal("1E-6");

  private final BigDecimal min;

  private final BigDecimal max;

  /**
   * Creates a {@code RangeValidator} with an <strong>inclusive</strong> range.
   *
   * <p>Uses an epsilon of 1e-6 on both sides of the range to account for floating point
   * representation inaccuracies, so the range is really [min - epsilon, max + epsilon].
   *
   * @param min the minimum allowable value (inclusive), cannot be null
   * @param max the maximum allowable value (inclusive), cannot be null and must be greater than
   *     {@code min}
   * @throws IllegalArgumentException if {@code max} is not greater than {@code min} or either is
   *     null
   */
  public RangeValidator(final BigDecimal min, final BigDecimal max) {
    this(min, max, DEFAULT_EPSILON);
  }

  /**
   * Creates a {@code RangeValidator} with an <strong>inclusive</strong> range and the provided
   * epsilon.
   *
   * <p>Uses the provided epsilon on both sides of the range to account for floating point
   * representation inaccuracies, so the range is really [min - epsilon, max + epsilon].
   *
   * @param min the minimum allowable value (inclusive), cannot be null
   * @param max the maximum allowable value (inclusive), cannot be null and must be greater than
   *     {@code min}
   * @param epsilon the epsilon value, cannot be null and must be positive
   * @throws IllegalArgumentException if {@code max} is not greater than {@code min}, {@code
   *     epsilon} is not positive, or if any argument is null
   */
  public RangeValidator(final BigDecimal min, final BigDecimal max, final BigDecimal epsilon) {
    Preconditions.checkArgument(min != null, "The minimum cannot be null.");
    Preconditions.checkArgument(max != null, "The maximum cannot be null.");
    Preconditions.checkArgument(epsilon != null, "The epsilon cannot be null.");
    Preconditions.checkArgument(
        min.compareTo(max) < 0, "The maximum must be greater than the minimum.");
    Preconditions.checkArgument(
        epsilon.compareTo(BigDecimal.ZERO) > 0, "The epsilon must be greater than 0.");

    this.min = min.subtract(epsilon);
    this.max = max.add(epsilon);
  }

  @Override
  public Optional<AttributeValidationReport> validate(final Attribute attribute) {
    Preconditions.checkArgument(attribute != null, "The attribute cannot be null.");

    final String name = attribute.getName();

    for (final Serializable value : attribute.getValues()) {
      final BigDecimal bdValue;
      if (value instanceof Number) {
        bdValue = new BigDecimal(value.toString());
      } else {
        continue;
      }

      if (!checkRange(bdValue)) {
        final String violationMessage =
            String.format(
                "%s must be between %s and %s", name, min.toPlainString(), max.toPlainString());
        final AttributeValidationReportImpl report = new AttributeValidationReportImpl();
        report.addViolation(
            new ValidationViolationImpl(
                Collections.singleton(name), violationMessage, Severity.ERROR));
        return Optional.of(report);
      }
    }

    return Optional.empty();
  }

  private boolean checkRange(final BigDecimal value) {
    return min.compareTo(value) <= 0 && value.compareTo(max) <= 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    RangeValidator that = (RangeValidator) o;

    return new EqualsBuilder().append(min, that.min).append(max, that.max).isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 41).append(min).append(max).toHashCode();
  }
}
