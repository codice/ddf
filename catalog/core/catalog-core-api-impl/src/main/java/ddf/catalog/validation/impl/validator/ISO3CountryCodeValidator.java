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

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.google.common.base.Preconditions;

import ddf.catalog.data.Attribute;
import ddf.catalog.validation.AttributeValidator;
import ddf.catalog.validation.impl.report.AttributeValidationReportImpl;
import ddf.catalog.validation.impl.violation.ValidationViolationImpl;
import ddf.catalog.validation.report.AttributeValidationReport;
import ddf.catalog.validation.violation.ValidationViolation;

/**
 * Validates an attribute's value(s) against the ISO_3166-1 Alpha3 country codes.
 * <p>
 * Is capable of validating {@link String}s.
 */
public class ISO3CountryCodeValidator implements AttributeValidator {
    private final boolean ignoreCase;

    private static final Set<String> COUNTRY_CODES = Arrays.stream(Locale.getISOCountries())
            .map(iso2CC -> new Locale("", iso2CC))
            .map(Locale::getISO3Country)
            .map(String::toUpperCase)
            .collect(Collectors.toSet());

    public ISO3CountryCodeValidator(final boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Validates only the values of {@code attribute} that are {@link String}s.
     */
    @Override
    public Optional<AttributeValidationReport> validate(Attribute attribute) {
        Preconditions.checkArgument(attribute != null, "The attribute cannot be null.");

        AttributeValidationReport report = buildReport(attribute);

        return report.getAttributeValidationViolations()
                .isEmpty() ? Optional.empty() : Optional.of(report);
    }

    private AttributeValidationReport buildReport(Attribute attribute) {
        AttributeValidationReportImpl report = new AttributeValidationReportImpl();

        attribute.getValues()
                .stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(ignoreCase ? String::toUpperCase : String::toString)
                .filter(s -> !COUNTRY_CODES.contains(s))
                .map(s -> new ValidationViolationImpl(Collections.singleton(attribute.getName()),
                        s + " is not a valid ISO_3166-1 Alpha3 country code.",
                        ValidationViolation.Severity.ERROR))
                .forEach(report::addViolation);

        COUNTRY_CODES.forEach(report::addSuggestedValue);

        return report;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ISO3CountryCodeValidator that = (ISO3CountryCodeValidator) o;

        return new EqualsBuilder().append(COUNTRY_CODES, that.COUNTRY_CODES)
                .append(ignoreCase, that.ignoreCase)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(COUNTRY_CODES)
                .toHashCode();
    }
}
