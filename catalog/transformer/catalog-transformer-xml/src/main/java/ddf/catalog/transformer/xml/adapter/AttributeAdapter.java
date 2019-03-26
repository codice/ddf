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
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transformer.xml.binding.AbstractAttributeType;
import ddf.catalog.transformer.xml.binding.Base64BinaryElement;
import ddf.catalog.transformer.xml.binding.BooleanElement;
import ddf.catalog.transformer.xml.binding.DateTimeElement;
import ddf.catalog.transformer.xml.binding.DoubleElement;
import ddf.catalog.transformer.xml.binding.FloatElement;
import ddf.catalog.transformer.xml.binding.GeometryElement;
import ddf.catalog.transformer.xml.binding.IntElement;
import ddf.catalog.transformer.xml.binding.LongElement;
import ddf.catalog.transformer.xml.binding.ObjectElement;
import ddf.catalog.transformer.xml.binding.ShortElement;
import ddf.catalog.transformer.xml.binding.StringElement;
import ddf.catalog.transformer.xml.binding.StringxmlElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.transform.TransformerException;
import org.jvnet.ogc.gml.v_3_1_1.jts.ConversionFailedException;

public class AttributeAdapter extends XmlAdapter<AbstractAttributeType, Attribute> {

  private MetacardType metacardType = null;

  public AttributeAdapter() {
    this(MetacardImpl.BASIC_METACARD);
  }

  public AttributeAdapter(MetacardType metacardType) {
    super();
    this.metacardType = metacardType;
  }

  @Override
  public AbstractAttributeType marshal(Attribute attribute) throws CatalogTransformerException {

    if (attribute == null) {
      return null;
    }

    AbstractAttributeType element = null;

    AttributeDescriptor descriptor = metacardType.getAttributeDescriptor(attribute.getName());
    if (descriptor == null
        || Core.ID.equals(descriptor.getName())
        || descriptor.getType() == null) {
      return null;
    }

    element = convertAttribute(attribute, descriptor.getType().getAttributeFormat());
    return element;
  }

  private AbstractAttributeType convertAttribute(Attribute attribute, AttributeFormat format)
      throws CatalogTransformerException {
    AbstractAttributeType element = null;

    try {
      switch (format) {
        case BINARY:
          element = Base64BinaryAdapter.marshalFrom(attribute);
          break;
        case BOOLEAN:
          element = BooleanAdapter.marshalFrom(attribute);
          break;
        case DATE:
          element = DateTimeAdapter.marshalFrom(attribute);
          break;
        case DOUBLE:
          element = DoubleAdapter.marshalFrom(attribute);
          break;
        case FLOAT:
          element = FloatAdapter.marshalFrom(attribute);
          break;
        case GEOMETRY:
          element = GeometryAdapter.marshalFrom(attribute);
          break;
        case INTEGER:
          element = IntAdapter.marshalFrom(attribute);
          break;
        case LONG:
          element = LongAdapter.marshalFrom(attribute);
          break;
        case OBJECT:
          element = ObjectAdapter.marshalFrom(attribute);
          break;
        case SHORT:
          element = ShortAdapter.marshalFrom(attribute);
          break;
        case STRING:
          element = StringAdapter.marshalFrom(attribute);
          break;
        case XML:
          element = StringxmlAdapter.marshalFrom(attribute);
          break;
        default:
          break;
      }
    } catch (/* Required by JAXB */ Exception e) {
      throw new CatalogTransformerException("Could not marshall Attribute", e);
    }
    return element;
  }

  @Override
  public Attribute unmarshal(AbstractAttributeType element)
      throws CatalogTransformerException, TransformerException, JAXBException,
          ConversionFailedException {

    Attribute attribute = null;

    if (element instanceof Base64BinaryElement) {
      attribute = Base64BinaryAdapter.unmarshalFrom((Base64BinaryElement) element);
    } else if (element instanceof BooleanElement) {
      attribute = BooleanAdapter.unmarshalFrom((BooleanElement) element);
    } else if (element instanceof DateTimeElement) {
      attribute = DateTimeAdapter.unmarshalFrom((DateTimeElement) element);
    } else if (element instanceof DoubleElement) {
      attribute = DoubleAdapter.unmarshalFrom((DoubleElement) element);
    } else if (element instanceof FloatElement) {
      attribute = FloatAdapter.unmarshalFrom((FloatElement) element);
    } else if (element instanceof GeometryElement) {
      attribute = GeometryAdapter.unmarshalFrom((GeometryElement) element);
    } else if (element instanceof IntElement) {
      attribute = IntAdapter.unmarshalFrom((IntElement) element);
    } else if (element instanceof LongElement) {
      attribute = LongAdapter.unmarshalFrom((LongElement) element);
    } else if (element instanceof ObjectElement) {
      attribute = ObjectAdapter.unmarshalFrom((ObjectElement) element);
    } else if (element instanceof ShortElement) {
      attribute = ShortAdapter.unmarshalFrom((ShortElement) element);
    } else if (element instanceof StringElement) {
      attribute = StringAdapter.unmarshalFrom((StringElement) element);
    } else if (element instanceof StringxmlElement) {
      attribute = StringxmlAdapter.unmarshalFrom((StringxmlElement) element);
    }
    return attribute;
  }
}
