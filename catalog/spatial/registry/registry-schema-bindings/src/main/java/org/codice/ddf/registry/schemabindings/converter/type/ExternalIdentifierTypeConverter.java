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

import static org.codice.ddf.registry.schemabindings.EbrimConstants.IDENTIFICATION_SCHEME;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.REGISTRY_OBJECT;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.RIM_FACTORY;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.VALUE;

import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExternalIdentifierType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectType;

public class ExternalIdentifierTypeConverter extends RegistryObjectTypeConverter {

    @Override
    protected RegistryObjectType createObjectInstance() {
        return RIM_FACTORY.createExternalIdentifierType();
    }

    /**
     * This method creates an ExternalIdentifierType from the values in the provided map.
     * The following keys are expected in the provided map (Taken from EbrimConstants):
     * <p>
     * IDENTIFICATION_SCHEME = "identificationScheme";
     * REGISTRY_OBJECT = "registryObject";
     * VALUE = "value";
     * <p>
     * <p>
     * This will also try to populate the RegistryObjectType values also looked for in the map.
     *
     * @param map the Map representation of the ExternalIdentifierType to generate, null returns empty Optional
     * @return Optional ExternalIdentifierType created from the values in the map
     */
    public Optional<ExternalIdentifierType> convert(Map<String, Object> map) {
        Optional<ExternalIdentifierType> optionalExternalIdentifier = Optional.empty();
        if (MapUtils.isEmpty(map)) {
            return optionalExternalIdentifier;
        }

        Optional<RegistryObjectType> optionalRot = super.convertRegistryObject(map);
        if (optionalRot.isPresent()) {
            optionalExternalIdentifier = Optional.of((ExternalIdentifierType) optionalRot.get());
        }

        String valueToPopulate = MapUtils.getString(map, IDENTIFICATION_SCHEME);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            if (!optionalExternalIdentifier.isPresent()) {
                optionalExternalIdentifier =
                        Optional.of(RIM_FACTORY.createExternalIdentifierType());
            }
            optionalExternalIdentifier.get()
                    .setIdentificationScheme(valueToPopulate);
        }

        valueToPopulate = MapUtils.getString(map, REGISTRY_OBJECT);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            if (!optionalExternalIdentifier.isPresent()) {
                optionalExternalIdentifier =
                        Optional.of(RIM_FACTORY.createExternalIdentifierType());
            }
            optionalExternalIdentifier.get()
                    .setRegistryObject(valueToPopulate);
        }

        valueToPopulate = MapUtils.getString(map, VALUE);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            if (!optionalExternalIdentifier.isPresent()) {
                optionalExternalIdentifier =
                        Optional.of(RIM_FACTORY.createExternalIdentifierType());
            }
            optionalExternalIdentifier.get()
                    .setValue(valueToPopulate);
        }

        return optionalExternalIdentifier;
    }
}
