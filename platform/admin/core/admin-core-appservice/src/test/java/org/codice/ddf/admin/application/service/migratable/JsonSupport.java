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
package org.codice.ddf.admin.application.service.migratable;

import com.google.common.collect.ImmutableMap;
import java.util.Map;

public class JsonSupport {

  public static Map<String, Object> toImmutableMap(Object... keysAndValues) {
    final ImmutableMap.Builder builder = ImmutableMap.builder();

    for (int i = 0; i < keysAndValues.length; i += 2) {
      builder.put(keysAndValues[i], keysAndValues[i + 1]);
    }
    return builder.build();
  }

  public static String toJsonString(Object... keysAndValues) {
    return JsonUtils.toJson(JsonSupport.toImmutableMap(keysAndValues));
  }
}
