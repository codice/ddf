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

import oasis.names.tc.ebxml_regrep.xsd.rim._3.AssociationType1;

public class AssociationWebConverter extends RegistryObjectWebConverter {
    public static final String ASSOCIATION_TYPE = "associationType";

    public static final String SOURCE_OBJECT = "sourceObject";

    public static final String TARGET_OBJECT = "targetObject";

    private WebMapHelper webMapHelper = new WebMapHelper();

    /**
     * This method creates a Map<String, Object> representation of the AssociationType1 provided.
     * The following keys will be added to the map (Taken from EbrimConstants):
     * <p>
     * ASSOCIATION_TYPE = "associationType";
     * SOURCE_OBJECT = "sourceObject";
     * TARGET_OBJECT = "targetObject";
     * <p>
     * This will also try to parse RegistryObjectType values to the map.
     *
     * @param association the AssociationType1 to be converted into a map, null returns empty Map
     * @return Map<String, Object> representation of the AssociationType1 provided
     */
    public Map<String, Object> convert(AssociationType1 association) {
        Map<String, Object> associationMap = new HashMap<>();
        if (association == null) {
            return associationMap;
        }

        webMapHelper.putAllIfNotEmpty(associationMap, super.convertRegistryObject(association));
        webMapHelper.putIfNotEmpty(associationMap,
                ASSOCIATION_TYPE,
                association.getAssociationType());
        webMapHelper.putIfNotEmpty(associationMap, SOURCE_OBJECT, association.getSourceObject());
        webMapHelper.putIfNotEmpty(associationMap, TARGET_OBJECT, association.getTargetObject());

        return associationMap;
    }
}
