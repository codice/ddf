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
package org.codice.ddf.catalog.ui.forms.builder;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import ddf.catalog.data.AttributeRegistry;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.codice.ddf.catalog.ui.forms.api.FilterNode;
import org.codice.ddf.catalog.ui.forms.api.FlatFilterBuilder;
import org.codice.ddf.catalog.ui.forms.model.FilterNodeImpl;

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
public class JsonModelBuilder implements FlatFilterBuilder<FilterNode> {
  private static final Map<String, String> BINARY_COMPARE_MAPPING =
      ImmutableMap.<String, String>builder()
          .put("PropertyIsEqualTo", "=")
          .put("PropertyIsNotEqualTo", "!=")
          .put("PropertyIsLessThan", "<")
          .put("PropertyIsLessThanOrEqualTo", "<=")
          .put("PropertyIsGreaterThan", ">")
          .put("PropertyIsGreaterThanOrEqualTo", ">=")
          .build();

  private static final String PROPERTY_IS_LIKE = "PropertyIsLike";

  private static final String ILIKE = "ILIKE";

  private static final String LIKE = "LIKE";

  private static final Map<String, String> BINARY_TEMPORAL_MAPPING =
      ImmutableMap.<String, String>builder()
          .put("Before", "BEFORE")
          .put("After", "AFTER")
          // used for 'date BETWEEN' ops by the UI - contains a range delineated by a slash '/'
          .put("During", "DURING")
          .build();

  private static final Set<String> BINARY_SPATIAL_OPS = ImmutableSet.of("INTERSECTS", "DWITHIN");

  private static final Set<String> LOGIC_COMPARE_OPS = ImmutableSet.of("AND", "OR");

  private final Deque<List<FilterNode>> depth;

  private final AttributeValueNormalizer normalizer;

  private FilterNode rootNode = null;

  private FilterNode nodeInProgress = null;

  private boolean complete = false;

