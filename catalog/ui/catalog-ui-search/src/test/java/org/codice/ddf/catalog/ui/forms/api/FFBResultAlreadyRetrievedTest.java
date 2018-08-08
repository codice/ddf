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
package org.codice.ddf.catalog.ui.forms.api;

import java.util.function.Supplier;
import org.codice.ddf.catalog.ui.filter.FlatFilterBuilder;
import org.junit.Test;

/**
 * The sole purpose of this conformance test suite is to ensure {@link FlatFilterBuilder}s do not
 * allow continued use after the result has been retrieved:
 *
 * <pre>
 *     builder.isEqualTo(false).property("name").value("bob").end().getResult();
 *     builder.and(); // IllegalStateException will be thrown
 * </pre>
 */
public class FFBResultAlreadyRetrievedTest extends AbstractFlatFilterBuilderTest {
  public FFBResultAlreadyRetrievedTest(Supplier<FlatFilterBuilder> builderSupplier, String label) {
    super(builderSupplier, label);
  }

  @Test(expected = IllegalStateException.class)
  public void testAndWhenResultWasAlreadyRetrieved() {
    setupDefaultTestValueAndGetResult();
    builder().and();
  }

  @Test(expected = IllegalStateException.class)
  public void testOrWhenResultWasAlreadyRetrieved() {
    setupDefaultTestValueAndGetResult();
    builder().or();
  }

  @Test(expected = IllegalStateException.class)
  public void testEndWhenResultWasAlreadyRetrieved() {
    setupDefaultTestValueAndGetResult();
    builder().end();
  }

  @Test(expected = IllegalStateException.class)
  public void testIsEqualToWhenResultWasAlreadyRetrieved() {
    setupDefaultTestValueAndGetResult();
    builder().isEqualTo(false);
  }

  @Test(expected = IllegalStateException.class)
  public void testIsNotEqualToWhenResultWasAlreadyRetrieved() {
    setupDefaultTestValueAndGetResult();
    builder().isNotEqualTo(false);
  }

  @Test(expected = IllegalStateException.class)
  public void testIsGreaterThanWhenResultWasAlreadyRetrieved() {
    setupDefaultTestValueAndGetResult();
    builder().isGreaterThan(false);
  }

  @Test(expected = IllegalStateException.class)
  public void testIsGreaterThanOrEqualToWhenResultWasAlreadyRetrieved() {
    setupDefaultTestValueAndGetResult();
    builder().isGreaterThanOrEqualTo(false);
  }

  @Test(expected = IllegalStateException.class)
  public void testIsLessThanWhenResultWasAlreadyRetrieved() {
    setupDefaultTestValueAndGetResult();
    builder().isLessThan(false);
  }

  @Test(expected = IllegalStateException.class)
  public void testIsLessThanOrEqualToWhenResultWasAlreadyRetrieved() {
    setupDefaultTestValueAndGetResult();
    builder().isLessThanOrEqualTo(false);
  }

  @Test(expected = IllegalStateException.class)
  public void testLikeWhenResultWasAlreadyRetrieved() {
    setupDefaultTestValueAndGetResult();
    builder().like(false, "", "", "");
  }

  @Test(expected = IllegalStateException.class)
  public void testBeforeWhenResultWasAlreadyRetrieved() {
    setupDefaultTestValueAndGetResult();
    builder().before();
  }

  @Test(expected = IllegalStateException.class)
  public void testAfterWhenResultWasAlreadyRetrieved() {
    setupDefaultTestValueAndGetResult();
    builder().after();
  }

  @Test(expected = IllegalStateException.class)
  public void testIntersectsWhenResultWasAlreadyRetrieved() {
    setupDefaultTestValueAndGetResult();
    builder().intersects();
  }

  @Test(expected = IllegalStateException.class)
  public void testPropertyWhenResultWasAlreadyRetrieved() {
    setupDefaultTestValueAndGetResult();
    builder().property("property");
  }

  @Test(expected = IllegalStateException.class)
  public void testValueStringWhenResultWasAlreadyRetrieved() {
    setupDefaultTestValueAndGetResult();
    builder().value("value");
  }

  @Test(expected = IllegalStateException.class)
  public void testValueBoolWhenResultWasAlreadyRetrieved() {
    setupDefaultTestValueAndGetResult();
    builder().value(false);
  }

  @Test(expected = IllegalStateException.class)
  public void testFunctionWhenResultWasAlreadyRetrieved() {
    setupDefaultTestValueAndGetResult();
    builder().function("name");
  }
}
