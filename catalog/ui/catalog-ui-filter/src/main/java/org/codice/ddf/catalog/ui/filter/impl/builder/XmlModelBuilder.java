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
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAttribute;
import net.opengis.filter.v_2_0.BinaryComparisonOpType;
import net.opengis.filter.v_2_0.BinaryLogicOpType;
import net.opengis.filter.v_2_0.BinarySpatialOpType;
import net.opengis.filter.v_2_0.BinaryTemporalOpType;
import net.opengis.filter.v_2_0.ComparisonOpsType;
import net.opengis.filter.v_2_0.FilterType;
import net.opengis.filter.v_2_0.FunctionType;
import net.opengis.filter.v_2_0.LiteralType;
import net.opengis.filter.v_2_0.LogicOpsType;
import net.opengis.filter.v_2_0.ObjectFactory;
import net.opengis.filter.v_2_0.PropertyIsLikeType;
import net.opengis.filter.v_2_0.SpatialOpsType;
import net.opengis.filter.v_2_0.TemporalOpsType;
import org.codice.ddf.catalog.ui.filter.FlatFilterBuilder;
import org.codice.ddf.catalog.ui.filter.impl.builder.tools.NodeReducer;
import org.codice.ddf.catalog.ui.filter.impl.builder.tools.NodeSupplier;
import org.codice.ddf.catalog.ui.filter.impl.builder.tools.PropertyValueNodeSupplier;
import org.codice.ddf.catalog.ui.filter.impl.builder.tools.UnboundedNodeSupplier;

public class XmlModelBuilder implements FlatFilterBuilder<JAXBElement> {
  private static final ObjectFactory FACTORY = new ObjectFactory();

  /**
   * Creating {@link BinaryLogicOpType}s requires the entire list of child nodes, which won't be
   * known until {@link #endBinaryLogicType()}. Cache the operations here until that time.
   */
  private final Deque<NodeReducer<JAXBElement<?>>> logicOpCache;

  /**
   * Cache the lists of child elements here for arbitrary levels of filter depth due to {@link
   * BinaryLogicOpType}s.
   */
  private final Deque<List<JAXBElement<?>>> depth;

  private NodeSupplier<JAXBElement<?>> supplierInProgress;

  private JAXBElement<?> rootNode;

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
  public XmlModelBuilder not() {
    throw new UnsupportedOperationException();
  }

  @Override
  public XmlModelBuilder and() {
    beginBinaryLogicType(Mapper::and);
    return this;
  }

  @Override
  public XmlModelBuilder or() {
    beginBinaryLogicType(Mapper::or);
    return this;
  }

