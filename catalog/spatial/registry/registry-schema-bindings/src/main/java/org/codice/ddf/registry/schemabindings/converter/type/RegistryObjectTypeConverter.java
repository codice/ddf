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

import static org.codice.ddf.registry.schemabindings.EbrimConstants.CLASSIFICATION_KEY;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.DESCRIPTION_KEY;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.EXTERNAL_IDENTIFIER_KEY;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.HOME_KEY;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.ID_KEY;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.LID_KEY;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.NAME_KEY;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.OBJECT_TYPE_KEY;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.RIM_FACTORY;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.SLOT;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.STATUS_KEY;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.VERSION_INFO_KEY;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.registry.schemabindings.helper.InternationalStringTypeHelper;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.ClassificationType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExternalIdentifierType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.InternationalStringType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.SlotType1;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.VersionInfoType;

public class RegistryObjectTypeConverter {

    protected static final InternationalStringTypeHelper INTERNATIONAL_STRING_TYPE_HELPER =
            new InternationalStringTypeHelper();

    public RegistryObjectTypeConverter() {
    }

    /**
     * This method creates an RegistryObjectType from the values in the provided map.
     * The following keys are expected in the provided map (Taken from EbrimConstants):
     * <p>
     * CLASSIFICATION_KEY = "Classification";
     * EXTERNAL_IDENTIFIER_KEY = "ExternalIdentifier";
     * NAME_KEY = "Name";
     * DESCRIPTION_KEY = "Description";
     * VERSION_INFO_KEY = "VersionInfo";
     * SLOT = "Slot";
     * ID_KEY = "id";
     * HOME_KEY = "home";
     * LID_KEY = "Lid";
     * STATUS_KEY = "Status";
     * OBJECT_TYPE_KEY = "objectType";
     * <p>
     * Uses:
     * ClassificationTypeConverter
     * ExternalIdentifierTypeConverter
     * SlotTypeConverter
     * InternationalStringTypeHelper
     *
     * @param map the Map representation of the RegistryObjectType to generate, null returns empty Optional
     * @return Optional RegistryObjectType created from the values in the map
     */
    public Optional<RegistryObjectType> convertRegistryObject(Map<String, Object> map) {
        Optional<RegistryObjectType> optionalRegistryObject = Optional.empty();

        if (map.containsKey(CLASSIFICATION_KEY)) {
            List<Map<String, Object>> classificationMaps = (List<Map<String, Object>>) map.get(
                    CLASSIFICATION_KEY);

            ClassificationTypeConverter classificationConverter = new ClassificationTypeConverter();

            Optional<ClassificationType> optionalClassification;
            for (Map<String, Object> classificationMap : classificationMaps) {
                optionalClassification = classificationConverter.convert(classificationMap);
                if (optionalClassification.isPresent()) {
                    if (!optionalRegistryObject.isPresent()) {
                        optionalRegistryObject = Optional.of(createObjectInstance());
                    }

                    optionalRegistryObject.get()
                            .getClassification()
                            .add(optionalClassification.get());
                }
            }
        }

        String valueToPopulate = MapUtils.getString(map, DESCRIPTION_KEY);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            if (!optionalRegistryObject.isPresent()) {
                optionalRegistryObject = Optional.of(createObjectInstance());
            }

            InternationalStringType istToPopulate = INTERNATIONAL_STRING_TYPE_HELPER.create(
                    valueToPopulate);
            if (istToPopulate != null) {
                optionalRegistryObject.get()
                        .setDescription(istToPopulate);
            }
        }

        if (map.containsKey(EXTERNAL_IDENTIFIER_KEY)) {
            Optional<ExternalIdentifierType> optionalExternalIdentifier;
            ExternalIdentifierTypeConverter eitConverter = new ExternalIdentifierTypeConverter();
            for (Map<String, Object> externalIdentifierMap : (List<Map<String, Object>>) map.get(
                    EXTERNAL_IDENTIFIER_KEY)) {
                optionalExternalIdentifier = eitConverter.convert(externalIdentifierMap);

                if (optionalExternalIdentifier.isPresent()) {
                    if (!optionalRegistryObject.isPresent()) {
                        optionalRegistryObject = Optional.of(createObjectInstance());
                    }

                    optionalRegistryObject.get()
                            .getExternalIdentifier()
                            .add(optionalExternalIdentifier.get());
                }

            }
        }

        valueToPopulate = MapUtils.getString(map, HOME_KEY);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            if (!optionalRegistryObject.isPresent()) {
                optionalRegistryObject = Optional.of(createObjectInstance());
            }

            optionalRegistryObject.get()
                    .setHome(valueToPopulate);
        }

        valueToPopulate = MapUtils.getString(map, ID_KEY);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            if (!optionalRegistryObject.isPresent()) {
                optionalRegistryObject = Optional.of(createObjectInstance());
            }

            optionalRegistryObject.get()
                    .setId(valueToPopulate);
        }

        valueToPopulate = MapUtils.getString(map, LID_KEY);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            if (!optionalRegistryObject.isPresent()) {
                optionalRegistryObject = Optional.of(RIM_FACTORY.createRegistryObjectType());
            }

            optionalRegistryObject.get()
                    .setLid(valueToPopulate);
        }

        valueToPopulate = MapUtils.getString(map, NAME_KEY);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            if (!optionalRegistryObject.isPresent()) {
                optionalRegistryObject = Optional.of(RIM_FACTORY.createRegistryObjectType());
            }

            InternationalStringType istToPopulate = INTERNATIONAL_STRING_TYPE_HELPER.create(
                    valueToPopulate);
            if (istToPopulate != null) {
                optionalRegistryObject.get()
                        .setName(istToPopulate);
            }
        }

        valueToPopulate = MapUtils.getString(map, OBJECT_TYPE_KEY);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            if (!optionalRegistryObject.isPresent()) {
                optionalRegistryObject = Optional.of(RIM_FACTORY.createRegistryObjectType());
            }

            optionalRegistryObject.get()
                    .setObjectType(valueToPopulate);
        }

        if (map.containsKey(SLOT)) {
            Optional<SlotType1> optionalSlot;
            SlotTypeConverter stConverter = new SlotTypeConverter();
            for (Map<String, Object> slotMap : (List<Map<String, Object>>) map.get(SLOT)) {
                optionalSlot = stConverter.convert(slotMap);
                if (optionalSlot.isPresent()) {
                    if (!optionalRegistryObject.isPresent()) {
                        optionalRegistryObject =
                                Optional.of(RIM_FACTORY.createRegistryObjectType());
                    }

                    optionalRegistryObject.get()
                            .getSlot()
                            .add(optionalSlot.get());
                }
            }
        }

        valueToPopulate = MapUtils.getString(map, STATUS_KEY);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            if (!optionalRegistryObject.isPresent()) {
                optionalRegistryObject = Optional.of(RIM_FACTORY.createRegistryObjectType());
            }

            optionalRegistryObject.get()
                    .setStatus(valueToPopulate);
        }

        valueToPopulate = MapUtils.getString(map, VERSION_INFO_KEY);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            if (!optionalRegistryObject.isPresent()) {
                optionalRegistryObject = Optional.of(RIM_FACTORY.createRegistryObjectType());
            }

            VersionInfoType versionInfo = RIM_FACTORY.createVersionInfoType();
            versionInfo.setVersionName(valueToPopulate);

            optionalRegistryObject.get()
                    .setVersionInfo(versionInfo);
        }

        return optionalRegistryObject;
    }

    protected RegistryObjectType createObjectInstance() {
        return RIM_FACTORY.createRegistryObjectType();
    }

}
