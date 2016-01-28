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
package ddf.catalog.util.impl;

import org.osgi.service.blueprint.container.Converter;
import org.osgi.service.blueprint.container.ReifiedType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This converter is used to allow {@link DescribableServiceMap} objects to pass through for
 * {@link java.util.Map} implementations. Without this converter, blueprint will copy the map and lose the reference.
 */
public class MapConverter implements Converter {
    private static final Logger LOGGER = LoggerFactory.getLogger(MapConverter.class);

    /**
     * @return true if sourceObject is an instance of DescribableServiceMap; false otherwise
     * @param sourceObject object considering to be converted
     * @param targetType The target type {@code T}.
     */
    @Override
    public boolean canConvert(Object sourceObject, ReifiedType targetType) {
        LOGGER.trace("Deciphering if canConvert ");

        if (targetType != null) {
            LOGGER.debug("ReifiedType: {}", targetType.getRawClass());
        }
        return (sourceObject instanceof DescribableServiceMap);
    }

    /**
     * Converts (casts) the sourceObject to a DescribableServiceMap.
     *
     * @return sourceObject cast to a DescribableServiceMap
     * @param sourceObject object being converted
     * @param targetType The target type {@code T}.
     */
    @Override
    public Object convert(Object sourceObject, ReifiedType targetType) throws Exception {

        LOGGER.trace("Converting {}", sourceObject);

        return ((DescribableServiceMap) sourceObject);
    }

}
