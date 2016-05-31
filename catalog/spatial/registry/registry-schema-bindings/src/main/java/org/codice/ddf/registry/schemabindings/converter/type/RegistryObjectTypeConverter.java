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

import java.util.Map;
import java.util.Optional;

import org.codice.ddf.registry.schemabindings.helper.MapToSchemaElement;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectType;

public class RegistryObjectTypeConverter
        extends AbstractRegistryObjectTypeConverter<RegistryObjectType> {

    public RegistryObjectTypeConverter(MapToSchemaElement<RegistryObjectType> mapToSchemaElement) {
        super(mapToSchemaElement);
    }

    public RegistryObjectTypeConverter() {
        this(new MapToSchemaElement<>(RIM_FACTORY::createRegistryObjectType));

    }

    @Override
    public Optional<RegistryObjectType> convert(Map<String, Object> map) {
        return super.convert(map);
    }
}
