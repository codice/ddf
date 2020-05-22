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
package org.codice.gsonsupport;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import javax.annotation.Nullable;
import org.apache.commons.collections4.map.LinkedMap;
import org.apache.commons.lang3.StringUtils;

/**
 * Utility class to generate Gson instances customized to work with our expected input/output
 * formats.
 */
public class GsonTypeAdapters {
  public static final Type MAP_STRING_TO_OBJECT_TYPE =
      new TypeToken<Map<String, Object>>() {}.getType();

  public static final Type LIST_STRING = new TypeToken<List<String>>() {}.getType();

  private GsonTypeAdapters() {}

  /** Adapter to convert read/write Dates as Longs */
  public static class DateLongFormatTypeAdapter extends TypeAdapter<Date> {
    @Override
    public void write(JsonWriter out, Date date) throws IOException {
      Long time = Optional.ofNullable(date).map(Date::getTime).orElse(null);
      out.value(time);
    }

    @Override
    public Date read(JsonReader in) throws IOException {
      return new Date(in.nextLong());
    }
  }

  /** Adapter to convert read/write Persistence Objects */
  public static class PersistenceMapTypeAdapter extends TypeAdapter<Object> {

    public static final String TEXT_SUFFIX = "_txt";

    public static final String XML_SUFFIX = "_xml";

    public static final String INT_SUFFIX = "_int";

    public static final String LONG_SUFFIX = "_lng";

    public static final String DATE_SUFFIX = "_tdt";

    public static final String BINARY_SUFFIX = "_bin";

    public static final String[] SUFFIXES =
        new String[] {TEXT_SUFFIX, XML_SUFFIX, INT_SUFFIX, LONG_SUFFIX, DATE_SUFFIX, BINARY_SUFFIX};

    static final String DATE_FORMAT = "dd MMM yyyy HH:mm:ss zzz";

    final SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);

