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

import com.google.common.collect.ImmutableMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.xml.bind.JAXBElement;
import net.opengis.filter.v_2_0.BinaryComparisonOpType;
import net.opengis.filter.v_2_0.BinaryLogicOpType;
import net.opengis.filter.v_2_0.BinarySpatialOpType;
import net.opengis.filter.v_2_0.FilterType;
import net.opengis.filter.v_2_0.FunctionType;
import net.opengis.filter.v_2_0.LiteralType;
import net.opengis.filter.v_2_0.LogicOpsType;
import net.opengis.filter.v_2_0.ObjectFactory;
import net.opengis.filter.v_2_0.SpatialOpsType;
import net.opengis.filter.v_2_0.TemporalOpsType;

public class XmlModelBuilder implements FlatFilterBuilder<JAXBElement> {
  private static final ObjectFactory FACTORY = new ObjectFactory();

  /**
   * Helper interface for cleanly representing operations that condense a list of XML elements to a
   * single element. Use cases include the creation of logical op types in a filter structure, or
   * the population of values for a terminal type.
   */
  @FunctionalInterface
  private interface MultiNodeReducer extends Function<List<JAXBElement<?>>, JAXBElement> {}

  // Possibly use a ValueAdapter to circumvent difference in return type; i.e. Literal vs Object
  private static final Map<String, MultiNodeReducer> TERMINAL_OPS =
      ImmutableMap.<String, MultiNodeReducer>builder()
          .put("=", Mapper::equalTo)
          .put("!=", Mapper::notEqualTo)
          .put(">", Mapper::greaterThan)
          .put(">=", Mapper::greaterThanOrEqualTo)
          .put("<", Mapper::lessThan)
          .put("<=", Mapper::lessThanOrEqualTo)
          .put("INTERSECTS", Mapper::intersects)
          .build();

  private static final Map<String, MultiNodeReducer> LOGICAL_OPS =
      ImmutableMap.<String, MultiNodeReducer>builder()
          .put("AND", Mapper::and)
          .put("OR", Mapper::or)
          .build();

  /**
   * Creating {@link BinaryLogicOpType}s requires the entire list of child nodes, which won't be
   * known until {@link #endBinaryLogicType()}. Cache the operations here until that time.
   */
  private final Deque<MultiNodeReducer> logicOpCache;

  /**
   * Cache the lists of child elements here for arbitrary levels of filter depth due to {@link
   * BinaryLogicOpType}s.
   */
  private final Deque<List<JAXBElement<?>>> depth;

  private JAXBElement rootNode;

  private TerminalNodeSupplier supplierInProgress;

  private boolean complete = false;

  public XmlModelBuilder() {
    this.logicOpCache = new ArrayDeque<>();
    this.depth = new ArrayDeque<>();
  }

  @Override
  public JAXBElement getResult() {
    if (!complete) {
      verifyTerminalNodeNotInProgress();
      verifyLogicalNodeNotInProgress();
      verifyResultNotNull();
      complete = true;
    }
    return Mapper.filter(rootNode);
  }

  @Override
  public XmlModelBuilder beginBinaryLogicType(String operator) {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeNotInProgress();
    MultiNodeReducer logicMapping = LOGICAL_OPS.get(operator);
    if (logicMapping == null) {
      throw new IllegalArgumentException("Invalid operator for logic comparison type: " + operator);
    }
    logicOpCache.push(logicMapping);
    depth.push(new ArrayList<>());
    return this;
  }

  @Override
  public XmlModelBuilder endBinaryLogicType() {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeNotInProgress();
    verifyLogicalNodeInProgress();
    verifyLogicalNodeHasChildren();
    JAXBElement result = logicOpCache.pop().apply(depth.pop());
    if (!depth.isEmpty()) {
      depth.peek().add(result);
    } else {
      rootNode = result;
    }
    return this;
  }

