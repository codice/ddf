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
  public static <T> T safeGet(Metacard metacard, String attributeName, Class<T> type) {
    Attribute attribute = metacard.getAttribute(attributeName);
    if (attribute == null) {
      LOGGER.debug("Attribute {} was null for metacard {}", attributeName, metacard);
      return null;
    }
    Serializable serializable = attribute.getValue();
    if (serializable == null) {
      LOGGER.debug(
          "Attribute {} for metacard {} was itself not null, but contained a null value",
          attributeName,
          metacard);
      return null;
    }
    if (!type.isInstance(serializable)) {
      LOGGER.debug(
          "Attribute {} for metacard {} had a value of unexpected type, was expecting {} but got {}",
          attributeName,
          metacard,
          type.getName(),
          serializable.getClass().getName());
      return null;
    }
    return type.cast(serializable);
  }

  @Nullable
  public static <T> List<T> safeGetList(Metacard metacard, String attributeName, Class<T> type) {
    Attribute attribute = metacard.getAttribute(attributeName);
    if (attribute == null) {
      LOGGER.debug("Attribute {} was null for metacard {}", attributeName, metacard);
      return null;
    }
    List<Serializable> serializables = attribute.getValues();
    if (serializables == null) {
      LOGGER.debug(
          "Attribute {} for metacard {} was itself not null, but contained a null list",
          attributeName,
          metacard);
      return null;
    }
    Optional<Serializable> entryWithBadType =
        serializables.stream().filter(o -> !type.isInstance(o)).findAny();
    if (entryWithBadType.isPresent()) {
      Serializable invalid = entryWithBadType.get();
      LOGGER.debug(
          "Attribute {} for metacard {} had a value of unexpected type, was expecting {} but got {}",
          attributeName,
          metacard,
          type.getName(),
          invalid.getClass().getName());
      return null;
    }
    return serializables.stream().map(type::cast).collect(Collectors.toList());
  }
}
