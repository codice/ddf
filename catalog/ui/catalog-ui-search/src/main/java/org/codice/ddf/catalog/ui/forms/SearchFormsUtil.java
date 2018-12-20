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

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchFormsUtil {
  private static final Logger LOGGER = LoggerFactory.getLogger(SearchFormsUtil.class);

  private static final String EXCEPTION_OCCURRED_PARSING = "Exception occurred parsing config file";

  public static boolean anyNull(Object... args) {
    return args == null || Arrays.stream(args).anyMatch(Objects::isNull);
  }

  public static <T> Map<String, T> safeGetMap(Map map, String key, Class<T> valueType) {
    Map<?, ?> unchecked = safeGet(map, key, Map.class);
    if (unchecked == null) {
      return Collections.emptyMap();
    }
    try {
      return unchecked
          .entrySet()
          .stream()
          .map(e -> new AbstractMap.SimpleEntry<>(String.class.cast(e.getKey()), e.getValue()))
          .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), valueType.cast(e.getValue())))
          .collect(
              Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    } catch (ClassCastException e) {
      LOGGER.debug(EXCEPTION_OCCURRED_PARSING, e);
      LOGGER.warn(
          "Form configuration field {} was malformed, expected a querySettings Map containing type {}",
          key,
          valueType.getName());
    }
    return Collections.emptyMap();
  }

  @Nullable
  public static <T> List<T> safeGetList(Map map, String key, Class<T> type) {
    List<?> unchecked = safeGet(map, key, List.class);
    if (unchecked == null) {
      return Collections.emptyList();
    }
    try {
      return (List<T>) unchecked.stream().map(type::cast).collect(Collectors.toList());
    } catch (ClassCastException e) {
      LOGGER.debug(EXCEPTION_OCCURRED_PARSING, e);
      LOGGER.warn(
          "Form configuration field {} was malformed, expected a List containing type {}",
          key,
          type.getName());
    }
    return Collections.emptyList();
  }

  @Nullable
  public static <T> T safeGet(Map map, String key, Class<T> type) {
    Object value = map.get(key);
    if (value == null) {
      LOGGER.debug("Unexpected null entry: {}", key);
      return null;
    }
    try {
      return type.cast(value);
    } catch (ClassCastException e) {
      LOGGER.debug(EXCEPTION_OCCURRED_PARSING, e);
      LOGGER.warn(
          "Form configuration field {} was malformed, expected a {} but got {}",
          key,
          type.getName(),
          value.getClass().getName());
    }
    return null;
  }
}
