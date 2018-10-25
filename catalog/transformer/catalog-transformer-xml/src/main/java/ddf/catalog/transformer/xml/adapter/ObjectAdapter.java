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
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transformer.xml.binding.ObjectElement;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObjectAdapter extends XmlAdapter<ObjectElement, Attribute> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ObjectAdapter.class);

  public static ObjectElement marshalFrom(Attribute attribute) throws CatalogTransformerException {

    ObjectElement element = new ObjectElement();
    element.setName(attribute.getName());
    if (attribute.getValue() != null) {
      for (Serializable value : attribute.getValues()) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos;
        try {
          oos = new ObjectOutputStream(baos);
          oos.writeObject(value);
          oos.close();
        } catch (IOException e) {
          throw new CatalogTransformerException(
              "Could not transform Metacard to XML.  Could not serialize Attribute Object value.",
              e);
        }
        element.getValue().add(baos.toByteArray());
      }
    }
    return element;
  }

  public static Attribute unmarshalFrom(ObjectElement element) {
    AttributeImpl attribute = null;
    String elementName = element.getName();

    for (byte[] value : element.getValue()) {
      Serializable unmarshalledValue = value;
      try (ObjectInputStream objectInputStream =
          new ObjectInputStream(new ByteArrayInputStream(value))) {
        unmarshalledValue = (Serializable) objectInputStream.readObject();
      } catch (IOException | ClassNotFoundException | RuntimeException e) {
        LOGGER.debug("Could not unmarshal Metacard object for attribute: {}", elementName, e);
      }

      if (attribute == null) {
        attribute = new AttributeImpl(elementName, unmarshalledValue);
      } else {
        attribute.addValue(unmarshalledValue);
      }
    }
    return attribute;
  }

  @Override
  public ObjectElement marshal(Attribute attribute) throws CatalogTransformerException {
    return marshalFrom(attribute);
  }

  @Override
  public Attribute unmarshal(ObjectElement element) {
    return unmarshalFrom(element);
  }
}
