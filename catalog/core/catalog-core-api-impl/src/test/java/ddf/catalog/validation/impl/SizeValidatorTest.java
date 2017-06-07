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
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import com.google.common.collect.Sets;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.validation.impl.validator.SizeValidator;
import ddf.catalog.validation.report.AttributeValidationReport;

public class SizeValidatorTest {

    @Test
    public void testValidStringValueOfEqualMinMax() {
        validateNoErrors(new AttributeImpl("test", StringUtils.repeat("a", 33)), 33, 33);
    }

    @Test
    public void testInvalidStringValueOfEqualMinMax() {
        validateWithErrors(new AttributeImpl("test", StringUtils.repeat("a", 32)), 33, 33, 1);
    }

    @Test
    public void testValidStringValue() {
        validateNoErrors(new AttributeImpl("test", StringUtils.repeat("a", 33)), 0, 36);
    }

    @Test
    public void testValidArrayValue() {
        validateNoErrors(new AttributeImpl("test", new byte[15]), 10, 15);
    }

    @Test
    public void testValidCollectionValue() {
        validateNoErrors(new AttributeImpl("test", Sets.newHashSet(1, 2, 3, 4, 5)), 4, 6);
    }

    @Test
    public void testValidMapValue() {
        final HashMap<Integer, Integer> map = new HashMap<>();
        map.put(1, 2);
        validateNoErrors(new AttributeImpl("test", map), 0, 5);
    }

    @Test
    public void testInvalidStringValue() {
        validateWithErrors(new AttributeImpl("test", StringUtils.repeat("a", 33)), 40, 50, 1);
    }

    @Test
    public void testInvalidArrayValue() {
        validateWithErrors(new AttributeImpl("test", new byte[15]), 20, 25, 1);
    }

    @Test
    public void testInvalidCollectionValue() {
        validateWithErrors(new AttributeImpl("test", Sets.newHashSet(1, 2, 3, 4, 5)), 6, 8, 1);
    }

    @Test
    public void testInvalidMapValue() {
        validateWithErrors(new AttributeImpl("test", new HashMap<>()), 5, 10, 1);
    }

    private void validateNoErrors(final Attribute attribute, final long min, final long max) {
        final Optional<AttributeValidationReport> reportOptional = getReportOptional(attribute,
                min,
                max);
        assertThat(reportOptional.isPresent(), is(false));
    }

    private void validateWithErrors(final Attribute attribute, final long min, final long max,
            final int expectedErrors) {
        final Optional<AttributeValidationReport> reportOptional = getReportOptional(attribute,
                min,
                max);
        assertThat(reportOptional.get()
                .getAttributeValidationViolations(), hasSize(expectedErrors));
    }

    private Optional<AttributeValidationReport> getReportOptional(final Attribute attribute,
            final long min, final long max) {
        final SizeValidator validator = new SizeValidator(min, max);
        return validator.validate(attribute);
    }

    @Test
    public void testEquals() {
        final SizeValidator validator1 = new SizeValidator(13, 1799);
        final SizeValidator validator2 = new SizeValidator(13, 1799);
        assertThat(validator1.equals(validator1), is(true));
        assertThat(validator1.equals(validator2), is(true));
        assertThat(validator2.equals(validator1), is(true));
    }

    @Test
    public void testEqualsSelf() {
        final SizeValidator validator = new SizeValidator(13, 1799);
        assertThat(validator.equals(validator), is(true));
    }

    @Test
    public void testEqualsNull() {
        final SizeValidator validator = new SizeValidator(13, 1799);
        assertThat(validator.equals(null), is(false));
    }

    @Test
    public void testEqualsDifferentMin() {
        final SizeValidator validator1 = new SizeValidator(13, 1799);
        final SizeValidator validator2 = new SizeValidator(0, 1799);
        assertThat(validator1.equals(validator2), is(false));
        assertThat(validator2.equals(validator1), is(false));
    }

    @Test
    public void testEqualsDifferentMax() {
        final SizeValidator validator1 = new SizeValidator(13, 1799);
        final SizeValidator validator2 = new SizeValidator(13, 2000);
        assertThat(validator1.equals(validator2), is(false));
        assertThat(validator2.equals(validator1), is(false));
    }

    @Test
    public void testHashCode() {
        final SizeValidator validator1 = new SizeValidator(13, 1799);
        final SizeValidator validator2 = new SizeValidator(13, 1799);
        assertThat(validator1.hashCode(), is(validator2.hashCode()));
    }

    @Test
    public void testHashCodeDifferentMin() {
        final SizeValidator validator1 = new SizeValidator(13, 1799);
        final SizeValidator validator2 = new SizeValidator(0, 1799);
        assertThat(validator1.hashCode(), not(validator2.hashCode()));
    }

    @Test
    public void testHashCodeDifferentMax() {
        final SizeValidator validator1 = new SizeValidator(13, 1799);
        final SizeValidator validator2 = new SizeValidator(13, 2000);
        assertThat(validator1.hashCode(), not(validator2.hashCode()));
    }
}
