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

import static org.apache.commons.lang3.Validate.notNull;

import java.util.List;
import org.boon.json.annotations.JsonProperty;
import org.codice.ddf.catalog.ui.forms.api.FilterNode;

public class IntermediateFilterNode implements FilterNode {
  @JsonProperty("type")
  private final String operator;

  @JsonProperty("filters")
  private final List<FilterNode> children;

  public IntermediateFilterNode(final String operator, final List<FilterNode> children) {
    notNull(operator);
    notNull(children);
    this.operator = operator;
    this.children = children;
  }

  @Override
  public String getOperator() {
    return operator;
  }

  @Override
  public void accept(Visitor visitor) {
    visitor.visit(this);
  }

  /**
   * If this node is a non-terminal node, fetch its list of children.
   *
   * @return a collection of filter nodes.
   * @throws IllegalStateException if this node was a terminal node and does not have children.
   * @throws IllegalArgumentException if invalid nodes were encountered while reading the children.
   */
  public List<FilterNode> getChildren() {
    return children;
  }
}
