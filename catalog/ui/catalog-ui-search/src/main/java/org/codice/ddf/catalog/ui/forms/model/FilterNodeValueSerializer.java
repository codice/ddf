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
package org.codice.ddf.catalog.ui.forms.model;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import java.lang.reflect.Field;
import org.boon.core.reflection.fields.FieldAccess;
import org.boon.json.serializers.CustomFieldSerializer;
import org.boon.json.serializers.JsonSerializerInternal;
import org.boon.primitive.CharBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link org.codice.ddf.catalog.ui.forms.model.FilterNodeImpl} requires polymorphic behavior which
 * causes issues with JSON serialization. To avoid values of the following complex form:
 *
 * <p>{@code
 * "value":{"class":"java.lang.String","value":[r,u,s,s,i,a,n,-,n,e,w,s,-,s,t,o,r,y],"hash":0}}
 *
 * <p>all values are treated as Strings and the following {@link CustomFieldSerializer} writes them
 * out to their proper type.
 *
 * <p><i>This code is experimental. While it is functional and tested, it may change or be removed
 * in a future version of the library.</i>
 */
public class FilterNodeValueSerializer implements CustomFieldSerializer {
  private static final Logger LOGGER = LoggerFactory.getLogger(FilterNodeValueSerializer.class);

  @Override
  public boolean serializeField(
      JsonSerializerInternal serializer, Object parent, FieldAccess fieldAccess, CharBuf builder) {
    Field field = fieldAccess.getField();
    String name = field.getName();

    boolean shouldManuallySerialize =
        ("value".equals(name) || "defaultValue".equals(name))
            && field.getType().equals(String.class);
    if (!shouldManuallySerialize) {
      LOGGER.trace("Special serialization is not needed for field '{}'", name);
      return false;
    }

    String fieldValue = null;
    try {
      fieldValue = (String) field.get(parent);
    } catch (IllegalAccessException e) {
      throw new UncheckedIllegalAccessException(
          "Access denied for field 'value' on FilterLeafNode", e);
    }

    if (fieldValue == null) {
      LOGGER.debug("Field '{}' held a null value, no special serialization needed", name);
      return false;
    }

    builder.addJsonFieldName(name);

    Integer i = Ints.tryParse(fieldValue);
    if (i != null) {
      LOGGER.debug("Found Integer '{}' on field '{}', serializing appropriately", i, name);
      builder.addInt(i);
      return true;
    }

    Double d = Doubles.tryParse(fieldValue);
    if (d != null) {
      LOGGER.debug("Found Double '{}' on field '{}', serializing appropriately", d, name);
      builder.addDouble(d);
      return true;
    }

    if (fieldValue.equalsIgnoreCase("true")) {
      LOGGER.debug("Found Boolean '{}' on field '{}', serializing appropriately", fieldValue, name);
      builder.addBoolean(true);
      return true;
    }

    if (fieldValue.equalsIgnoreCase("false")) {
      LOGGER.debug("Found Boolean '{}' on field '{}', serializing appropriately", fieldValue, name);
      builder.addBoolean(false);
      return true;
    }

    // Assume plain String
    LOGGER.debug("Found String '{}' on field '{}', serializing appropriately", fieldValue, name);
    builder.addQuoted(fieldValue);
    return true;
  }

  class UncheckedIllegalAccessException extends RuntimeException {
    UncheckedIllegalAccessException(String msg, Throwable t) {
      super(msg, t);
    }
  }
}
