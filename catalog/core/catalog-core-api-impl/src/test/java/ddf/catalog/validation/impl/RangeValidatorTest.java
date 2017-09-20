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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.validation.impl.validator.RangeValidator;
import ddf.catalog.validation.report.AttributeValidationReport;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.Test;

public class RangeValidatorTest {
  @Test
  public void testWithinIntegerRange() {
    final RangeValidator validator =
        new RangeValidator(new BigDecimal("-123456789123456"), new BigDecimal("987654321987654"));
    validateNoErrors(new AttributeImpl("", -123456789123456L), validator);
    validateNoErrors(new AttributeImpl("", 987654321987654L), validator);
    validateNoErrors(new AttributeImpl("", 0), validator);

    validateNoErrors(new AttributeImpl("", -123456789123455.9), validator);
    validateNoErrors(new AttributeImpl("", 987654321987653.9), validator);
  }

  @Test
  public void testOutsideIntegerRange() {
    final RangeValidator validator =
        new RangeValidator(new BigDecimal("-123456789123456"), new BigDecimal("987654321987654"));
    validateWithErrors(new AttributeImpl("", -123456789123457L), validator, 1);
    validateWithErrors(new AttributeImpl("", 987654321987655L), validator, 1);

    validateWithErrors(new AttributeImpl("", -123456789123456.1), validator, 1);
    validateWithErrors(new AttributeImpl("", 987654321987654.1), validator, 1);
  }

  @Test
  public void testWithinLargeDecimalRange() {
    final RangeValidator validator =
        new RangeValidator(
            new BigDecimal("-123456789123456.857"),
            new BigDecimal("987654321987654.923"),
            new BigDecimal("0.01"));
    validateNoErrors(new AttributeImpl("", -123456789123456.857), validator);
    validateNoErrors(new AttributeImpl("", 987654321987654.923), validator);
    validateNoErrors(new AttributeImpl("", 0), validator);

    validateNoErrors(new AttributeImpl("", -123456789123456L), validator);
    validateNoErrors(new AttributeImpl("", 987654321987654L), validator);
  }

  @Test
  public void testOutsideLargeDecimalRange() {
    final RangeValidator validator =
        new RangeValidator(
            new BigDecimal("-123456789123456.857"),
            new BigDecimal("987654321987654.923"),
            new BigDecimal("0.01"));
    validateWithErrors(new AttributeImpl("", -123456789123456.87), validator, 1);
    validateWithErrors(new AttributeImpl("", 987654321987654.94), validator, 1);

    validateWithErrors(new AttributeImpl("", -123456789123457L), validator, 1);
    validateWithErrors(new AttributeImpl("", 987654321987655L), validator, 1);
  }

  @Test
  public void testWithinSmallDecimalRange() {
    final RangeValidator validator =
        new RangeValidator(
            new BigDecimal("1.2457515"), new BigDecimal("1.2487595"), new BigDecimal("1E-7"));
    validateNoErrors(new AttributeImpl("", 1.2457515), validator);
    validateNoErrors(new AttributeImpl("", 1.2487595), validator);
    validateNoErrors(new AttributeImpl("", 1.246), validator);
  }

  @Test
  public void testOutsideSmallDecimalRange() {
    final RangeValidator validator =
        new RangeValidator(
            new BigDecimal("1.2457515"), new BigDecimal("1.2487595"), new BigDecimal("1E-7"));
    validateWithErrors(new AttributeImpl("", 1.24575135), validator, 1);
    validateWithErrors(new AttributeImpl("", 1.24875965), validator, 1);

    validateWithErrors(new AttributeImpl("", 1), validator, 1);
    validateWithErrors(new AttributeImpl("", 2), validator, 1);
  }

  private void validateNoErrors(final Attribute attribute, final RangeValidator validator) {
    final Optional<AttributeValidationReport> reportOptional = validator.validate(attribute);
    assertThat(
        "Expected no validation violations but a report was returned (which indicates that there are violations).",
        reportOptional.isPresent(),
        is(false));
  }

