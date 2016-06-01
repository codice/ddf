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
import static org.codice.ddf.registry.schemabindings.converter.web.RegistryPackageWebConverter.REGISTRY_OBJECT_LIST_KEY;

import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections.MapUtils;
import org.codice.ddf.registry.schemabindings.helper.MapToSchemaElement;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectListType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;

public class RegistryPackageTypeConverter
        extends AbstractRegistryObjectTypeConverter<RegistryPackageType> {

    public RegistryPackageTypeConverter(MapToSchemaElement<RegistryPackageType> mapToSchemaElement) {
        super(mapToSchemaElement);
    }

    public RegistryPackageTypeConverter() {
        this(new MapToSchemaElement<>(RIM_FACTORY::createRegistryPackageType));

    }

    /**
     * This method creates an RegistryPackageType from the values in the provided map.
     * The following keys are expected in the provided map (Taken from EbrimConstants):
     * <p>
     * REGISTRY_OBJECT_LIST_KEY = "RegistryObjectList";
     * <p>
     * This will also try to populate the RegistryObjectType values also looked for in the map.
     * <p>
     * Uses:
     * RegistryObjectListTypeConverter
     *
     * @param map the Map representation of the RegistryPackageType to generate, null returns empty Optional
     * @return Optional RegistryPackageType created from the values in the map
     */
    public Optional<RegistryPackageType> convert(Map<String, Object> map) {
        Optional<RegistryPackageType> optionalRegistryPackage = Optional.empty();
        if (MapUtils.isEmpty(map)) {
            return optionalRegistryPackage;
        }

        optionalRegistryPackage = super.convert(map);

        if (map.containsKey(REGISTRY_OBJECT_LIST_KEY)) {
            RegistryObjectListTypeConverter registryObjectListConverter =
                    new RegistryObjectListTypeConverter();
            Optional<RegistryObjectListType> optionalRegistryObjectList =
                    registryObjectListConverter.convert((Map<String, Object>) map.get(
                            REGISTRY_OBJECT_LIST_KEY));
            if (optionalRegistryObjectList.isPresent()) {
                if (!optionalRegistryPackage.isPresent()) {
                    optionalRegistryPackage = Optional.of(mapToSchemaElement.getObjectFactory()
                            .get());
                }

                optionalRegistryPackage.get()
                        .setRegistryObjectList(optionalRegistryObjectList.get());
            }
        }

        return optionalRegistryPackage;
    }
}
