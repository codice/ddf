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

import static org.codice.ddf.registry.schemabindings.EbrimConstants.RIM_FACTORY;
import static org.codice.ddf.registry.schemabindings.converter.web.ExternalIdentifierWebConverter.IDENTIFICATION_SCHEME;
import static org.codice.ddf.registry.schemabindings.converter.web.ExternalIdentifierWebConverter.REGISTRY_OBJECT;
import static org.codice.ddf.registry.schemabindings.converter.web.ExternalIdentifierWebConverter.VALUE;

import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections.MapUtils;
import org.codice.ddf.registry.schemabindings.helper.MapToSchemaElement;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExternalIdentifierType;

public class ExternalIdentifierTypeConverter
        extends AbstractRegistryObjectTypeConverter<ExternalIdentifierType> {

    public ExternalIdentifierTypeConverter(
            MapToSchemaElement<ExternalIdentifierType> mapToSchemaElement) {
        super(mapToSchemaElement);
    }

    public ExternalIdentifierTypeConverter() {
        this(new MapToSchemaElement<>(RIM_FACTORY::createExternalIdentifierType));
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

        optionalExternalIdentifier = super.convert(map);

        optionalExternalIdentifier = mapToSchemaElement.populateStringElement(map,
                IDENTIFICATION_SCHEME,
                optionalExternalIdentifier,
                (valueToPopulate, externalIdentifier) -> externalIdentifier.setIdentificationScheme(
                        valueToPopulate));

        optionalExternalIdentifier = mapToSchemaElement.populateStringElement(map,
                REGISTRY_OBJECT,
                optionalExternalIdentifier,
                (valueToPopulate, externalIdentifier) -> externalIdentifier.setRegistryObject(
                        valueToPopulate));

        optionalExternalIdentifier = mapToSchemaElement.populateStringElement(map,
                VALUE,
                optionalExternalIdentifier,
                (valueToPopulate, externalIdentifier) -> externalIdentifier.setValue(valueToPopulate));

        return optionalExternalIdentifier;
    }
}
