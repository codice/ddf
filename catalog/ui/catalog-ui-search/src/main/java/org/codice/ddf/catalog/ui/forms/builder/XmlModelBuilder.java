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

import static org.apache.commons.lang3.Validate.notNull;

import com.google.common.collect.ImmutableMap;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBElement;
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
import org.apache.commons.lang.Validate;
import org.codice.ddf.catalog.ui.forms.api.FlatFilterBuilder;
import org.codice.ddf.catalog.ui.forms.util.QNameMapper;

public class XmlModelBuilder implements FlatFilterBuilder<JAXBElement> {
  private static final ObjectFactory FACTORY = new ObjectFactory();

  private static final String FUNCTION_PROPERTY_NAME = "filterFunctionName";

  private static final String FUNCTION_PROPERTY_PARAMS = "params";

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
          .put("ILIKE", Mapper::like)
          .put("LIKE", Mapper::likeMatchCase)
          .put("INTERSECTS", Mapper::intersects)
          .put("BEFORE", Mapper::before)
          .put("AFTER", Mapper::after)
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
  public FlatFilterBuilder beginPropertyIsLikeType(String operator) {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeNotInProgress();
    MultiNodeReducer comparisonMapping = TERMINAL_OPS.get(operator);
    if (comparisonMapping == null) {
      throw new IllegalArgumentException("Cannot find mapping for like operator: " + operator);
    }
    supplierInProgress = new TerminalNodeSupplier(comparisonMapping);
    return this;
  }

  @Override
  public FlatFilterBuilder beginPropertyIsILikeType(String operator) {
    return beginPropertyIsLikeType(operator);
  }

  @Override
  public FlatFilterBuilder beginBinaryTemporalType(String operator) {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeNotInProgress();
    MultiNodeReducer temporalMapping = TERMINAL_OPS.get(operator);
    if (temporalMapping == null) {
      throw new IllegalArgumentException(
          "Cannot find mapping for binary temporal operator: " + operator);
    }
    supplierInProgress = new TerminalNodeSupplier(temporalMapping);
    return null;
  }

  @Override
  public XmlModelBuilder beginBinarySpatialType(String operator) {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeNotInProgress();
    MultiNodeReducer spatialMapping = TERMINAL_OPS.get(operator);
    validateOperatorMapping(
        spatialMapping, "Cannot find mapping for binary spatial operator: " + operator);
    supplierInProgress = new TerminalNodeSupplier(spatialMapping);
    return this;
  }

  private void validateOperatorMapping(MultiNodeReducer multiNodeReducer, String message) {
    if (multiNodeReducer == null) {
      throw new IllegalArgumentException(message);
    }
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
  public XmlModelBuilder setLiteralProperty(Object literalProperty) {

    LiteralType literalType = FACTORY.createLiteralType().withContent(literalProperty.toString());

    literalType.setType(QNameMapper.convert(literalProperty));

    supplierInProgress.setLiteralPropertyNode(FACTORY.createLiteral(literalType));

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
  public XmlModelBuilder setFunctionValues(Map<String, Object> functionProperties) {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeInProgress();
    validateFunctionProperties(functionProperties);

    supplierInProgress.setValue(
        FACTORY.createFunction(
            new FunctionType()
                .withName(extractFunctionName(functionProperties))
                .withExpression(extractFunctionParameters(functionProperties))));
    return this;
  }

  /** Call {@link #validateFunctionProperties(Map)} before calling this method. */
  private String extractFunctionName(Map<String, Object> functionProperties) {
    return (String) functionProperties.get(FUNCTION_PROPERTY_NAME);
  }

  /** Call {@link #validateFunctionProperties(Map)} before calling this method. */
  @SuppressWarnings("unchecked")
  private List<JAXBElement<?>> extractFunctionParameters(Map<String, Object> functionProperties) {
    return ((List<Object>) functionProperties.get(FUNCTION_PROPERTY_PARAMS))
        .stream()
        .filter(Serializable.class::isInstance)
        .map(Serializable.class::cast)
        .map(this::createLiteralType)
        .map(FACTORY::createLiteral)
        .collect(Collectors.toList());
  }

  private void validateFunctionProperties(Map<String, Object> functionProperties) {

    if (!(functionProperties.get(FUNCTION_PROPERTY_NAME) instanceof String)) {
      throw new IllegalArgumentException(
          String.format(
              "Function properties must include the key \"%s\" with a String value",
              FUNCTION_PROPERTY_NAME));
    }

    if (!(functionProperties.get(FUNCTION_PROPERTY_PARAMS) instanceof List)) {
      throw new IllegalArgumentException(
          String.format(
              "Function properties must include the key \"%s\" with a List value",
              FUNCTION_PROPERTY_PARAMS));
    }
  }

  private LiteralType createLiteralType(Serializable serializable) {
    return new LiteralType()
        .withContent(serializable.toString())
        .withType(QNameMapper.convert(serializable));
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

  private static class TerminalNodeSupplier implements Supplier<JAXBElement<?>> {
    private final MultiNodeReducer reducer;
    private JAXBElement<String> propertyNode = null;
    private JAXBElement<LiteralType> literalPropertyNode = null;
    private JAXBElement<?> valueNode;

    TerminalNodeSupplier(final MultiNodeReducer reducer) {
      notNull(reducer);
      this.reducer = reducer;
    }

    public void setProperty(JAXBElement<String> propertyNode) {
      notNull(propertyNode);
      this.propertyNode = propertyNode;
    }

    private void setLiteralPropertyNode(JAXBElement<LiteralType> literalPropertyNode) {
      this.literalPropertyNode = literalPropertyNode;
    }

    public void setValue(JAXBElement<?> valueNode) {
      notNull(valueNode);
      this.valueNode = valueNode;
    }

    @Override
    public JAXBElement<?> get() {
      validatePropertyNodeXorLiteralPropertyNode();
      notNull(valueNode);
      List<JAXBElement<?>> terminals = new ArrayList<>();

      if (propertyNode != null) {
        terminals.add(propertyNode);
      } else {
        terminals.add(literalPropertyNode);
      }

      terminals.add(valueNode);
      return reducer.apply(terminals);
    }

    /** There must be a property node or a literal node, but not both. */
    private void validatePropertyNodeXorLiteralPropertyNode() {
      Validate.isTrue(propertyNode != null ^ literalPropertyNode != null);
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
      return new PropertyIsLikeType()
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
}
