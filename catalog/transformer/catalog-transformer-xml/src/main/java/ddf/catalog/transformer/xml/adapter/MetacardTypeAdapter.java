/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.transformer.xml.adapter;

import static ddf.catalog.data.impl.BasicTypes.BASIC_METACARD;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.MetacardType;
import ddf.catalog.data.MetacardTypeRegistry;
import ddf.catalog.transform.CatalogTransformerException;

public class MetacardTypeAdapter extends XmlAdapter<String, MetacardType> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetacardTypeAdapter.class);

    private final MetacardTypeRegistry registry;

    public MetacardTypeAdapter(MetacardTypeRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String marshal(MetacardType type) throws CatalogTransformerException {
        if (type == null) {
            throw new CatalogTransformerException(
                    "Could not transform XML into Metacard.  Invalid MetacardType");
        }
        return type.getName();
    }

    private MetacardType lookupMetacardType(String metacardTypeName)
            throws CatalogTransformerException {
        return registry.lookup(metacardTypeName)
                .orElseThrow(() -> new CatalogTransformerException(
                        "MetacardType [" + metacardTypeName
                                + "] has not been registered with the system. Cannot parse input."));
    }

    @Override
    public MetacardType unmarshal(String typeName) throws CatalogTransformerException {
        LOGGER.debug("typeName: '{}'", typeName);

        if (StringUtils.isEmpty(typeName)) {
            LOGGER.debug(
                    "MetacardType specified in input is null or empty. Assuming default MetacardType.");
            return lookupMetacardType(BASIC_METACARD.getName());
        }

        return lookupMetacardType(typeName);
    }
}
