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

import org.codice.ddf.registry.schemabindings.helper.WebMapHelper;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.SpecificationLinkType;

public class SpecificationLinkWebConverter extends RegistryObjectWebConverter {
    // SpecificationLink converters constants
    public static final String SERVICE_BINDING = "serviceBinding";

    public static final String SPECIFICATION_OBJECT = "specificationObject";

    public static final String USAGE_DESCRIPTION = "UsageDescription";

    public static final String USAGE_PARAMETERS = "UsageParameters";

    private WebMapHelper webMapHelper = new WebMapHelper();

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

        webMapHelper.putAllIfNotEmpty(specificationLinkMap,
                super.convertRegistryObject(specificationLink));
        webMapHelper.putIfNotEmpty(specificationLinkMap,
                SERVICE_BINDING,
                specificationLink.getServiceBinding());
        webMapHelper.putIfNotEmpty(specificationLinkMap,
                SPECIFICATION_OBJECT,
                specificationLink.getSpecificationObject());
        webMapHelper.putIfNotEmpty(specificationLinkMap,
                USAGE_DESCRIPTION,
                specificationLink.getUsageDescription());
        webMapHelper.putIfNotEmpty(specificationLinkMap,
                USAGE_PARAMETERS,
                specificationLink.getUsageParameter());

        return specificationLinkMap;
    }
}
