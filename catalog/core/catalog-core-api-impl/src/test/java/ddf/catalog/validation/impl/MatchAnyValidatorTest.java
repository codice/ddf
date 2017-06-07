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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import com.google.common.collect.ImmutableSet;

import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.validation.impl.report.AttributeValidationReportImpl;
import ddf.catalog.validation.impl.validator.EnumerationValidator;
import ddf.catalog.validation.impl.validator.MatchAnyValidator;
import ddf.catalog.validation.impl.validator.SizeValidator;
import ddf.catalog.validation.report.AttributeValidationReport;

public class MatchAnyValidatorTest {

    private static final AttributeImpl VALID_ATTRIBUTE_1 = new AttributeImpl(Core.TITLE, "bob");

    private static final AttributeImpl VALID_ATTRIBUTE_2 = new AttributeImpl(Core.TITLE, "bobby");

    private static final AttributeImpl INVALID_ATTRIBUTE = new AttributeImpl(Core.TITLE, "robert");

    private static final Set<String> VALID_ENUMERATIONS = ImmutableSet.of("bob", "rob", "bobby");

    private static final Set<String> VALID_ENUMERATIONS_2 = ImmutableSet.of("ben");

    private MatchAnyValidator matchAnyValidator;

    private EnumerationValidator stubEmptyValidator;

    private EnumerationValidator enumerationValidator;

    private EnumerationValidator enumerationValidator2;

    @Before
    public void setUp() {
        stubEmptyValidator = mock(EnumerationValidator.class);
        when(stubEmptyValidator.validate(Matchers.any(AttributeImpl.class))).thenReturn(Optional.of(
                new AttributeValidationReportImpl()));

        enumerationValidator = new EnumerationValidator(VALID_ENUMERATIONS, false);
        enumerationValidator2 = new EnumerationValidator(VALID_ENUMERATIONS_2, false);
        SizeValidator sizeValidator = new SizeValidator(1, 3);
        matchAnyValidator = new MatchAnyValidator(Arrays.asList(enumerationValidator,
                sizeValidator));
    }

    @Test
    public void testMatchAnyValidatorPassesWhenValidatorListIsNull() {
        matchAnyValidator = new MatchAnyValidator(null);
        Optional<AttributeValidationReport> attributeValidationReportOptional =
                matchAnyValidator.validate(VALID_ATTRIBUTE_1);
        assertThat(attributeValidationReportOptional.isPresent(), is(false));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testMatchAnyValidatorPassesWhenValidatorListIsEmpty() {
        matchAnyValidator = new MatchAnyValidator(Collections.EMPTY_LIST);
        Optional<AttributeValidationReport> attributeValidationReportOptional =
                matchAnyValidator.validate(VALID_ATTRIBUTE_1);
        assertThat(attributeValidationReportOptional.isPresent(), is(false));
    }

    @Test
    public void testMatchAnyValidatorPassesWhenValidatorAttributeIsNull() {
        Optional<AttributeValidationReport> attributeValidationReportOptional =
                matchAnyValidator.validate(null);
        assertThat(attributeValidationReportOptional.isPresent(), is(false));
    }

    @Test
    public void testMatchAnyValidatorPassesWhenValidationReportIsEmpty() {
        matchAnyValidator = new MatchAnyValidator(Collections.singletonList(stubEmptyValidator));
        Optional<AttributeValidationReport> attributeValidationReportOptional =
                matchAnyValidator.validate(VALID_ATTRIBUTE_1);
        assertThat(attributeValidationReportOptional.isPresent(), is(false));
    }

    @Test
    public void testMatchAnyValidatorFailsWhenAllValidatorsHaveErrors() {
        Optional<AttributeValidationReport> attributeValidationReportOptional =
                matchAnyValidator.validate(INVALID_ATTRIBUTE);
        AttributeValidationReport attributeValidationReport =
                attributeValidationReportOptional.get();
        assertThat(attributeValidationReport.getAttributeValidationViolations(), not(empty()));
        assertThat(attributeValidationReport.getSuggestedValues(), not(empty()));
    }

    @Test
    public void testMatchAnyValidatorPassesWhenNoValidatorsHaveErrors() {
        Optional<AttributeValidationReport> attributeValidationReportOptional =
                matchAnyValidator.validate(VALID_ATTRIBUTE_1);
        assertThat(attributeValidationReportOptional.isPresent(), is(false));
    }

    @Test
    public void testMatchAnyValidatorPassesWhenOneValidatorHasErrors() {
        Optional<AttributeValidationReport> attributeValidationReportOptional =
                matchAnyValidator.validate(VALID_ATTRIBUTE_2);
        assertThat(attributeValidationReportOptional.isPresent(), is(false));
    }

    @Test
    public void testMatchAnyValidatorPassesWithTwoEnumerationValidators() {
        matchAnyValidator = new MatchAnyValidator(Arrays.asList(enumerationValidator,
                enumerationValidator2));
        Optional<AttributeValidationReport> attributeValidationReportOptional =
                matchAnyValidator.validate(VALID_ATTRIBUTE_2);
        assertThat(attributeValidationReportOptional.isPresent(), is(false));
    }
}
