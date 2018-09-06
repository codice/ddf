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

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.codice.ddf.catalog.ui.filter.FlatFilterBuilder;
import org.codice.ddf.catalog.ui.filter.impl.builder.tools.NodeReducer;
import org.codice.ddf.catalog.ui.filter.impl.builder.tools.NodeSupplier;
import org.codice.ddf.catalog.ui.filter.impl.builder.tools.PropertyValueNodeSupplier;
import org.codice.ddf.catalog.ui.filter.impl.builder.tools.UnboundedNodeSupplier;
import org.codice.ddf.catalog.ui.filter.json.FilterJson;

/**
 * Single-use object for constructing a filter structure that is serializable to JSON, typically for
 * use on the frontend. Also supports building filter nodes with additional metadata in them.
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
public class JsonModelBuilder extends AbstractUnsupportedBuilder<Map<String, Object>>
    implements FlatFilterBuilder<Map<String, Object>> {
  private final Deque<NodeReducer<Map<String, Object>>> logicOpCache;

  private final Deque<List<Map<String, Object>>> depth;

  private NodeSupplier<Map<String, Object>> supplierInProgress = null;

  private Map<String, Object> rootNode = null;

  private boolean complete = false;

  public JsonModelBuilder() {
    this.logicOpCache = new ArrayDeque<>();
    this.depth = new ArrayDeque<>();
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
  public Map<String, Object> getResult() {
    if (!complete) {
      verifyTerminalNodeNotInProgress();
      verifyLogicalNodeNotInProgress();
      verifyResultNotNull();
      complete = true;
    }
    return rootNode;
  }

  @Override
  public JsonModelBuilder and() {
    beginBinaryLogicType(FilterJson.Ops.AND);
    return this;
  }

  @Override
  public JsonModelBuilder or() {
    beginBinaryLogicType(FilterJson.Ops.OR);
    return this;
  }

  @Override
  public JsonModelBuilder end() {
    if (supplierInProgress != null) {
      if (supplierInProgress.getParent() != null) {
        endFunctionType();
        return this;
      }
      endTerminalType();
      return this;
    }
    endBinaryLogicType();
    return this;
  }

  @Override
  public JsonModelBuilder isEqualTo(boolean matchCase) {
    beginTerminalType(FilterJson.Ops.EQ);
    return this;
  }

  @Override
  public JsonModelBuilder isNotEqualTo(boolean matchCase) {
    beginTerminalType(FilterJson.Ops.NOT_EQ);
    return this;
  }

  @Override
  public JsonModelBuilder isGreaterThan(boolean matchCase) {
    beginTerminalType(FilterJson.Ops.GT);
    return this;
  }

  @Override
  public JsonModelBuilder isGreaterThanOrEqualTo(boolean matchCase) {
    beginTerminalType(FilterJson.Ops.GT_OR_ET);
    return this;
  }

  @Override
  public JsonModelBuilder isLessThan(boolean matchCase) {
    beginTerminalType(FilterJson.Ops.LT);
    return this;
  }

  @Override
  public JsonModelBuilder isLessThanOrEqualTo(boolean matchCase) {
    beginTerminalType(FilterJson.Ops.LT_OR_ET);
    return this;
  }

  @Override
  public JsonModelBuilder like(
      boolean matchCase, String wildcard, String singleChar, String escape) {
    super.like(matchCase, wildcard, singleChar, escape);
    String operator = (matchCase) ? FilterJson.Ops.LIKE : FilterJson.Ops.ILIKE;
    beginTerminalType(operator);
    return this;
  }

  @Override
  public JsonModelBuilder before() {
    beginTerminalType(FilterJson.Ops.BEFORE);
    return this;
  }

  @Override
  public JsonModelBuilder after() {
    beginTerminalType(FilterJson.Ops.AFTER);
    return this;
  }

  @Override
  public JsonModelBuilder intersects() {
    beginTerminalType(FilterJson.Ops.INTERSECTS);
    return this;
  }

  @Override
  public JsonModelBuilder function(String name) {
    verifyResultNotYetRetrieved();
    // Per the 2.0 schema functions CAN be predicates themselves, but we've no use for the added
    // complexity so we validate against it. Supported can be added later if desired.
    verifyTerminalNodeInProgress(); // <--|
    supplierInProgress =
        new UnboundedNodeSupplier<>(maps -> reduceFunction(name, maps), supplierInProgress);
    return this;
  }

  @Override
  public JsonModelBuilder property(String name) {
    setProperty(name);
    return this;
  }

  @Override
  public JsonModelBuilder value(Serializable value) {
    setValue(value);
    return this;
  }

  @Override
  public JsonModelBuilder value(String value) {
    setValue(value);
    return this;
  }

  @Override
  public JsonModelBuilder value(boolean value) {
    setValue(value);
    return this;
  }

  private void beginBinaryLogicType(String operator) {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeNotInProgress();
    logicOpCache.push(maps -> reduceLogic(operator, maps));
    depth.push(new ArrayList<>());
  }

  private void beginTerminalType(String operator) {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeNotInProgress();
    supplierInProgress = new PropertyValueNodeSupplier<>(maps -> reduceTerminal(operator, maps));
  }

  private void endBinaryLogicType() {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeNotInProgress();
    verifyLogicalNodeInProgress();
    verifyLogicalNodeHasChildren();
    verifyLogicalNodeHasEnoughChildrenPerSchema();
    Map<String, Object> result = logicOpCache.pop().apply(depth.pop());
    if (depth.isEmpty()) {
      rootNode = result;
    } else {
      depth.peek().add(result);
    }
  }

  /**
   * Performs necessary validation and state transition to finish a function type's construction
   * that is in progress, particularly for support of nested functions.
   */
  private void endFunctionType() {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeInProgress();
    NodeSupplier<Map<String, Object>> parent = supplierInProgress.getParent();
    if (parent == null) {
      throw new IllegalStateException(
          "Null parent should not have passed the check in the end() method, "
              + "verify the implementation of JsonModelBuilder for errors");
    }

    Map<String, Object> result = supplierInProgress.get();
    parent.setNext(result);
    supplierInProgress = parent;
  }

  private void endTerminalType() {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeInProgress();
    if (depth.isEmpty()) {
      rootNode = supplierInProgress.get();
    } else {
      depth.peek().add(supplierInProgress.get());
    }
    supplierInProgress = null;
  }

  private void setProperty(String property) {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeInProgress();
    supplierInProgress.setNext(Collections.singletonMap(FilterJson.Keys.PROPERTY, property));
  }

  private void setValue(Serializable value) {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeInProgress();
    supplierInProgress.setNext(Collections.singletonMap(FilterJson.Keys.VALUE, value));
  }

  private Map<String, Object> reduceLogic(String op, List<Map<String, Object>> children) {
    Map<String, Object> node = new HashMap<>();
    node.put(FilterJson.Keys.TYPE, op);
    node.put(FilterJson.Keys.FILTERS, children);
    return node;
  }

  private Map<String, Object> reduceFunction(String name, List<Map<String, Object>> args) {
    Map<String, Object> node = new HashMap<>();
    node.put(FilterJson.Keys.TYPE, FilterJson.Ops.FUNC);
    node.put(FilterJson.Keys.NAME, name);
    node.put(
        FilterJson.Keys.PARAMS,
        args.stream().map(this::selectTerminalEntity).collect(Collectors.toList()));
    return node;
  }

  private Map<String, Object> reduceTerminal(String op, List<Map<String, Object>> children) {
    Map<String, Object> propertyExpression = children.get(0);
    Map<String, Object> valueExpression = children.get(1);
    Map<String, Object> result = new HashMap<>();
    result.put(FilterJson.Keys.TYPE, op);
    result.put(FilterJson.Keys.PROPERTY, selectTerminalEntity(propertyExpression));
    result.put(FilterJson.Keys.VALUE, selectTerminalEntity(valueExpression));
    return result;
  }

  /**
   * A terminal filter (the map) may contain a single entry as our component, or may itself BE the
   * component we're looking for.
   *
   * <p>For example, since a property declaration and a function are both valid expressions, we need
   * to handle the difference between maps like this:
   *
   * <pre>
   *     {
   *         "property": "title"
   *     }
   * </pre>
   *
   * And this:
   *
   * <pre>
   *     {
   *         "type": "FILTER_FUNCTION",
   *         "name": "myFunction",
   *         "params": [ ... ]
   *     }
   * </pre>
   *
   * Both are valid expressions.
   *
   * @param filter the Json filter itself; may only be a partial filter.
   * @throws IllegalArgumentException if the given filter has an unexpected format.
   * @return a stream of map entries that can be combined into a Json filter.
   */
  private Object selectTerminalEntity(Map<String, Object> filter) {
    if (filter.containsKey(FilterJson.Keys.TYPE)) {
      return filter;
    }
    if (filter.isEmpty() || filter.size() > 1) {
      throw new IllegalArgumentException(
          "Terminal filter expected to have exactly 1 entry " + filter.toString());
    }
    return filter.values().iterator().next();
  }

  private void verifyResultNotYetRetrieved() {
    if (complete) {
      throw new IllegalStateException(
          "This builder's result has been retrieved and no further modification is permitted");
    }
  }

  private void verifyTerminalNodeInProgress() {
    if (supplierInProgress == null) {
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

  private void verifyLogicalNodeHasEnoughChildrenPerSchema() {
    if (!depth.isEmpty() && depth.peek().size() < 2) {
      throw new IllegalStateException(
          "Expected at minimum 2 child filters in binary logic op filter, but found only "
              + depth.peek().size());
    }
  }

  private void verifyResultNotNull() {
    if (rootNode == null) {
      throw new IllegalStateException(
          "Cannot end the node or return a result, no data was specified");
    }
  }

  private void verifyTerminalNodeNotInProgress() {
    if (supplierInProgress != null) {
      throw new IllegalStateException("Cannot complete operation, a leaf node is in progress");
    }
  }
}
