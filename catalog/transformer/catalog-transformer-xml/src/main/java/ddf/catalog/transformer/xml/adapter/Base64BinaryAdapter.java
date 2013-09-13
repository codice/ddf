/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.catalog.transformer.xml.adapter;

import java.io.Serializable;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeImpl;
import ddf.catalog.transformer.xml.binding.Base64BinaryElement;

public class Base64BinaryAdapter extends XmlAdapter<Base64BinaryElement, Attribute> {

    @Override
    public Base64BinaryElement marshal(Attribute attribute) {
        return marshalFrom(attribute);
    }

    public static Base64BinaryElement marshalFrom(Attribute attribute) {
        Base64BinaryElement element = new Base64BinaryElement();
        element.setName(attribute.getName());
        if (attribute.getValue() != null) {
            element.getValue().addAll(
                    new AdaptedList<Serializable, byte[]>(attribute.getValues(), byte[].class));
        }
        return element;
    }

    @Override
    public Attribute unmarshal(Base64BinaryElement element) {
        return unmarshalFrom(element);
    }

    public static Attribute unmarshalFrom(Base64BinaryElement element) {
        AttributeImpl attribute = null;
        for (byte[] value : element.getValue()) {
            if (attribute == null) {
                attribute = new AttributeImpl(element.getName(), value);
            } else {
                attribute.addValue(value);
            }
        }
        return attribute;
    }

}
