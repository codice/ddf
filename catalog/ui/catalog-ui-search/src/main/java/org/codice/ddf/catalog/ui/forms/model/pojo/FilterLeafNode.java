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
package org.codice.ddf.catalog.ui.forms.model.pojo;

/**
 * Provides data model pojo that can be annotated and sent to Boon for JSON serialization.
 *
 * <p>{@link FilterLeafNode} represents basic terminal nodes such as comparisons, and supports all
 * node types that reference a property and a value. {@link FilterLeafNode#getNodes()} should always
 * return null because this class of node should not have children.
 *
 * <p>This type of node, along with subclasses, support additional fields to be attached to the node
 * beyond the UI's defaults. For example, {@link FilterLeafNode#templated} helps the UI determine if
 * additional processing is necessary because additional template data is present. Currently {@link
 * FilterLeafNode#isTemplated()} means it's safe to cast to {@link FilterTemplatedLeafNode} but that
 * may not hold true in the future.
 *
 * <p><i>This code is experimental. While it is functional and tested, it may change or be removed
 * in a future version of the library.</i>
 */
public class FilterLeafNode extends FilterNode {
  private String property;

  // If changed, update the FilterNodeValueSerializer as well
  private String value;

  private boolean templated;

  public FilterLeafNode(String type) {
    super(type, null);
    setLeaf(true);
    setTemplated(false);
  }

  public FilterLeafNode(FilterLeafNode node) {
    super(node.getType(), null);
    setLeaf(true);
    setProperty(node.getProperty());
    setValue(node.getValue());
    setTemplated(node.isTemplated());
  }

  public String getProperty() {
    return property;
  }

  public String getValue() {
    return value;
  }

  public boolean isTemplated() {
    return templated;
  }

  public void setProperty(String property) {
    this.property = property;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public void setTemplated(boolean templated) {
    this.templated = templated;
  }
}
