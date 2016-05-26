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

import static org.codice.ddf.registry.schemabindings.EbrimConstants.CONTENT_VERSION_INFO;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.IS_OPAQUE;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.MIME_TYPE;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.RIM_FACTORY;

import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExtrinsicObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.VersionInfoType;

public class ExtrinsicObjectTypeConverter extends RegistryObjectTypeConverter {

    @Override
    protected RegistryObjectType createObjectInstance() {
        return RIM_FACTORY.createExtrinsicObjectType();
    }

    /**
     * This method creates an ExtrinsicObjectType from the values in the provided map.
     * The following keys are expected in the provided map (Taken from EbrimConstants):
     * <p>
     * CONTENT_VERSION_INFO = "ContentVersionInfo";
     * IS_OPAQUE = "isOpaque";
     * MIME_TYPE = "mimeType";
     * <p>
     * This will also try to populate the RegistryObjectType values also looked for in the map.
     *
     * @param map the Map representation of the ExtrinsicObjectType to generate, null returns empty Optional
     * @return Optional ExtrinsicObjectType created from the values in the map
     */
    public Optional<ExtrinsicObjectType> convert(Map<String, Object> map) {
        Optional<ExtrinsicObjectType> optionalExtrinsicObject = Optional.empty();
        if (MapUtils.isEmpty(map)) {
            return optionalExtrinsicObject;
        }

        Optional<RegistryObjectType> optionalRegistryObject = super.convertRegistryObject(map);
        if (optionalRegistryObject.isPresent()) {
            optionalExtrinsicObject =
                    Optional.of((ExtrinsicObjectType) optionalRegistryObject.get());
        }

        String valueToPopulate = MapUtils.getString(map, CONTENT_VERSION_INFO);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            if (!optionalExtrinsicObject.isPresent()) {
                optionalExtrinsicObject = Optional.of(RIM_FACTORY.createExtrinsicObjectType());
            }
            VersionInfoType contentVersionInfo = RIM_FACTORY.createVersionInfoType();
            contentVersionInfo.setVersionName(valueToPopulate);

            optionalExtrinsicObject.get()
                    .setContentVersionInfo(contentVersionInfo);
        }

        valueToPopulate = MapUtils.getString(map, MIME_TYPE);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            if (!optionalExtrinsicObject.isPresent()) {
                optionalExtrinsicObject = Optional.of(RIM_FACTORY.createExtrinsicObjectType());
            }
            optionalExtrinsicObject.get()
                    .setMimeType(valueToPopulate);
        }

        Boolean booleanToPopulate = MapUtils.getBoolean(map, IS_OPAQUE);
        if (booleanToPopulate != null) {
            if (!optionalExtrinsicObject.isPresent()) {
                optionalExtrinsicObject = Optional.of(RIM_FACTORY.createExtrinsicObjectType());
            }
            optionalExtrinsicObject.get()
                    .setIsOpaque(booleanToPopulate);
        }

        return optionalExtrinsicObject;
    }
}
