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
package org.codice.ddf.registry.schemabindings.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBElement;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.AssociationType1;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExtrinsicObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.IdentifiableType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.OrganizationType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.PersonType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectListType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ServiceBindingType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ServiceType;

public class RegistryPackageTypeHelper {

    private RegistryPackageType registryPackageType;

    private List<ServiceBindingType> serviceBindings = new ArrayList<>();
    private List<ServiceType> services = new ArrayList<>();
    private List<ExtrinsicObjectType> extrinsicObjects = new ArrayList<>();
    private List<OrganizationType> organizations = new ArrayList<>();
    private List<PersonType> persons = new ArrayList<>();
    private List<AssociationType1> associations = new ArrayList<>();

    public RegistryPackageTypeHelper() {
    }

    public RegistryPackageTypeHelper(RegistryPackageType registryPackage) {
        setRegistryPackage(registryPackage);
    }

    public void setRegistryPackage(RegistryPackageType registryPackage) {
        this.registryPackageType = registryPackage;
        populateLists();
    }

    /**
     * This is a convenience method that returns all of the ServiceBindingTypes found in the provided RegistryPackageType
     *
     * @param registryPackage
     *   the RegistryPackageType that will be crawled
     *   null returns an empty list
     * @return
     *   a List containing the ServiceBindingTypes found in the RegistryPackageType
     *   an empty list if the RegistryPackageType provided is null
     */
    public List<ServiceBindingType> getBindingTypes(RegistryPackageType registryPackage) {
        if (registryPackage == null) {
            return Collections.emptyList();
        }

        return getServices(registryPackage).stream().flatMap(service -> service.getServiceBinding().stream()).collect(Collectors.toList());
    }

    /**
     * This is a convenience method that returns all of the ServiceBindingTypes found in the provided RegistryObjectListType
     *
     * @param registryObjectList
     *   the RegistryObjectListType that will be crawled
     *   null returns an empty list
     * @return
     *   a List containing the ServiceBindingTypes found in the RegistryObjectListType
     *   an empty list if the RegistryObjectListType provided is null
     */
    public  List<ServiceBindingType> getBindingTypes(RegistryObjectListType registryObjectList) {
        if (registryObjectList == null) {
            return Collections.emptyList();
        }

        return getServices(registryObjectList).stream().flatMap(service -> service.getServiceBinding().stream()).collect(Collectors.toList());
    }

    /**
     * This is a convenience method that returns the list of ServiceBindingTypes extracted from this class's RegistryPackageType
     *
     * @return
     *   a List containing the ServiceBindingTypes
     */
    public  List<ServiceBindingType> getBindingTypes() {
        return serviceBindings;
    }

    /**
     * This is a convenience method that returns all of the ServiceTypes found in the provided RegistryPackageType
     *
     * @param registryPackage
     *   the RegistryPackageType that will be crawled
     *   null returns an empty list
     * @return
     *   a List containing the ServiceTypes found in the RegistryPackageType
     *   an empty list if the RegistryPackageType provided is null
     */
    public  List<ServiceType> getServices(RegistryPackageType registryPackage) {
        return getObjectsFromRegistryObjectList(registryPackage, ServiceType.class);
    }

    /**
     * This is a convenience method that returns all of the ServiceTypes found in the provided RegistryObjectListType
     *
     * @param registryObjectList
     *   the RegistryObjectListType that will be crawled
     *   null returns an empty list
     * @return
     *   a List containing the ServiceTypes found in the RegistryObjectListType
     *   an empty list if the RegistryObjectListType provided is null
     */
    public  List<ServiceType> getServices(RegistryObjectListType registryObjectList) {
        return getObjectsFromRegistryObjectList(registryObjectList, ServiceType.class);
    }

    /**
     * This is a convenience method that returns the list of ServiceTypes extracted from this class's RegistryPackageType
     *
     * @return
     *   a List containing the ServiceTypes
     */
    public  List<ServiceType> getServices() {
        return services;
    }

    /**
     * This is a convenience method that returns all of the ExtrinsicObjectTypes found in the provided RegistryPackageType
     *
     * @param registryPackage
     *   the RegistryPackageType that will be crawled
     *   null returns an empty list
     * @return
     *   a List containing the ExtrinsicObjectTypes found in the RegistryPackageType
     *   an empty list if the RegistryPackageType provided is null
     */
    public  List<ExtrinsicObjectType> getExtrinsicObjects(
            RegistryPackageType registryPackage) {
        return getObjectsFromRegistryObjectList(registryPackage, ExtrinsicObjectType.class);
    }

    /**
     * This is a convenience method that returns all of the ExtrinsicObjectTypes found in the provided RegistryObjectListType
     *
     * @param registryObjectList
     *   the RegistryObjectListType that will be crawled
     *   null returns an empty list
     * @return
     *   a List containing the ExtrinsicObjectTypes found in the RegistryObjectListType
     *   an empty list if the RegistryObjectListType provided is null
     */
    public  List<ExtrinsicObjectType> getExtrinsicObjects(
            RegistryObjectListType registryObjectList) {
        return getObjectsFromRegistryObjectList(registryObjectList, ExtrinsicObjectType.class);
    }

    /**
     * This is a convenience method that returns the list of ExtrinsicObjectTypes extracted from this class's RegistryPackageType
     *
     * @return
     *   a List containing the ExtrinsicObjectTypes
     */
    public  List<ExtrinsicObjectType> getExtrinsicObjects() {
        return extrinsicObjects;
    }

    /**
     * This is a convenience method that returns all of the OrganizationTypes found in the provided RegistryPackageType
     *
     * @param registryPackage
     *   the RegistryPackageType that will be crawled
     *   null returns an empty list
     * @return
     *   a List containing the OrganizationTypes found in the RegistryPackageType
     *   an empty list if the RegistryPackageType provided is null
     */
    public  List<OrganizationType> getOrganizations(RegistryPackageType registryPackage) {
        return getObjectsFromRegistryObjectList(registryPackage, OrganizationType.class);
    }