  // Note: Currently taking in JSON type symbol as the "operator"
  @Override
  public XmlModelBuilder beginBinaryComparisonType(String operator) {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeNotInProgress();
    MultiNodeReducer comparisonMapping = TERMINAL_OPS.get(operator);
    if (comparisonMapping == null) {
      throw new IllegalArgumentException(
          "Cannot find mapping for binary comparison operator: " + operator);
    }
    supplierInProgress = new TerminalNodeSupplier(comparisonMapping);
    return this;
  }

  @Override
  public XmlModelBuilder beginBinarySpatialType(String operator) {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeNotInProgress();
    MultiNodeReducer spatialMapping = TERMINAL_OPS.get(operator);
    if (spatialMapping == null) {
      throw new IllegalArgumentException(
          "Cannot find mapping for binary spatial operator: " + operator);
    }
    supplierInProgress = new TerminalNodeSupplier(spatialMapping);
    return this;
  }

  @Override
  public XmlModelBuilder endTerminalType() {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeInProgress();
    if (depth.isEmpty()) {
      rootNode = supplierInProgress.get();
    } else {
      depth.peek().add(supplierInProgress.get());
    }
    supplierInProgress = null;
    return this;
  }

  @Override
  public XmlModelBuilder setProperty(String property) {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeInProgress();
    supplierInProgress.setProperty(FACTORY.createValueReference(property));
    return this;
  }

  @Override
  public XmlModelBuilder setValue(String value) {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeInProgress();
    supplierInProgress.setValue(
        FACTORY.createLiteral(new LiteralType().withContent(Collections.singletonList(value))));
    return this;
  }

  @Override
  public XmlModelBuilder setTemplatedValues(Map<String, Object> templateProps) {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeInProgress();

    String defaultValue = (String) templateProps.get("defaultValue");
    String nodeId = (String) templateProps.get("nodeId");
    boolean isVisible = (boolean) templateProps.get("isVisible");
    boolean isReadOnly = (boolean) templateProps.get("isReadOnly");

    supplierInProgress.setValue(
        FACTORY.createFunction(
            new FunctionType()
                .withName("template.value.v1")
                .withExpression(
                    FACTORY.createLiteral(
                        new LiteralType().withContent(Collections.singleton(defaultValue))),
                    FACTORY.createLiteral(
                        new LiteralType().withContent(Collections.singleton(nodeId))),
                    FACTORY.createLiteral(
                        new LiteralType()
                            .withContent(Collections.singleton(Boolean.toString(isVisible)))),
                    FACTORY.createLiteral(
                        new LiteralType()
                            .withContent(Collections.singleton(Boolean.toString(isReadOnly)))))));
    return this;
  }

  // CanModify
  private void verifyResultNotYetRetrieved() {
    if (complete) {
      throw new IllegalStateException(
          "This builder's result has been retrieved and no further modification is permitted");
    }
  }

  // CanSetField
  private void verifyTerminalNodeInProgress() {
    if (supplierInProgress == null) {
      throw new IllegalStateException("Cannot complete operation, no leaf node in progress");
    }
  }

  // ~
  private void verifyLogicalNodeInProgress() {
    if (depth.isEmpty()) {
      throw new IllegalStateException("Cannot end the logic node, no node in progress");
    }
  }

  // ~
  private void verifyLogicalNodeNotInProgress() {
    if (!depth.isEmpty()) {
      throw new IllegalStateException("Logic node in progress, results not ready for return");
    }
  }

  // ~
  private void verifyLogicalNodeHasChildren() {
    if (!depth.isEmpty() && depth.peek().isEmpty()) {
      throw new IllegalStateException("Cannot end the logic node, no children provided");
    }
  }

  // CanReturn
  private void verifyResultNotNull() {
    if (rootNode == null) {
      throw new IllegalStateException(
          "Cannot end the node or return a result, no data was specified");
    }
  }

  // CanStartNew
  // CanEnd
  private void verifyTerminalNodeNotInProgress() {
    if (supplierInProgress != null) {
      throw new IllegalStateException("Cannot complete operation, a leaf node is in progress");
    }
  }

  private static class TerminalNodeSupplier implements Supplier<JAXBElement<?>> {
    private final MultiNodeReducer reducer;
    private JAXBElement<String> propertyNode = null;
    private JAXBElement<?> valueNode;

