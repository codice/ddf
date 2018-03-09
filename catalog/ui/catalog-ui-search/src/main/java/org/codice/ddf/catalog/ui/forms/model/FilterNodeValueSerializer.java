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

/**
 * {@link org.codice.ddf.catalog.ui.forms.model.JsonModel.FilterLeafNode} requires polymorphic
 * behavior which causes issues with JSON serialization. To avoid values of the following complex
 * form:
 *
 * <p>{@code
 * "value":{"class":"java.lang.String","value":[r,u,s,s,i,a,n,-,n,e,w,s,-,s,t,o,r,y],"hash":0}}
 *
 * <p>all values are treated as Strings and the following {@link CustomFieldSerializer} writes them
 * out to their proper type.
 */
public class FilterNodeValueSerializer implements CustomFieldSerializer {
  @Override
  public boolean serializeField(
      JsonSerializerInternal serializer, Object parent, FieldAccess fieldAccess, CharBuf builder) {
    Field field = fieldAccess.getField();
    String name = field.getName();

    boolean shouldManuallySerialize =
        (name.equals("value") || name.equals("defaultValue"))
            && field.getType().equals(String.class);
    if (!shouldManuallySerialize) {
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
      return false;
    }

    builder.addJsonFieldName(name);

    Integer i = Ints.tryParse(fieldValue);
    if (i != null) {
      builder.addInt(i);
      return true;
    }

    Double d = Doubles.tryParse(fieldValue);
    if (d != null) {
      builder.addDouble(d);
      return true;
    }

    if (fieldValue.equalsIgnoreCase("true")) {
      builder.addBoolean(true);
      return true;
    }

    if (fieldValue.equalsIgnoreCase("false")) {
      builder.addBoolean(false);
      return true;
    }

    // Assume plain String
    builder.addQuoted(fieldValue);
    return true;
  }

  class UncheckedIllegalAccessException extends RuntimeException {
    UncheckedIllegalAccessException(String msg, Throwable t) {
      super(msg, t);
    }
  }
}
