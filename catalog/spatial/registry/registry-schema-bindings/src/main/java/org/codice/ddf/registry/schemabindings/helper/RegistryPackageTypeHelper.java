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
<<<<<<< HEAD
import java.util.List;
=======
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
>>>>>>> master
import java.util.stream.Collectors;

import javax.xml.bind.JAXBElement;

<<<<<<< HEAD
=======
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

>>>>>>> master
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
<<<<<<< HEAD
    private List<ServiceType> services = new ArrayList<>();
    private List<ExtrinsicObjectType> extrinsicObjects = new ArrayList<>();
    private List<OrganizationType> organizations = new ArrayList<>();
    private List<PersonType> persons = new ArrayList<>();
=======

    private List<ServiceType> services = new ArrayList<>();

    private List<ExtrinsicObjectType> extrinsicObjects = new ArrayList<>();

    private List<OrganizationType> organizations = new ArrayList<>();

    private List<PersonType> persons = new ArrayList<>();

>>>>>>> master
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
<<<<<<< HEAD
     * @param registryPackage
     *   the RegistryPackageType that will be crawled
     *   null returns an empty list
     * @return
     *   a List containing the ServiceBindingTypes found in the RegistryPackageType
     *   an empty list if the RegistryPackageType provided is null
=======
     * @param registryPackage the RegistryPackageType that will be crawled
     *                        null returns an empty list
     * @return a List containing the ServiceBindingTypes found in the RegistryPackageType
     * an empty list if the RegistryPackageType provided is null
