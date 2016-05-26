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
import static org.codice.ddf.registry.schemabindings.EbrimConstants.SERVICE_BINDING_KEY;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections.MapUtils;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ServiceBindingType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ServiceType;

public class ServiceTypeConverter extends RegistryObjectTypeConverter {

    @Override
    protected RegistryObjectType createObjectInstance() {
        return RIM_FACTORY.createServiceType();
    }

    /**
     * This method creates an ServiceType from the values in the provided map.
     * The following keys are expected in the provided map (Taken from EbrimConstants):
     * <p>
     * SERVICE_BINDING_KEY = "ServiceBinding";
     * <p>
     * This will also try to populate the RegistryObjectType values also looked for in the map.
     * <p>
     * Uses:
     * ServiceBindingTypeConverter
     *
     * @param map the Map representation of the ServiceType to generate, null returns empty Optional
     * @return Optional ServiceType created from the values in the map
     */
    public Optional<ServiceType> convert(Map<String, Object> map) {
        Optional<ServiceType> optionalService = Optional.empty();
        if (MapUtils.isEmpty(map)) {
            return optionalService;
        }

        Optional<RegistryObjectType> optionalRegistryObject = super.convertRegistryObject(map);
        if (optionalRegistryObject.isPresent()) {
            optionalService = Optional.of((ServiceType) optionalRegistryObject.get());
        }

        if (map.containsKey(SERVICE_BINDING_KEY)) {
            ServiceBindingTypeConverter bindingConverter = new ServiceBindingTypeConverter();
            Optional<ServiceBindingType> optionalBinding;
            for (Map<String, Object> bindingMap : (List<Map<String, Object>>) map.get(
                    SERVICE_BINDING_KEY)) {
                optionalBinding = bindingConverter.convert(bindingMap);

                if (optionalBinding.isPresent()) {
                    if (!optionalService.isPresent()) {
                        optionalService = Optional.of(RIM_FACTORY.createServiceType());
                    }

                    optionalService.get()
                            .getServiceBinding()
                            .add(optionalBinding.get());
                }
            }
        }

        return optionalService;
    }
}
