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
 * Verifying the terminal node state of a {@link FlatFilterBuilder} ensures that core methods are
 * being used appropriately and in the correct order. They help enforce the following correct use:
 *
 * <pre>
 *     builder
 *       .like(false, "*", "?", "\")
 *         .property("title")
 *         .value("*headlines from paris*")
 *       .end();
 * </pre>
 *
 * And report misuse:
 *
 * <pre>
 *     builder
 *       .like(false, "*", "?", "\")
 *         .isEqualTo(false)
 *         ...
 *
 *     builder
 *       .like(false, "*", "?", "\")
 *         .and()
 *         ...
 * </pre>
 */
public class FFBTerminalNodeStateTest extends AbstractFlatFilterBuilderTest {
  public FFBTerminalNodeStateTest(Supplier<FlatFilterBuilder> builderSupplier, String label) {
    super(builderSupplier, label);
  }

  @Test(expected = IllegalStateException.class)
  public void testBinaryLogicTypeWhenTerminalNodeInProgress() {
    builder().isEqualTo(false).and();
  }

  @Test(expected = IllegalStateException.class)
  public void testBinaryComparisonTypeWhenTerminalNodeInProgresss() {
    builder().isEqualTo(false).isEqualTo(false);
  }

  @Test(expected = IllegalStateException.class)
  public void testGetResultWhenTerminalNodeInProgress() {
    builder().isEqualTo(false).getResult();
  }

  @Test(expected = IllegalStateException.class)
  public void testPropertyWhenTerminalNodeNotInProgress() {
    builder().property("property");
  }

  @Test(expected = IllegalStateException.class)
  public void testValueWhenTerminalNodeNotInProgress() {
    builder().value("value");
  }
}
