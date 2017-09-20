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
package ddf.catalog.filter.delegate;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Validation;
import ddf.catalog.source.UnsupportedQueryException;
import java.util.Arrays;
import java.util.Date;
import org.junit.Test;

public class ValidationQueryDelegateTest {

  private static ValidationQueryDelegate testValidationQueryDelegate =
      new ValidationQueryDelegate();

  @Test
  public void testOrTrueTrue() {
    assertThat(testValidationQueryDelegate.or(Arrays.asList(true, true)), is(true));
  }

  @Test
  public void testOrTrueFalse() {
    assertThat(testValidationQueryDelegate.or(Arrays.asList(true, false)), is(true));
  }

  @Test
  public void testOrFalseFalse() {
    assertThat(testValidationQueryDelegate.or(Arrays.asList(false, false)), is(false));
  }

  @Test
  public void testPropertyIsEqualToStringCaseSensitive() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsEqualTo(Metacard.ANY_TEXT, "helloworld"), is(false));
  }

  @Test
  public void testPropertyIsEqualToDate() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsEqualTo(Metacard.ANY_TEXT, mock(Date.class)),
        is(false));
  }

  @Test
  public void testPropertyIsEqualToInt() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsEqualTo(Metacard.ANY_TEXT, (int) 0), is(false));
  }

  @Test
  public void testPropertyIsEqualToShort() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsEqualTo(Metacard.ANY_TEXT, (short) 0), is(false));
  }

  @Test
  public void testPropertyIsEqualToLong() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsEqualTo(Metacard.ANY_TEXT, (long) 0), is(false));
  }

  @Test
  public void testPropertyIsEqualToFloat() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsEqualTo(Metacard.ANY_TEXT, (float) 0), is(false));
  }

  @Test
  public void testPropertyIsEqualToDouble() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsEqualTo(Metacard.ANY_TEXT, (double) 0), is(false));
  }

  @Test
  public void testPropertyIsEqualToByteArray() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsEqualTo(Metacard.ANY_TEXT, new byte[0]), is(false));
  }

  @Test
  public void testPropertyIsEqualToObject() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsEqualTo(Metacard.ANY_TEXT, (Object) 0), is(false));
  }

  @Test
  public void testPropertyIsNotEqualToString() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsNotEqualTo(Metacard.ANY_TEXT, "helloworld"),
        is(false));
  }

  @Test
  public void testPropertyIsNotEqualToDate() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsNotEqualTo(Metacard.ANY_TEXT, mock(Date.class)),
        is(false));
  }

  @Test
  public void testPropertyIsNotEqualToInt() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsNotEqualTo(Metacard.ANY_TEXT, (int) 0), is(false));
  }

  @Test
  public void testPropertyIsNotEqualToShort() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsNotEqualTo(Metacard.ANY_TEXT, (short) 0), is(false));
  }

  @Test
  public void testPropertyIsNotEqualToLong() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsNotEqualTo(Metacard.ANY_TEXT, (long) 0), is(false));
  }

  @Test
  public void testPropertyIsNotEqualToFloat() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsNotEqualTo(Metacard.ANY_TEXT, (float) 0), is(false));
  }

  @Test
  public void testPropertyIsNotEqualToDouble() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsNotEqualTo(Metacard.ANY_TEXT, (double) 0), is(false));
  }

  @Test
  public void testPropertyIsNotEqualToByteArray() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsNotEqualTo(Metacard.ANY_TEXT, new byte[0]),
        is(false));
  }

  @Test
  public void testPropertyIsNotEqualToObject() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsNotEqualTo(Metacard.ANY_TEXT, (Object) 0), is(false));
  }

  @Test
  public void testPropertyIsGreaterThanString() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsNotEqualTo(Metacard.ANY_TEXT, "helloworld"),
        is(false));
  }

  @Test
  public void testPropertyIsGreaterThanDate() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsNotEqualTo(Metacard.ANY_TEXT, mock(Date.class)),
        is(false));
  }

  @Test
  public void testPropertyIsGreaterThanInt() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsGreaterThan(Metacard.ANY_TEXT, (int) 0), is(false));
  }

  @Test
  public void testPropertyIsGreaterThanShort() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsGreaterThan(Metacard.ANY_TEXT, (short) 0), is(false));
  }

  @Test
  public void testPropertyIsGreaterThanLong() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsGreaterThan(Metacard.ANY_TEXT, (long) 0), is(false));
  }

  @Test
  public void testPropertyIsGreaterThanFloat() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsGreaterThan(Metacard.ANY_TEXT, (float) 0), is(false));
  }

  @Test
  public void testPropertyIsGreaterThanDouble() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsGreaterThan(Metacard.ANY_TEXT, (double) 0),
        is(false));
  }

  @Test
  public void testPropertyIsGreaterThanByteArray() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsGreaterThan(Metacard.ANY_TEXT, new byte[0]),
        is(false));
  }

  @Test
  public void testPropertyIsGreaterThanObject() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsGreaterThan(Metacard.ANY_TEXT, (Object) 0),
        is(false));
  }

  @Test
  public void testPropertyIsGreaterThanOrEqualToString() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsGreaterThanOrEqualTo(Metacard.ANY_TEXT, "helloworld"),
        is(false));
  }

  @Test
  public void testPropertyIsGreaterThanOrEqualToDate() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsGreaterThanOrEqualTo(
            Metacard.ANY_TEXT, mock(Date.class)),
        is(false));
  }

  @Test
  public void testPropertyIsGreaterThanOrEqualToInt() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsGreaterThanOrEqualTo(Metacard.ANY_TEXT, (int) 0),
        is(false));
  }

  @Test
  public void testPropertyIsGreaterThanOrEqualToShort() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsGreaterThanOrEqualTo(Metacard.ANY_TEXT, (short) 0),
        is(false));
  }

  @Test
  public void testPropertyIsGreaterThanOrEqualToLong() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsGreaterThanOrEqualTo(Metacard.ANY_TEXT, (long) 0),
        is(false));
  }

  @Test
  public void testPropertyIsGreaterThanOrEqualToFloat() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsGreaterThanOrEqualTo(Metacard.ANY_TEXT, (float) 0),
        is(false));
  }

  @Test
  public void testPropertyIsGreaterThanOrEqualToDouble() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsGreaterThanOrEqualTo(Metacard.ANY_TEXT, (double) 0),
        is(false));
  }

  @Test
  public void testPropertyIsGreaterThanOrEqualToByteArray() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsGreaterThanOrEqualTo(Metacard.ANY_TEXT, new byte[0]),
        is(false));
  }

  @Test
  public void testPropertyIsGreaterThanOrEqualToObject() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsGreaterThanOrEqualTo(Metacard.ANY_TEXT, (Object) 0),
        is(false));
  }

  @Test
  public void testPropertyIsLessThanString() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsLessThan(Metacard.ANY_TEXT, "helloworld"), is(false));
  }

  @Test
  public void testPropertyIsLessThanDate() {
    assertThat(
        testValidationQueryDelegate.propertyIsLessThan(Metacard.ANY_TEXT, mock(Date.class)),
        is(false));
  }

  @Test
  public void testPropertyIsLessThanInt() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsLessThan(Metacard.ANY_TEXT, (int) 0), is(false));
  }

  @Test
  public void testPropertyIsLessThanShort() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsLessThan(Metacard.ANY_TEXT, (short) 0), is(false));
  }

  @Test
  public void testPropertyIsLessThanLong() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsLessThan(Metacard.ANY_TEXT, (long) 0), is(false));
  }

  @Test
  public void testPropertyIsLessThanFloat() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsLessThan(Metacard.ANY_TEXT, (float) 0), is(false));
  }

  @Test
  public void testPropertyIsLessThanDouble() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsLessThan(Metacard.ANY_TEXT, (double) 0), is(false));
  }

  @Test
  public void testPropertyIsLessThanByteArray() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsLessThan(Metacard.ANY_TEXT, new byte[0]), is(false));
  }

  @Test
  public void testPropertyIsLessThanObject() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsLessThan(Metacard.ANY_TEXT, (Object) 0), is(false));
  }

  @Test
  public void testPropertyIsLessThanOrEqualToString() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsLessThanOrEqualTo(Metacard.ANY_TEXT, "helloworld"),
        is(false));
  }

  @Test
  public void testPropertyIsLessThanOrEqualToDate() {
    assertThat(
        testValidationQueryDelegate.propertyIsLessThanOrEqualTo(
            Metacard.ANY_TEXT, mock(Date.class)),
        is(false));
  }

  @Test
  public void testPropertyIsLessThanOrEqualToInt() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsLessThanOrEqualTo(Metacard.ANY_TEXT, (int) 0),
        is(false));
  }

  @Test
  public void testPropertyIsLessThanOrEqualToShort() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsLessThanOrEqualTo(Metacard.ANY_TEXT, (short) 0),
        is(false));
  }

  @Test
  public void testPropertyIsLessThanOrEqualToLong() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsLessThanOrEqualTo(Metacard.ANY_TEXT, (long) 0),
        is(false));
  }

  @Test
  public void testPropertyIsLessThanOrEqualToFloat() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsLessThanOrEqualTo(Metacard.ANY_TEXT, (float) 0),
        is(false));
  }

  @Test
  public void testPropertyIsLessThanOrEqualToDouble() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsLessThanOrEqualTo(Metacard.ANY_TEXT, (double) 0),
        is(false));
  }

  @Test
  public void testPropertyIsLessThanOrEqualToByteArray() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsLessThanOrEqualTo(Metacard.ANY_TEXT, new byte[0]),
        is(false));
  }

  @Test
  public void testPropertyIsLessThanOrEqualToObject() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsLessThanOrEqualTo(Metacard.ANY_TEXT, (Object) 0),
        is(false));
  }

  @Test
  public void testPropertyIsBetweenString() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsBetween(
            Metacard.ANY_TEXT, "helloworld", "helloworld"),
        is(false));
  }

  @Test
  public void testPropertyIsBetweenDate() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsBetween(Metacard.ANY_TEXT, new Date(), new Date()),
        is(false));
  }

  @Test
  public void testPropertyIsBetweenInt() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsBetween(Metacard.ANY_TEXT, (int) 0, (int) 0),
        is(false));
  }

  @Test
  public void testPropertyIsBetweenShort() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsBetween(Metacard.ANY_TEXT, (short) 0, (short) 0),
        is(false));
  }

  @Test
  public void testPropertyIsBetweenLong() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsBetween(Metacard.ANY_TEXT, (long) 0, (long) 0),
        is(false));
  }

  @Test
  public void testPropertyIsBetweenFloat() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsBetween(Metacard.ANY_TEXT, (float) 0, (float) 0),
        is(false));
  }

  @Test
  public void testPropertyIsBetweenDouble() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsBetween(Metacard.ANY_TEXT, (double) 0, (double) 0),
        is(false));
  }

  @Test
  public void testPropertyIsBetweenByteArray() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsBetween(Metacard.ANY_TEXT, new byte[0], new byte[0]),
        is(false));
  }

  @Test
  public void testPropertyIsBetweenObject() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsBetween(Metacard.ANY_TEXT, (Object) 0, (Object) 0),
        is(false));
  }

  @Test
  public void testPropertyIsNullTrueValidationErrors() throws UnsupportedQueryException {
    assertThat(testValidationQueryDelegate.propertyIsNull(Validation.VALIDATION_ERRORS), is(true));
  }

  @Test
  public void testPropertyIsNullTrueValidationWarnings() throws UnsupportedQueryException {
    assertThat(
        testValidationQueryDelegate.propertyIsNull(Validation.VALIDATION_WARNINGS), is(true));
  }

  @Test
  public void testPropertyIsNullFalse() throws UnsupportedQueryException {
    assertThat(testValidationQueryDelegate.propertyIsNull("test"), is(false));
  }

  @Test
  public void testPropertyIsLikeFalse() {
    assertThat(
        testValidationQueryDelegate.propertyIsLike(Metacard.ANY_TEXT, "helloworld", true),
        is(false));
  }

  @Test
  public void testPropertyIsLikeTrueErrors() {
    assertThat(
        testValidationQueryDelegate.propertyIsLike(
            Validation.VALIDATION_ERRORS, "sample-validator", true),
        is(true));
  }

  @Test
  public void testPropertyIsLikeTrueWarnings() {
    assertThat(
        testValidationQueryDelegate.propertyIsLike(
            Validation.VALIDATION_WARNINGS, "sample-validator", true),
        is(true));
  }

  @Test
  public void testPropertyIsFuzzy() {
    assertThat(
        testValidationQueryDelegate.propertyIsFuzzy(Metacard.ANY_TEXT, "helloworld"), is(false));
  }

  @Test
  public void testXpathExists() {
    assertThat(testValidationQueryDelegate.xpathExists("some/xpath/"), is(false));
  }

  @Test
  public void testXpathIsLike() {
    assertThat(
        testValidationQueryDelegate.xpathIsLike("some/xpath/", "helloworld", true), is(false));
  }

  @Test
  public void testXpathIsFuzzy() {
    assertThat(testValidationQueryDelegate.xpathIsFuzzy("some/xpath/", "helloworld"), is(false));
  }

  @Test
  public void testBeyond() {
    assertThat(
        testValidationQueryDelegate.beyond(Metacard.ANY_TEXT, "thisisaWKT", (double) 0), is(false));
  }

  @Test
  public void testContains() {
    assertThat(testValidationQueryDelegate.contains(Metacard.ANY_TEXT, "thisisaWKT"), is(false));
  }

  @Test
  public void testCrosses() {
    assertThat(testValidationQueryDelegate.crosses(Metacard.ANY_TEXT, "thisisaWKT"), is(false));
  }

  @Test
  public void testDisjoint() {
    assertThat(testValidationQueryDelegate.disjoint(Metacard.ANY_TEXT, "thisisaWKT"), is(false));
  }

  @Test
  public void testDwithin() {
    assertThat(
        testValidationQueryDelegate.dwithin(Metacard.ANY_TEXT, "thisisaWKT", (double) 0),
        is(false));
  }

  @Test
  public void testIntersects() {
    assertThat(testValidationQueryDelegate.intersects(Metacard.ANY_TEXT, "thisisaWKT"), is(false));
  }

  @Test
  public void testOverlaps() {
    assertThat(testValidationQueryDelegate.overlaps(Metacard.ANY_TEXT, "thisisaWKT"), is(false));
  }

  @Test
  public void testTouches() {
    assertThat(testValidationQueryDelegate.touches(Metacard.ANY_TEXT, "thisisaWKT"), is(false));
  }

  @Test
  public void testWithin() {
    assertThat(testValidationQueryDelegate.within(Metacard.ANY_TEXT, "thisisaWKT"), is(false));
  }

  @Test
  public void testAfter() {
    assertThat(testValidationQueryDelegate.after(Metacard.ANY_TEXT, mock(Date.class)), is(false));
  }

  @Test
  public void testBefore() {
    assertThat(testValidationQueryDelegate.before(Metacard.ANY_TEXT, mock(Date.class)), is(false));
  }

  @Test
  public void testDuring() {
    assertThat(
        testValidationQueryDelegate.during(Metacard.ANY_TEXT, new Date(), new Date()), is(false));
  }

  @Test
  public void testRelative() {
    assertThat(testValidationQueryDelegate.relative(Metacard.ANY_TEXT, (long) 0), is(false));
  }
}
