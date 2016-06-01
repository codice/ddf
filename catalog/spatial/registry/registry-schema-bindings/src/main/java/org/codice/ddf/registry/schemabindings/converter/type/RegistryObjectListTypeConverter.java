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
import static org.codice.ddf.registry.schemabindings.converter.web.RegistryObjectListWebConverter.ASSOCIATION_KEY;
import static org.codice.ddf.registry.schemabindings.converter.web.RegistryObjectListWebConverter.EXTRINSIC_OBJECT_KEY;
import static org.codice.ddf.registry.schemabindings.converter.web.RegistryObjectListWebConverter.ORGANIZATION_KEY;
import static org.codice.ddf.registry.schemabindings.converter.web.RegistryObjectListWebConverter.PERSON_KEY;
import static org.codice.ddf.registry.schemabindings.converter.web.RegistryObjectListWebConverter.SERVICE_KEY;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections.MapUtils;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.AssociationType1;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExtrinsicObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.OrganizationType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.PersonType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectListType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ServiceType;

public class RegistryObjectListTypeConverter {

    /**
     * This method creates an RegistryObjectListType from the values in the provided map.
     * The following keys are expected in the provided map (Taken from EbrimConstants):
     * <p>
     * ASSOCIATION_KEY = "Association";
     * EXTRINSIC_OBJECT_KEY = "ExtrinsicObject";
     * ORGANIZATION_KEY = "Organization";
     * PERSON_KEY = "Person";
     * SERVICE_KEY = "Service";
     * <p>
     * <p>
     * Uses:
     * ExtrinsicObjectTypeConverter
     * ServiceTypeConverter
     * OrganizationTypeConverter
     * PersonTypeConverter
     * AssociationTypeConverter
     *
     * @param map the Map representation of the RegistryObjectListType to generate, null returns empty Optional
     * @return Optional RegistryObjectListType created from the values in the map
     */
    public Optional<RegistryObjectListType> convert(Map<String, Object> map) {
        Optional<RegistryObjectListType> optionalRegistryObjectList = Optional.empty();
        if (MapUtils.isEmpty(map)) {
            return optionalRegistryObjectList;
        }

        if (map.containsKey(EXTRINSIC_OBJECT_KEY)) {
            ExtrinsicObjectTypeConverter extrinsicObjectConverter =
                    new ExtrinsicObjectTypeConverter();
            Optional<ExtrinsicObjectType> optionalExtrinsicObject;
            for (Map<String, Object> extrinsicObjectMap : (List<Map<String, Object>>) map.get(
                    EXTRINSIC_OBJECT_KEY)) {
                optionalExtrinsicObject = extrinsicObjectConverter.convert(extrinsicObjectMap);

                if (optionalExtrinsicObject.isPresent()) {
                    if (!optionalRegistryObjectList.isPresent()) {
                        optionalRegistryObjectList =
                                Optional.of(RIM_FACTORY.createRegistryObjectListType());
                    }

                    optionalRegistryObjectList.get()
                            .getIdentifiable()
                            .add(RIM_FACTORY.createExtrinsicObject(optionalExtrinsicObject.get()));
                }
            }
        }

        if (map.containsKey(SERVICE_KEY)) {
            ServiceTypeConverter serviceConverter = new ServiceTypeConverter();
            Optional<ServiceType> optionalService;
            for (Map<String, Object> serviceMap : (List<Map<String, Object>>) map.get(SERVICE_KEY)) {
                optionalService = serviceConverter.convert(serviceMap);

                if (optionalService.isPresent()) {
                    if (!optionalRegistryObjectList.isPresent()) {
                        optionalRegistryObjectList =
                                Optional.of(RIM_FACTORY.createRegistryObjectListType());
                    }

                    optionalRegistryObjectList.get()
                            .getIdentifiable()
                            .add(RIM_FACTORY.createService(optionalService.get()));
                }
            }
        }

        if (map.containsKey(ORGANIZATION_KEY)) {
            OrganizationTypeConverter organizationConverter = new OrganizationTypeConverter();
            Optional<OrganizationType> optionalOrganization;
            for (Map<String, Object> organizationMap : (List<Map<String, Object>>) map.get(
                    ORGANIZATION_KEY)) {
                optionalOrganization = organizationConverter.convert(organizationMap);

                if (optionalOrganization.isPresent()) {
                    if (!optionalRegistryObjectList.isPresent()) {
                        optionalRegistryObjectList =
                                Optional.of(RIM_FACTORY.createRegistryObjectListType());
                    }

                    optionalRegistryObjectList.get()
                            .getIdentifiable()
                            .add(RIM_FACTORY.createOrganization(optionalOrganization.get()));
                }
            }
        }

        if (map.containsKey(PERSON_KEY)) {
            PersonTypeConverter personConverter = new PersonTypeConverter();
            Optional<PersonType> optionalPerson;
            for (Map<String, Object> personMap : (List<Map<String, Object>>) map.get(PERSON_KEY)) {
                optionalPerson = personConverter.convert(personMap);

                if (optionalPerson.isPresent()) {
                    if (!optionalRegistryObjectList.isPresent()) {
                        optionalRegistryObjectList =
                                Optional.of(RIM_FACTORY.createRegistryObjectListType());
                    }

                    optionalRegistryObjectList.get()
                            .getIdentifiable()
                            .add(RIM_FACTORY.createPerson(optionalPerson.get()));
                }
            }
        }

        if (map.containsKey(ASSOCIATION_KEY)) {
            AssociationTypeConverter associationConverter = new AssociationTypeConverter();
            Optional<AssociationType1> optionalAssociation;
            for (Map<String, Object> associationMap : (List<Map<String, Object>>) map.get(
                    ASSOCIATION_KEY)) {
                optionalAssociation = associationConverter.convert(associationMap);

                if (optionalAssociation.isPresent()) {
                    if (!optionalRegistryObjectList.isPresent()) {
                        optionalRegistryObjectList =
                                Optional.of(RIM_FACTORY.createRegistryObjectListType());
                    }

                    optionalRegistryObjectList.get()
                            .getIdentifiable()
                            .add(RIM_FACTORY.createAssociation(optionalAssociation.get()));
                }
            }
        }

        return optionalRegistryObjectList;
    }
}
