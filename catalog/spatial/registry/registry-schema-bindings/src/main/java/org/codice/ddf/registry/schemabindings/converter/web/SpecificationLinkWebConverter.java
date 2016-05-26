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

import static org.codice.ddf.registry.schemabindings.EbrimConstants.SERVICE_BINDING;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.SPECIFICATION_OBJECT;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.USAGE_DESCRIPTION;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.USAGE_PARAMETERS;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.MapUtils;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.SpecificationLinkType;

public class SpecificationLinkWebConverter extends RegistryObjectWebConverter {

    /**
     * This method creates a Map<String, Object> representation of the SpecificationLinkType provided.
     * The following keys will be added to the map (Taken from EbrimConstants):
     * <p>
     * SERVICE_BINDING = "serviceBinding";
     * SPECIFICATION_OBJECT = "specificationObject";
     * USAGE_DESCRIPTION = "UsageDescription";
     * USAGE_PARAMETERS = "UsageParameters";
     * <p>
     * This will also try to parse RegistryObjectType values to the map.
     * <p>
     * Uses:
     * InternationalStringTypeHelper
     *
     * @param specificationLink the SpecificationLinkType to be converted into a map, null returns empty Map
     * @return Map<String, Object> representation of the SpecificationLinkType provided
     */
    public Map<String, Object> convert(SpecificationLinkType specificationLink) {
        Map<String, Object> specificationLinkMap = new HashMap<>();
        if (specificationLink == null) {
            return specificationLinkMap;
        }

        Map<String, Object> registryObjectMap = super.convertRegistryObject(specificationLink);
        if (MapUtils.isNotEmpty(registryObjectMap)) {
            specificationLinkMap.putAll(registryObjectMap);
        }

        if (specificationLink.isSetServiceBinding()) {
            specificationLinkMap.put(SERVICE_BINDING, specificationLink.getServiceBinding());
        }

        if (specificationLink.isSetSpecificationObject()) {
            specificationLinkMap.put(SPECIFICATION_OBJECT,
                    specificationLink.getSpecificationObject());

        }

        if (specificationLink.isSetUsageDescription()) {
            specificationLinkMap.put(USAGE_DESCRIPTION,
                    INTERNATIONAL_STRING_TYPE_HELPER.getString(specificationLink.getUsageDescription()));
        }

        if (specificationLink.isSetUsageParameter()) {
            specificationLinkMap.put(USAGE_PARAMETERS, specificationLink.getUsageParameter());
        }

        return specificationLinkMap;
    }
}
