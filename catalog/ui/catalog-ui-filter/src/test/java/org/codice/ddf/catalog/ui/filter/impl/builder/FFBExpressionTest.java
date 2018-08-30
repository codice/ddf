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
package org.codice.ddf.catalog.ui.filter.impl.builder;

import java.util.function.Supplier;
import org.codice.ddf.catalog.ui.filter.FlatFilterBuilder;
import org.junit.Test;

/**
 * Validate the use of a builder when constructing schema compliant expressions. Sample violations
 * include not respecting the property-value 2-expression requirement on most terminal nodes or
 * providing a function with zero arguments.
 */
public class FFBExpressionTest extends AbstractFlatFilterBuilderTest {
  public FFBExpressionTest(Supplier<FlatFilterBuilder> builderSupplier, String label) {
    super(builderSupplier, label);
  }

  @Test(expected = IllegalStateException.class)
  public void testEndBinaryComparisonTypeWithoutData() {
    builder().isEqualTo(false).end();
  }

  @Test(expected = IllegalStateException.class)
  public void testEndBinaryComparisonTypeWithOnlyProperty() {
    builder().isEqualTo(false).property("name").end();
  }

  @Test(expected = IllegalStateException.class)
  public void testEndBinaryComparisonTypeWithOnlyValueString() {
    builder().isEqualTo(false).value("value").end();
  }

  @Test(expected = IllegalStateException.class)
  public void testEndBinaryComparisonTypeWithOnlyValueBool() {
    builder().isEqualTo(false).value(false).end();
  }

  @Test(expected = IllegalStateException.class)
  public void testEndFunctionWithZeroArguments() {
    builder().function("name").end();
  }

  @Test(expected = IllegalStateException.class) // TODO Move elsewhere this is schema enforcement
  public void testFailureConditionFunctionWhenTerminalNodeNotInProgress() {
    // This may be a valid use of .function("")
    builder().function("name");
  }
}
