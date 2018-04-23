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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;

import java.util.Map;

/** As more test cases are added, more support functions will be needed. */
class FilterNodeAssertionSupport {
  private FilterNodeAssertionSupport() {}

  static void assertParentNode(FilterNode node, String expectedType, int expectedChildCount) {
    assertParentNode(node, expectedType);
    assertThat(node.getChildren(), hasSize(expectedChildCount));
  }

  static void assertParentNode(FilterNode node, String expectedType) {
    assertThat(node.getOperator(), is(expectedType));
    assertThat(node.getChildren(), notNullValue());
    assertThat(node.isLeaf(), is(false));
  }

  static void assertLeafNode(
      FilterNode node, String expectedType, String expectedProperty, String expectedValue) {
    assertThat(node.getOperator(), is(expectedType));
    assertThat(node.isLeaf(), is(true));

    assertThat(node.getProperty(), is(expectedProperty));
    assertThat(node.getValue(), is(expectedValue));
    assertThat(node.isTemplated(), is(false));
  }

  static void assertTemplatedNode(
      FilterNode node,
      String expectedType,
      String expectedProperty,
      String defaultValue,
      String nodeId) {
    assertThat(node.getOperator(), is(expectedType));
    assertThat(node.isLeaf(), is(true));

    assertThat(node.getProperty(), is(expectedProperty));
    assertThat(node.getValue(), is(nullValue()));
    assertThat(node.isTemplated(), is(true));

    Map<String, Object> templateProps = ((FilterNodeImpl) node).getTemplateProperties();
    assertThat(templateProps.get("defaultValue"), is(defaultValue));
    assertThat(templateProps.get("nodeId"), is(nodeId));
  }
}
