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

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.codice.ddf.catalog.ui.forms.model.JsonModel.FilterLeafNode;
import org.codice.ddf.catalog.ui.forms.model.JsonModel.FilterNode;

/** As more test cases are added, more support functions will be needed. */
class FilterNodeAssertionSupport {
  private FilterNodeAssertionSupport() {}

  static void assertLeafNode(
      FilterNode node, String expectedType, String expectedProperty, String expectedValue) {
    assertThat(node.getType(), is(expectedType));
    assertThat(node.getNodes(), is(nullValue()));
    assertThat(FilterLeafNode.class.isInstance(node), is(true));

    FilterLeafNode leaf = (FilterLeafNode) node;
    assertThat(leaf.getProperty(), is(expectedProperty));
    assertThat(leaf.getValue(), is(expectedValue));
  }
}
