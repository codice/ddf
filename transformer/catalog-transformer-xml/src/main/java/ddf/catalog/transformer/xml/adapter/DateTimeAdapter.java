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
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.log4j.Logger;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeImpl;
import ddf.catalog.transformer.xml.binding.DateTimeElement;

public class DateTimeAdapter extends XmlAdapter<DateTimeElement, Attribute> {

	private static final Logger LOGGER = Logger
			.getLogger(DateTimeAdapter.class);

	@Override
	public DateTimeElement marshal(Attribute attribute) {
		return marshalFrom(attribute);
	}

	public static DateTimeElement marshalFrom(Attribute attribute) {

		DateTimeElement element = new DateTimeElement();
		element.setName(attribute.getName());
		if (attribute.getValue() != null) {
			for (Serializable value : attribute.getValues()) {
				if (!(value instanceof Date)) {
					continue;
				}
				Date date = (Date) value;
				GregorianCalendar cal = new GregorianCalendar();
				cal.setTime(date);
				try {
					((DateTimeElement) element).getValue().add(
							DatatypeFactory.newInstance()
									.newXMLGregorianCalendar(cal));
				} catch (DatatypeConfigurationException e) {
					LOGGER.debug(
							"Could not parse Metacard Attribute. XML Date could not be generated.",
							e);
				}
			}
		}
		return element;
	}

	@Override
	public Attribute unmarshal(DateTimeElement element) {
		return unmarshalFrom(element);
	}

	public static Attribute unmarshalFrom(DateTimeElement element) {
		AttributeImpl attribute = null;
		for (XMLGregorianCalendar xcal : element.getValue()) {
			Date date = xcal.toGregorianCalendar().getTime();
			if (attribute == null) {
				attribute = new AttributeImpl(element.getName(), date);
			} else {
				attribute.addValue(date);
			}
		}
		return attribute;
	}

}
