/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.validation.impl.report;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Preconditions;

import ddf.catalog.validation.report.AttributeValidationReport;
import ddf.catalog.validation.violation.ValidationViolation;

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
     * Adds a suggested attribute value to the report.
     *
     * @param value a suggested attribute value to add to the report
     * @throws IllegalArgumentException if {@code value} is null
     */
    public void addSuggestedValue(final String value) {
        Preconditions.checkArgument(value != null, "The suggested value cannot be null.");

        suggestedValues.add(value);
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
