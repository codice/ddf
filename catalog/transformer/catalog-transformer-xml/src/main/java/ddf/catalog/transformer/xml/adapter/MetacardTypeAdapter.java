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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.BasicTypes;
import ddf.catalog.data.MetacardType;
import ddf.catalog.transform.CatalogTransformerException;

public class MetacardTypeAdapter extends XmlAdapter<String, MetacardType> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetacardTypeAdapter.class);
    
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
    public MetacardType unmarshal(String typeName) throws CatalogTransformerException {

        LOGGER.debug("typeName: '{}'", typeName);
        LOGGER.debug("types: {}", types);

        if (StringUtils.isEmpty(typeName) || CollectionUtils.isEmpty(types)
                || typeName.equals(BasicTypes.BASIC_METACARD.getName())) {
            return BasicTypes.BASIC_METACARD;
        }

        LOGGER.debug("Searching through registerd metacard types {} for '{}'.", types, typeName);
        for (MetacardType type : types) {
            if (typeName.equals(type.getName())) {
                return type;
            }
        }

        LOGGER.debug("Metacard type '{}' is not registered.  Using metacard type of '{}'.",
                typeName, BasicTypes.BASIC_METACARD.getName());

        return BasicTypes.BASIC_METACARD;
    }
}