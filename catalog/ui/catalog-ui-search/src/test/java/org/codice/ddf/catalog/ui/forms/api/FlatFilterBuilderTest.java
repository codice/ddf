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

import java.util.Arrays;
import java.util.function.Supplier;
import org.codice.ddf.catalog.ui.filter.FlatFilterBuilder;
import org.codice.ddf.catalog.ui.forms.builder.JsonModelBuilder;
import org.codice.ddf.catalog.ui.forms.builder.XmlModelBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Conformance test suite for implementations of {@link
 * org.codice.ddf.catalog.ui.filter.FlatFilterBuilder}s to ensure their states are being managed
 * correctly. This test suite validates correct use, not necessarily expected results.
 *
 * <p>The below test names follow a simple convention: {@code (context) + (builder method) +
 * (validation method or meaningful commentary)}. There are several contexts currently in use.
 *
 * <ol>
 *   <li>Failure Condition - these map directly to the validation methods found in builder impls
 *   <li>Input - ensure implementations validate input correctly, to the degree the contract states
 *   <li>Awkward Case - misc tests that don't map to validation methods but are still useful, if not
 *       immediately obvious
 * </ol>
 *
 * <p>Most of the time, test names clearly define a mapping between builder function and the
 * verification that occurs behind the scenes. Refer to the state checks found in the builder
 * implementations.
 */
@RunWith(Parameterized.class)
public class FlatFilterBuilderTest {
  @Parameterized.Parameters(name = "FlatFilterBuilder impl {index}: {1}")
  public static Iterable<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {(Supplier<FlatFilterBuilder>) JsonModelBuilder::new, "JSON"},
          {(Supplier<FlatFilterBuilder>) XmlModelBuilder::new, "XML"}
        });
  }

  private final Supplier<FlatFilterBuilder> builderSupplier;

  private FlatFilterBuilder builder;

  public FlatFilterBuilderTest(
      Supplier<FlatFilterBuilder> builderSupplier,
      // Label only used for identifying the parameterized run of the test
      String label) {
    this.builderSupplier = builderSupplier;
  }

  @Before
  public void setup() {
    this.builder = builderSupplier.get();
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureConditionBeginBinaryLogicTypeWhenResultNotYetRetrieved() {
    setupDefaultTestValue(builder);
    builder.and();
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureConditionBeginBinaryLogicTypeWhenTerminalNodeNotInProgress() {
    builder.isEqualTo(false).and();
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureConditionEndBinaryLogicTypeWhenResultNotYetRetrieved() {
    setupDefaultTestValue(builder);
    builder.end();
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureConditionEndBinaryLogicTypeWhenTerminalNodeNotInProgress() {
    builder.isEqualTo(false).end();
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureConditionEndBinaryLogicTypeWhenResultNotNull() {
    builder.end();
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureConditionBeginBinaryComparisonTypeWhenResultNotYetRetrieved() {
    setupDefaultTestValue(builder);
    builder.isEqualTo(false);
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureConditionBeginBinaryComparisonTypeWhenTerminalNodeNotInProgresss() {
    builder.isEqualTo(false).isGreaterThan(false);
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureConditionEndBinaryComparisonTypeWhenResultNotYetRetrieved() {
    setupDefaultTestValue(builder);
    builder.end();
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureConditionGetResultWhenTerminalNodeNotInProgress() {
    builder.isEqualTo(false).getResult();
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureConditionGetResultWhenResultNotNull() {
    builder.getResult();
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureConditionSetPropertyWhenResultNotYetRetrieved() {
    setupDefaultTestValue(builder);
    builder.property("property");
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureConditionSetPropertyWhenTerminalNodeInProgress() {
    builder.property("property");
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureConditionSetValueWhenResultNotYetRetrieved() {
    setupDefaultTestValue(builder);
    builder.value("value");
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureConditionSetValueWhenTerminalNodeInProgress() {
    builder.value("value");
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureConditionFunctionWhenResultNotYetRetrieved() {
    setupDefaultTestValue(builder);
    builder.function("name");
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureConditionFunctionWhenTerminalNodeInProgress() {
    builder.function("name");
  }

  @Test(expected = IllegalStateException.class)
  public void testAwkwardCaseEndTerminalTypeCalledTwiceIncorrectly() {
    builder.isEqualTo(false).property("name").value("value").end().end();
  }

  @Test(expected = IllegalStateException.class)
  public void testAwkwardCaseEndBinaryLogicTypeWithOnlyOneChild() {
    builder.and().isEqualTo(false).property("name").value("value").end().end();
  }

  @Test(expected = IllegalStateException.class)
  public void testAwkwardCaseEndWhenNothingInProgress() {
    builder.end();
  }

  @Test(expected = IllegalStateException.class)
  public void testAwkwardCaseEndBinaryLogicTypeWithoutAnyChildren() {
    builder.and().end();
  }

  private void setupDefaultTestValue(FlatFilterBuilder builder) {
    builder.isEqualTo(false).property("name").value("value").end().getResult();
  }
}
