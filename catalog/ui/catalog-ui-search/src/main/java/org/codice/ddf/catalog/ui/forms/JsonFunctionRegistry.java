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
package org.codice.ddf.catalog.ui.forms;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonFunctionRegistry {

  private static final List<JsonFunctionTransform> TRANSFORMS =
      ImmutableList.<JsonFunctionTransform>builder().add(new TemplatePropertiesTransform()).build();

  public List<JsonFunctionTransform> getTransforms() {
    return TRANSFORMS;
  }

  private static class TemplatePropertiesTransform implements JsonFunctionTransform {

    @Override
    public boolean canApplyToFunction(Map<String, ?> customJson) {
      return customJson.containsKey("templateProperties");
    }

    @Override
    public boolean canApplyFromFunction(Map<String, ?> filterJson) {
      Object val = filterJson.get("value");
      return val instanceof Map
          && "FILTER_FUNCTION".equals(((Map) val).get("type"))
          && "template.value.v1".equals(((Map) val).get("name"));
    }

    @Override
    public Map<String, ?> toFunction(Map<String, ?> customJson) {
      Map<String, Object> templateProperties =
          (Map<String, Object>) customJson.get("templateProperties");
      List<Object> args = Lists.newArrayList();
      args.add(templateProperties.get("defaultValue"));
      args.add(templateProperties.get("nodeId"));
      args.add(templateProperties.get("isVisible"));
      args.add(templateProperties.get("isReadOnly"));
      return ImmutableMap.<String, Object>builder()
          .put("type", customJson.get("type"))
          .put("property", customJson.get("property"))
          .put(
              "value",
              ImmutableMap.<String, Object>builder()
                  .put("type", "FILTER_FUNCTION")
                  .put("name", "template.value.v1")
                  .put("args", args))
          .build();
    }

    @Override
    public Map<String, ?> fromFunction(Map<String, ?> filterJson) {
      Map<String, Object> func = (Map<String, Object>) filterJson.get("value");
      List<Object> args = (List<Object>) func.get("args");
      Map<String, Object> templateProperties = new HashMap<>();
      templateProperties.put("defaultValue", args.get(0));
      templateProperties.put("nodeId", args.get(1));
      templateProperties.put("isVisible", args.get(2));
      templateProperties.put("isReadOnly", args.get(3));
      return ImmutableMap.<String, Object>builder()
          .put("type", filterJson.get("type"))
          .put("property", filterJson.get("property"))
          .put("templateProperties", templateProperties)
          .build();
    }
  }
}
