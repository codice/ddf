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
package org.codice.ddf.configuration.migration;

import com.google.common.annotations.VisibleForTesting;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.boon.json.JsonFactory;
import org.boon.json.ObjectMapper;
import org.codice.ddf.migration.MigrationException;

/**
 * This class provides utility functions for dealing with Json objects returned by the <code>
 * org.boon.json</code> library.
 */
public class JsonUtils {
  // PMD.DefaultPackage - designed as an internal service within this package
  @SuppressWarnings("PMD.DefaultPackage")
  @VisibleForTesting
  static final ObjectMapper MAPPER = JsonFactory.create();

  private JsonUtils() {}

  /**
   * Converts a given Json object to a Json map.
   *
   * @param o the Json object to return as a map
   * @return the corresponding map
   * @throws MigrationException if the given object is not a map
   */
  public static Map<String, Object> convertToMap(@Nullable Object o) {
    if (o == null) {
      return Collections.emptyMap();
    }
    if (!(o instanceof Map)) {
      throw new MigrationException(Messages.IMPORT_METADATA_FORMAT_ERROR, "expecting a Json map");
    }
    return (Map<String, Object>) o;
  }

  /**
   * Retrieves a Json map from the specified Json map given its key.
   *
   * @param map the map to retrieve an entry from
   * @param key the key for the entry to retrieve
   * @return the corresponding Json map or an empty one if none defined
   * @throws MigrationException if the specified entry is not a Json map
   */
  public static Map<String, Object> getMapFrom(
      @Nullable Map<String, Object> map, @Nullable String key) {
    final Map<String, Object> m = JsonUtils.getObjectFrom(Map.class, map, key, false);

    return (m != null) ? m : Collections.emptyMap();
  }

  /**
   * Retrieves a Json array from the specified Json map given its key.
   *
   * @param map the map to retrieve an entry from
   * @param key the key for the entry to retrieve
   * @return the corresponding Json array as a list or an empty one if none defined
   * @throws MigrationException if the specified entry is not a Json array
   */
  public static List<Object> getListFrom(@Nullable Map<String, Object> map, @Nullable String key) {
    final List<Object> l = JsonUtils.getObjectFrom(List.class, map, key, false);

    return (l != null) ? l : Collections.emptyList();
  }

  /**
   * Retrieves a Json string from the specified Json map given its key.
   *
   * @param map the map to retrieve an entry from
   * @param key the key for the entry to retrieve
   * @param required <code>true</code> if the entry must exist in the map otherwise an error is
   *     generated; <code>false</code> to return <code>null</code> if it doesn't exist
   * @return the corresponding Json string or <code>null</code> if it doesn't exist and <code>
   *     required</code> is <code>false</code>
   * @throws MigrationException if the specified entry is not a Json string or if it doesn't exist
   *     and <code>required</code> is true
   */
  public static String getStringFrom(
      @Nullable Map<String, Object> map, @Nullable String key, boolean required) {
    return JsonUtils.getObjectFrom(String.class, map, key, required);
  }

  /**
   * Retrieves a Json number as a long from the specified Json map given its key.
   *
   * @param map the map to retrieve an entry from
   * @param key the key for the entry to retrieve
   * @param required <code>true</code> if the entry must exist in the map otherwise an error is
   *     generated; <code>false</code> to return <code>null</code> if it doesn't exist
   * @return the corresponding Json number as a long or <code>null</code> if it doesn't exist and
   *     <code>required</code> is <code>false</code>
   * @throws MigrationException if the specified entry is not a Json number or if it doesn't exist
   *     and <code>required</code> is true
   */
  public static Long getLongFrom(
      @Nullable Map<String, Object> map, @Nullable String key, boolean required) {
    final Number n = JsonUtils.getObjectFrom(Number.class, map, key, required);

    return (n != null) ? n.longValue() : null;
  }

  /**
   * Retrieves a Json boolean from the specified Json map given its key.
   *
   * @param map the map to retrieve an entry from
   * @param key the key for the entry to retrieve
   * @param required <code>true</code> if the entry must exist in the map otherwise an error is
   *     generated; <code>false</code> to return <code>null</code> if it doesn't exist
   * @return the corresponding Json boolean or <code>false</code> if it doesn't exist and <code>
   *     required</code> is <code>false</code>
   * @throws MigrationException if the specified entry is not a Json boolean or if it doesn't exist
   *     and <code>required</code> is true
   */
  public static boolean getBooleanFrom(
      @Nullable Map<String, Object> map, @Nullable String key, boolean required) {
    final Boolean b = JsonUtils.getObjectFrom(Boolean.class, map, key, required);

    return (b != null) ? b : false;
  }

  private static <T> T getObjectFrom(
      Class<T> clazz, @Nullable Map<String, Object> info, @Nullable String key, boolean required) {
    final Object v = (info != null) ? info.get(key) : null;

    if (v == null) {
      if (required) {
        throw new MigrationException(
            Messages.IMPORT_METADATA_FORMAT_ERROR, String.format("missing required [%s]", key));
      }
      return null;
    }
    if (!clazz.isInstance(v)) {
      throw new MigrationException(
          Messages.IMPORT_METADATA_FORMAT_ERROR,
          String.format("[%s] is not a Json %s", key, clazz.getSimpleName().toLowerCase()));
    }
    return clazz.cast(v);
  }
}
