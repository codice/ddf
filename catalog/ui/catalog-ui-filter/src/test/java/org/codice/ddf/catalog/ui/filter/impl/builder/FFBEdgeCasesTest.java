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
 * Verify misc edge and corner cases, with particular interest to schema compliance. Example
 * violations might include a logic filter without any children:
 *
 * <pre>
 *     builder.and().end();
 * </pre>
 *
 * Or calling {@code end()} incorrectly:
 *
 * <pre>
 *     builder.end();
 *     builder.isEqualTo(false).property("name").value("value").end().end();
 * </pre>
 */
public class FFBEdgeCasesTest extends AbstractFlatFilterBuilderTest {
  public FFBEdgeCasesTest(Supplier<FlatFilterBuilder> builderSupplier, String label) {
    super(builderSupplier, label);
  }

  @Test(expected = IllegalStateException.class)
  public void testEndWhenNothingWasBuilt() {
    builder().end();
  }

  @Test(expected = IllegalStateException.class)
  public void testGetResultWhenNothingWasBuilt() {
    builder().getResult();
  }

  @Test(expected = IllegalStateException.class)
  public void testEndTerminalTypeCalledTwiceIncorrectly() {
    builder().isEqualTo(false).property("name").value("value").end().end();
  }

  @Test(expected = IllegalStateException.class)
  public void testEndBinaryLogicTypeWithoutAnyChildren() {
    builder().and().end();
  }

  @Test(expected = IllegalStateException.class)
  public void testEndBinaryLogicTypeWithOnlyOneChild() {
    builder().and().isEqualTo(false).property("name").value("value").end().end();
  }
}
