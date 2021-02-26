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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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

  private static final String LIKE = "LIKE";

  private static final String FAKE_PROPERTY_OPERATOR = "PROPERTY";

  private static final String FAKE_VALUE_OPERATOR = "VALUE";

  private static final String FAKE_DISTANCE_OPERATOR = "DISTANCE";

  private static final Map<String, BiConsumer<FilterVisitor2, VisitableElement>> VISIT_METHODS =
      ImmutableMap.<String, BiConsumer<FilterVisitor2, VisitableElement>>builder()
          // Fake operators to give a flat structure an XML-like "embedded" structure
          .put(FAKE_PROPERTY_OPERATOR, FilterVisitor2::visitString)
          .put(FAKE_VALUE_OPERATOR, FilterVisitor2::visitLiteralType)
          .put(FAKE_DISTANCE_OPERATOR, FilterVisitor2::visitDistanceType)
          // Misc
          .put("IS NULL", FilterVisitor2::visitPropertyIsNilType) // Date 'IS EMPTY' operator
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
          .put("LIKE", FilterVisitor2::visitPropertyIsLikeType)
          .put("ILIKE", FilterVisitor2::visitPropertyIsLikeType)
          .build();

  private final BiConsumer<FilterVisitor2, VisitableElement> visitMethod;
  private final String name;
  private final Object value;

  public static VisitableJsonElementImpl create(final FilterNode node) {
    return new VisitableJsonElementImpl(node);
  }

  private VisitableJsonElementImpl(String prop) {
    this.visitMethod = FilterVisitor2::visitString;
    this.name = null;
    this.value = prop;
  }

  private VisitableJsonElementImpl(final FilterNode node) {
    if (LIKE.equals(node.getOperator())) {
      throw new UnsupportedOperationException("LIKE (case sensitive) currently is not supported");
      // Ticket for adding support - https://codice.atlassian.net/browse/DDF-3829
    }

    this.visitMethod = VISIT_METHODS.get(node.getOperator());
    if (this.visitMethod == null) {
      throw new FilterProcessingException(
          "Encountered an unexpected or unsupported type: " + node.getOperator());
    }

    this.name = node.getOperator();

    if (!node.isLeaf()) {
      LOGGER.trace("Found JSON logical node [{}] with children", name);
      this.value = wrap(node.getChildren());
      return;
    }

    String prop = node.getProperty();
    String val = node.getValue();
    Double distance = node.getDistance();

    if (node.isTemplated()) {
      Map<String, Object> template = node.getTemplateProperties();
      LOGGER.trace(
          "Found JSON templated node [{}] with property [{}] and template [{}]",
          name,
          prop,
          template);
      this.value = wrap(prop, template);
      return;
    }

    if (distance != null) {
      LOGGER.trace(
          "Found JSON distance buffer node [{}] with property [{}], value [{}], and distance [{}]",
          name,
          prop,
          val,
          distance);
      this.value = wrap(prop, val, distance);
      return;
    }

    if (val == null) {
      LOGGER.trace("Found JSON nil node [{}] for property [{}]", name, prop);
      this.value = new VisitableJsonElementImpl(prop);
      return;
    }

    this.value = wrap(prop, val);
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
    return ImmutableList.of(
        new VisitableJsonElementImpl(FAKE_PROPERTY_OPERATOR, property),
        new VisitableJsonElementImpl(FAKE_VALUE_OPERATOR, Collections.singletonList(value)));
  }

  private static List<VisitableElement<?>> wrap(String property, String value, Double distance) {
    return ImmutableList.of(
        new VisitableJsonElementImpl(FAKE_PROPERTY_OPERATOR, property),
        new VisitableJsonElementImpl(FAKE_DISTANCE_OPERATOR, distance),
        new VisitableJsonElementImpl(FAKE_VALUE_OPERATOR, Collections.singletonList(value)));
  }

  private static List<VisitableElement<?>> wrap(
      String property, Map<String, Object> templateProperties) {
    return ImmutableList.of(
        new VisitableJsonElementImpl(FAKE_PROPERTY_OPERATOR, property),
        new VisitableJsonElementImpl(
            FAKE_VALUE_OPERATOR, templateProperties, FilterVisitor2::visitFunctionType));
  }

  private static List<VisitableElement<?>> wrap(List<FilterNode> children) {
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
