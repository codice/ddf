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

import static org.codice.ddf.registry.schemabindings.EbrimConstants.ACCESS_URI;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.RIM_FACTORY;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.SERVICE;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.SPECIFICATION_LINK_KEY;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.TARGET_BINDING;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ServiceBindingType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.SpecificationLinkType;

public class ServiceBindingTypeConverter extends RegistryObjectTypeConverter {

    @Override
    protected RegistryObjectType createObjectInstance() {
        return RIM_FACTORY.createServiceBindingType();
    }

    /**
     * This method creates an ServiceBindingType from the values in the provided map.
     * The following keys are expected in the provided map (Taken from EbrimConstants):
     * <p>
     * ACCESS_URI = "accessUri";
     * SERVICE = "service";
     * TARGET_BINDING = "targetBinding";
     * SPECIFICATION_LINK_KEY = "SpecificationLink";
     * <p>
     * This will also try to populate the RegistryObjectType values also looked for in the map.
     * <p>
     * Uses:
     * SpecificationLinkTypeConverter
     *
     * @param map the Map representation of the ServiceBindingType to generate, null returns empty Optional
     * @return Optional ServiceBindingType created from the values in the map
     */
    public Optional<ServiceBindingType> convert(Map<String, Object> map) {
        Optional<ServiceBindingType> optionalBinding = Optional.empty();
        if (MapUtils.isEmpty(map)) {
            return optionalBinding;
        }

        Optional<RegistryObjectType> optionalRegistryObject = super.convertRegistryObject(map);
        if (optionalRegistryObject.isPresent()) {
            optionalBinding = Optional.of((ServiceBindingType) optionalRegistryObject.get());
        }

        String valueToPopulate = MapUtils.getString(map, ACCESS_URI);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            if (!optionalBinding.isPresent()) {
                optionalBinding = Optional.of(RIM_FACTORY.createServiceBindingType());
            }
            optionalBinding.get()
                    .setAccessURI(valueToPopulate);
        }

        valueToPopulate = MapUtils.getString(map, SERVICE);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            if (!optionalBinding.isPresent()) {
                optionalBinding = Optional.of(RIM_FACTORY.createServiceBindingType());
            }
            optionalBinding.get()
                    .setService(valueToPopulate);
        }

        if (map.containsKey(SPECIFICATION_LINK_KEY)) {
            SpecificationLinkTypeConverter slConverter = new SpecificationLinkTypeConverter();
            Optional<SpecificationLinkType> optionalSpec;

            for (Map<String, Object> specMap : (List<Map<String, Object>>) map.get(
                    SPECIFICATION_LINK_KEY)) {
                optionalSpec = slConverter.convert(specMap);

                if (optionalBinding.isPresent()) {
                    if (!optionalBinding.isPresent()) {
                        optionalBinding = Optional.of(RIM_FACTORY.createServiceBindingType());
                    }

                    optionalBinding.get()
                            .getSpecificationLink()
                            .add(optionalSpec.get());
                }
            }
        }

        valueToPopulate = MapUtils.getString(map, TARGET_BINDING);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            if (!optionalBinding.isPresent()) {
                optionalBinding = Optional.of(RIM_FACTORY.createServiceBindingType());
            }
            optionalBinding.get()
                    .setTargetBinding(valueToPopulate);
        }

        return optionalBinding;
    }
}
