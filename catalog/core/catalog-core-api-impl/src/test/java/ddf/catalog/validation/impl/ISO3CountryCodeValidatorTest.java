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
package ddf.catalog.validation.impl;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Optional;

import org.junit.Test;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.validation.impl.validator.ISO3CountryCodeValidator;
import ddf.catalog.validation.report.AttributeValidationReport;

public class ISO3CountryCodeValidatorTest {

    @Test
    public void testValidISO3CountryCode() {
        validateNoErrors(new AttributeImpl("location.country_code", "USA"), false);
    }

    @Test
    public void testInvalidDataType() {
        validateNoErrors(new AttributeImpl("location.country_code", new byte[10]), false);
    }

    @Test
    public void testInvalidISO3CountryCodes() {
        validateWithErrors(new AttributeImpl("location.country_code",
                Arrays.asList(new String[] {"Usa", "123"})), 2, false);
    }

    @Test
    public void testValidISO3CountryCodeIgnoreCase() {
        validateNoErrors(new AttributeImpl("location.country_code", "usa"), true);
    }

    @Test
    public void testInvalidISO3CountryCodesIgnoreCase() {
        validateWithErrors(new AttributeImpl("location.country_code",
                Arrays.asList(new String[] {"zzz", "abc"})), 2, false);
    }

    private void validateNoErrors(final Attribute attribute, boolean ignoreCase) {
        ISO3CountryCodeValidator validator = new ISO3CountryCodeValidator(ignoreCase);
        final Optional<AttributeValidationReport> reportOptional = validator.validate(attribute);
        assertThat(reportOptional.isPresent(), is(false));
    }

    private void validateWithErrors(final Attribute attribute, final int expectedErrors,
            boolean ignoreCase) {
        ISO3CountryCodeValidator validator = new ISO3CountryCodeValidator(ignoreCase);
        final Optional<AttributeValidationReport> reportOptional = validator.validate(attribute);
        assertThat(reportOptional.get()
                .getAttributeValidationViolations(), hasSize(expectedErrors));
    }

    @Test
    public void testEquals() {
        final ISO3CountryCodeValidator validator1 = new ISO3CountryCodeValidator(false);
        final ISO3CountryCodeValidator validator2 = new ISO3CountryCodeValidator(false);
        assertThat(validator1.equals(validator2), is(true));
        assertThat(validator2.equals(validator1), is(true));
    }

    @Test
    public void testEqualsSelf() {
        final ISO3CountryCodeValidator validator = new ISO3CountryCodeValidator(false);
        assertThat(validator.equals(validator), is(true));
    }

    @Test
    public void testEqualsNull() {
        final ISO3CountryCodeValidator validator = new ISO3CountryCodeValidator(false);
        assertThat(validator.equals(null), is(false));
    }

    @Test
    public void testHashCode() {
        final ISO3CountryCodeValidator validator1 = new ISO3CountryCodeValidator(false);
        final ISO3CountryCodeValidator validator2 = new ISO3CountryCodeValidator(false);
        assertThat(validator1.hashCode(), is(validator2.hashCode()));
    }
}