>>>>>>> master
     */
    public List<ServiceBindingType> getBindingTypes(RegistryPackageType registryPackage) {
        if (registryPackage == null) {
            return Collections.emptyList();
        }

<<<<<<< HEAD
        return getServices(registryPackage).stream().flatMap(service -> service.getServiceBinding().stream()).collect(Collectors.toList());
=======
        return getServices(registryPackage).stream()
                .flatMap(service -> service.getServiceBinding()
                        .stream())
                .collect(Collectors.toList());
>>>>>>> master
    }

    /**
     * This is a convenience method that returns all of the ServiceBindingTypes found in the provided RegistryObjectListType
     *
<<<<<<< HEAD
     * @param registryObjectList
     *   the RegistryObjectListType that will be crawled
     *   null returns an empty list
     * @return
     *   a List containing the ServiceBindingTypes found in the RegistryObjectListType
     *   an empty list if the RegistryObjectListType provided is null
     */
    public  List<ServiceBindingType> getBindingTypes(RegistryObjectListType registryObjectList) {
=======
     * @param registryObjectList the RegistryObjectListType that will be crawled
     *                           null returns an empty list
     * @return a List containing the ServiceBindingTypes found in the RegistryObjectListType
     * an empty list if the RegistryObjectListType provided is null
     */
    public List<ServiceBindingType> getBindingTypes(RegistryObjectListType registryObjectList) {
>>>>>>> master
        if (registryObjectList == null) {
            return Collections.emptyList();
        }

<<<<<<< HEAD
        return getServices(registryObjectList).stream().flatMap(service -> service.getServiceBinding().stream()).collect(Collectors.toList());
=======
        return getServices(registryObjectList).stream()
                .flatMap(service -> service.getServiceBinding()
                        .stream())
                .collect(Collectors.toList());
>>>>>>> master
    }

    /**
     * This is a convenience method that returns the list of ServiceBindingTypes extracted from this class's RegistryPackageType
     *
<<<<<<< HEAD
     * @return
     *   a List containing the ServiceBindingTypes
     */
    public  List<ServiceBindingType> getBindingTypes() {
=======
     * @return a List containing the ServiceBindingTypes
     */
    public List<ServiceBindingType> getBindingTypes() {
>>>>>>> master
        return serviceBindings;
    }

    /**
     * This is a convenience method that returns all of the ServiceTypes found in the provided RegistryPackageType
     *
<<<<<<< HEAD
     * @param registryPackage
     *   the RegistryPackageType that will be crawled
     *   null returns an empty list
     * @return
     *   a List containing the ServiceTypes found in the RegistryPackageType
     *   an empty list if the RegistryPackageType provided is null
     */
    public  List<ServiceType> getServices(RegistryPackageType registryPackage) {
=======
     * @param registryPackage the RegistryPackageType that will be crawled
     *                        null returns an empty list
     * @return a List containing the ServiceTypes found in the RegistryPackageType
     * an empty list if the RegistryPackageType provided is null
     */
    public List<ServiceType> getServices(RegistryPackageType registryPackage) {
>>>>>>> master
        return getObjectsFromRegistryObjectList(registryPackage, ServiceType.class);
    }

    /**
     * This is a convenience method that returns all of the ServiceTypes found in the provided RegistryObjectListType
     *
<<<<<<< HEAD
     * @param registryObjectList
     *   the RegistryObjectListType that will be crawled
     *   null returns an empty list
     * @return
     *   a List containing the ServiceTypes found in the RegistryObjectListType
     *   an empty list if the RegistryObjectListType provided is null
     */
    public  List<ServiceType> getServices(RegistryObjectListType registryObjectList) {
=======
     * @param registryObjectList the RegistryObjectListType that will be crawled
     *                           null returns an empty list
     * @return a List containing the ServiceTypes found in the RegistryObjectListType
     * an empty list if the RegistryObjectListType provided is null
     */
    public List<ServiceType> getServices(RegistryObjectListType registryObjectList) {
>>>>>>> master
        return getObjectsFromRegistryObjectList(registryObjectList, ServiceType.class);
    }

    /**
     * This is a convenience method that returns the list of ServiceTypes extracted from this class's RegistryPackageType
     *
<<<<<<< HEAD
     * @return
     *   a List containing the ServiceTypes
     */
    public  List<ServiceType> getServices() {
=======
     * @return a List containing the ServiceTypes
     */
    public List<ServiceType> getServices() {
>>>>>>> master
        return services;
    }

    /**
     * This is a convenience method that returns all of the ExtrinsicObjectTypes found in the provided RegistryPackageType
     *
<<<<<<< HEAD
     * @param registryPackage
     *   the RegistryPackageType that will be crawled
     *   null returns an empty list
     * @return
     *   a List containing the ExtrinsicObjectTypes found in the RegistryPackageType
     *   an empty list if the RegistryPackageType provided is null
     */
    public  List<ExtrinsicObjectType> getExtrinsicObjects(
            RegistryPackageType registryPackage) {
=======
     * @param registryPackage the RegistryPackageType that will be crawled
     *                        null returns an empty list
     * @return a List containing the ExtrinsicObjectTypes found in the RegistryPackageType
     * an empty list if the RegistryPackageType provided is null
     */
    public List<ExtrinsicObjectType> getExtrinsicObjects(RegistryPackageType registryPackage) {
>>>>>>> master
        return getObjectsFromRegistryObjectList(registryPackage, ExtrinsicObjectType.class);
    }

    /**
     * This is a convenience method that returns all of the ExtrinsicObjectTypes found in the provided RegistryObjectListType
     *
<<<<<<< HEAD
     * @param registryObjectList
     *   the RegistryObjectListType that will be crawled
     *   null returns an empty list
     * @return
     *   a List containing the ExtrinsicObjectTypes found in the RegistryObjectListType
     *   an empty list if the RegistryObjectListType provided is null
     */
    public  List<ExtrinsicObjectType> getExtrinsicObjects(
=======
     * @param registryObjectList the RegistryObjectListType that will be crawled
     *                           null returns an empty list
     * @return a List containing the ExtrinsicObjectTypes found in the RegistryObjectListType
     * an empty list if the RegistryObjectListType provided is null
     */
    public List<ExtrinsicObjectType> getExtrinsicObjects(
>>>>>>> master
            RegistryObjectListType registryObjectList) {
        return getObjectsFromRegistryObjectList(registryObjectList, ExtrinsicObjectType.class);
    }

    /**
     * This is a convenience method that returns the list of ExtrinsicObjectTypes extracted from this class's RegistryPackageType
     *
<<<<<<< HEAD
     * @return
     *   a List containing the ExtrinsicObjectTypes
     */
    public  List<ExtrinsicObjectType> getExtrinsicObjects() {
=======
     * @return a List containing the ExtrinsicObjectTypes
     */
    public List<ExtrinsicObjectType> getExtrinsicObjects() {
>>>>>>> master
        return extrinsicObjects;
    }

    /**
     * This is a convenience method that returns all of the OrganizationTypes found in the provided RegistryPackageType
     *
<<<<<<< HEAD
     * @param registryPackage
     *   the RegistryPackageType that will be crawled
     *   null returns an empty list
     * @return
     *   a List containing the OrganizationTypes found in the RegistryPackageType
     *   an empty list if the RegistryPackageType provided is null
     */
    public  List<OrganizationType> getOrganizations(RegistryPackageType registryPackage) {
=======
     * @param registryPackage the RegistryPackageType that will be crawled
     *                        null returns an empty list
     * @return a List containing the OrganizationTypes found in the RegistryPackageType
     * an empty list if the RegistryPackageType provided is null
     */
    public List<OrganizationType> getOrganizations(RegistryPackageType registryPackage) {
>>>>>>> master
        return getObjectsFromRegistryObjectList(registryPackage, OrganizationType.class);
    }

    /**
     * This is a convenience method that returns all of the OrganizationTypes found in the provided RegistryObjectListType
     *
<<<<<<< HEAD
     * @param registryObjectList
     *   the RegistryObjectListType that will be crawled
     *   null returns an empty list
     * @return
     *   a List containing the OrganizationTypes found in the RegistryObjectListType
     *   an empty list if the RegistryObjectListType provided is null
     */
    public  List<OrganizationType> getOrganizations(
            RegistryObjectListType registryObjectList) {
=======
     * @param registryObjectList the RegistryObjectListType that will be crawled
     *                           null returns an empty list
     * @return a List containing the OrganizationTypes found in the RegistryObjectListType
     * an empty list if the RegistryObjectListType provided is null
     */
    public List<OrganizationType> getOrganizations(RegistryObjectListType registryObjectList) {
>>>>>>> master
        return getObjectsFromRegistryObjectList(registryObjectList, OrganizationType.class);
    }

    /**
     * This is a convenience method that returns the list of OrganizationTypes extracted from this class's RegistryPackageType
     *
<<<<<<< HEAD
     * @return
     *   a List containing the OrganizationTypes
     */
    public  List<OrganizationType> getOrganizations() {
=======
     * @return a List containing the OrganizationTypes
     */
    public List<OrganizationType> getOrganizations() {
>>>>>>> master
        return organizations;
    }

    /**
     * This is a convenience method that returns all of the PersonTypes found in the provided RegistryPackageType
     *
<<<<<<< HEAD
     * @param registryPackage
     *   the RegistryPackageType that will be crawled
     *   null returns an empty list
     * @return
     *   a List containing the PersonTypes found in the RegistryPackageType
     *   an empty list if the RegistryPackageType provided is null
     */
    public  List<PersonType> getPersons(RegistryPackageType registryPackage) {
=======
     * @param registryPackage the RegistryPackageType that will be crawled
     *                        null returns an empty list
     * @return a List containing the PersonTypes found in the RegistryPackageType
     * an empty list if the RegistryPackageType provided is null
     */
    public List<PersonType> getPersons(RegistryPackageType registryPackage) {
>>>>>>> master
        return getObjectsFromRegistryObjectList(registryPackage, PersonType.class);
    }

    /**
     * This is a convenience method that returns all of the PersonTypes found in the provided RegistryObjectListType
     *
<<<<<<< HEAD
     * @param registryObjectList
     *   the RegistryObjectListType that will be crawled
     *   null returns an empty list
     * @return
     *   a List containing the PersonTypes found in the RegistryObjectListType
     *   an empty list if the RegistryObjectListType provided is null
     */
    public  List<PersonType> getPersons(RegistryObjectListType registryObjectList) {
=======
     * @param registryObjectList the RegistryObjectListType that will be crawled
     *                           null returns an empty list
     * @return a List containing the PersonTypes found in the RegistryObjectListType
     * an empty list if the RegistryObjectListType provided is null
     */
    public List<PersonType> getPersons(RegistryObjectListType registryObjectList) {
>>>>>>> master
        return getObjectsFromRegistryObjectList(registryObjectList, PersonType.class);
    }

    /**
     * This is a convenience method that returns the list of PersonTypes extracted from this class's RegistryPackageType
     *
<<<<<<< HEAD
     * @return
     *   a List containing the PersonTypes
     */
    public  List<PersonType> getPersons() {
=======
     * @return a List containing the PersonTypes
     */
    public List<PersonType> getPersons() {
>>>>>>> master
        return persons;
    }

    /**
     * This is a convenience method that returns all of the AssociationTypes found in the provided RegistryPackageType
     *
<<<<<<< HEAD
     * @param registryPackage
     *   the RegistryPackageType that will be crawled
     *   null returns an empty list
     * @return
     *   a List containing the AssociationTypes found in the RegistryPackageType
     *   an empty list if the RegistryPackageType provided is null
     */
    public  List<AssociationType1> getAssociations(RegistryPackageType registryPackage) {
=======
     * @param registryPackage the RegistryPackageType that will be crawled
     *                        null returns an empty list
     * @return a List containing the AssociationTypes found in the RegistryPackageType
     * an empty list if the RegistryPackageType provided is null
     */
    public List<AssociationType1> getAssociations(RegistryPackageType registryPackage) {
>>>>>>> master
        return getObjectsFromRegistryObjectList(registryPackage, AssociationType1.class);
    }

    /**
     * This is a convenience method that returns all of the AssociationTypes found in the provided RegistryObjectListType
     *
<<<<<<< HEAD
     * @param registryObjectList
     *   the RegistryObjectListType that will be crawled, null returns an empty list
     * @return
     *   a List containing the AssociationTypes found in the RegistryObjectListType
     */
    public  List<AssociationType1> getAssociations(
            RegistryObjectListType registryObjectList) {
=======
     * @param registryObjectList the RegistryObjectListType that will be crawled, null returns an empty list
     * @return a List containing the AssociationTypes found in the RegistryObjectListType
     */
    public List<AssociationType1> getAssociations(RegistryObjectListType registryObjectList) {
>>>>>>> master
        return getObjectsFromRegistryObjectList(registryObjectList, AssociationType1.class);
    }

    /**
     * This is a convenience method that returns the list of AssociationTypes extracted from this class's RegistryPackageType
     *
<<<<<<< HEAD
     * @return
     *   a List containing the AssociationTypes
     */
    public  List<AssociationType1> getAssociations() {
        return associations;
    }

=======
     * @return a List containing the AssociationTypes
     */
    public List<AssociationType1> getAssociations() {
        return associations;
    }

    /**
     * This is a convenience method that returns all Objects associated with the provided id extracted from the provided RegistryPackageType
     *
     * @param registryPackage the RegistryPackageType that will be crawled, null returns an empty list
     * @param id              to be used as the sourceObjectId of the Association
     * @param type            Type of the object to find associations for
     * @return a List containing the objects associated to the provided id
     */
    public <T extends RegistryObjectType> List<T> getAssociatedObjects(
            RegistryPackageType registryPackage, String id, Class<T> type) {
        if (StringUtils.isEmpty(id) || registryPackage == null) {
            return Collections.emptyList();
        }

        return getAssociatedObjects(registryPackage.getRegistryObjectList(), id, type);
    }

    /**
     * This is a convenience method that returns all Objects associated with the provided id extracted from the provided RegistryObjectList
     *
     * @param registryObjectList the RegistryObjectList that will be crawled, null returns an empty list
     * @param id                 to be used as the sourceObjectId of the Association
     * @param type               Type of the object to find associations for
     * @return a List containing the objects associated to the provided id
     */
    public <T extends RegistryObjectType> List<T> getAssociatedObjects(
            RegistryObjectListType registryObjectList, String id, Class<T> type) {
        if (StringUtils.isEmpty(id)) {
            return Collections.emptyList();
        }

        Set<String> targetObjectIds = getAssociationTargetObjectIds(registryObjectList, id);
        if (CollectionUtils.isEmpty(targetObjectIds)) {
            return Collections.emptyList();
        }

        return getObjectsFromRegistryObjectList(registryObjectList, type).stream()
                .filter(isAssociatedObject(targetObjectIds))
                .collect(Collectors.toList());
    }

    /**
     * This is a convenience method that returns all Objects associated with the provided id extracted from this class's RegistryPackageType
     *
     * @param id   to be used as the sourceObjectId of the Association
     * @param type Type of the object to find associations for
     * @return a List containing the objects associated to the provided id
     */
    public <T extends RegistryObjectType> List<T> getAssociatedObjects(String id, Class<T> type) {
        if (StringUtils.isEmpty(id)) {
            return Collections.emptyList();
        }
        Set<String> targetObjectIds = getAssociationTargetObjectIds(id);
        if (CollectionUtils.isEmpty(targetObjectIds)) {
            return Collections.emptyList();
        }

        return getObjectsFromRegistryObjectList(registryPackageType, type).stream()
                .filter(isAssociatedObject(targetObjectIds))
                .collect(Collectors.toList());
    }

    private Predicate<RegistryObjectType> isAssociatedObject(Set<String> associatedIds) {
        return p -> associatedIds.contains(p.getId());
    }

    private Set<String> getAssociationTargetObjectIds(RegistryObjectListType registryObjectList,
            String sourceId) {
        if (registryObjectList == null) {
            return new HashSet<>();
        }
        return getAssociations(registryObjectList).stream()
                .filter(association -> sourceId.equals(association.getSourceObject()))
                .map(AssociationType1::getTargetObject)
                .collect(Collectors.toSet());
    }

    private Set<String> getAssociationTargetObjectIds(String sourceId) {
        return getAssociations().stream()
                .filter(association -> sourceId.equals(association.getSourceObject()))
                .map(AssociationType1::getTargetObject)
                .collect(Collectors.toSet());
    }
>>>>>>> master

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
<<<<<<< HEAD
                RegistryObjectType registryObject = (RegistryObjectType) identifiableType.getValue();
=======
                RegistryObjectType registryObject =
                        (RegistryObjectType) identifiableType.getValue();
>>>>>>> master

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

<<<<<<< HEAD
    private  <T extends RegistryObjectType> List<T> getObjectsFromRegistryObjectList(
=======
    private <T extends RegistryObjectType> List<T> getObjectsFromRegistryObjectList(
>>>>>>> master
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

<<<<<<< HEAD
    private  <T extends RegistryObjectType> List<T> getObjectsFromRegistryObjectList(RegistryObjectListType registryObjectList, Class<T> type) {
=======
    private <T extends RegistryObjectType> List<T> getObjectsFromRegistryObjectList(
            RegistryObjectListType registryObjectList, Class<T> type) {
>>>>>>> master
        if (registryObjectList == null) {
            return Collections.emptyList();
        }

<<<<<<< HEAD
        return registryObjectList.getIdentifiable().stream().filter(identifiable -> type.isInstance(identifiable.getValue())).map(identifiable -> (T) identifiable.getValue()).collect(
                Collectors.toList());
=======
        return registryObjectList.getIdentifiable()
                .stream()
                .filter(identifiable -> type.isInstance(identifiable.getValue()))
                .map(identifiable -> (T) identifiable.getValue())
                .collect(Collectors.toList());
>>>>>>> master
    }

}
