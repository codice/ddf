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

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.codice.ddf.catalog.ui.forms.api.FilterNode;

public class FilterNodeImpl implements FilterNode {
  @SerializedName("type")
  private final String operator;

  @SerializedName("filters")
  private final List<FilterNode> children;

  private Object property;

  private Object value;

  private Double distance;

  private String from;

  private String to;

  private Map<String, Object> templateProperties;

  private String filterFunctionName;

  private List<Object> params;

  private Number lowerBoundary;

  private Number upperBoundary;

  public FilterNodeImpl(final String operator, final List<FilterNode> children) {
    notNull(operator);
    notNull(children);
    this.operator = operator;
    this.children = children;

    this.property = null;
    this.value = null;
    this.distance = null;
    this.templateProperties = null;

    this.from = null;
    this.to = null;

    this.lowerBoundary = null;
    this.upperBoundary = null;

    this.filterFunctionName = null;
    this.params = null;
  }

  public FilterNodeImpl(String operator) {
    notNull(operator);
    this.operator = operator;
    this.children = null;

    this.property = null;
    this.value = null;
    this.distance = null;
    this.templateProperties = null;

    this.from = null;
    this.to = null;

    this.lowerBoundary = null;
    this.upperBoundary = null;

    this.filterFunctionName = null;
    this.params = null;
  }

  public FilterNodeImpl(String operator, String filterFunctionName) {
    notNull(operator);
    this.operator = operator;
    this.children = null;

    this.property = null;
    this.value = null;
    this.distance = null;
    this.templateProperties = null;

    this.from = null;
    this.to = null;

    this.lowerBoundary = null;
    this.upperBoundary = null;

    this.filterFunctionName = filterFunctionName;
    this.params = new ArrayList<>();
  }

  public FilterNodeImpl(FilterNode node, Map<String, Object> templateProperties) {
    notNull(node);
    this.operator = node.getOperator();
    this.children = null;

    if (!node.isLeaf()) {
      throw new IllegalArgumentException("Any node with template properties must be a leaf");
    }

    notNull(templateProperties);
    this.property = node.getProperty();
    this.value = node.getValue();
    this.distance = node.getDistance();
    this.templateProperties = templateProperties;

    // don't need from/to on template property nodes
    // don't need function props on template property nodes
  }

  @Override
  public boolean isLeaf() {
    return children == null;
  }

  @Override
  public boolean isTemplated() {
    return templateProperties != null;
  }

  @Override
  public boolean isFunction() {
    return "FILTER_FUNCTION".equals(operator);
  }

  @Override
  public String getOperator() {
    return operator;
  }

  @Override
  public List<FilterNode> getChildren() {
    if (isLeaf()) {
      throw new IllegalStateException("Leaf nodes do not have children");
    }
    return children;
  }

  @Override
  @Nullable
  public String getProperty() {
    if (!isLeaf()) {
      throw new IllegalStateException("No property value exists for a logical operator");
    }
    if (!(property instanceof String)) {
      throw new IllegalStateException("Property value is a function, not a string");
    }
    return (String) property;
  }

  @Nullable
  @Override
  public FilterNode getPropertyFunction() {
    if (!(property instanceof FilterNode)) {
      return null;
    }
    return (FilterNode) property;
  }

  @Override
  @Nullable
  public Object getValue() {
    if (!isLeaf()) {
      throw new IllegalStateException("No target value exists for a logical operator");
    }
    return value;
  }

  @Override
  @Nullable
  public Double getDistance() {
    if (!isLeaf()) {
      throw new IllegalStateException("No distance value exists for a logical operator");
    }
    return distance;
  }

  @Nullable
  @Override
  public String getFunctionName() {
    if (!isFunction()) {
      throw new IllegalStateException("Function name only exists for function type");
    }
    if (filterFunctionName == null) {
      throw new IllegalStateException("Malformed data provided on function type");
    }
    return filterFunctionName;
  }

  @Nullable
  @Override
  public List<Object> getParams() {
    if (!isFunction()) {
      throw new IllegalStateException("Function name only exists for function type");
    }
    if (params == null) {
      throw new IllegalStateException("Malformed data provided on function type");
    }
    return params;
  }

  @Override
  public void setProperty(Object property) {
    notNull(property);
    if (!(property instanceof String) && !(property instanceof FilterNode)) {
      throw new IllegalStateException("Property can either be a String or FilterNode");
    }
    this.property = property;
  }

  @Override
  public void setValue(Object value) {
    notNull(value);
    this.value = value;

    if (value instanceof Map) {
      Map mapVal = (Map) value;
      if (mapVal.containsKey("lower") && mapVal.containsKey("upper")) {
        this.lowerBoundary = (Number) mapVal.get("lower");
        this.upperBoundary = (Number) mapVal.get("upper");
        return;
      }
    }

    if (!(value instanceof String)) {
      return;
    }

    String str = (String) value;

    if (!str.contains("/")) {
      return;
    }

    String[] range = str.split("/");
    if (range.length != 2) {
      throw new IllegalArgumentException(
          String.format("Filter node range-value '%s' has too many delimiters", value));
    }

    this.from = range[0];
    this.to = range[1];
  }

  @Override
  public void setDistance(Double distance) {
    this.distance = distance;
  }

  @Override
  public void addArg(Object arg) {
    this.params.add(arg);
  }

  @Override
  public Map<String, Object> getTemplateProperties() {
    if (!isTemplated()) {
      throw new IllegalStateException("Non-templated nodes do not have template properties");
    }
    return templateProperties;
  }
}
