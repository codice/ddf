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
package org.codice.ddf.registry.schemabindings.converter.type;

import static org.codice.ddf.registry.schemabindings.EbrimConstants.ASSOCIATION_TYPE;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.RIM_FACTORY;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.SOURCE_OBJECT;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.TARGET_OBJECT;

import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.AssociationType1;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectType;

public class AssociationTypeConverter extends RegistryObjectTypeConverter {

    @Override
    protected RegistryObjectType createObjectInstance() {
        return RIM_FACTORY.createAssociationType1();
    }

    /**
     * This method creates an AssociationType1 from the values in the provided map.
     * The following keys are expected in the provided map (Taken from EbrimConstants):
     * <p>
     * ASSOCIATION_TYPE = "associationType";
     * SOURCE_OBJECT = "sourceObject";
     * TARGET_OBJECT = "targetObject";
     * <p>
     * This will also try to populate the RegistryObjectType values also looked for in the map.
     *
     * @param map the Map representation of the AssociationType1 to generate, null returns empty Optional
     * @return Optional AssociationType1 created from the values in the map
     */
    public Optional<AssociationType1> convert(Map<String, Object> map) {
        Optional<AssociationType1> optionalAssociation = Optional.empty();

        if (MapUtils.isEmpty(map)) {
            return optionalAssociation;
        }
        Optional<RegistryObjectType> optionalRegistryObject = super.convertRegistryObject(map);

        if (optionalRegistryObject.isPresent()) {
            optionalAssociation = Optional.of((AssociationType1) optionalRegistryObject.get());
        }

        String valueToPopulate = MapUtils.getString(map, ASSOCIATION_TYPE);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            if (!optionalAssociation.isPresent()) {
                optionalAssociation = Optional.of(RIM_FACTORY.createAssociationType1());
            }

            optionalAssociation.get()
                    .setAssociationType(valueToPopulate);
        }

        valueToPopulate = MapUtils.getString(map, SOURCE_OBJECT);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            if (!optionalAssociation.isPresent()) {
                optionalAssociation = Optional.of(RIM_FACTORY.createAssociationType1());
            }
            optionalAssociation.get()
                    .setSourceObject(valueToPopulate);
        }

        valueToPopulate = MapUtils.getString(map, TARGET_OBJECT);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            if (!optionalAssociation.isPresent()) {
                optionalAssociation = Optional.of(RIM_FACTORY.createAssociationType1());
            }
            optionalAssociation.get()
                    .setTargetObject(valueToPopulate);
        }

        return optionalAssociation;
    }
}
