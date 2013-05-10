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

import java.util.List;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import ddf.catalog.data.BasicTypes;
import ddf.catalog.data.MetacardType;
import ddf.catalog.transform.CatalogTransformerException;

public class MetacardTypeAdapter extends XmlAdapter<String, MetacardType> {

	private List<MetacardType> types;

	public MetacardTypeAdapter(List<MetacardType> types) {
		this.types = types;
	}

	public MetacardTypeAdapter() {
		this(null);
	}

	@Override
	public String marshal(MetacardType type) throws CatalogTransformerException {
		if (type == null) {
			throw new CatalogTransformerException(
					"Could not transform XML into Metacard.  Invalid MetacardType");
		}
		return type.getName();
	}

	@Override
	public MetacardType unmarshal(String typeName)
			throws CatalogTransformerException {

		if (typeName == null
				|| typeName.equals(BasicTypes.BASIC_METACARD.getName())) {
			return BasicTypes.BASIC_METACARD;
		}

		if (types != null && types.size() > 0) {
			for (MetacardType type : types) {
				if (typeName.equals(type.getName())) {
					return type;
				}
			}
		}

		throw new CatalogTransformerException(
				"Could not transform XML into Metacard.  Metacard Type '"
						+ typeName + "' is not registered.");
	}

}