  @Override
  public XmlModelBuilder end() {
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
  public XmlModelBuilder isEqualTo(boolean matchCase) {
    beginTerminalType(Mapper::equalTo);
    return this;
  }

  @Override
  public XmlModelBuilder isNotEqualTo(boolean matchCase) {
    beginTerminalType(Mapper::notEqualTo);
    return this;
  }

  @Override
  public XmlModelBuilder isGreaterThan(boolean matchCase) {
    beginTerminalType(Mapper::greaterThan);
    return this;
  }

  @Override
  public XmlModelBuilder isGreaterThanOrEqualTo(boolean matchCase) {
    beginTerminalType(Mapper::greaterThanOrEqualTo);
    return this;
  }

  @Override
  public XmlModelBuilder isLessThan(boolean matchCase) {
    beginTerminalType(Mapper::lessThan);
    return this;
  }

  @Override
  public XmlModelBuilder isLessThanOrEqualTo(boolean matchCase) {
    beginTerminalType(Mapper::lessThanOrEqualTo);
    return this;
  }

  @Override
  public XmlModelBuilder like(
      boolean matchCase, String wildcard, String singleChar, String escape) {
    if (matchCase) {
      beginTerminalType(Mapper::likeMatchCase);
      return this;
    }
    beginTerminalType(Mapper::like);
    return this;
  }

  @Override
  public XmlModelBuilder before() {
    beginTerminalType(Mapper::before);
    return this;
  }

  @Override
  public XmlModelBuilder after() {
    beginTerminalType(Mapper::after);
    return this;
  }

  @Override
  public XmlModelBuilder intersects() {
    beginTerminalType(Mapper::intersects);
    return this;
  }

  @Override
  public XmlModelBuilder dwithin(double distance, String units) {
    throw new UnsupportedOperationException(
        "DWITHIN is not currently supported, use INTERSECTS instead");
  }

  @Override
  public XmlModelBuilder function(String name) {
    verifyResultNotYetRetrieved();
    // Per the 2.0 schema functions CAN be predicates themselves, but we've no use for the added
    // complexity so we validate against it. Supported can be added later if desired.
    verifyTerminalNodeInProgress(); // <--|
    supplierInProgress =
        new UnboundedNodeSupplier<>(
            args ->
                FACTORY.createFunction(
                    new FunctionType().withName(name).withExpression(new ArrayList<>(args))),
            supplierInProgress);
    return this;
  }

  @Override
  public XmlModelBuilder property(String name) {
    setProperty(name);
    return this;
  }

  @Override
  public XmlModelBuilder value(String value) {
    setValue(value);
    return this;
  }

  @Override
  public XmlModelBuilder value(Serializable value) {
    setValue(value);
    return this;
  }

  @Override
  public XmlModelBuilder value(boolean value) {
    // Unable to marshal type "java.lang.Boolean" as an element because it is missing an
    // @XmlRootElement annotation
    setValue(Boolean.toString(value));
    return this;
  }

  private void beginBinaryLogicType(NodeReducer<JAXBElement<?>> reducer) {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeNotInProgress();
    logicOpCache.push(reducer);
    depth.push(new ArrayList<>());
  }

  private void beginTerminalType(NodeReducer<JAXBElement<?>> reducer) {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeNotInProgress();
    supplierInProgress = new PropertyValueNodeSupplier<>(reducer);
  }

  /**
   * Performs necessary validation and state transition to finish a binary logic type's construction
   * that is in progress.
   */
  private void endBinaryLogicType() {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeNotInProgress();
    verifyLogicalNodeInProgress();
    verifyLogicalNodeHasChildren();
    verifyLogicalNodeHasEnoughChildrenPerSchema();
    JAXBElement result = logicOpCache.pop().apply(depth.pop());
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
    NodeSupplier<JAXBElement<?>> parent = supplierInProgress.getParent();
    if (parent == null) {
      throw new NullPointerException(
          // By definition, if the parent was null we should not have been called
          "Null parent should not have passed the check in the end() method, "
              + "verify the implementation of XmlModelBuilder for errors");
    }

    JAXBElement<?> result = supplierInProgress.get();
    parent.setNext(result);
    supplierInProgress = parent;
  }

  /**
   * Performs the necessary validation and state transition to finish a terminal type's construction
   * that is in progress.
   */
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
    supplierInProgress.setNext(FACTORY.createValueReference(property));
  }

