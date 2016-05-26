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

import static org.codice.ddf.registry.schemabindings.EbrimConstants.ASSOCIATION_KEY;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.EXTRINSIC_OBJECT_KEY;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.ORGANIZATION_KEY;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.PERSON_KEY;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.SERVICE_KEY;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBElement;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.AssociationType1;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExtrinsicObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.IdentifiableType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.OrganizationType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.PersonType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectListType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ServiceType;

public class RegistryObjectListWebConverter {

    /**
     * This method creates a Map<String, Object> representation of the RegistryObjectListType provided.
     * The following keys will be added to the map (Taken from EbrimConstants):
     * <p>
     * ASSOCIATION_KEY = "Association";
     * EXTRINSIC_OBJECT_KEY = "ExtrinsicObject";
     * ORGANIZATION_KEY = "Organization";
     * PERSON_KEY = "Person";
     * SERVICE_KEY = "Service";
     * <p>
     * <p>
     * Uses:
     * AssociationWebConverter
     * ExtrinsicObjectWebConverter
     * OrganizationWebConverter
     * PersonWebConverter
     * ServiceWebConverter
     *
     * @param registryObjectList the RegistryObjectListType to be converted into a map, null returns empty Map
     * @return Map<String, Object> representation of the RegistryObjectListType provided
     */
    public Map<String, Object> convert(RegistryObjectListType registryObjectList) {
        Map<String, Object> registryObjectListMap = new HashMap<>();
        if (registryObjectList == null) {
            return registryObjectListMap;
        }

        List<Map<String, Object>> associations = new ArrayList<>();
        List<Map<String, Object>> extrinsicObjects = new ArrayList<>();
        List<Map<String, Object>> organizations = new ArrayList<>();
        List<Map<String, Object>> people = new ArrayList<>();
        List<Map<String, Object>> services = new ArrayList<>();

        AssociationWebConverter associationConverter = new AssociationWebConverter();
        ExtrinsicObjectWebConverter extrinsicObjectConverter = new ExtrinsicObjectWebConverter();
        OrganizationWebConverter organizationConverter = new OrganizationWebConverter();
        PersonWebConverter personConverter = new PersonWebConverter();
        ServiceWebConverter serviceConverter = new ServiceWebConverter();

        for (JAXBElement<? extends IdentifiableType> identifiable : registryObjectList.getIdentifiable()) {
            RegistryObjectType registryObject = (RegistryObjectType) identifiable.getValue();
            Map<String, Object> identifiableMap;

            if (registryObject instanceof ExtrinsicObjectType) {
                identifiableMap =
                        extrinsicObjectConverter.convert((ExtrinsicObjectType) registryObject);

                if (MapUtils.isNotEmpty(identifiableMap)) {
                    extrinsicObjects.add(identifiableMap);
                }
            } else if (registryObject instanceof ServiceType) {
                identifiableMap = serviceConverter.convert((ServiceType) registryObject);

                if (MapUtils.isNotEmpty(identifiableMap)) {
                    services.add(identifiableMap);
                }
            } else if (registryObject instanceof OrganizationType) {
                identifiableMap = organizationConverter.convert((OrganizationType) registryObject);

                if (MapUtils.isNotEmpty(identifiableMap)) {
                    organizations.add(identifiableMap);
                }
            } else if (registryObject instanceof PersonType) {
                identifiableMap = personConverter.convert((PersonType) registryObject);

                if (MapUtils.isNotEmpty(identifiableMap)) {
                    people.add(identifiableMap);
                }
            } else if (registryObject instanceof AssociationType1) {
                identifiableMap = associationConverter.convert((AssociationType1) registryObject);

                if (MapUtils.isNotEmpty(identifiableMap)) {
                    associations.add(identifiableMap);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(associations)) {
            registryObjectListMap.put(ASSOCIATION_KEY, associations);
        }
        if (CollectionUtils.isNotEmpty(extrinsicObjects)) {
            registryObjectListMap.put(EXTRINSIC_OBJECT_KEY, extrinsicObjects);
        }
        if (CollectionUtils.isNotEmpty(organizations)) {
            registryObjectListMap.put(ORGANIZATION_KEY, organizations);
        }
        if (CollectionUtils.isNotEmpty(people)) {
            registryObjectListMap.put(PERSON_KEY, people);
        }
        if (CollectionUtils.isNotEmpty(services)) {
            registryObjectListMap.put(SERVICE_KEY, services);
        }

        return registryObjectListMap;
    }
}
