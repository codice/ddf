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

public class FilterNodeMapImpl implements FilterNode {
  private static final String CHILDREN = "filters";

  private static final String PROPERTY = "property";

  private static final String VALUE = "value";

  private static final String TEMPLATE_PROPERTIES = "templateProperties";

  private final String type;

  private final Map<String, Object> json;

  public FilterNodeMapImpl(final Map<String, Object> json) {
    notNull(json);
    this.type = (String) json.get("type");

    notNull(type);
    this.json = json;

    if (!(isValidLogical(json) || isValidTerminal(json))) {
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
    return (Map<String, Object>) json.get(TEMPLATE_PROPERTIES);
  }

  @Override
  public String getProperty() {
    return (String) json.get(PROPERTY);
  }

  @Override
  public String getValue() {
    return Objects.toString(json.get(VALUE));
  }

  @Override
  public void setProperty(String property) {
    notNull(property);
    json.put(PROPERTY, property);
  }

  @Override
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