    public static final TypeAdapterFactory FACTORY =
        new TypeAdapterFactory() {
          public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
            return (TypeAdapter<T>) new PersistenceMapTypeAdapter(gson);
          }
        };

    private final Gson gson;

    PersistenceMapTypeAdapter(Gson gson) {
      this.gson = gson;
    }

    @Override
    public void write(JsonWriter out, Object value) throws IOException {
      if (value == null) {
        out.nullValue();
      } else {
        TypeAdapter<Object> typeAdapter = gson.getAdapter((Class<Object>) value.getClass());
        if (typeAdapter instanceof PersistenceMapTypeAdapter) {
          write(out, (Map) value);
        } else {
          typeAdapter.write(out, value);
        }
      }
    }

    private void write(JsonWriter out, Map<String, Object> map) throws IOException {
      out.beginObject();

      for (Entry<String, Object> row : map.entrySet()) {
        Object value = row.getValue();
        if (value != null) {
          out.name(row.getKey());
          if (value instanceof byte[]) {
            out.value(Base64.getEncoder().encodeToString((byte[]) value));
          } else if (value instanceof Date) {
            out.value(formatter.format((Date) value));
          } else if (value instanceof String) {
            out.value((String) value);
          } else if (value instanceof Long) {
            out.value((Long) value);
          } else if (value instanceof Integer) {
            out.value((Integer) value);
          }
        }
      }
      out.endObject();
    }

    @Override
    public Object read(JsonReader in) throws IOException {
      JsonToken token = in.peek();

      switch (token) {
        case BEGIN_OBJECT:
          return readObject(in);
        case STRING:
          return in.nextString();
        case NUMBER:
          return readNumber(in);
        case BOOLEAN:
          return in.nextBoolean();
        case NULL:
          in.nextNull();
          return null;
        default:
          throw new IllegalStateException("unknown gson token: " + token);
      }
    }

    /**
     * All numbers without decimal points are read in as Longs.
     *
     * @param in reader with next value as number
     * @return parsed number
     * @throws IOException if there is a problem reading the stream
     */
    private Object readNumber(JsonReader in) throws IOException {
      final String n = in.nextString();

      if (n.indexOf('.') == -1) {
        try {
          return Long.parseLong(n);
        } catch (NumberFormatException e) { // ignore and handle it as a double
        }
      }
      return Double.parseDouble(n);
    }

    private Object readInt(JsonReader in) throws IOException {
      final String n = in.nextString();
      try {
        return Integer.parseInt(n);
      } catch (NumberFormatException e) { // ignore and handle it as a double
      }
      return null;
    }

    private Object readObject(JsonReader in) throws IOException {
      final Map<String, Object> map = new LinkedMap<>();

      in.beginObject();
      while (in.hasNext()) {
        String key = in.nextName();
        String attributeType = extractTypeSuffix(key);
        map.put(key, readValue(in, attributeType));
      }
      in.endObject();
      return map;
    }

    private String extractTypeSuffix(String key) {
      int index = StringUtils.lastIndexOfAny(key, SUFFIXES);
      if (index > 0) {
        return key.substring(index);
      } else {
        return null;
      }
    }

    public Object readValue(JsonReader in, String attributeType) throws IOException {
      JsonToken token = in.peek();
      switch (token) {
        case BEGIN_OBJECT:
          return readObject(in);
        case STRING:
        case NUMBER:
          if (attributeType == null) {
            return null;
          }
          switch (attributeType.toLowerCase()) {
            case BINARY_SUFFIX:
              return Base64.getDecoder().decode(String.valueOf(in.nextString()));
            case DATE_SUFFIX:
              try {
                return formatter.parse(String.valueOf(in.nextString()));
              } catch (ParseException e) {
                return null;
              }
            case INT_SUFFIX:
              return readInt(in);
            case LONG_SUFFIX:
              return readNumber(in);
            case TEXT_SUFFIX:
            case XML_SUFFIX:
              return in.nextString();
            default:
              return null;
          }
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

  /** Adapter to mimic our old Boon-style numeric representations with Gson. */
  public static class LongDoubleTypeAdapter extends TypeAdapter<Object> {
    @SuppressWarnings("squid:S1604" /* Interface is not generic; method is. */)
    public static final TypeAdapterFactory FACTORY =
        new TypeAdapterFactory() {

          public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
            Type type = typeToken.getType();
            Class<? super T> rawType = typeToken.getRawType();

            if (Number.class.isAssignableFrom(rawType)) {
              return (TypeAdapter<T>) new LongDoubleTypeAdapter(gson);
            }
            if (Map.class.isAssignableFrom(rawType)) {
              Type[] mapKeyAndValueTypes = GsonTypes.getMapKeyAndValueTypes(type, rawType);
              return buildTypeAdapter(gson, mapKeyAndValueTypes[1]);
            }
            if (List.class.isAssignableFrom(rawType)) {
              Type collectionType = GsonTypes.getCollectionElementType(type, rawType);
              return buildTypeAdapter(gson, collectionType);
            }

            return null;
          }

          private <T> TypeAdapter<T> buildTypeAdapter(Gson gson, Type collectionType) {
            if (TypeToken.get(collectionType).getRawType() == Object.class) {
              return (TypeAdapter<T>) new LongDoubleTypeAdapter(gson);
            } else {
              TypeAdapter<?> typeAdapter = gson.getAdapter(TypeToken.get(collectionType));
              return (TypeAdapter<T>) new LongDoubleTypeAdapter(gson, typeAdapter);
            }
          }
        };

    private final Gson gson;

    @Nullable private final TypeAdapter<?> collectionTypeAdapter;

    LongDoubleTypeAdapter(Gson gson) {
      this(gson, null);
    }

    LongDoubleTypeAdapter(Gson gson, @Nullable TypeAdapter<?> collectionTypeAdapter) {
      this.gson = gson;
      this.collectionTypeAdapter = collectionTypeAdapter;
    }

    @Override
    public Object read(JsonReader in) throws IOException {
      final JsonToken token = in.peek();

      switch (token) {
        case BEGIN_ARRAY:
          return readArray(in);
        case BEGIN_OBJECT:
          return readObject(in);
        case STRING:
          return in.nextString();
        case NUMBER:
          return readNumber(in);
        case BOOLEAN:
          return in.nextBoolean();
        case NULL:
          in.nextNull();
          return null;
        default:
          throw new IllegalStateException("unknown gson token: " + token);
      }
    }

    public void write(JsonWriter out, Object value) throws IOException {
      if (value == null) {
        out.nullValue();
      } else {
        TypeAdapter<Object> typeAdapter = gson.getAdapter((Class<Object>) value.getClass());
        if (typeAdapter instanceof LongDoubleTypeAdapter) {
          if (value instanceof Number) {
            write(out, (Number) value);
          } else if (value instanceof Map) {
            write(out, (Map) value);
          } else if (value instanceof List) {
            write(out, (List) value);
          }
        } else {
          typeAdapter.write(out, value);
        }
      }
    }

    /**
     * All numbers without decimal points are read in as Longs.
     *
     * @param in reader with next value as number
     * @return parsed number
     * @throws IOException if there is a problem reading the stream
     */
    private Object readNumber(JsonReader in) throws IOException {
      final String n = in.nextString();

      if (n.indexOf('.') == -1) {
        try {
          return Long.parseLong(n);
        } catch (NumberFormatException e) { // ignore and handle it as a double
        }
      }
      return Double.parseDouble(n);
    }

    private Object readObject(JsonReader in) throws IOException {
      final Map<String, Object> map = new LinkedMap<>();

      in.beginObject();
      if (collectionTypeAdapter == null) {
        while (in.hasNext()) {
          map.put(in.nextName(), read(in));
        }
      } else {
        while (in.hasNext()) {
          map.put(in.nextName(), collectionTypeAdapter.read(in));
        }
      }
      in.endObject();
      return map;
    }

    private Object readArray(JsonReader in) throws IOException {
      final List<Object> list = new ArrayList<>();

      in.beginArray();
      if (collectionTypeAdapter == null) {
        while (in.hasNext()) {
          list.add(read(in));
        }
      } else {
        while (in.hasNext()) {
          list.add(collectionTypeAdapter.read(in));
        }
      }
      in.endArray();
      return list;
    }

    private void write(JsonWriter out, Number number) throws IOException {
      if (number instanceof Long || number instanceof Integer) {
        out.value(number.longValue());
      } else {
        out.value(number);
      }
    }

    private void write(JsonWriter out, Map<String, Object> map) throws IOException {
      out.beginObject();

      for (Entry<String, Object> row : map.entrySet()) {
        if (row.getValue() != null) {
          out.name(row.getKey());
          write(out, row.getValue());
        }
      }

      out.endObject();
    }

    private void write(JsonWriter out, List list) throws IOException {
      out.beginArray();

      for (Object o : list) {
        if (o != null) {
          write(out, o);
        }
      }

      out.endArray();
    }
  }
}
