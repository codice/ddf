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
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.boon.core.value.LazyValueMap;

public class FilterJsonNode {
  private static final String CHILDREN = "filters";

  private static final String PROPERTY = "property";

  private static final String VALUE = "value";

  private final String type;

  private final Map<String, Object> json;

  public FilterJsonNode(final Map<String, Object> json) {
    notNull(json);
    this.type = (String) json.get("type");

    notNull(type);
    this.json = json;

    if (!(isValidLogical(json) || isValidTerminal(json))) {
      throw new IllegalArgumentException("Filter node properties are invalid: " + json.toString());
    }
  }

  public boolean isLeaf() {
    return !(hasChildren() || isFunction());
  }

  public String getOperator() {
    return type;
  }

  public boolean hasChildren() {
    return json.get(CHILDREN) != null;
  }

  public boolean isFunction() {
    return json.get(PROPERTY) instanceof LazyValueMap;
  }

  public Map<String, Object> getFunctionArguments() {
    if (!isFunction()) {
      throw new IllegalStateException("Non-function nodes do not have a property map.");
    }
    return (LazyValueMap) json.get(PROPERTY);
  }

  public List<FilterJsonNode> getChildrenNew() {
    return Stream.of(json.get(CHILDREN))
        .map(List.class::cast)
        .flatMap(List::stream)
        .map(Map.class::cast)
        .map(FilterJsonNode::new)
        .collect(Collectors.toList());
  }

  @Nullable
  public String getProperty() {
    if (!isLeaf()) {
      throw new IllegalStateException("No property value exists for a logical operator");
    }
    return (String) json.get(PROPERTY);
  }

  @Nullable
  public String getValue() {
    if (hasChildren()) {
      throw new IllegalStateException("No target value exists for a logical operator");
    }
    return Objects.toString(json.get(VALUE));
  }

  public void setProperty(String property) {
    notNull(property);
    json.put(PROPERTY, property);
  }

  public void setValue(String value) {
    notNull(value);
    json.put(VALUE, value);
  }

  private static boolean isValidLogical(Map<String, Object> json) {
    return json.containsKey(CHILDREN) && !json.containsKey(PROPERTY) && !json.containsKey(VALUE);
  }

  private static boolean isValidTerminal(Map<String, Object> json) {
    return !json.containsKey(CHILDREN) && json.containsKey(PROPERTY) && json.containsKey(VALUE);
  }
}
