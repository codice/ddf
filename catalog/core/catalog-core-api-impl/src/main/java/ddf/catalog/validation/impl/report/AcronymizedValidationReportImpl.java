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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AcronymizedValidationReportImpl extends AttributeValidationReportImpl {

  private final Set<Map<String, String>> suggestedValues = new HashSet<>();

  public AcronymizedValidationReportImpl() {
    super();
  }

  /**
   * Adds a suggested attribute value to the report.
   *
   * @param acronym a suggested attribute value to add to the report
   * @throws IllegalArgumentException if {@code value} is null
   */
  @Override
  public void addSuggestedValue(final String acronym, final String meaning) {
    Preconditions.checkArgument(acronym != null, "The suggested acronym cannot be null.");
    Preconditions.checkArgument(meaning != null, "The suggested meaning cannot be null.");

    Map<String, String> suggestedValue = new HashMap<>();
    suggestedValue.put("label", acronym + " (" + meaning + ")");
    suggestedValue.put("value", acronym);
    suggestedValues.add(suggestedValue);
  }

  /**
   * Adds a a set of attribute values to the report.
   *
   * @param values a set of suggested attribute values to add to the report, cannot be null or empty
   * @throws IllegalArgumentException if {@code value} is null or empty
   */
  @Override
  public void addSuggestedValues(Set<Map<String, String>> values) {
    Preconditions.checkArgument(values != null, "The suggested values cannot be null.");
    Preconditions.checkArgument(!values.isEmpty(), "The suggested values cannot be empty.");

    suggestedValues.addAll(values);
  }

  @Override
  public Set<Map<String, String>> getSuggestedValues() {
    return Collections.unmodifiableSet(suggestedValues);
  }
}
