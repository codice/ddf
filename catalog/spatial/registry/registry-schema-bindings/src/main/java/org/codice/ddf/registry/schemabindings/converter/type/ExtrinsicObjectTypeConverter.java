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
import static org.codice.ddf.registry.schemabindings.converter.web.ExtrinsicObjectWebConverter.CONTENT_VERSION_INFO;
import static org.codice.ddf.registry.schemabindings.converter.web.ExtrinsicObjectWebConverter.IS_OPAQUE;
import static org.codice.ddf.registry.schemabindings.converter.web.ExtrinsicObjectWebConverter.MIME_TYPE;

import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections.MapUtils;
import org.codice.ddf.registry.schemabindings.helper.MapToSchemaElement;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExtrinsicObjectType;

public class ExtrinsicObjectTypeConverter
        extends AbstractRegistryObjectTypeConverter<ExtrinsicObjectType> {

    public ExtrinsicObjectTypeConverter(
            MapToSchemaElement<ExtrinsicObjectType> mapToSchemaElement) {
        super(mapToSchemaElement);
    }

    public ExtrinsicObjectTypeConverter() {
        this(new MapToSchemaElement<>(RIM_FACTORY::createExtrinsicObjectType));

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

        optionalExtrinsicObject = super.convert(map);

        optionalExtrinsicObject = mapToSchemaElement.populateVersionInfoTypeElement(map,
                CONTENT_VERSION_INFO,
                optionalExtrinsicObject,
                (versionInfo, extrinsicObject) -> extrinsicObject.setContentVersionInfo(versionInfo));

        optionalExtrinsicObject = mapToSchemaElement.populateBooleanElement(map,
                IS_OPAQUE,
                optionalExtrinsicObject,
                (boolToPopulate, extrinsicObject) -> extrinsicObject.setIsOpaque(boolToPopulate));

        optionalExtrinsicObject = mapToSchemaElement.populateStringElement(map,
                MIME_TYPE,
                optionalExtrinsicObject,
                (valueToPopulate, extrinsicObject) -> extrinsicObject.setMimeType(valueToPopulate));

        return optionalExtrinsicObject;
    }
}
