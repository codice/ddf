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
package ddf.catalog.validation.impl.report;

import com.google.common.base.Preconditions;
import ddf.catalog.validation.report.AttributeValidationReport;
import ddf.catalog.validation.violation.ValidationViolation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class AttributeValidationReportImpl implements AttributeValidationReport {
  private final Set<ValidationViolation> attributeValidationViolations = new HashSet<>();

  private final Set<String> suggestedValues = new HashSet<>();

  /**
   * Adds a {@link ValidationViolation} to the report.
   *
   * @param violation the violation to add to the report, cannot be null
   * @throws IllegalArgumentException if {@code violation} is null
   */
  public void addViolation(final ValidationViolation violation) {
    Preconditions.checkArgument(violation != null, "The violation cannot be null.");

    attributeValidationViolations.add(violation);
  }

  /**
   * Adds a set of {@link ValidationViolation} to the report.
   *
   * @param violations the violation set to add to the report, cannot be null or empty
   * @throws IllegalArgumentException if {@code violation} is null or empty
   */
  public void addViolations(Set<ValidationViolation> violations) {
    Preconditions.checkArgument(violations != null, "The violation list cannot be null.");
    Preconditions.checkArgument(violations.size() > 0, "The violation list cannot be empty.");

    attributeValidationViolations.addAll(violations);
  }

  /**
   * Adds a suggested attribute value to the report.
   *
   * @param value a suggested attribute value to add to the report
   * @throws IllegalArgumentException if {@code value} is null
   */
  public void addSuggestedValue(final String value) {
    Preconditions.checkArgument(value != null, "The suggested value cannot be null.");

    suggestedValues.add(value);
  }

  /**
   * Adds a a set of attribute values to the report.
   *
   * @param values a set of suggested attribute values to add to the report, cannot be null or empty
   * @throws IllegalArgumentException if {@code value} is null or empty
   */
  public void addSuggestedValues(Set<String> values) {
    Preconditions.checkArgument(values != null, "The suggested values cannot be null.");
    Preconditions.checkArgument(values.size() > 0, "The suggested values cannot be empty.");

    suggestedValues.addAll(values);
  }

  @Override
  public Set<ValidationViolation> getAttributeValidationViolations() {
    return Collections.unmodifiableSet(attributeValidationViolations);
  }

  @Override
  public Set<String> getSuggestedValues() {
    return Collections.unmodifiableSet(suggestedValues);
  }
}
