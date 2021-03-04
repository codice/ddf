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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.codice.ddf.catalog.ui.forms.api.FilterNode;

public class FilterNodeMapImpl implements FilterNode {
  private static final String CHILDREN = "filters";

  private static final String PROPERTY = "property";

  private static final String VALUE = "value";

  private static final String DISTANCE = "distance";

  private static final String TEMPLATE_PROPERTIES = "templateProperties";

  private final String type;

  private final Map<String, Object> json;

  public FilterNodeMapImpl(final Map<String, Object> json) {
    notNull(json);
    this.type = (String) json.get("type");

    notNull(type);
    this.json = json;

    if (!(isValidLogical(json) || isValidTerminal(json) || isValidFunctional(json))) {
      throw new IllegalArgumentException("Filter node properties are invalid: " + json.toString());
    }
  }

  @Override
  public boolean isLeaf() {
    return json.get(CHILDREN) == null;
  }

  @Override
  public boolean isTemplated() {
    return json.get(TEMPLATE_PROPERTIES) != null;
  }

  @Override
  public boolean isFunction() {
    return "FILTER_FUNCTION".equals(type);
  }

  @Override
  public String getOperator() {
    return type;
  }

  @Override
  public List<FilterNode> getChildren() {
    return Stream.of(json.get(CHILDREN))
        .map(List.class::cast)
        .flatMap(List::stream)
        .map(Map.class::cast)
        .map(FilterNodeMapImpl::new)
        .collect(Collectors.toList());
  }

  @Override
  @SuppressWarnings("unchecked")
  public Map<String, Object> getTemplateProperties() {
    if (!isTemplated()) {
      throw new IllegalStateException("Non-templated nodes do not have template properties");
    }
    return (Map<String, Object>) json.get(TEMPLATE_PROPERTIES);
  }

  @Override
  @Nullable
  public String getProperty() {
    if (!isLeaf()) {
      throw new IllegalStateException("No property value exists for a logical operator");
    }
    Object property = json.get(PROPERTY);
    if (!(property instanceof String)) {
      throw new IllegalStateException("Property value is a function, not a string");
    }
    return (String) property;
  }

  @Nullable
  @Override
  public FilterNode getPropertyFunction() {
    Object property = json.get(PROPERTY);
    if (!(property instanceof Map)) {
      return null;
    }
    return new FilterNodeMapImpl((Map<String, Object>) property);
  }

  @Override
  @Nullable
  public Object getValue() {
    if (!isLeaf()) {
      throw new IllegalStateException("No target value exists for a logical operator");
    }
    return json.get(VALUE);
  }

  @Override
  public Double getDistance() {
    if (!isLeaf()) {
      throw new IllegalStateException("No distance value exists for a logical operator");
    }

    Object distance = json.get(DISTANCE);
    if (distance == null) {
      return null;
    }

    if (distance instanceof Number) {
      return ((Number) distance).doubleValue();
    }

    throw new IllegalStateException("The distance value could not be converted into a double");
  }

  @Nullable
  @Override
  public String getFunctionName() {
    if (!isFunction()) {
      throw new IllegalStateException("Function name only exists for function type");
    }
    Object filterFunctionName = json.get("filterFunctionName");
    if (!(filterFunctionName instanceof String)) {
      throw new IllegalStateException("Malformed data provided on function type");
    }
    return (String) filterFunctionName;
  }

  @Nullable
  @Override
  public List<Object> getParams() {
    if (!isFunction()) {
      throw new IllegalStateException("Function name only exists for function type");
    }
    Object params = json.get("params");
    if (!(params instanceof List)) {
      throw new IllegalStateException("Malformed data provided on function type");
    }
    return (List<Object>) params;
  }

  @Override
  public void setProperty(Object property) {
    notNull(property);
    if (!(property instanceof String) && !(property instanceof FilterNode)) {
      throw new IllegalStateException("Property can either be a String or FilterNode");
    }
    json.put(PROPERTY, property);
  }

  @Override
  public void setValue(Object value) {
    notNull(value);
    json.put(VALUE, value);
  }

  @Override
  public void setDistance(Double distance) {
    json.put(DISTANCE, distance);
  }

  @Override
  public void addArg(Object arg) {
    throw new UnsupportedOperationException("No need to add args to the map impl");
  }

  private static boolean isValidFunctional(Map<String, Object> json) {
    return !json.containsKey(CHILDREN)
        && !json.containsKey(PROPERTY)
        && !json.containsKey(VALUE)
        && json.containsKey("filterFunctionName")
        && json.containsKey("params");
  }

  private static boolean isValidLogical(Map<String, Object> json) {
    return json.containsKey(CHILDREN) && !json.containsKey(PROPERTY) && !json.containsKey(VALUE);
  }

  private static boolean isValidTerminal(Map<String, Object> json) {
    return !json.containsKey(CHILDREN) && json.containsKey(PROPERTY) && json.containsKey(VALUE);
  }
}
