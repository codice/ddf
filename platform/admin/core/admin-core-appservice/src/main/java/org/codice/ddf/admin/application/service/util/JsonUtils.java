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
package org.codice.ddf.admin.application.service.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import org.osgi.framework.Version;

/** Provides useful functions for dealing with Json. */
public class JsonUtils {

  private static final Gson GSON;

  static {
    GSON =
        new GsonBuilder()
            .registerTypeHierarchyAdapter(Collection.class, new EmptyCollectionTypeAdapter())
            .registerTypeHierarchyAdapter(Map.class, new EmptyMapTypeAdapter())
            .registerTypeAdapter(Version.class, new VersionTypeAdapter())
            .create();
  }

  private JsonUtils() {
    throw new UnsupportedOperationException();
  }

  public static void writeValue(OutputStream os, Object value) throws IOException {
    final OutputStreamWriter writer = new OutputStreamWriter(os);

    JsonUtils.GSON.toJson(value, writer);
    writer.flush();
  }

  public static String toJson(Object value) {
    return JsonUtils.GSON.toJson(value);
  }

  public static <T> T fromJson(String json, Class<T> clazz) {
    return JsonUtils.validate(JsonUtils.GSON.fromJson(json, clazz));
  }

  public static <T> T fromJson(String json, Type type) {
    return JsonUtils.validate(JsonUtils.GSON.fromJson(json, type));
  }

  private static <T> T validate(T t) {
    if (t instanceof JsonValidatable) {
      ((JsonValidatable) t).validate();
    }
    return t;
  }

  /** Gson type adapter used to support Version serialization. */
  private static class VersionTypeAdapter
      implements JsonSerializer<Version>, JsonDeserializer<Version> {
    @Override
    public Version deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
      return new Version(json.getAsString());
    }

    @Override
    public JsonElement serialize(Version src, Type typeOfSrc, JsonSerializationContext context) {
      return (src != null) ? context.serialize(src.toString()) : null;
    }
  }

  /** Gson type adapter used to handle numbers as long the way Boon was doing. */
  private static class NumberTypeAdapter
      implements JsonSerializer<Number>, JsonDeserializer<Number> {
    @Override
    public Number deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
      final String n = json.getAsString();

      if (n.indexOf('.') == -1) {
        try {
          return Long.parseLong(n);
        } catch (NumberFormatException e) { // ignore and handle it as a double
        }
      }
      return Double.parseDouble(n);
    }

    @Override
    public JsonElement serialize(Number src, Type typeOfSrc, JsonSerializationContext context) {
      return (src != null) ? new JsonPrimitive(src) : null;
    }
  }

  /** Gson type adapter for collections to not serialize empty ones. */
  private static class EmptyCollectionTypeAdapter implements JsonSerializer<Collection<?>> {
    @Override
    public JsonElement serialize(
        Collection<?> src, Type typeOfSrc, JsonSerializationContext context) {
      if ((src == null) || src.isEmpty()) {
        return null;
      }
      final JsonArray array = new JsonArray();

      for (final Object child : src) {
        array.add(context.serialize(child));
      }
      return array;
    }
  }

  /** Gson type adapter for maps to not serialize empty ones. */
  private static class EmptyMapTypeAdapter implements JsonSerializer<Map<?, ?>> {
    @Override
    public JsonElement serialize(Map<?, ?> src, Type typeOfSrc, JsonSerializationContext context) {
      if ((src == null) || src.isEmpty()) {
        return null;
      }
      final JsonObject obj = new JsonObject();

      for (final Map.Entry<?, ?> e : src.entrySet()) {
        obj.add(e.getKey().toString(), context.serialize(e.getValue()));
      }
      return obj;
    }
  }
}
