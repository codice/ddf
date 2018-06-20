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
package org.codice.ddf.catalog.ui.forms.filter;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import net.opengis.filter.v_2_0.BBOXType;
import net.opengis.filter.v_2_0.FilterType;
import net.opengis.filter.v_2_0.PropertyIsBetweenType;
import net.opengis.filter.v_2_0.PropertyIsNilType;
import net.opengis.filter.v_2_0.PropertyIsNullType;
import net.opengis.filter.v_2_0.UnaryLogicOpType;
import org.codice.ddf.catalog.ui.forms.api.FilterVisitor2;
import org.codice.ddf.catalog.ui.forms.api.VisitableElement;
import org.codice.ddf.catalog.ui.forms.model.FilterJsonNode;

/**
 * Notes on the JSON to XML mapping representation.
 *
 * <p>Mapping for {@link FilterType} is not necessary, because the JSON representation does not
 * utilize an external envelope for data nodes. The filter simply begins with the first concrete
 * data node.
 *
 * <p>The mapping for {@link net.opengis.filter.v_2_0.FunctionType} is a special case for JSON, and
 * is handled in the constructor separate from the direct mapping in static state.
 *
 * <p>Still need to evaluate support for several types.
 *
 * @see UnaryLogicOpType
 * @see BBOXType
 * @see PropertyIsNullType
 * @see PropertyIsNilType
 * @see PropertyIsBetweenType
 */
public class VisitableJsonElementImpl implements VisitableElement<Object> {

  private static final String FAKE_PROPERTY_OPERATOR = "PROPERTY";

  private static final String FAKE_VALUE_OPERATOR = "VALUE";

  private static final Map<String, BiConsumer<FilterVisitor2, VisitableElement>> VISIT_METHODS =
      ImmutableMap.<String, BiConsumer<FilterVisitor2, VisitableElement>>builder()
          // Fake operators to give a flat structure an XML-like "embedded" structure
          .put(FAKE_PROPERTY_OPERATOR, FilterVisitor2::visitString)
          .put(FAKE_VALUE_OPERATOR, FilterVisitor2::visitLiteralType)
          .put("LITERAL_PROPERTY", FilterVisitor2::visitLiteralProperty)
          // Logical operator mapping
          .put("AND", FilterVisitor2::visitBinaryLogicType)
          .put("OR", FilterVisitor2::visitBinaryLogicType)
          .put("NOT", FilterVisitor2::visitUnaryLogicType)
          // Temporal operator mapping
          .put("BEFORE", FilterVisitor2::visitBinaryTemporalType)
          .put("AFTER", FilterVisitor2::visitBinaryTemporalType)
          // Spatial operator mapping
          .put("INTERSECTS", FilterVisitor2::visitBinarySpatialType)
          .put("DWITHIN", FilterVisitor2::visitDistanceBufferType)
          // Comparison operator mapping
          .put("=", FilterVisitor2::visitBinaryComparisonType)
          .put("!=", FilterVisitor2::visitBinaryComparisonType)
          .put(">", FilterVisitor2::visitBinaryComparisonType)
          .put(">=", FilterVisitor2::visitBinaryComparisonType)
          .put("<", FilterVisitor2::visitBinaryComparisonType)
          .put("<=", FilterVisitor2::visitBinaryComparisonType)
          .put("LIKE", FilterVisitor2::visitPropertyIsLikeType)
          .put("ILIKE", FilterVisitor2::visitPropertyIsILikeType)
          .build();

  private final BiConsumer<FilterVisitor2, VisitableElement> visitMethod;
  private final String name;
  private final Object value;

  public static VisitableJsonElementImpl create(final FilterJsonNode node) {
    return new VisitableJsonElementImpl(node);
  }

  private VisitableJsonElementImpl(final FilterJsonNode node) {

    this.visitMethod = VISIT_METHODS.get(node.getOperator());
    if (this.visitMethod == null) {
      throw new FilterProcessingException(
          "Encountered an unexpected or unsupported type: " + node.getOperator());
    }

    this.name = node.getOperator();

    if (node.isFunction()) {
      this.value = handleFunctionNode(node);
    } else if (node.isLeaf()) {
      this.value = handleLeafNode(node);
    } else if (node.hasChildren()) {
      this.value = handleIntermediateNode(node);
    } else {
      throw new FilterProcessingException(
          "Encountered an unhandled node (not function, leaf or intermediate)");
    }
  }

  private Object handleIntermediateNode(FilterJsonNode node) {
    return wrap(node.getChildrenNew());
  }

  private Object handleLeafNode(FilterJsonNode node) {
    return wrap(node.getProperty(), node.getValue());
  }

  private Object handleFunctionNode(FilterJsonNode node) {
    return wrap(node.getFunctionArguments(), Boolean.valueOf(node.getValue()));
  }

  private VisitableJsonElementImpl(final String operator, final Object value) {
    this.name = operator;
    this.value = value;
    this.visitMethod = VISIT_METHODS.get(operator);
    if (this.visitMethod == null) {
      throw new FilterProcessingException(
          "Encountered an unexpected or unsupported type: " + operator);
    }
  }

  private VisitableJsonElementImpl(
      final String operator,
      final Object value,
      final BiConsumer<FilterVisitor2, VisitableElement> visitMethod) {
    this.name = operator;
    this.value = value;
    this.visitMethod = visitMethod;
  }

  private static List<VisitableElement<?>> wrap(String property, String value) {
    return Arrays.asList(
        new VisitableJsonElementImpl(FAKE_PROPERTY_OPERATOR, property),
        new VisitableJsonElementImpl(FAKE_VALUE_OPERATOR, Collections.singletonList(value)));
  }

  private static List<VisitableElement<?>> wrap(
      Map<String, Object> functionArguments, Boolean functionTarget) {
    return Arrays.asList(
        new VisitableJsonElementImpl("LITERAL_PROPERTY", functionTarget),
        new VisitableJsonElementImpl(
            FAKE_VALUE_OPERATOR, functionArguments, FilterVisitor2::visitFunctionType));
  }

  private static List<VisitableElement<?>> wrap(List<FilterJsonNode> children) {
    return children.stream().map(VisitableJsonElementImpl::new).collect(Collectors.toList());
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Object getValue() {
    return value;
  }

  @Override
  public void accept(FilterVisitor2 visitor) {
    visitMethod.accept(visitor, this);
  }
}
