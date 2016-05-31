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
package org.codice.ddf.registry.schemabindings.converter.web;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.MapUtils;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExternalIdentifierType;

public class ExternalIdentifierWebConverter extends RegistryObjectWebConverter {
    public static final String IDENTIFICATION_SCHEME = "identificationScheme";

    public static final String REGISTRY_OBJECT = "registryObject";

    public static final String VALUE = "value";

    /**
     * This method creates a Map<String, Object> representation of the ExternalIdentifierType provided.
     * The following keys will be added to the map (Taken from EbrimConstants):
     * <p>
     * IDENTIFICATION_SCHEME = "identificationScheme";
     * REGISTRY_OBJECT = "registryObject";
     * VALUE = "value";
     * <p>
     * This will also try to parse RegistryObjectType values to the map.
     *
     * @param externalIdentifier the ExternalIdentifierType to be converted into a map, null returns empty Map
     * @return Map<String, Object> representation of the ExternalIdentifierType provided
     */
    public Map<String, Object> convert(ExternalIdentifierType externalIdentifier) {
        Map<String, Object> externalIdentifierMap = new HashMap<>();
        if (externalIdentifier == null) {
            return externalIdentifierMap;
        }

        Map<String, Object> registryObjectMap = super.convertRegistryObject(externalIdentifier);
        if (MapUtils.isNotEmpty(registryObjectMap)) {
            externalIdentifierMap.putAll(registryObjectMap);
        }

        if (externalIdentifier.isSetIdentificationScheme()) {
            externalIdentifierMap.put(IDENTIFICATION_SCHEME,
                    externalIdentifier.getIdentificationScheme());
        }

        if (externalIdentifier.isSetRegistryObject()) {
            externalIdentifierMap.put(REGISTRY_OBJECT, externalIdentifier.getRegistryObject());
        }

        if (externalIdentifier.isSetValue()) {
            externalIdentifierMap.put(VALUE, externalIdentifier.getValue());
        }

        return externalIdentifierMap;
    }
}
