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
import ddf.catalog.data.AttributeRegistry;
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
import javax.xml.bind.annotation.XmlAttribute;
import net.opengis.filter.v_2_0.BinaryComparisonOpType;
import net.opengis.filter.v_2_0.BinaryLogicOpType;
import net.opengis.filter.v_2_0.BinarySpatialOpType;
import net.opengis.filter.v_2_0.BinaryTemporalOpType;
import net.opengis.filter.v_2_0.ComparisonOpsType;
import net.opengis.filter.v_2_0.DistanceBufferType;
import net.opengis.filter.v_2_0.FilterType;
import net.opengis.filter.v_2_0.FunctionType;
import net.opengis.filter.v_2_0.LiteralType;
import net.opengis.filter.v_2_0.LogicOpsType;
import net.opengis.filter.v_2_0.MeasureType;
import net.opengis.filter.v_2_0.ObjectFactory;
import net.opengis.filter.v_2_0.PropertyIsLikeType;
import net.opengis.filter.v_2_0.PropertyIsNilType;
import net.opengis.filter.v_2_0.SpatialOpsType;
import net.opengis.filter.v_2_0.TemporalOpsType;
import org.codice.ddf.catalog.ui.forms.api.FlatFilterBuilder;
import org.codice.ddf.catalog.ui.forms.filter.FilterProcessingException;

public class XmlModelBuilder implements FlatFilterBuilder<JAXBElement> {
  private static final ObjectFactory FACTORY = new ObjectFactory();

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
          .put("LIKE", Mapper::likeMatchCase) // For now, will never be selected
          .put("INTERSECTS", Mapper::intersects)
          .put("DWITHIN", Mapper::dwithin)
          .put("BEFORE", Mapper::before)
          .put("AFTER", Mapper::after)
          // used for date 'BETWEEN' ops by the UI - contains a range delineated by a slash '/'
          .put("DURING", Mapper::during)
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

  private final AttributeValueNormalizer normalizer;

  private JAXBElement rootNode;

  private TerminalNodeSupplier supplierInProgress;

  private boolean complete = false;