  private void setValue(Serializable value) {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeInProgress();
    supplierInProgress.setNext(
        FACTORY.createLiteral(new LiteralType().withContent(Collections.singletonList(value))));
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

  private static class Mapper {

    @SuppressWarnings("unchecked")
    private static JAXBElement<FilterType> filter(JAXBElement<?> root) {
      if (LogicOpsType.class.isAssignableFrom(root.getDeclaredType())) {
        return FACTORY.createFilter(
            new FilterType().withLogicOps((JAXBElement<? extends LogicOpsType>) root));
      }
      if (ComparisonOpsType.class.isAssignableFrom(root.getDeclaredType())) {
        return FACTORY.createFilter(
            new FilterType()
                .withComparisonOps((JAXBElement<? extends BinaryComparisonOpType>) root));
      }
      if (SpatialOpsType.class.isAssignableFrom(root.getDeclaredType())) {
        return FACTORY.createFilter(
            new FilterType().withSpatialOps((JAXBElement<? extends SpatialOpsType>) root));
      }
      if (TemporalOpsType.class.isAssignableFrom(root.getDeclaredType())) {
        return FACTORY.createFilter(
            new FilterType().withTemporalOps((JAXBElement<? extends TemporalOpsType>) root));
      }

      throw new UnsupportedOperationException(
          "Attempting to build XML filter with unsupported element: "
              + root.getDeclaredType().getName());
    }

    private static JAXBElement<BinaryLogicOpType> and(List<JAXBElement<?>> children) {
      return FACTORY.createAnd(binaryLogicType(children));
    }

    private static JAXBElement<BinaryLogicOpType> or(List<JAXBElement<?>> children) {
      return FACTORY.createOr(binaryLogicType(children));
    }

    private static JAXBElement<BinaryComparisonOpType> equalTo(List<JAXBElement<?>> children) {
      return FACTORY.createPropertyIsEqualTo(binaryComparisonType(children));
    }

    private static JAXBElement<BinaryComparisonOpType> notEqualTo(List<JAXBElement<?>> children) {
      return FACTORY.createPropertyIsNotEqualTo(binaryComparisonType(children));
    }

    private static JAXBElement<BinaryComparisonOpType> greaterThan(List<JAXBElement<?>> children) {
      return FACTORY.createPropertyIsGreaterThan(binaryComparisonType(children));
    }

    private static JAXBElement<BinaryComparisonOpType> greaterThanOrEqualTo(
        List<JAXBElement<?>> children) {
      return FACTORY.createPropertyIsGreaterThanOrEqualTo(binaryComparisonType(children));
    }

    private static JAXBElement<BinaryComparisonOpType> lessThan(List<JAXBElement<?>> children) {
      return FACTORY.createPropertyIsLessThan(binaryComparisonType(children));
    }

    private static JAXBElement<BinaryComparisonOpType> lessThanOrEqualTo(
        List<JAXBElement<?>> children) {
      return FACTORY.createPropertyIsLessThanOrEqualTo(binaryComparisonType(children));
    }

    private static JAXBElement<PropertyIsLikeType> like(List<JAXBElement<?>> children) {
      return FACTORY.createPropertyIsLike(likeType(children, false));
    }

    private static JAXBElement<PropertyIsLikeType> likeMatchCase(List<JAXBElement<?>> children) {
      return FACTORY.createPropertyIsLike(likeType(children, true));
    }

    private static JAXBElement<BinaryTemporalOpType> before(List<JAXBElement<?>> children) {
      return FACTORY.createBefore(binaryTemporalType(children));
    }

    private static JAXBElement<BinaryTemporalOpType> after(List<JAXBElement<?>> children) {
      return FACTORY.createAfter(binaryTemporalType(children));
    }

    private static JAXBElement<BinarySpatialOpType> intersects(List<JAXBElement<?>> children) {
      return FACTORY.createIntersects(binarySpatialType(children));
    }

    private static BinaryLogicOpType binaryLogicType(List<JAXBElement<?>> ops) {
      return new BinaryLogicOpType().withOps(ops);
    }

    private static BinaryComparisonOpType binaryComparisonType(List<JAXBElement<?>> children) {
      return new BinaryComparisonOpType().withExpression(children);
    }

    private static PropertyIsLikeType likeType(List<JAXBElement<?>> children, boolean matchCase) {
      return new PropertyIsLikeTypeWithMatchCase()
          .withMatchCase(matchCase)
          .withEscapeChar("\\")
          .withWildCard("%")
          .withSingleChar("_")
          .withExpression(children);
    }

    private static BinaryTemporalOpType binaryTemporalType(List<JAXBElement<?>> children) {
      return new BinaryTemporalOpType().withExpressionOrAny(new ArrayList<>(children));
    }

    private static BinarySpatialOpType binarySpatialType(List<JAXBElement<?>> children) {
      return new BinarySpatialOpType().withExpressionOrAny(new ArrayList<>(children));
    }
  }

  @SuppressWarnings("squid:S2160" /* Not being used in comparisons */)
  private static class PropertyIsLikeTypeWithMatchCase extends PropertyIsLikeType {
    @XmlAttribute(name = "matchCase")
    protected Boolean matchCase;

    public boolean isMatchCase() {
      if (matchCase == null) {
        return true;
      } else {
        return matchCase;
      }
    }

    public void setMatchCase(boolean value) {
      this.matchCase = value;
    }

    PropertyIsLikeTypeWithMatchCase withMatchCase(boolean value) {
      setMatchCase(value);
      return this;
    }
  }
}
