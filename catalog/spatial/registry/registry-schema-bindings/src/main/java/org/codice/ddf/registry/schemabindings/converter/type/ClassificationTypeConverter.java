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
import static org.codice.ddf.registry.schemabindings.converter.web.ClassificationWebConverter.CLASSIFICATION_NODE;
import static org.codice.ddf.registry.schemabindings.converter.web.ClassificationWebConverter.CLASSIFICATION_SCHEME;
import static org.codice.ddf.registry.schemabindings.converter.web.ClassificationWebConverter.CLASSIFIED_OBJECT;
import static org.codice.ddf.registry.schemabindings.converter.web.ClassificationWebConverter.NODE_REPRESENTATION;

import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections.MapUtils;
import org.codice.ddf.registry.schemabindings.helper.MapToSchemaElement;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.ClassificationType;

public class ClassificationTypeConverter
        extends AbstractRegistryObjectTypeConverter<ClassificationType> {

    public ClassificationTypeConverter(MapToSchemaElement<ClassificationType> mapToSchemaElement) {
        super(mapToSchemaElement);
    }

    public ClassificationTypeConverter() {
        this(new MapToSchemaElement<>(RIM_FACTORY::createClassificationType));

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

        optionalClassification = super.convert(map);

        optionalClassification = mapToSchemaElement.populateStringElement(map,
                CLASSIFICATION_NODE,
                optionalClassification,
                (valueToPopulate, classification) -> classification.setClassificationNode(
                        valueToPopulate));
        optionalClassification = mapToSchemaElement.populateStringElement(map,
                CLASSIFIED_OBJECT,
                optionalClassification,
                (valueToPopulate, classification) -> classification.setClassifiedObject(
                        valueToPopulate));
        optionalClassification = mapToSchemaElement.populateStringElement(map,
                CLASSIFICATION_SCHEME,
                optionalClassification,
                (valueToPopulate, classification) -> classification.setClassificationScheme(
                        valueToPopulate));
        optionalClassification = mapToSchemaElement.populateStringElement(map,
                NODE_REPRESENTATION,
                optionalClassification,
                (valueToPopulate, classification) -> classification.setNodeRepresentation(
                        valueToPopulate));

        return optionalClassification;
    }
}
