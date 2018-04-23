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
package org.codice.ddf.catalog.ui.forms.model;

import java.util.Arrays;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Conformance test suite for implementations of {@link FlatFilterBuilder}s to ensure their states
 * are being managed correctly. This test suite validates correct use, not necessarily expected
 * results.
 *
 * <p>The below test names follow a simple convention: (builder method) + (validation method)
 *
 * <p>This way the test names clearly define a mapping between builder function and the verification
 * that occurs behind the scenes. Refer to the state checks found in the builder implementations.
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
  public void testBeginBinaryLogicTypeCanModify() {
    setupDefaultTestValue(builder);
    builder.beginBinaryLogicType(logicalOp);
  }

  @Test(expected = IllegalStateException.class)
  public void testBeginBinaryLogicTypeCanStartNew() {
    builder.beginBinaryComparisonType(comparisonOp).beginBinaryLogicType(logicalOp);
  }

  @Test(expected = IllegalStateException.class)
  public void testEndBinaryLogicTypeCanModify() {
    setupDefaultTestValue(builder);
    builder.endBinaryLogicType();
  }

  @Test(expected = IllegalStateException.class)
  public void testEndBinaryLogicTypeCanEnd() {
    builder.beginBinaryComparisonType(comparisonOp).endBinaryLogicType();
  }

  @Test(expected = IllegalStateException.class)
  public void testEndBinaryLogicTypeCanReturn() {
    builder.endBinaryLogicType();
  }

  @Test(expected = IllegalStateException.class)
  public void testBeginBinaryComparisonTypeCanModify() {
    setupDefaultTestValue(builder);
    builder.beginBinaryComparisonType(comparisonOp);
  }

  @Test(expected = IllegalStateException.class)
  public void testBeginBinaryComparisonTypeCanStartNew() {
    builder
        .beginBinaryComparisonType(comparisonOp)
        .beginBinaryComparisonType("PropertyIsGreaterThan");
  }

  @Test(expected = IllegalStateException.class)
  public void testEndBinaryComparisonTypeCanModify() {
    setupDefaultTestValue(builder);
    builder.endTerminalType();
  }

  @Test(expected = IllegalStateException.class)
  public void testGetResultCanEnd() {
    builder.beginBinaryComparisonType(comparisonOp).getResult();
  }

  @Test(expected = IllegalStateException.class)
  public void testGetResultCanReturn() {
    builder.getResult();
  }

  @Test(expected = IllegalStateException.class)
  public void testSetPropertyCanModify() {
    setupDefaultTestValue(builder);
    builder.setProperty("property");
  }

  @Test(expected = IllegalStateException.class)
  public void testSetPropertyCanSetField() {
    builder.setProperty("property");
  }

  @Test(expected = IllegalStateException.class)
  public void testSetValueCanModify() {
    setupDefaultTestValue(builder);
    builder.setValue("value");
  }

  @Test(expected = IllegalStateException.class)
  public void testSetValueCanSetField() {
    builder.setValue("value");
  }

  @Test(expected = IllegalStateException.class)
  public void testSetTemplatedValuesCanModify() {
    setupDefaultTestValue(builder);
    builder.setTemplatedValues("default", "id", true, false);
  }

  @Test(expected = IllegalStateException.class)
  public void testSetTemplatedValuesCanSetField() {
    builder.setTemplatedValues("default", "id", true, false);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBinaryLogicTypeBadOperator() {
    builder.beginBinaryLogicType("bad");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBinaryComparisonTypeBadOperator() {
    builder.beginBinaryComparisonType("bad");
  }

  @Test(expected = IllegalStateException.class)
  public void testEndTerminalNodeTwice() {
    builder
        .beginBinaryComparisonType(comparisonOp)
        .setProperty("name")
        .setValue("value")
        .endTerminalType()
        .endTerminalType();
  }

  @Test(expected = IllegalStateException.class)
  public void testEndTerminalNodeInCompound() {
    builder
        .beginBinaryLogicType(logicalOp)
        .beginBinaryComparisonType(comparisonOp)
        .setProperty("name")
        .setValue("value")
        .endTerminalType()
        .endTerminalType();
  }

  @Test(expected = IllegalStateException.class)
  public void testEndBinaryLogicWhenNoneInProgress() {
    builder.endBinaryLogicType();
  }

  @Test(expected = IllegalStateException.class)
  public void testEndBinaryLogicWithoutAnyChildren() {
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
