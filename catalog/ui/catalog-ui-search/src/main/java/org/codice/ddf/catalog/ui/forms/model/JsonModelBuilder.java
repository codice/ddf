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

import com.google.common.collect.ImmutableSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Single-use object for constructing a {@link FilterNode} that is serializable to JSON, typically
 * for use on the frontend. Also supports building filter nodes with additional metadata in them,
 * such as {@link FilterNodeImpl}s in a templated state.
 *
 * <p>As mentioned before, this object is single-use and only supports building a single model. It
 * cannot be modified by builder methods once the result has been retrieved by calling {@link
 * #getResult()}, but multiple calls to {@link #getResult()} are allowed to access the same
 * reference to the resultant root node.
 *
 * <p>All filter nodes are constructed by opening (begin) them and closing (end) them, with data or
 * subsequent nodes specified in the middle as appropriate per a Filter's structure. Proper use of
 * this builder implies that all open nodes must be closed at some point before {@link #getResult()}
 * is called.
 *
 * <p><i>This code is experimental. While it is functional and tested, it may change or be removed
 * in a future version of the library.</i>
 */
public class JsonModelBuilder {
  private static final Set<String> BINARY_COMPARE_OPS =
      ImmutableSet.of("=", "!=", ">", ">=", "<", "<=");

  private static final Set<String> BINARY_SPATIAL_OPS = ImmutableSet.of("INTERSECTS");

  private static final Set<String> LOGIC_COMPARE_OPS = ImmutableSet.of("AND", "OR");

  private final Deque<List<FilterNode>> depth;

  private FilterNode rootNode = null;

  private FilterNode nodeInProgress = null;

  private boolean complete = false;

  public JsonModelBuilder() {
    depth = new ArrayDeque<>();
  }

  /**
   * Retrieve the root of the filter tree that this builder was creating, and prevent subsequent
   * builder operations upon this object. Can reliably be called multiple times to retrieve the same
   * result. The result will not be null.
   *
   * <p><i>This code is experimental. While it is functional and tested, it may change or be removed
   * in a future version of the library.</i>
   *
   * @return the root of the filter tree that this builder was creating.
   * @throws IllegalStateException if begun/open nodes are not properly ended/closed or if no data
   *     exists to return.
   */
  public FilterNode getResult() {
    if (!complete) {
      canEnd();
      canReturn();
      depth.clear();
      complete = true;
    }
    return rootNode;
  }

  /**
   * Begin describing a binary logic operation. Supports nested structures.
   *
   * @param operator either "AND" or "OR" to denote the logical operation to perform (case
   *     sensitive).
   * @return {@code this} model builder to continue the fluent API.
   * @throws IllegalStateException if this builder can no longer be modified, or if a leaf node has
   *     begun but was never ended.
   */
  public JsonModelBuilder beginBinaryLogicType(String operator) {
    canModify();
    canStartNew();
    if (!LOGIC_COMPARE_OPS.contains(operator)) {
      throw new IllegalArgumentException("Invalid operator for logic comparison type: " + operator);
    }
    List<FilterNode> nodes = new ArrayList<>();
    if (rootNode == null) {
      rootNode = new FilterNodeImpl(operator, nodes);
    } else {
      depth.peek().add(new FilterNodeImpl(operator, nodes));
    }
    depth.push(nodes);
    return this;
  }

  public JsonModelBuilder endBinaryLogicType() {
    canModify();
    canEnd();
    canReturn();
    depth.pop();
    return this;
  }

  public JsonModelBuilder beginBinaryComparisonType(String operator) {
    canModify();
    canStartNew();
    if (!BINARY_COMPARE_OPS.contains(operator)) {
      throw new IllegalArgumentException(
          "Invalid operator for binary comparison type: " + operator);
    }
    nodeInProgress = new FilterNodeImpl(operator);
    return this;
  }

  public JsonModelBuilder beginBinarySpatialType(String operator) {
    canModify();
    canStartNew();
    if (!BINARY_SPATIAL_OPS.contains(operator)) {
      throw new IllegalArgumentException("Invalid operator for binary spatial type: " + operator);
    }
    nodeInProgress = new FilterNodeImpl(operator);
    return this;
  }

  public JsonModelBuilder endTerminalType() {
    canModify();
    if (depth.isEmpty() && rootNode != null) {
      throw new IllegalStateException("If stack is empty, the root node should not be initialized");
    }
    if (depth.isEmpty()) {
      rootNode = nodeInProgress;
    } else {
      depth.peek().add(nodeInProgress);
    }
    nodeInProgress = null;
    return this;
  }

  public JsonModelBuilder setProperty(String property) {
    canModify();
    canSetField();
    nodeInProgress.setProperty(property);
    return this;
  }

  public JsonModelBuilder setValue(String value) {
    canModify();
    canSetField();
    nodeInProgress.setValue(value);
    return this;
  }

  public JsonModelBuilder setTemplatedValues(
      String defaultValue, String nodeId, boolean isVisible, boolean isReadOnly) {
    canModify();
    canSetField();

    Map<String, Object> templateProps = new HashMap<>();
    templateProps.put("defaultValue", defaultValue);
    templateProps.put("nodeId", nodeId);
    templateProps.put("isVisible", isVisible);
    templateProps.put("isReadOnly", isReadOnly);

    nodeInProgress = new FilterNodeImpl(nodeInProgress, templateProps);
    return this;
  }

  private void canModify() {
    if (complete) {
      throw new IllegalStateException(
          "This builder's result has been retrieved and no further modification is permitted");
    }
  }

  private void canSetField() {
    if (nodeInProgress == null) {
      throw new IllegalStateException("Cannot set field, no leaf node in progress");
    }
  }

  private void canStartNew() {
    if (nodeInProgress != null) {
      throw new IllegalStateException("Cannot start node, a leaf node in progress");
    }
  }

  private void canReturn() {
    if (rootNode == null) {
      throw new IllegalStateException(
          "Cannot end the node or return a result, no data was specified");
    }
  }

  private void canEnd() {
    if (nodeInProgress != null) {
      throw new IllegalStateException("Cannot end node, a leaf node in progress");
    }
  }
}