  public XmlModelBuilder(AttributeRegistry registry) {
    this.logicOpCache = new ArrayDeque<>();
    this.depth = new ArrayDeque<>();
    this.normalizer = new AttributeValueNormalizer(registry);
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
  public XmlModelBuilder beginPropertyIsLikeType(String operator, boolean matchCase) {
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
  public XmlModelBuilder beginBinaryTemporalType(String operator) {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeNotInProgress();
    MultiNodeReducer temporalMapping = TERMINAL_OPS.get(operator);
    if (temporalMapping == null) {
      throw new IllegalArgumentException(
          "Cannot find mapping for binary temporal operator: " + operator);
    }
    supplierInProgress = new TerminalNodeSupplier(temporalMapping);
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
  public XmlModelBuilder beginNilType() {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeNotInProgress();
    // used for date 'IS EMPTY' ops by the UI
    supplierInProgress = new TerminalNodeSupplier(Mapper::nil);
    return this;
  }

  @Override
  public XmlModelBuilder addFunctionType(String functionName, List<Object> args) {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeNotInProgress();

    JAXBElement<FunctionType> func =
        FACTORY.createFunction(
            new FunctionType()
                .withName(functionName)
                .withExpression(
                    args.stream()
                        // binding workaround - unable to marshal type "java.lang.Integer" as
                        // an element because it is missing an @XmlRootElement annotation
                        .map(arg -> arg == null ? null : arg.toString())
                        .map(Serializable.class::cast)
                        .map(
                            arg ->
                                FACTORY.createLiteral(
                                    new LiteralType().withContent(Collections.singletonList(arg))))
                        .collect(Collectors.toList())));

    if (depth.isEmpty()) {
      rootNode = func;
    } else {
      depth.peek().add(func);
    }
    return this;
  }

  @Override
  public XmlModelBuilder addBetweenType(String property, Long lower, Long upper) {
    throw new UnsupportedOperationException("No native BETWEEN structure exists for XML");
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
  public XmlModelBuilder setProperty(String functionName, List<Object> args) {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeInProgress();

    JAXBElement<FunctionType> func =
        FACTORY.createFunction(
            new FunctionType()
                .withName(functionName)
                .withExpression(
                    args.stream()
                        // binding workaround - unable to marshal type "java.lang.Integer" as
                        // an element because it is missing an @XmlRootElement annotation
                        .map(arg -> arg == null ? null : arg.toString())
                        .map(Serializable.class::cast)
                        .map(
                            arg ->
                                FACTORY.createLiteral(
                                    new LiteralType().withContent(Collections.singletonList(arg))))
                        .collect(Collectors.toList())));

    supplierInProgress.setProperty(func);
    return this;
  }

  @Override
  public XmlModelBuilder setValue(String value) {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeInProgress();

    String normalizedValue;
    if (supplierInProgress.propertyNode.getValue() instanceof String) {
      String propertyName = (String) supplierInProgress.propertyNode.getValue();
      normalizedValue = normalizer.normalizeForXml(propertyName, value);
    } else {
      normalizedValue = value;
    }

    supplierInProgress.setValue(
        FACTORY.createLiteral(
            new LiteralType().withContent(Collections.singletonList(normalizedValue))));
    return this;
  }

  public XmlModelBuilder setDistance(Double distance) {
    verifyResultNotYetRetrieved();
    verifyTerminalNodeInProgress();
    supplierInProgress = new DistanceBufferSupplier(supplierInProgress, distance);
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

  /**
   * Helper interface for cleanly representing operations that condense a list of XML elements to a
   * single element. Use cases include the creation of logical op types in a filter structure, or
   * the population of values for a terminal type.
   */
  @FunctionalInterface
  private interface MultiNodeReducer extends Function<List<JAXBElement<?>>, JAXBElement> {}

  private static class TerminalNodeSupplier implements Supplier<JAXBElement<?>> {
    private final MultiNodeReducer reducer;
    private JAXBElement<?> propertyNode = null;
    private JAXBElement<?> valueNode;

    private TerminalNodeSupplier(final MultiNodeReducer reducer) {
      notNull(reducer);
      this.reducer = reducer;
    }

    public void setProperty(JAXBElement<?> propertyNode) {
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
      // notNull(valueNode);
      List<JAXBElement<?>> terminals = new ArrayList<>();
      terminals.add(propertyNode);
      terminals.add(valueNode);
      return reducer.apply(terminals);
    }
  }

  private static class DistanceBufferSupplier extends TerminalNodeSupplier {
    private static final String UOM_METERS = "m";
    private final Double distance;

    DistanceBufferSupplier(final TerminalNodeSupplier original, Double distance) {
      super(original.reducer);
      if (original.propertyNode != null) {
        this.setProperty(original.propertyNode);
      }
      if (original.valueNode != null) {
        this.setValue(original.valueNode);
      }
      this.distance = distance;
    }

    @Override
    public JAXBElement<?> get() {
      JAXBElement<?> reduced = super.get();
      notNull(distance, "The Distance element cannot be created without a distance value");
      if (!DistanceBufferType.class.equals(reduced.getDeclaredType())
          || !(reduced.getValue() instanceof DistanceBufferType)) {
        throw new FilterProcessingException(
            "Incorrect use of DistanceBufferSupplier for given type " + reduced.toString());
      }
      DistanceBufferType value = (DistanceBufferType) reduced.getValue();
      value.setDistance(new MeasureType().withUom(UOM_METERS).withValue(distance));
      return reduced;
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
      if (FunctionType.class.isAssignableFrom(root.getDeclaredType())) {
        return FACTORY.createFilter(new FilterType().withFunction((FunctionType) root.getValue()));
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

    private static JAXBElement<BinaryTemporalOpType> during(List<JAXBElement<?>> children) {
      return FACTORY.createDuring(binaryTemporalType(children));
    }

    private static JAXBElement<BinarySpatialOpType> intersects(List<JAXBElement<?>> children) {
      return FACTORY.createIntersects(binarySpatialType(children));
    }

    private static JAXBElement<DistanceBufferType> dwithin(List<JAXBElement<?>> children) {
      return FACTORY.createDWithin(
          new DistanceBufferType().withExpressionOrAny(new ArrayList<>(children)));
    }

    private static JAXBElement<PropertyIsNilType> nil(List<JAXBElement<?>> children) {
      return FACTORY.createPropertyIsNil(new PropertyIsNilType().withExpression(children.get(0)));
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
