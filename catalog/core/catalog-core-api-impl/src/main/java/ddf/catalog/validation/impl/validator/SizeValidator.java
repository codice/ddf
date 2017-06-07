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
package ddf.catalog.validation.impl.validator;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.google.common.base.Preconditions;

import ddf.catalog.data.Attribute;
import ddf.catalog.validation.AttributeValidator;
import ddf.catalog.validation.impl.report.AttributeValidationReportImpl;
import ddf.catalog.validation.impl.violation.ValidationViolationImpl;
import ddf.catalog.validation.report.AttributeValidationReport;
import ddf.catalog.validation.violation.ValidationViolation.Severity;

/**
 * Validates the size of an attribute's value(s).
 * <p>
 * Is capable of validating the sizes of {@link CharSequence}s, {@link Collection}s, {@link Map}s,
 * and arrays.
 */
public class SizeValidator implements AttributeValidator {
    private final long min;

    private final long max;

    /**
     * Creates a {@code SizeValidator} with an <strong>inclusive</strong> range (i.e., [min, max]).
     * <p>
     * The minimum must be non-negative and the maximum must be greater than or equal to the minimum.
     *
     * @param min the minimum allowable size (inclusive), must be non-negative
     * @param max the maximum allowable size (inclusive), must be greater than or equal to {@code min}
     * @throws IllegalArgumentException if 0 <= min <= max does not hold
     */
    public SizeValidator(final long min, final long max) {
        Preconditions.checkArgument(0 <= min && min <= max,
                "The minimum must be non-negative and the maximum must be greater than or equal to the minimum.");

        this.min = min;
        this.max = max;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Validates only the values of {@code attribute} that are {@link CharSequence}s,
     * {@link Collection}s, {@link Map}s, or arrays.
     */
    @Override
    public Optional<AttributeValidationReport> validate(final Attribute attribute) {
        Preconditions.checkArgument(attribute != null, "The attribute cannot be null.");

        final String name = attribute.getName();
        for (final Serializable value : attribute.getValues()) {
            int size;
            if (value instanceof CharSequence) {
                size = ((CharSequence) value).length();
            } else if (value instanceof Collection) {
                size = ((Collection) value).size();
            } else if (value instanceof Map) {
                size = ((Map) value).size();
            } else if (value != null && value.getClass()
                    .isArray()) {
                size = Array.getLength(value);
            } else {
                continue;
            }

            if (!checkSize(size)) {
                final String violationMessage = String.format("%s size must be between %d and %d",
                        name,
                        min,
                        max);
                final AttributeValidationReportImpl report = new AttributeValidationReportImpl();
                report.addViolation(new ValidationViolationImpl(Collections.singleton(name),
                        violationMessage,
                        Severity.ERROR));
                return Optional.of(report);
            }
        }

        return Optional.empty();
    }

    private boolean checkSize(final int size) {
        return min <= size && size <= max;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SizeValidator validator = (SizeValidator) o;

        return new EqualsBuilder().append(min, validator.min)
                .append(max, validator.max)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(29, 37).append(min)
                .append(max)
                .toHashCode();
    }
}
