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
package ddf.catalog.validation.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import com.google.common.collect.Sets;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.validation.impl.validator.EnumerationValidator;
import ddf.catalog.validation.report.AttributeValidationReport;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;

public class EnumerationValidatorTest {
  private static final String[] ENUMERATED_VALUES = {"hearts", "spades", "diamonds", "clubs"};

  @Test
  public void testValidValue() {
    validateNoErrors(new AttributeImpl("test", "spades"), false);
  }

  @Test
  public void testInvalidValue() {
    validateWithErrors(new AttributeImpl("test", "other"), 1, false);
  }

  @Test
  public void testValidValueIgnoreCase() {
    validateNoErrors(new AttributeImpl("test", "SpAdES"), true);
  }

  @Test
  public void testInvalidValueIgnoreCase() {
    validateWithErrors(new AttributeImpl("test", "SpAdES"), 1, false);
  }

  @Test
  public void testSuggestedValues() {
    final Optional<AttributeValidationReport> reportOptional =
        getReport(false, new AttributeImpl("test", "something"));
    assertThat(
        reportOptional
            .get()
            .getSuggestedValues()
            .stream()
            .map(pair -> pair.get("label"))
            .collect(Collectors.toSet()),
        containsInAnyOrder(ENUMERATED_VALUES));
  }

  private Optional<AttributeValidationReport> getReport(
      boolean ignoreCase, final Attribute attribute) {
    final Set<String> enumeratedValuesSet =
        Arrays.stream(ENUMERATED_VALUES).collect(Collectors.toSet());
    final EnumerationValidator validator =
        new EnumerationValidator(enumeratedValuesSet, ignoreCase);
    return validator.validate(attribute);
  }

  private void validateNoErrors(final Attribute attribute, boolean ignoreCase) {
    final Optional<AttributeValidationReport> reportOptional = getReport(ignoreCase, attribute);
    assertThat(reportOptional.isPresent(), is(false));
  }

  private void validateWithErrors(
      final Attribute attribute, final int expectedErrors, boolean ignoreCase) {
    final Optional<AttributeValidationReport> reportOptional = getReport(false, attribute);
    assertThat(reportOptional.get().getAttributeValidationViolations(), hasSize(expectedErrors));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullAttribute() {
    new EnumerationValidator(Sets.newHashSet("first"), false).validate(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullEnumerationValues() {
    new EnumerationValidator(null, false);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyEnumerationValues() {
    new EnumerationValidator(new HashSet<>(), false);
  }

  @Test
  public void testEquals() {
    final EnumerationValidator validator1 =
        new EnumerationValidator(Sets.newHashSet("first", "second"), false);
    final EnumerationValidator validator2 =
        new EnumerationValidator(Sets.newHashSet("first", "second"), false);
    assertThat(validator1.equals(validator2), is(true));
    assertThat(validator2.equals(validator1), is(true));
  }

  @Test
  public void testEqualsSelf() {
    final EnumerationValidator validator =
        new EnumerationValidator(Sets.newHashSet("first"), false);
    assertThat(validator.equals(validator), is(true));
  }

  @Test
  public void testEqualsNull() {
    final EnumerationValidator validator1 =
        new EnumerationValidator(Sets.newHashSet("first"), false);
    assertThat(validator1.equals(null), is(false));
  }

  @Test
  public void testEqualsDifferentEnumerationValues() {
    final EnumerationValidator validator1 =
        new EnumerationValidator(Sets.newHashSet("first", "second"), false);
    final EnumerationValidator validator2 =
        new EnumerationValidator(Sets.newHashSet("first", "third"), false);
    assertThat(validator1.equals(validator2), is(false));
    assertThat(validator2.equals(validator1), is(false));
  }

  @Test
  public void testHashCode() {
    final EnumerationValidator validator1 =
        new EnumerationValidator(Sets.newHashSet("first", "second"), false);
    final EnumerationValidator validator2 =
        new EnumerationValidator(Sets.newHashSet("first", "second"), false);
    assertThat(validator1.hashCode(), is(validator2.hashCode()));
  }

  @Test
  public void testHashCodeDifferentEnumerationValues() {
    final EnumerationValidator validator1 =
        new EnumerationValidator(Sets.newHashSet("first", "second"), false);
    final EnumerationValidator validator2 =
        new EnumerationValidator(Sets.newHashSet("first", "third"), false);
    assertThat(validator1.hashCode(), not(validator2.hashCode()));
  }
}
