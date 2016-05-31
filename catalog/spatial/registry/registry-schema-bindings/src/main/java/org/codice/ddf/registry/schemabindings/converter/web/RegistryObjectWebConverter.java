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
package org.codice.ddf.registry.schemabindings.converter.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.registry.schemabindings.helper.InternationalStringTypeHelper;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.ClassificationType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExternalIdentifierType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.SlotType1;

public class RegistryObjectWebConverter {
    public static final String CLASSIFICATION_KEY = "Classification";

    public static final String DESCRIPTION_KEY = "Description";

    public static final String EXTERNAL_IDENTIFIER_KEY = "ExternalIdentifier";

    public static final String HOME_KEY = "home";

    public static final String ID_KEY = "id";

    public static final String LID_KEY = "Lid";

    public static final String NAME_KEY = "Name";

    public static final String OBJECT_TYPE_KEY = "objectType";

    public static final String SLOT = "Slot";

    public static final String STATUS_KEY = "Status";

    public static final String VERSION_INFO_KEY = "VersionInfo";

    protected static final InternationalStringTypeHelper INTERNATIONAL_STRING_TYPE_HELPER =
            new InternationalStringTypeHelper();

    /**
     * This method creates a Map<String, Object> representation of the RegistryObjectType provided.
     * The following keys will be added to the map (Taken from EbrimConstants):
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
     * ClassificationWebConverter
     * ExternalIdentifierWebConverter
     * SlotWebConverter
     * InternationalStringTypeHelper
     *
     * @param registryObject the RegistryObjectType to be converted into a map, null returns empty Map
     * @return Map<String, Object> representation of the RegistryObjectType provided
     */
    public Map<String, Object> convertRegistryObject(RegistryObjectType registryObject) {
        Map<String, Object> registryObjectMap = new HashMap<>();

        if (registryObject.isSetClassification()) {
            List<Map<String, Object>> classifications = new ArrayList<>();
            ClassificationWebConverter classificationConverter = new ClassificationWebConverter();

            for (ClassificationType classification : registryObject.getClassification()) {
                Map<String, Object> classficationMap = classificationConverter.convert(
                        classification);

                if (MapUtils.isNotEmpty(classficationMap)) {
                    classifications.add(classficationMap);
                }
            }

            if (CollectionUtils.isNotEmpty(classifications)) {
                registryObjectMap.put(CLASSIFICATION_KEY, classifications);
            }
        }

        if (registryObject.isSetDescription()) {
            String description =
                    INTERNATIONAL_STRING_TYPE_HELPER.getString(registryObject.getDescription());
            if (StringUtils.isNotBlank(description)) {
                registryObjectMap.put(DESCRIPTION_KEY, description);
            }
        }

        if (registryObject.isSetExternalIdentifier()) {
            List<Map<String, Object>> externalIdentifiers = new ArrayList<>();
            ExternalIdentifierWebConverter externalIdentifierConverter =
                    new ExternalIdentifierWebConverter();

            for (ExternalIdentifierType externalIdentifier : registryObject.getExternalIdentifier()) {
                Map<String, Object> externalIdentifierMap = externalIdentifierConverter.convert(
                        externalIdentifier);
                if (MapUtils.isNotEmpty(externalIdentifierMap)) {
                    externalIdentifiers.add(externalIdentifierMap);
                }
            }

            if (CollectionUtils.isNotEmpty(externalIdentifiers)) {
                registryObjectMap.put(EXTERNAL_IDENTIFIER_KEY, externalIdentifiers);
            }
        }

        if (registryObject.isSetHome()) {
            registryObjectMap.put(HOME_KEY, registryObject.getHome());
        }

        if (registryObject.isSetId()) {
            registryObjectMap.put(ID_KEY, registryObject.getId());
        }

        if (registryObject.isSetLid()) {
            registryObjectMap.put(LID_KEY, registryObject.getLid());
        }

        if (registryObject.isSetName()) {
            String name = INTERNATIONAL_STRING_TYPE_HELPER.getString(registryObject.getName());
            if (StringUtils.isNotBlank(name)) {
                registryObjectMap.put(NAME_KEY, name);
            }
        }

        if (registryObject.isSetObjectType()) {
            registryObjectMap.put(OBJECT_TYPE_KEY, registryObject.getObjectType());
        }

        if (registryObject.isSetSlot()) {
            List<Map<String, Object>> slots = new ArrayList<>();
            SlotWebConverter slotConverter = new SlotWebConverter();

            for (SlotType1 slot : registryObject.getSlot()) {
                Map<String, Object> slotMap = slotConverter.convert(slot);
                if (MapUtils.isNotEmpty(slotMap)) {
                    slots.add(slotMap);
                }
            }

            if (CollectionUtils.isNotEmpty(slots)) {
                registryObjectMap.put(SLOT, slots);
            }
        }

        if (registryObject.isSetStatus()) {
            registryObjectMap.put(STATUS_KEY, registryObject.getStatus());
        }

        if (registryObject.isSetVersionInfo()) {
            String versionName = registryObject.getVersionInfo()
                    .getVersionName();
            if (StringUtils.isNotBlank(versionName)) {
                registryObjectMap.put(VERSION_INFO_KEY, versionName);
            }
        }

        return registryObjectMap;
    }
}
