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

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.codice.ddf.catalog.ui.filter.json.JsonFunctionTransform;

public class JsonFunctionRegistry {

  private static final List<JsonFunctionTransform> TRANSFORMS =
      Collections.singletonList(new TemplatePropertiesTransform());

  public List<JsonFunctionTransform> getTransforms() {
    return TRANSFORMS;
  }

  private static class TemplatePropertiesTransform implements JsonFunctionTransform {

    @Override
    public boolean canApplyToFunction(Map<String, Object> customJson) {
      notNull(customJson);
      return customJson.containsKey("templateProperties");
    }

    @Override
    public boolean canApplyFromFunction(Map<String, Object> filterJson) {
      notNull(filterJson);
      Object val = filterJson.get("value");
      return val instanceof Map
          && "FILTER_FUNCTION".equals(((Map) val).get("type"))
          && "template.value.v1".equals(((Map) val).get("name"));
    }

    @Override
    public Map<String, Object> toFunction(Map<String, Object> customJson) {
      notNull(customJson);
      Map<String, Object> templateProperties = getTemplateProperties(customJson);
      return ImmutableMap.<String, Object>builder()
          .put("type", customJson.get("type"))
          .put("property", customJson.get("property"))
          .put(
              "value",
              ImmutableMap.<String, Object>builder()
                  .put("type", "FILTER_FUNCTION")
                  .put("name", "template.value.v1")
                  .put(
                      "args",
                      keyValuesToOrderedList(
                          templateProperties, "defaultValue", "nodeId", "isVisible", "isReadOnly")))
          .build();
    }

    @Override
    public Map<String, Object> fromFunction(Map<String, Object> filterJson) {
      notNull(filterJson);
      List<Object> args = getArgs(filterJson);
      return ImmutableMap.<String, Object>builder()
          .put("type", filterJson.get("type"))
          .put("property", filterJson.get("property"))
          .put(
              "templateProperties",
              orderedListToMap(args, "defaultValue", "nodeId", "isVisible", "isReadOnly"))
          .build();
    }

    private Map<String, Object> getTemplateProperties(Map<String, Object> customJson) {
      return (Map<String, Object>) customJson.get("templateProperties");
    }

    private List<Object> getArgs(Map<String, Object> filterJson) {
      Map<String, Object> func = (Map<String, Object>) filterJson.get("value");
      return (List<Object>) func.get("args");
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
