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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.collections4.map.LinkedMap;
import org.codice.ddf.migration.MigrationException;

/** This class provides utility functions for dealing with Json objects. */
public class JsonUtils {
  @SuppressWarnings("PMD.DefaultPackage" /* designed as an internal service within this package */)
  @VisibleForTesting
  static final ObjectMapper MAPPER = new ObjectMapper();

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

  /** Dummy class used to replace Boon with Gson. */
  public static class ObjectMapper {
    private final Gson gson;

    private ObjectMapper() {
      final CustomizedObjectTypeAdapter adapter = new CustomizedObjectTypeAdapter();

      this.gson =
          new GsonBuilder()
              .serializeNulls()
              .registerTypeHierarchyAdapter(Map.class, adapter)
              .registerTypeHierarchyAdapter(List.class, adapter)
              .registerTypeAdapter(Double.class, adapter)
              .registerTypeAdapter(String.class, adapter)
              .create();
    }

    /**
     * Dummy method used to simulate the old boon library into.
     *
     * @return this
     */
    public ObjectMapper parser() {
      return this;
    }

    /**
     * Serializes any Java value as JSON output, using output stream provided (using encoding UTF8).
     *
     * <p><i>Note:</i> This method does not close the underlying stream explicitly here.
     *
     * @param out the output stream where to write the correspond Json
     * @param value the value to be converted to Json
     * @throws IOException if an I/O error occurred
     */
    public void writeValue(OutputStream out, Object value) throws IOException {
      final OutputStreamWriter writer = new OutputStreamWriter(out);

      gson.toJson(value, writer);
      writer.flush();
    }

    /**
     * Parses the given Json string into a corresponding map.
     *
     * @param json the Json string to be parsed
     * @return the corresponding map
     */
    public Map<String, Object> parseMap(String json) {
      return gson.fromJson(json, new TypeToken<Map<String, Object>>() {}.getType());
    }
  }

  /** Gson type adapter used to handle numbers as long the way Boon was doing. */
  private static class CustomizedObjectTypeAdapter extends TypeAdapter<Object> {
    private final TypeAdapter<Object> delegate = new Gson().getAdapter(Object.class);

    @Override
    public void write(JsonWriter out, Object value) throws IOException {
      delegate.write(out, value);
    }

    @Override
    public Object read(JsonReader in) throws IOException {
      final JsonToken token = in.peek();

      switch (token) {
        case BEGIN_ARRAY:
          final List<Object> list = new ArrayList<>();

          in.beginArray();
          while (in.hasNext()) {
            list.add(read(in));
          }
          in.endArray();
          return list;
        case BEGIN_OBJECT:
          final Map<String, Object> map = new LinkedMap<>();

          in.beginObject();
          while (in.hasNext()) {
            map.put(in.nextName(), read(in));
          }
          in.endObject();
          return map;
        case STRING:
          return in.nextString();
        case NUMBER:
          final String n = in.nextString();

          if (n.indexOf('.') == -1) {
            try {
              return Long.parseLong(n);
            } catch (NumberFormatException e) { // ignore and handle it as a double
            }
          }
          return Double.parseDouble(n);
        case BOOLEAN:
          return in.nextBoolean();
        case NULL:
          in.nextNull();
          return null;
        default:
          throw new IllegalStateException("unknown gson token: " + token);
      }
    }
  }
}
