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
package org.codice.ddf.catalog.ui.filter.impl;

import static org.apache.commons.lang3.Validate.notNull;
import static org.codice.ddf.catalog.ui.filter.impl.json.FilterJsonUtils.isBinaryLogic;
import static org.codice.ddf.catalog.ui.filter.impl.json.FilterJsonUtils.isTerminal;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.codice.ddf.catalog.ui.filter.FilterNode;
import org.codice.ddf.catalog.ui.filter.json.FilterJson;

public class FilterNodeMapImpl implements FilterNode {
  private final String type;

  private final Map<String, Object> json;

  public FilterNodeMapImpl(final Map<String, Object> json) {
    notNull(json);
    this.type = (String) json.get(FilterJson.Keys.TYPE);

    notNull(type);
    this.json = json;

    if (!(isBinaryLogic(json) || isTerminal(json))) {
      throw new IllegalArgumentException("Filter node properties are invalid: " + json.toString());
    }
  }

  @Override
  public boolean isLeaf() {
    return json.get(FilterJson.Keys.FILTERS) == null;
  }

  @Override
  public boolean isTemplated() {
    return json.get(FilterJson.Keys.TEMPLATE_PROPS) != null;
  }

  @Override
  public String getOperator() {
    return type;
  }

  @Override
  public List<FilterNode> getChildren() {
    return Stream.of(json.get(FilterJson.Keys.FILTERS))
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
    return (Map<String, Object>) json.get(FilterJson.Keys.TEMPLATE_PROPS);
  }

  @Override
  @Nullable
  public String getProperty() {
    if (!isLeaf()) {
      throw new IllegalStateException("No property value exists for a logical operator");
    }
    return (String) json.get(FilterJson.Keys.PROPERTY);
  }

  @Override
  @Nullable
  public String getValue() {
    if (!isLeaf()) {
      throw new IllegalStateException("No target value exists for a logical operator");
    }
    Object obj = json.get(FilterJson.Keys.VALUE);
    if (obj == null) {
      return null;
    }
    return Objects.toString(obj);
  }

  @Override
  public void setProperty(String property) {
    notNull(property);
    json.put(FilterJson.Keys.PROPERTY, property);
  }

  @Override
  public void setValue(String value) {
    notNull(value);
    json.put(FilterJson.Keys.VALUE, value);
  }
}