  private void validateWithErrors(
      final Attribute attribute, final RangeValidator validator, final int expectedErrors) {
    final Optional<AttributeValidationReport> reportOptional = validator.validate(attribute);
    assertThat(
        "Expected some validation violations but no report was returned (which indicates that there are no violations).",
        reportOptional.isPresent(),
        is(true));
    assertThat(reportOptional.get().getAttributeValidationViolations(), hasSize(expectedErrors));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullMin() {
    new RangeValidator(null, BigDecimal.ONE);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullMax() {
    new RangeValidator(BigDecimal.ONE, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullEpsilon() {
    new RangeValidator(BigDecimal.ONE, BigDecimal.TEN, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidRange() {
    new RangeValidator(BigDecimal.TEN, BigDecimal.ONE);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNegativeEpsilon() {
    new RangeValidator(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.valueOf(-1));
  }

  @Test
  public void testEquals() {
    final RangeValidator validator1 =
        new RangeValidator(new BigDecimal("1.5"), new BigDecimal("2.5"));
    final RangeValidator validator2 =
        new RangeValidator(new BigDecimal("1.5"), new BigDecimal("2.5"));
    assertThat(validator1.equals(validator2), is(true));
    assertThat(validator2.equals(validator1), is(true));
  }

  @Test
  public void testEqualsSelf() {
    final RangeValidator validator =
        new RangeValidator(new BigDecimal("1.5"), new BigDecimal("2.5"));
    assertThat(validator.equals(validator), is(true));
  }

  @Test
  public void testEqualsNull() {
    final RangeValidator validator =
        new RangeValidator(new BigDecimal("1.5"), new BigDecimal("2.5"));
    assertThat(validator.equals(null), is(false));
  }

  @Test
  public void testEqualsDifferentMin() {
    final RangeValidator validator1 =
        new RangeValidator(new BigDecimal("1.5"), new BigDecimal("2.5"));
    final RangeValidator validator2 =
        new RangeValidator(new BigDecimal("1.1"), new BigDecimal("2.5"));
    assertThat(validator1.equals(validator2), is(false));
    assertThat(validator2.equals(validator1), is(false));
  }

  @Test
  public void testEqualsDifferentMax() {
    final RangeValidator validator1 =
        new RangeValidator(new BigDecimal("1.5"), new BigDecimal("2.5"));
    final RangeValidator validator2 =
        new RangeValidator(new BigDecimal("1.5"), new BigDecimal("2.1"));
    assertThat(validator1.equals(validator2), is(false));
    assertThat(validator2.equals(validator1), is(false));
  }

  @Test
  public void testHashCode() {
    final RangeValidator validator1 =
        new RangeValidator(new BigDecimal("1.5"), new BigDecimal("2.5"));
    final RangeValidator validator2 =
        new RangeValidator(new BigDecimal("1.5"), new BigDecimal("2.5"));
    assertThat(validator1.hashCode(), is(validator2.hashCode()));
  }

  @Test
  public void testHashCodeDifferentMin() {
    final RangeValidator validator1 =
        new RangeValidator(new BigDecimal("1.5"), new BigDecimal("2.5"));
    final RangeValidator validator2 =
        new RangeValidator(new BigDecimal("1.1"), new BigDecimal("2.5"));
    assertThat(validator1.hashCode(), not(validator2.hashCode()));
  }

  @Test
  public void testHashCodeDifferentMax() {
    final RangeValidator validator1 =
        new RangeValidator(new BigDecimal("1.5"), new BigDecimal("2.5"));
    final RangeValidator validator2 =
        new RangeValidator(new BigDecimal("1.5"), new BigDecimal("2.1"));
    assertThat(validator1.hashCode(), not(validator2.hashCode()));
  }
}
