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
package org.codice.ddf.catalog.ui.forms.filter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.opengis.filter.v_2_0.LiteralType;

public class FunctionResolver {
  private static final Function<Serializable, Boolean> BOOL_FUNC =
      s -> Boolean.parseBoolean((String) s);

  private static final String TEMPLATE_SUBSTITUTIONS_V1 = "template.value.v1";

  private static final Integer DEFAULT_VALUE_INDEX = 0;

  private static final Integer NODE_ID_INDEX = 1;

  private static final Integer IS_VISIBLE_INDEX = 2;

  private static final Integer IS_READONLY_INDEX = 3;

  // Should not be instantiated
  private FunctionResolver() {}

  public static Map<String, Object> resolve(String functionName, List<LiteralType> args) {
    if (!TEMPLATE_SUBSTITUTIONS_V1.equals(functionName)) {
      throw new FilterProcessingException("Unrecognized function:  " + functionName);
    }

    List<Optional<Serializable>> wrappedArgs =
        args.stream()
            .map(LiteralType::getContent)
            .flatMap(
                content ->
                    (content == null || content.isEmpty())
                        ? Stream.of(Optional.<Serializable>empty())
                        : content.stream().map(Optional::of))
            .collect(Collectors.toList());

    Map<String, Object> result = new HashMap<>();
    result.put("defaultValue", get(wrappedArgs, DEFAULT_VALUE_INDEX, String.class));
    result.put("nodeId", get(wrappedArgs, NODE_ID_INDEX, String.class));
    result.put("isVisible", get(wrappedArgs, IS_VISIBLE_INDEX, BOOL_FUNC));
    result.put("isReadOnly", get(wrappedArgs, IS_READONLY_INDEX, BOOL_FUNC));
    return result;
  }

  private static <T> T get(List<Optional<Serializable>> args, int i, Class<T> expectedType) {
    return get(args, i, expectedType::cast);
  }

  private static <T> T get(
      List<Optional<Serializable>> args, int i, Function<Serializable, T> transform) {
    return transform.apply(args.get(i).orElse(null));
  }
}
