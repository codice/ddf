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

import java.util.List;
import javax.annotation.Nullable;
import org.boon.json.annotations.JsonIgnore;
import org.boon.json.annotations.JsonProperty;

/**
 * Provides data model pojo that can be annotated and sent to Boon for JSON serialization.
 *
 * <p>{@link FilterNode} is the simplest representation of a filter node and the top of the node
 * inheritence hierarchy. It almost always contains child nodes in {@link FilterNode#getNodes()}
 * because it typically represents logical operations. Currently {@link FilterNode#isLeaf()} means
 * it's safe to cast to {@link FilterLeafNode} but that may not hold true in the future.
 *
 * <p><i>This code is experimental. While it is functional and tested, it may change or be removed
 * in a future version of the library.</i>
 */
public class FilterNode {
  // The operator for this node represented as a string
  private final String type;

  @JsonIgnore private boolean isLeaf;

  @JsonProperty("filters")
  private List<FilterNode> nodes;

  public FilterNode(String type, List<FilterNode> nodes) {
    this.type = type;
    this.nodes = nodes;
    this.isLeaf = false;
  }

  public String getType() {
    return type;
  }

  public boolean isLeaf() {
    return isLeaf;
  }

  @Nullable
  public List<FilterNode> getNodes() {
    return nodes;
  }

  protected void setLeaf(boolean isLeaf) {
    this.isLeaf = isLeaf;
  }

  public void setNodes(List<FilterNode> nodes) {
    this.nodes = nodes;
  }
}
