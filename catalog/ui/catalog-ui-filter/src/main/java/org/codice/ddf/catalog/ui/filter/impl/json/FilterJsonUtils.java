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
package org.codice.ddf.catalog.ui.filter.impl.json;

import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.codice.ddf.catalog.ui.filter.json.FilterJson;

public class FilterJsonUtils {
  private static final ImmutableSet<String> TERMINAL_OPS =
      ImmutableSet.<String>builder()
          .add(FilterJson.Ops.EQ)
          .add(FilterJson.Ops.NOT_EQ)
          .add(FilterJson.Ops.GT)
          .add(FilterJson.Ops.GT_OR_ET)
          .add(FilterJson.Ops.LT)
          .add(FilterJson.Ops.LT_OR_ET)
          .add(FilterJson.Ops.ILIKE)
          .add(FilterJson.Ops.LIKE)
          .add(FilterJson.Ops.BEFORE)
          .add(FilterJson.Ops.AFTER)
          .add(FilterJson.Ops.INTERSECTS)
          .add(FilterJson.Ops.DWITHIN)
          .build();

  private FilterJsonUtils() {}

  public static boolean isBinaryLogic(Map<String, Object> predicate) {
    Object type = predicate.get(FilterJson.Keys.TYPE);
    if (!(FilterJson.Ops.AND.equals(type) || FilterJson.Ops.OR.equals(type))) {
      return false;
    }
    Object filters = predicate.get(FilterJson.Keys.FILTERS);
    return filters instanceof List;
  }

  public static boolean isTerminal(Map<String, Object> predicate) {
    Object type = predicate.get(FilterJson.Keys.TYPE);
    if (!(type instanceof String)) {
      return false;
    }
    String typeStr = (String) type;
    if (!TERMINAL_OPS.contains(typeStr)) {
      return false;
    }
    Object property = predicate.get(FilterJson.Keys.PROPERTY);
    if (!(property instanceof String || property instanceof Map)) {
      return false;
    }
    Object value = predicate.get(FilterJson.Keys.VALUE);
    return value == null || value instanceof Serializable || value instanceof Map;
  }

  public static boolean isFunction(Map<String, Object> predicate) {
    Object type = predicate.get(FilterJson.Keys.TYPE);
    if (!FilterJson.Ops.FUNC.equals(type)) {
      return false;
    }
    Object name = predicate.get(FilterJson.Keys.NAME);
    if (!(name instanceof String)) {
      return false;
    }
    Object params = predicate.get(FilterJson.Keys.PARAMS);
    return params instanceof List;
  }

  public static String getFuncName(Map<String, Object> func) {
    return (String) func.get(FilterJson.Keys.NAME);
  }

  public static List<Object> getParams(Map<String, Object> func) {
    return (List<Object>) func.get(FilterJson.Keys.PARAMS);
  }

  @Deprecated
  public static Map<String, Object> getTemplateProperties(Map<String, Object> pred) {
    return (Map<String, Object>) pred.get(FilterJson.Keys.TEMPLATE_PROPS);
  }
}