    TerminalNodeSupplier(final MultiNodeReducer reducer) {
      notNull(reducer);
      this.reducer = reducer;
    }

    public void setProperty(JAXBElement<String> propertyNode) {
      notNull(propertyNode);
      this.propertyNode = propertyNode;
    }

    public void setValue(JAXBElement<?> valueNode) {
      notNull(valueNode);
      this.valueNode = valueNode;
    }

    @Override
    public JAXBElement<?> get() {
      notNull(propertyNode);
      notNull(valueNode);
      List<JAXBElement<?>> terminals = new ArrayList<>();
      terminals.add(propertyNode);
      terminals.add(valueNode);
      return reducer.apply(terminals);
    }
  }

  private static class Mapper {

    @SuppressWarnings("unchecked")
    private static JAXBElement<FilterType> filter(JAXBElement<?> root) {
      if (BinaryLogicOpType.class.equals(root.getDeclaredType())) {
        return FACTORY.createFilter(
            new FilterType().withLogicOps((JAXBElement<? extends LogicOpsType>) root));
      }
      if (BinaryComparisonOpType.class.equals(root.getDeclaredType())) {
        return FACTORY.createFilter(
            new FilterType()
                .withComparisonOps((JAXBElement<? extends BinaryComparisonOpType>) root));
      }
      if (SpatialOpsType.class.equals(root.getDeclaredType())) {
        return FACTORY.createFilter(
            new FilterType().withSpatialOps((JAXBElement<? extends SpatialOpsType>) root));
      }
      if (TemporalOpsType.class.equals(root.getDeclaredType())) {
        return FACTORY.createFilter(
            new FilterType().withTemporalOps((JAXBElement<? extends TemporalOpsType>) root));
      }

      throw new UnsupportedOperationException(
          "Unsupported type on filter: " + root.getDeclaredType().getName());
    }

    private static JAXBElement<BinaryLogicOpType> and(List<JAXBElement<?>> children) {
      return FACTORY.createAnd(binaryLogic(children));
    }

    private static JAXBElement<BinaryLogicOpType> or(List<JAXBElement<?>> children) {
      return FACTORY.createOr(binaryLogic(children));
    }

    private static JAXBElement<BinaryComparisonOpType> equalTo(List<JAXBElement<?>> children) {
      return FACTORY.createPropertyIsEqualTo(binaryComparison(children));
    }

    private static JAXBElement<BinaryComparisonOpType> notEqualTo(List<JAXBElement<?>> children) {
      return FACTORY.createPropertyIsNotEqualTo(binaryComparison(children));
    }

    private static JAXBElement<BinaryComparisonOpType> greaterThan(List<JAXBElement<?>> children) {
      return FACTORY.createPropertyIsGreaterThan(binaryComparison(children));
    }

    private static JAXBElement<BinaryComparisonOpType> greaterThanOrEqualTo(
        List<JAXBElement<?>> children) {
      return FACTORY.createPropertyIsGreaterThanOrEqualTo(binaryComparison(children));
    }

    private static JAXBElement<BinaryComparisonOpType> lessThan(List<JAXBElement<?>> children) {
      return FACTORY.createPropertyIsLessThan(binaryComparison(children));
    }

    private static JAXBElement<BinaryComparisonOpType> lessThanOrEqualTo(
        List<JAXBElement<?>> children) {
      return FACTORY.createPropertyIsLessThanOrEqualTo(binaryComparison(children));
    }

    private static JAXBElement<BinarySpatialOpType> intersects(List<JAXBElement<?>> children) {
      return FACTORY.createIntersects(binarySpatial(children));
    }

    private static BinaryLogicOpType binaryLogic(List<JAXBElement<?>> ops) {
      return new BinaryLogicOpType().withOps(ops);
    }

    private static BinaryComparisonOpType binaryComparison(List<JAXBElement<?>> children) {
      return new BinaryComparisonOpType().withExpression(children);
    }

    private static BinarySpatialOpType binarySpatial(List<JAXBElement<?>> children) {
      return new BinarySpatialOpType().withExpressionOrAny(new ArrayList<>(children));
    }
  }
}
