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

import static org.codice.ddf.registry.schemabindings.EbrimConstants.CLASSIFICATION_NODE;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.CLASSIFICATION_SCHEME;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.CLASSIFIED_OBJECT;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.NODE_REPRESENTATION;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.RIM_FACTORY;

import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.ClassificationType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectType;

public class ClassificationTypeConverter extends RegistryObjectTypeConverter {
    @Override
    protected RegistryObjectType createObjectInstance() {
        return RIM_FACTORY.createClassificationType();
    }

    /**
     * This method creates an ClassificationType from the values in the provided map.
     * The following keys are expected in the provided map (Taken from EbrimConstants):
     * <p>
     * CLASSIFICATION_NODE = "classificationNode";
     * CLASSIFIED_OBJECT = "classifiedObject";
     * CLASSIFICATION_SCHEME = "classificationScheme";
     * NODE_REPRESENTATION = "nodeRepresentation";
     * <p>
     * This will also try to populate the RegistryObjectType values also looked for in the map.
     *
     * @param map the Map representation of the ClassificationType to generate, null returns empty Optional
     * @return Optional ClassificationType created from the values in the map
     */
    public Optional<ClassificationType> convert(Map<String, Object> map) {
        Optional<ClassificationType> optionalClassification = Optional.empty();
        if (MapUtils.isEmpty(map)) {
            return optionalClassification;
        }

        Optional<RegistryObjectType> optionalRot = convertRegistryObject(map);
        if (optionalRot.isPresent()) {
            optionalClassification = Optional.of((ClassificationType) optionalRot.get());
        }

        String valueToPopulate = MapUtils.getString(map, CLASSIFICATION_NODE);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            if (!optionalClassification.isPresent()) {
                optionalClassification = Optional.of(RIM_FACTORY.createClassificationType());
            }
            optionalClassification.get()
                    .setClassificationNode(valueToPopulate);
        }

        valueToPopulate = MapUtils.getString(map, CLASSIFICATION_SCHEME);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            if (!optionalClassification.isPresent()) {
                optionalClassification = Optional.of(RIM_FACTORY.createClassificationType());
            }
            optionalClassification.get()
                    .setClassificationScheme(valueToPopulate);
        }

        valueToPopulate = MapUtils.getString(map, CLASSIFIED_OBJECT);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            if (!optionalClassification.isPresent()) {
                optionalClassification = Optional.of(RIM_FACTORY.createClassificationType());
            }
            optionalClassification.get()
                    .setClassifiedObject(valueToPopulate);
        }

        valueToPopulate = MapUtils.getString(map, NODE_REPRESENTATION);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            if (!optionalClassification.isPresent()) {
                optionalClassification = Optional.of(RIM_FACTORY.createClassificationType());
            }
            optionalClassification.get()
                    .setNodeRepresentation(valueToPopulate);
        }

        return optionalClassification;
    }
}
