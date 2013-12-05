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
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.transformer.xml.binding.StringElement;

public class StringAdapter extends XmlAdapter<StringElement, Attribute> {

    @Override
    public StringElement marshal(Attribute attribute) {
        return marshalFrom(attribute);
    }

    public static StringElement marshalFrom(Attribute attribute) {

        StringElement element = new StringElement();
        element.setName(attribute.getName());
        if (attribute.getValue() != null) {
            element.getValue().addAll(
                    new AdaptedList<Serializable, String>(attribute.getValues(), String.class));
        }
        return element;
    }

    @Override
    public Attribute unmarshal(StringElement element) {
        return unmarshalFrom(element);
    }

    public static Attribute unmarshalFrom(StringElement element) {
        AttributeImpl attribute = null;
        for (String value : element.getValue()) {
            if (attribute == null) {
                attribute = new AttributeImpl(element.getName(), value);
            } else {
                attribute.addValue(value);
            }
        }
        return attribute;
    }

}
