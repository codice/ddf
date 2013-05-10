/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.catalog.transformer.xml.adapter;

import java.io.Serializable;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeImpl;
import ddf.catalog.transformer.xml.binding.ShortElement;

public class ShortAdapter extends XmlAdapter<ShortElement, Attribute> {

	@Override
	public ShortElement marshal(Attribute attribute) {
		return marshalFrom(attribute);
	}

	public static ShortElement marshalFrom(Attribute attribute) {

		ShortElement element = new ShortElement();
		element.setName(attribute.getName());
		if (attribute.getValue() != null) {
			element.getValue().addAll(
					new AdaptedList<Serializable, Short>(attribute.getValues(),
							Short.class));
		}
		return element;
	}

	@Override
	public Attribute unmarshal(ShortElement element) {
		return unmarshalFrom(element);
	}

	public static Attribute unmarshalFrom(ShortElement element) {
		AttributeImpl attribute = null;
		for (Short value : element.getValue()) {
			if (attribute == null) {
				attribute = new AttributeImpl(element.getName(), value);
			} else {
				attribute.addValue(value);
			}
		}
		return attribute;
	}

}
