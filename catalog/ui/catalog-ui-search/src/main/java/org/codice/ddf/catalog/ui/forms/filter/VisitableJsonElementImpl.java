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
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.opengis.filter.v_2_0.BBOXType;
import net.opengis.filter.v_2_0.FilterType;
import net.opengis.filter.v_2_0.PropertyIsBetweenType;
import net.opengis.filter.v_2_0.PropertyIsNilType;
import net.opengis.filter.v_2_0.PropertyIsNullType;
import net.opengis.filter.v_2_0.UnaryLogicOpType;
import org.codice.ddf.catalog.ui.forms.api.FilterNode;
import org.codice.ddf.catalog.ui.forms.api.FilterVisitor2;
import org.codice.ddf.catalog.ui.forms.api.VisitableElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private static final Logger LOGGER = LoggerFactory.getLogger(VisitableJsonElementImpl.class);

  private static final Map<String, BiConsumer<FilterVisitor2, VisitableElement>> VISIT_METHODS =
      ImmutableMap.<String, BiConsumer<FilterVisitor2, VisitableElement>>builder()
          // Misc
          .put("IS NULL", FilterVisitor2::visitPropertyIsNilType) // Date 'IS EMPTY' operator
          .put("FILTER_FUNCTION", FilterVisitor2::visitFunctionType) // Text 'NEAR' operator
          .put("BETWEEN", FilterVisitor2::visitMapType) // Numeric 'RANGE' operator
          // --
          // Issue with XML binding library so it's mapped to a custom function
          .put("LIKE", FilterVisitor2::visitMapType)
          // Logical operator mapping
          .put("AND", FilterVisitor2::visitBinaryLogicType)
          .put("OR", FilterVisitor2::visitBinaryLogicType)
          // Temporal operator mapping
          .put("BEFORE", FilterVisitor2::visitBinaryTemporalType)
          .put("AFTER", FilterVisitor2::visitBinaryTemporalType)
          .put("DURING", FilterVisitor2::visitBinaryTemporalType) // Date 'BETWEEN' operator
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
          .put("ILIKE", FilterVisitor2::visitPropertyIsLikeType)
          .build();

  private final BiConsumer<FilterVisitor2, VisitableElement> visitMethod;
  private final String name;
  private final Object value;

  private String functionName;

  public static VisitableJsonElementImpl create(final FilterNode node) {
    return new VisitableJsonElementImpl(node);
  }

  /**
   * Base constructor.
   *
   * @param visitMethod dispatch method for visiting.
   * @param value value of this node.
   */
  private VisitableJsonElementImpl(
      final BiConsumer<FilterVisitor2, VisitableElement> visitMethod, final Object value) {
    this.name = null;
    this.functionName = null;

    this.visitMethod = visitMethod;
    this.value = value;
  }

  /**
   * Primary constructor for evaluating a {@link FilterNode} and constructing the visitable tree
   * accordingly.
   *
   * @param node the node to construct the tree for.
   */
  private VisitableJsonElementImpl(final FilterNode node) {
    this.visitMethod = VISIT_METHODS.get(node.getOperator());
    if (this.visitMethod == null) {
      throw new FilterProcessingException(
          "Encountered an unexpected or unsupported type: " + node.getOperator());
    }

    this.name = node.getOperator();

    if (!node.isLeaf()) {
      LOGGER.trace("Found JSON logical node [{}] with children", name);
      this.value =
          node.getChildren()
              .stream()
              .map(VisitableJsonElementImpl::new)
              .collect(Collectors.toList());
      return;
    }

    if (node.isFunction()) {
      LOGGER.trace("Found JSON function node");
      this.functionName = node.getFunctionName();
      this.value = node.getParams();
      return;
    }

    FilterNode propFunc = node.getPropertyFunction();
    Object val = node.getValue();

    if (propFunc != null) {
      LOGGER.trace("Found JSON node with function on property: {}", node.getOperator());
      this.value =
          Arrays.asList(
              new VisitableJsonElementImpl(propFunc),
              new VisitableJsonElementImpl(
                  FilterVisitor2::visitLiteralType, Collections.singletonList(val)));
      return;
    }

    String prop = node.getProperty();
    Double distance = node.getDistance();

    if (node.isTemplated()) {
      Map<String, Object> template = node.getTemplateProperties();
      LOGGER.trace(
          "Found JSON templated node [{}] with property [{}] and template [{}]",
          name,
          prop,
          template);
      this.value =
          Arrays.asList(
              new VisitableJsonElementImpl(FilterVisitor2::visitString, prop),
              new VisitableJsonElementImpl(FilterVisitor2::visitTemplateType, template));
      return;
    }

    if (distance != null) {
      LOGGER.trace(
          "Found JSON distance buffer node [{}] with property [{}], value [{}], and distance [{}]",
          name,
          prop,
          val,
          distance);
      this.value =
          Arrays.asList(
              new VisitableJsonElementImpl(FilterVisitor2::visitString, prop),
              new VisitableJsonElementImpl(FilterVisitor2::visitDoubleType, distance),
              new VisitableJsonElementImpl(
                  FilterVisitor2::visitLiteralType, Collections.singletonList(val)));
      return;
    }

    if (val == null) {
      LOGGER.trace("Found JSON nil node [{}] for property [{}]", name, prop);
      this.value = new VisitableJsonElementImpl(FilterVisitor2::visitString, prop);
      return;
    }

    if ("BETWEEN".equals(node.getOperator()) || "LIKE".equals(node.getOperator())) {
      LOGGER.trace(
          "Found JSON map-visitable node [{}] for property [{}] and value [{}]", name, prop, val);
      Map<String, Object> remapped = new HashMap<>();
      remapped.put("type", node.getOperator());
      remapped.put("property", prop);
      remapped.put("value", val);
      this.value = remapped;
      return;
    }

    LOGGER.trace(
        "Found JSON comparison node [{}] for property [{}] and value [{}]", name, prop, val);
    this.value =
        Arrays.asList(
            new VisitableJsonElementImpl(FilterVisitor2::visitString, prop),
            new VisitableJsonElementImpl(
                FilterVisitor2::visitLiteralType, Collections.singletonList(val)));
  }

  @Override
  public String getName() {
    return name;
  }

  @Nullable
  @Override
  public String getFunctionName() {
    return functionName;
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
