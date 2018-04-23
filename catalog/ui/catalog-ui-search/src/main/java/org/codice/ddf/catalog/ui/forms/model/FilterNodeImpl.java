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
import org.boon.json.annotations.JsonProperty;

public class FilterNodeImpl implements FilterNode {
  @JsonProperty("type")
  private final String operator;

  @JsonProperty("filters")
  private final List<FilterNode> children;

  private String property;

  // If changed, update the FilterNodeValueSerializer as well
  private String value;

  private Map<String, Object> templateProperties;

  public FilterNodeImpl(final String operator, final List<FilterNode> children) {
    notNull(operator);
    notNull(children);
    this.operator = operator;
    this.children = children;

    this.property = null;
    this.value = null;
    this.templateProperties = null;
  }

  public FilterNodeImpl(String operator) {
    notNull(operator);
    this.operator = operator;
    this.children = null;

    this.property = null;
    this.value = null;
    this.templateProperties = null;
  }

  public FilterNodeImpl(String operator, String property, String value) {
    notNull(operator);
    this.operator = operator;
    this.children = null;

    notNull(property);
    notNull(value);
    this.property = property;
    this.value = value;
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
  public String getProperty() {
    if (!isLeaf()) {
      throw new IllegalStateException("No property value exists for a logical operator");
    }
    return property;
  }

  @Override
  public String getValue() {
    if (!isLeaf()) {
      throw new IllegalStateException("No target value exists for a logical operator");
    }
    return value;
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

  Map<String, Object> getTemplateProperties() {
    if (!isTemplated()) {
      throw new IllegalStateException("Non-templated nodes do not have template properties");
    }
    return templateProperties;
  }
}
