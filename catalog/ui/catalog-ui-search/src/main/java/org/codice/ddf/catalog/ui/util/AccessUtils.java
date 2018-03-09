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
package org.codice.ddf.catalog.ui.util;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccessUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(AccessUtils.class);

  private AccessUtils() {
    // Should not be instantiated
  }

  @Nullable
  public static <T> T safeGet(Metacard m, String name, Class<T> type) {
    Attribute a = m.getAttribute(name);
    if (a == null) {
      LOGGER.debug("Attribute {} was null for metacard {}", name, m);
      return null;
    }
    Serializable s = a.getValue();
    if (s == null) {
      LOGGER.debug(
          "Attribute {} for metacard {} was itself not null, but contained a null value", name, m);
      return null;
    }
    if (!type.isInstance(s)) {
      LOGGER.debug(
          "Attribute {} for metacard {} had a value of unexpected type, was expecting {} but got {}",
          name,
          m,
          type.getName(),
          s.getClass().getName());
      return null;
    }
    return type.cast(s);
  }

  @Nullable
  public static <T> List<T> safeGetList(Metacard m, String name, Class<T> type) {
    Attribute a = m.getAttribute(name);
    if (a == null) {
      LOGGER.debug("Attribute {} was null for metacard {}", name, m);
      return null;
    }
    List<Serializable> s = a.getValues();
    if (s == null) {
      LOGGER.debug(
          "Attribute {} for metacard {} was itself not null, but contained a null list", name, m);
      return null;
    }
    Optional<Serializable> entryWithBadType = s.stream().filter(o -> !type.isInstance(o)).findAny();
    if (entryWithBadType.isPresent()) {
      Serializable invalid = entryWithBadType.get();
      LOGGER.debug(
          "Attribute {} for metacard {} had a value of unexpected type, was expecting {} but got {}",
          name,
          m,
          type.getName(),
          invalid.getClass().getName());
      return null;
    }
    return s.stream().map(type::cast).collect(Collectors.toList());
  }
}
