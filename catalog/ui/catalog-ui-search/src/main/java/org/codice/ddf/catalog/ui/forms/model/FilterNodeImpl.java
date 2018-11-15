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
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.codice.ddf.catalog.ui.forms.api.FilterNode;

public class FilterNodeImpl implements FilterNode {
  @SerializedName("type")
  private final String operator;

  @SerializedName("filters")
  private final List<FilterNode> children;

  private String property;

  private String value;

  private Double distance;

  private Map<String, Object> templateProperties;

  public FilterNodeImpl(final String operator, final List<FilterNode> children) {
    notNull(operator);
    notNull(children);
    this.operator = operator;
    this.children = children;

    this.property = null;
    this.value = null;
    this.distance = null;
    this.templateProperties = null;
  }

  public FilterNodeImpl(String operator) {
    notNull(operator);
    this.operator = operator;
    this.children = null;

    this.property = null;
    this.value = null;
    this.distance = null;
    this.templateProperties = null;
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
    return property;
  }

  @Override
  @Nullable
  public String getValue() {
    if (!isLeaf()) {
      throw new IllegalStateException("No target value exists for a logical operator");
    }
    return value;
  }

  @Override
  @Nullable
  public Double getDistance() {
    return distance;
  }

  @Override
  public void setProperty(String property) {
    notNull(property);
    this.property = property;
  }

  @Override
  public void setValue(String value) {
    notNull(value);
    this.value = value;
  }

  @Override
  public void setDistance(Double distance) {
    this.distance = distance;
  }

  @Override
  public Map<String, Object> getTemplateProperties() {
    if (!isTemplated()) {
      throw new IllegalStateException("Non-templated nodes do not have template properties");
    }
    return templateProperties;
  }
}