    /**
     * This is a convenience method that returns all of the OrganizationTypes found in the provided RegistryObjectListType
     *
     * @param registryObjectList
     *   the RegistryObjectListType that will be crawled
     *   null returns an empty list
     * @return
     *   a List containing the OrganizationTypes found in the RegistryObjectListType
     *   an empty list if the RegistryObjectListType provided is null
     */
    public  List<OrganizationType> getOrganizations(
            RegistryObjectListType registryObjectList) {
        return getObjectsFromRegistryObjectList(registryObjectList, OrganizationType.class);
    }

    /**
     * This is a convenience method that returns the list of OrganizationTypes extracted from this class's RegistryPackageType
     *
     * @return
     *   a List containing the OrganizationTypes
     */
    public  List<OrganizationType> getOrganizations() {
        return organizations;
    }

    /**
     * This is a convenience method that returns all of the PersonTypes found in the provided RegistryPackageType
     *
     * @param registryPackage
     *   the RegistryPackageType that will be crawled
     *   null returns an empty list
     * @return
     *   a List containing the PersonTypes found in the RegistryPackageType
     *   an empty list if the RegistryPackageType provided is null
     */
    public  List<PersonType> getPersons(RegistryPackageType registryPackage) {
        return getObjectsFromRegistryObjectList(registryPackage, PersonType.class);
    }

    /**
     * This is a convenience method that returns all of the PersonTypes found in the provided RegistryObjectListType
     *
     * @param registryObjectList
     *   the RegistryObjectListType that will be crawled
     *   null returns an empty list
     * @return
     *   a List containing the PersonTypes found in the RegistryObjectListType
     *   an empty list if the RegistryObjectListType provided is null
     */
    public  List<PersonType> getPersons(RegistryObjectListType registryObjectList) {
        return getObjectsFromRegistryObjectList(registryObjectList, PersonType.class);
    }

    /**
     * This is a convenience method that returns the list of PersonTypes extracted from this class's RegistryPackageType
     *
     * @return
     *   a List containing the PersonTypes
     */
    public  List<PersonType> getPersons() {
        return persons;
    }

    /**
     * This is a convenience method that returns all of the AssociationTypes found in the provided RegistryPackageType
     *
     * @param registryPackage
     *   the RegistryPackageType that will be crawled
     *   null returns an empty list
     * @return
     *   a List containing the AssociationTypes found in the RegistryPackageType
     *   an empty list if the RegistryPackageType provided is null
     */
    public  List<AssociationType1> getAssociations(RegistryPackageType registryPackage) {
        return getObjectsFromRegistryObjectList(registryPackage, AssociationType1.class);
    }

    /**
     * This is a convenience method that returns all of the AssociationTypes found in the provided RegistryObjectListType
     *
     * @param registryObjectList
     *   the RegistryObjectListType that will be crawled, null returns an empty list
     * @return
     *   a List containing the AssociationTypes found in the RegistryObjectListType
     */
    public  List<AssociationType1> getAssociations(
            RegistryObjectListType registryObjectList) {
        return getObjectsFromRegistryObjectList(registryObjectList, AssociationType1.class);
    }

    /**
     * This is a convenience method that returns the list of AssociationTypes extracted from this class's RegistryPackageType
     *
     * @return
     *   a List containing the AssociationTypes
     */
    public  List<AssociationType1> getAssociations() {
        return associations;
    }


    private void populateLists() {
        serviceBindings.clear();
        services.clear();
        extrinsicObjects.clear();
        organizations.clear();
        persons.clear();
        associations.clear();

        if (registryPackageType == null) {
            return;
        }

        if (registryPackageType.isSetRegistryObjectList()) {
            RegistryObjectListType registryObjectList = registryPackageType.getRegistryObjectList();
            for (JAXBElement<? extends IdentifiableType> identifiableType : registryObjectList.getIdentifiable()) {
                RegistryObjectType registryObject = (RegistryObjectType) identifiableType.getValue();

                if (registryObject instanceof ExtrinsicObjectType) {
                    extrinsicObjects.add((ExtrinsicObjectType) registryObject);
                } else if (registryObject instanceof ServiceType) {
                    ServiceType service = (ServiceType) registryObject;
                    services.add(service);
                    serviceBindings.addAll(service.getServiceBinding());
                } else if (registryObject instanceof OrganizationType) {
                    organizations.add((OrganizationType) registryObject);
                } else if (registryObject instanceof PersonType) {
                    persons.add((PersonType) registryObject);
                } else if (registryObject instanceof AssociationType1) {
                    associations.add((AssociationType1) registryObject);
                }
            }
        }
    }

    private  <T extends RegistryObjectType> List<T> getObjectsFromRegistryObjectList(
            RegistryPackageType registryPackage, Class<T> type) {
        List<T> registryObjects = new ArrayList<>();

        if (registryPackage == null) {
            return registryObjects;
        }

        if (registryPackage.isSetRegistryObjectList()) {
            registryObjects =
                    getObjectsFromRegistryObjectList(registryPackage.getRegistryObjectList(), type);
        }

        return registryObjects;
    }

    private  <T extends RegistryObjectType> List<T> getObjectsFromRegistryObjectList(RegistryObjectListType registryObjectList, Class<T> type) {
        if (registryObjectList == null) {
            return Collections.emptyList();
        }

        return registryObjectList.getIdentifiable().stream().filter(identifiable -> type.isInstance(identifiable.getValue())).map(identifiable -> (T) identifiable.getValue()).collect(
                Collectors.toList());
    }

}
