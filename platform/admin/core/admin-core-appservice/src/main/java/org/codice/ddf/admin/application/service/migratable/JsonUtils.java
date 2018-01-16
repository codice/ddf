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

import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import org.boon.Exceptions.SoftenedException;
import org.boon.json.JsonFactory;
import org.boon.json.JsonParserFactory;
import org.boon.json.JsonSerializerFactory;
import org.boon.json.ObjectMapper;
import org.boon.json.serializers.JsonSerializerInternal;
import org.boon.json.serializers.impl.AbstractCustomObjectSerializer;
import org.boon.primitive.CharBuf;
import org.osgi.framework.Version;

/** Provides useful functions for dealing with Json. */
public class JsonUtils {

  private JsonUtils() {
    throw new UnsupportedOperationException();
  }

  private static final ObjectMapper MAPPER =
      JsonFactory.create(
          new JsonParserFactory().useAnnotations(),
          new JsonSerializerFactory()
              .useAnnotations()
              .addTypeSerializer(
                  Version.class,
                  new AbstractCustomObjectSerializer(Version.class) {
                    @Override
                    public void serializeObject(
                        JsonSerializerInternal serializer, Object instance, CharBuf builder) {
                      serializer.serializeString(instance.toString(), builder);
                    }
                  }));

  public static void writeValue(OutputStream os, Object value) {
    JsonUtils.MAPPER.writeValue(os, value);
  }

  public static String toJson(Object value) {
    return JsonUtils.MAPPER.toJson(value);
  }

  // FYI, Boon has a bug when dealing with streams where it stops reading the stream at whatever is
  // returned from the first read with a buffer of 8K. If the stream returns less or 8K, the stream
  // is closed. So if you are dealing with large Json files, that chops the file.
  // See https://github.com/boonproject/boon/issues/320 describes the problem
  // if you follow the suggested instructions then it stops at 1K and never goes back.
  public static <T> T fromJson(String json, Class<T> clazz) {
    try {
      return JsonUtils.validate(JsonUtils.MAPPER.fromJson(json, clazz));
    } catch (SoftenedException e) {
      JsonUtils.handleException(e);
      throw e;
    }
  }

  private static <T> T validate(T t) {
    if (t instanceof JsonValidatable) {
      ((JsonValidatable) t).validate();
    }
    return t;
  }

  private static void handleException(Throwable t) {
    if (t instanceof SoftenedException) {
      JsonUtils.handleException(t.getCause());
    } else if (t instanceof InvocationTargetException) {
      JsonUtils.handleException(t.getCause());
    } else if (t instanceof Error) {
      throw (Error) t;
    } else if (t instanceof RuntimeException) {
      throw (RuntimeException) t;
    } // else - Let the calling code above deal with throwing the original exception
  }
}
