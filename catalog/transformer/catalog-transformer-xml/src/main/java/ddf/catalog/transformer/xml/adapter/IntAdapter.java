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
package ddf.catalog.transformer.xml.adapter;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.transformer.xml.binding.IntElement;
import java.io.Serializable;
import javax.xml.bind.annotation.adapters.XmlAdapter;

public class IntAdapter extends XmlAdapter<IntElement, Attribute> {

  public static IntElement marshalFrom(Attribute attribute) {

    IntElement element = new IntElement();
    element.setName(attribute.getName());
    if (attribute.getValue() != null) {
      element
          .getValue()
          .addAll(new AdaptedList<Serializable, Integer>(attribute.getValues(), Integer.class));
    }
    return element;
  }

  public static Attribute unmarshalFrom(IntElement element) {
    AttributeImpl attribute = null;
    for (Integer value : element.getValue()) {
      if (attribute == null) {
        attribute = new AttributeImpl(element.getName(), value);
      } else {
        attribute.addValue(value);
      }
    }
    return attribute;
  }

  @Override
  public IntElement marshal(Attribute attribute) {
    return marshalFrom(attribute);
  }

  @Override
  public Attribute unmarshal(IntElement element) {
    return unmarshalFrom(element);
  }
}