  public JsonModelBuilder(AttributeRegistry registry) {
    this.depth = new ArrayDeque<>();
    this.normalizer = new AttributeValueNormalizer(registry);
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
  @Override
  public FilterNode getResult() {
    if (!complete) {
      verifyTerminalNodeNotInProgress();
      verifyLogicalNodeNotInProgress();
      verifyResultNotNull();
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
  @Override
  public JsonModelBuilder beginBinaryLogicType(String operator) {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeNotInProgress();
    operator = operator.toUpperCase();
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

  @Override
  public JsonModelBuilder endBinaryLogicType() {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeNotInProgress();
    verifyLogicalNodeInProgress();
    verifyLogicalNodeHasChildren();
    depth.pop();
    return this;
  }

  // Note: Currently taking in the XML local part as the "operator"
  @Override
  public JsonModelBuilder beginBinaryComparisonType(String operator) {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeNotInProgress();
    String jsonOperator = BINARY_COMPARE_MAPPING.get(operator);
    if (jsonOperator == null) {
      throw new IllegalArgumentException(
          "Cannot find mapping for binary comparison operator: " + operator);
    }
    nodeInProgress = new FilterNodeImpl(jsonOperator);
    return this;
  }

  @Override
  public JsonModelBuilder beginPropertyIsLikeType(String operator, boolean matchCase) {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeNotInProgress();
    if (!PROPERTY_IS_LIKE.equals(operator)) {
      throw new IllegalArgumentException("Cannot find mapping for like operator: " + operator);
    }
    String jsonOperator = (matchCase) ? LIKE : ILIKE;
    nodeInProgress = new FilterNodeImpl(jsonOperator);
    return this;
  }

  @Override
  public JsonModelBuilder beginBinaryTemporalType(String operator) {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeNotInProgress();
    String jsonOperator = BINARY_TEMPORAL_MAPPING.get(operator);
    if (jsonOperator == null) {
      throw new IllegalArgumentException(
          "Cannot find mapping for binary temporal operator: " + operator);
    }
    nodeInProgress = new FilterNodeImpl(jsonOperator);
    return this;
  }

  @Override
  public JsonModelBuilder beginBinarySpatialType(String operator) {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeNotInProgress();
    if (!BINARY_SPATIAL_OPS.contains(operator)) {
      throw new IllegalArgumentException("Invalid operator for binary spatial type: " + operator);
    }
    nodeInProgress = new FilterNodeImpl(operator);
    return this;
  }

  @Override
  public JsonModelBuilder beginNilType() {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeNotInProgress();
    // used for date 'IS EMPTY' ops by the UI
    nodeInProgress = new FilterNodeImpl("IS NULL");
    return this;
  }

  @Override
  public JsonModelBuilder addFunctionType(String functionName, List<Object> args) {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeNotInProgress();

    FilterNode funcNode = new FilterNodeImpl("FILTER_FUNCTION", functionName);
    args.forEach(funcNode::addArg);

    if (depth.isEmpty()) {
      rootNode = funcNode;
    } else {
      depth.peek().add(funcNode);
    }
    return this;
  }

  @Override
  public JsonModelBuilder addBetweenType(String property, Long lower, Long upper) {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeNotInProgress();

    FilterNode between = new FilterNodeImpl("BETWEEN");
    between.setProperty(property);

    Map<String, Object> value = new HashMap<>();
    value.put("lower", lower);
    value.put("upper", upper);
    between.setValue(value);

    if (depth.isEmpty()) {
      rootNode = between;
    } else {
      depth.peek().add(between);
    }
    return this;
  }

  @Override
  public JsonModelBuilder endTerminalType() {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeInProgress();
    if (depth.isEmpty()) {
      rootNode = nodeInProgress;
    } else {
      depth.peek().add(nodeInProgress);
    }
    nodeInProgress = null;
    return this;
  }

  @Override
  public JsonModelBuilder setProperty(String property) {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeInProgress();
    nodeInProgress.setProperty(property);
    return this;
  }

  @Override
  public JsonModelBuilder setProperty(String functionName, List<Object> args) {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeInProgress();

    FilterNode innerNode = new FilterNodeImpl("FILTER_FUNCTION", functionName);
    args.forEach(innerNode::addArg);

    nodeInProgress.setProperty(innerNode);
    return this;
  }

  @Override
  public JsonModelBuilder setValue(String value) {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeInProgress();

    if (nodeInProgress.getPropertyFunction() != null) {
      nodeInProgress.setValue(Boolean.parseBoolean(value));
    } else {
      String normalizedValue = normalizer.normalizeForJson(nodeInProgress.getProperty(), value);
      nodeInProgress.setValue(normalizedValue);
    }

    return this;
  }

  @Override
  public JsonModelBuilder setDistance(Double distance) {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeInProgress();
    nodeInProgress.setDistance(distance);
    return this;
  }

  @Override
  public JsonModelBuilder setTemplatedValues(Map<String, Object> templateProps) {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeInProgress();
    nodeInProgress = new FilterNodeImpl(nodeInProgress, templateProps);
    return this;
  }

  private void verifyResultNotYetRetrieved() {
    if (complete) {
      throw new IllegalStateException(
          "This builder's result has been retrieved and no further modification is permitted");
    }
  }

  private void verifyTerminalNodeInProgress() {
    if (nodeInProgress == null) {
      throw new IllegalStateException("Cannot complete operation, no leaf node in progress");
    }
  }

  // Verify coverage: https://codice.atlassian.net/browse/DDF-3832
  private void verifyLogicalNodeInProgress() {
    if (depth.isEmpty()) {
      throw new IllegalStateException("Cannot end the logic node, no node in progress");
    }
  }

  // Verify coverage: https://codice.atlassian.net/browse/DDF-3832
  private void verifyLogicalNodeNotInProgress() {
    if (!depth.isEmpty()) {
      throw new IllegalStateException("Logic node in progress, results not ready for return");
    }
  }

  // Verify coverage: https://codice.atlassian.net/browse/DDF-3832
  private void verifyLogicalNodeHasChildren() {
    if (!depth.isEmpty() && depth.peek().isEmpty()) {
      throw new IllegalStateException("Cannot end the logic node, no children provided");
    }
  }

  private void verifyResultNotNull() {
    if (rootNode == null) {
      throw new IllegalStateException(
          "Cannot end the node or return a result, no data was specified");
    }
  }

  private void verifyTerminalNodeNotInProgress() {
    if (nodeInProgress != null) {
      throw new IllegalStateException("Cannot complete operation, a leaf node is in progress");
    }
  }
}
