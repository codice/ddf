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

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
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
  @Parameterized.Parameters(name = "FlatFilterBuilder impl {index}: {3}")
  public static Iterable<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {(Supplier<FlatFilterBuilder>) JsonModelBuilder::new, "And", "PropertyIsEqualTo", "JSON"},
          {(Supplier<FlatFilterBuilder>) XmlModelBuilder::new, "AND", "=", "XML"}
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
  public void testFailureCondition_BeginBinaryLogicType_WhenResultNotYetRetrieved() {
    setupDefaultTestValue(builder);
    builder.beginBinaryLogicType(logicalOp);
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureCondition_BeginBinaryLogicType_WhenTerminalNodeNotInProgress() {
    builder.beginBinaryComparisonType(comparisonOp).beginBinaryLogicType(logicalOp);
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureCondition_EndBinaryLogicType_WhenResultNotYetRetrieved() {
    setupDefaultTestValue(builder);
    builder.endBinaryLogicType();
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureCondition_EndBinaryLogicType_WhenTerminalNodeNotInProgress() {
    builder.beginBinaryComparisonType(comparisonOp).endBinaryLogicType();
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureCondition_EndBinaryLogicType_WhenResultNotNull() {
    builder.endBinaryLogicType();
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureCondition_BeginBinaryComparisonType_WhenResultNotYetRetrieved() {
    setupDefaultTestValue(builder);
    builder.beginBinaryComparisonType(comparisonOp);
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureCondition_BeginBinaryComparisonType_WhenTerminalNodeNotInProgresss() {
    builder
        .beginBinaryComparisonType(comparisonOp)
        .beginBinaryComparisonType("PropertyIsGreaterThan");
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureCondition_EndBinaryComparisonType_WhenResultNotYetRetrieved() {
    setupDefaultTestValue(builder);
    builder.endTerminalType();
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureCondition_GetResult_WhenTerminalNodeNotInProgress() {
    builder.beginBinaryComparisonType(comparisonOp).getResult();
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureCondition_GetResult_WhenResultNotNull() {
    builder.getResult();
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureCondition_SetProperty_WhenResultNotYetRetrieved() {
    setupDefaultTestValue(builder);
    builder.setProperty("property");
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureCondition_SetProperty_WhenTerminalNodeInProgress() {
    builder.setProperty("property");
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureCondition_SetValue_WhenResultNotYetRetrieved() {
    setupDefaultTestValue(builder);
    builder.setValue("value");
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureCondition_SetValue_WhenTerminalNodeInProgress() {
    builder.setValue("value");
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureCondition_SetTemplatedValues_WhenResultNotYetRetrieved() {
    setupDefaultTestValue(builder);
    builder.setTemplatedValues(
        ImmutableMap.of(
            "defaultValue", "5", "nodeId", "id", "isVisible", true, "isReadOnly", false));
  }

  @Test(expected = IllegalStateException.class)
  public void testFailureCondition_SetTemplatedValues_WhenTerminalNodeInProgress() {
    builder.setTemplatedValues(
        ImmutableMap.of(
            "defaultValue", "5", "nodeId", "id", "isVisible", true, "isReadOnly", false));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInput_BinaryLogicType_BadOperator() {
    builder.beginBinaryLogicType("bad");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInput_BinaryComparisonType_BadOperator() {
    builder.beginBinaryComparisonType("bad");
  }

  @Test(expected = IllegalStateException.class)
  public void testAwkwardCase_EndTerminalType_CalledTwiceIncorrectly() {
    builder
        .beginBinaryComparisonType(comparisonOp)
        .setProperty("name")
        .setValue("value")
        .endTerminalType()
        .endTerminalType();
  }

  @Test(expected = IllegalStateException.class)
  public void testAwkwardCase_EndTerminalType_CalledTwiceInLogicTypeIncorrectly() {
    builder
        .beginBinaryLogicType(logicalOp)
        .beginBinaryComparisonType(comparisonOp)
        .setProperty("name")
        .setValue("value")
        .endTerminalType()
        .endTerminalType();
  }

  @Test(expected = IllegalStateException.class)
  public void testAwkwardCase_EndBinaryLogicType_WhenNothingInProgress() {
    builder.endBinaryLogicType();
  }

  @Test(expected = IllegalStateException.class)
  public void testAwkwardCase_EndBinaryLogicType_WithoutAnyChildren() {
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
