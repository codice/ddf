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

import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;
import static org.codice.ddf.catalog.ui.filter.impl.json.FilterJsonUtils.getFuncName;
import static org.codice.ddf.catalog.ui.filter.impl.json.FilterJsonUtils.getParams;
import static org.codice.ddf.catalog.ui.filter.impl.json.FilterJsonUtils.getTemplateProperties;
import static org.codice.ddf.catalog.ui.filter.impl.json.FilterJsonUtils.isFunction;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.codice.ddf.catalog.ui.filter.json.FilterJson;
import org.codice.ddf.catalog.ui.filter.json.JsonFunctionTransform;

public class JsonFunctionRegistry {

  private static final List<JsonFunctionTransform> TRANSFORMS =
      Collections.singletonList(new TemplatePropertiesTransform());

  @Deprecated private static final String TEMPLATE_VALUE_V1 = "template.value.v1";

  public List<JsonFunctionTransform> getTransforms() {
    return TRANSFORMS;
  }

  private static class TemplatePropertiesTransform implements JsonFunctionTransform {

    @Override
    public boolean canApplyToFunction(Map<String, Object> customJson) {
      notNull(customJson);
      return customJson.containsKey(FilterJson.Keys.TEMPLATE_PROPS);
    }

    @Override
    public boolean canApplyFromFunction(Map<String, Object> filterJson) {
      notNull(filterJson);
      Object val = filterJson.get(FilterJson.Keys.VALUE);
      return val instanceof Map
          && isFunction((Map) val)
          && TEMPLATE_VALUE_V1.equals(getFuncName((Map) val));
    }

    @Override
    public Map<String, Object> toFunction(Map<String, Object> customJson) {
      notNull(customJson);
      Map<String, Object> templateProperties = getTemplateProperties(customJson);
      return ImmutableMap.<String, Object>builder()
          .put(FilterJson.Keys.TYPE, customJson.get(FilterJson.Keys.TYPE))
          .put(FilterJson.Keys.PROPERTY, customJson.get(FilterJson.Keys.PROPERTY))
          .put(
              FilterJson.Keys.VALUE,
              ImmutableMap.<String, Object>builder()
                  .put(FilterJson.Keys.TYPE, FilterJson.Ops.FUNC)
                  .put(FilterJson.Keys.NAME, TEMPLATE_VALUE_V1)
                  .put(
                      "args",
                      keyValuesToOrderedList(
                          templateProperties, "defaultValue", "nodeId", "isVisible", "isReadOnly")))
          .build();
    }

    @Override
    public Map<String, Object> fromFunction(Map<String, Object> filterJson) {
      notNull(filterJson);
      List<Object> params = getParams((Map) filterJson.get(FilterJson.Keys.VALUE));
      return ImmutableMap.<String, Object>builder()
          .put(FilterJson.Keys.TYPE, filterJson.get(FilterJson.Keys.TYPE))
          .put(FilterJson.Keys.PROPERTY, filterJson.get(FilterJson.Keys.PROPERTY))
          .put(
              FilterJson.Keys.TEMPLATE_PROPS,
              orderedListToMap(params, "defaultValue", "nodeId", "isVisible", "isReadOnly"))
          .build();
    }

    private List<Object> keyValuesToOrderedList(Map<String, Object> src, String... keys) {
      notNull(src);
      notEmpty(keys);
      return Arrays.stream(keys).map(src::get).collect(Collectors.toList());
    }

    private Map<String, Object> orderedListToMap(List<Object> src, String... keys) {
      notNull(src);
      notEmpty(keys);
      if (src.size() != keys.length) {
        throw new IllegalArgumentException(
            String.format(
                "List of objects (%s) should map 1-to-1 with keys (%s)",
                src.toString(), Arrays.toString(keys)));
      }
      Map<String, Object> result = new HashMap<>();
      for (int i = 0; i < src.size(); i++) {
        String key = keys[i];
        Object val = src.get(i);
        notNull(key);
        // Null values allowed
        result.put(key, val);
      }
      return result;
    }
  }
}
