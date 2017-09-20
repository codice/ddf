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
import ddf.catalog.validation.report.MetacardValidationReport;
import ddf.catalog.validation.violation.ValidationViolation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class MetacardValidationReportImpl implements MetacardValidationReport {
  private final Set<ValidationViolation> attributeValidationViolations = new HashSet<>();

  private final Set<ValidationViolation> metacardValidationViolations = new HashSet<>();

  /**
   * Adds an attribute-level {@link ValidationViolation} to the report.
   *
   * @param violation the attribute-level violation to add to the report, cannot be null
   * @throws IllegalArgumentException if {@code violation} is null
   */
  public void addAttributeViolation(final ValidationViolation violation) {
    Preconditions.checkArgument(violation != null, "The violation cannot be null.");

    attributeValidationViolations.add(violation);
  }

  /**
   * Adds a metacard-level {@link ValidationViolation} to the report.
   *
   * @param violation the metacard-level violation to add to the report, cannot be null
   * @throws IllegalArgumentException if {@code violation} is null
   */
  public void addMetacardViolation(final ValidationViolation violation) {
    Preconditions.checkArgument(violation != null, "The violation cannot be null.");

    metacardValidationViolations.add(violation);
  }

  @Override
  public Set<ValidationViolation> getAttributeValidationViolations() {
    return Collections.unmodifiableSet(attributeValidationViolations);
  }

  @Override
  public Set<ValidationViolation> getMetacardValidationViolations() {
    return Collections.unmodifiableSet(metacardValidationViolations);
  }
}
