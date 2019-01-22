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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import ddf.catalog.data.AttributeRegistry;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;
import org.codice.ddf.catalog.ui.forms.builder.JsonModelBuilder;
import org.codice.ddf.catalog.ui.forms.builder.XmlModelBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Conformance test suite for implementations of {@link
 * org.codice.ddf.catalog.ui.forms.api.FlatFilterBuilder}s to ensure their states are being managed
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
  private static JsonModelBuilder json() {
    AttributeRegistry registry = mock(AttributeRegistry.class);
    when(registry.lookup(any())).thenReturn(Optional.empty());
    return new JsonModelBuilder(registry);
  }

  private static XmlModelBuilder xml() {
    AttributeRegistry registry = mock(AttributeRegistry.class);
    when(registry.lookup(any())).thenReturn(Optional.empty());
    return new XmlModelBuilder(registry);
  }

  @Parameterized.Parameters(name = "FlatFilterBuilder impl {index}: {3}")
  public static Iterable<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {
            (Supplier<FlatFilterBuilder>) FlatFilterBuilderTest::json,
            "And",
            "PropertyIsEqualTo",
            "JSON"
          },
          {(Supplier<FlatFilterBuilder>) FlatFilterBuilderTest::xml, "AND", "=", "XML"}
        });
  }

  private final Supplier<FlatFilterBuilder> builderSupplier;

  private final String logicalOp;

  private final String comparisonOp;

  private FlatFilterBuilder builder;

  public FlatFilterBuilderTest(
      Supplier<FlatFilterBuilder> builderSupplier,
      String logicalOp,
      String comparisonOp,
      // Label only used for identifying the parameterized run of the test
      String label) {
    this.builderSupplier = builderSupplier;
    this.logicalOp = logicalOp;
    this.comparisonOp = comparisonOp;
  }

  @Before
  public void setup() {
    this.builder = builderSupplier.get();
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureConditionBeginBinaryLogicTypeWhenResultNotYetRetrieved() {
    setupDefaultTestValue(builder);
    builder.beginBinaryLogicType(logicalOp);
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureConditionBeginBinaryLogicTypeWhenTerminalNodeNotInProgress() {
    builder.beginBinaryComparisonType(comparisonOp).beginBinaryLogicType(logicalOp);
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureConditionEndBinaryLogicTypeWhenResultNotYetRetrieved() {
    setupDefaultTestValue(builder);
    builder.endBinaryLogicType();
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureConditionEndBinaryLogicTypeWhenTerminalNodeNotInProgress() {
    builder.beginBinaryComparisonType(comparisonOp).endBinaryLogicType();
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureConditionEndBinaryLogicTypeWhenResultNotNull() {
    builder.endBinaryLogicType();
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureConditionBeginBinaryComparisonTypeWhenResultNotYetRetrieved() {
    setupDefaultTestValue(builder);
    builder.beginBinaryComparisonType(comparisonOp);
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureConditionBeginBinaryComparisonTypeWhenTerminalNodeNotInProgresss() {
    builder
        .beginBinaryComparisonType(comparisonOp)
        .beginBinaryComparisonType("PropertyIsGreaterThan");
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureConditionEndBinaryComparisonTypeWhenResultNotYetRetrieved() {
    setupDefaultTestValue(builder);
    builder.endTerminalType();
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureConditionGetResultWhenTerminalNodeNotInProgress() {
    builder.beginBinaryComparisonType(comparisonOp).getResult();
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureConditionGetResultWhenResultNotNull() {
    builder.getResult();
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureConditionSetPropertyWhenResultNotYetRetrieved() {
    setupDefaultTestValue(builder);
    builder.setProperty("property");
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureConditionSetPropertyWhenTerminalNodeInProgress() {
    builder.setProperty("property");
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureConditionSetValueWhenResultNotYetRetrieved() {
    setupDefaultTestValue(builder);
    builder.setValue("value");
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureConditionSetValueWhenTerminalNodeInProgress() {
    builder.setValue("value");
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureConditionSetTemplatedValuesWhenResultNotYetRetrieved() {
    setupDefaultTestValue(builder);
    builder.setTemplatedValues(
        ImmutableMap.of(
            "defaultValue", "5", "nodeId", "id", "isVisible", true, "isReadOnly", false));
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureConditionSetTemplatedValuesWhenTerminalNodeInProgress() {
    builder.setTemplatedValues(
        ImmutableMap.of(
            "defaultValue", "5", "nodeId", "id", "isVisible", true, "isReadOnly", false));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInputBinaryLogicTypeBadOperator() {
    builder.beginBinaryLogicType("bad");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInputBinaryComparisonTypeBadOperator() {
    builder.beginBinaryComparisonType("bad");
  }

  @Test(expected = IllegalStateException.class)
  public void testAwkwardCaseEndTerminalTypeCalledTwiceIncorrectly() {
    builder
        .beginBinaryComparisonType(comparisonOp)
        .setProperty("name")
        .setValue("value")
        .endTerminalType()
        .endTerminalType();
  }

  @Test(expected = IllegalStateException.class)
  public void testAwkwardCaseEndTerminalTypeCalledTwiceInLogicTypeIncorrectly() {
    builder
        .beginBinaryLogicType(logicalOp)
        .beginBinaryComparisonType(comparisonOp)
        .setProperty("name")
        .setValue("value")
        .endTerminalType()
        .endTerminalType();
  }

  @Test(expected = IllegalStateException.class)
  public void testAwkwardCaseEndBinaryLogicTypeWhenNothingInProgress() {
    builder.endBinaryLogicType();
  }

  @Test(expected = IllegalStateException.class)
  public void testAwkwardCaseEndBinaryLogicTypeWithoutAnyChildren() {
    builder.beginBinaryLogicType(logicalOp).endBinaryLogicType();
  }

  private void setupDefaultTestValue(FlatFilterBuilder builder) {
    builder
        .beginBinaryComparisonType(comparisonOp)
        .setProperty("name")
        .setValue("value")
        .endTerminalType()
        .getResult();
  }
}
