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
package ddf.catalog.registry.federationadmin.converter;

import static org.joda.time.format.ISODateTimeFormat.dateOptionalTimeParser;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBElement;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import net.opengis.cat.wrs.v_1_0_2.AnyValueType;
import net.opengis.gml.v_3_1_1.DirectPositionType;
import net.opengis.gml.v_3_1_1.PointType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.AssociationType1;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ClassificationType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.EmailAddressType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExternalIdentifierType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExtrinsicObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.InternationalStringType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.LocalizedStringType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ObjectFactory;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.OrganizationType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.PersonNameType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.PersonType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.PostalAddressType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectListType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ServiceBindingType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ServiceType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.SlotType1;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.SpecificationLinkType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.TelephoneNumberType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ValueListType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.VersionInfoType;

public class RegistryPackageWebConverter {

    private static final String ID_KEY = "id";

    private static final String HOME_KEY = "home";

    private static final String OBJECT_TYPE_KEY = "objectType";

    private static final String NAME = "name";

    private static final String NAME_KEY = "Name";

    private static final String DESCRIPTION_KEY = "Description";

    private static final String VERSION_INFO_KEY = "VersionInfo";

    private static final String SLOT = "Slot";

    private static final String VALUE = "value";

    private static final String SLOT_TYPE = "slotType";

    private static final String SERVICE_KEY = "Service";

    private static final String REGISTRY_OBJECT_LIST_KEY = "RegistryObjectList";

    private static final String SERVICE_BINDING_KEY = "ServiceBinding";

    private static final String POINT_KEY = "Point";

    private static final String SRS_DIMENSION = "srsDimension";

    private static final String SRS_NAME = "srsName";

    private static final String POSITION = "pos";

    private static final String XML_DATE_TIME_TYPE = ":dateTime";

    private static final String XML_GEO_TYPE = ":GM_Point";

    private static final String ADDRESS_CITY = "city";

    private static final String ADDRESS_COUNTRY = "country";

    private static final String ADDRESS_POSTAL_CODE = "postalCode";

    private static final String ADDRESS_STATE_OR_PROVINCE = "stateOrProvince";

    private static final String ADDRESS_STREET = "street";

    private static final String ADDRESS_KEY = "Address";

    private static final String ORGANIZATION_KEY = "Organization";

    private static final String PHONE_AREA_CODE = "areaCode";

    private static final String PHONE_NUMBER = "number";

    private static final String PHONE_EXTENSION = "extension";

    private static final String TELEPHONE_KEY = "TelephoneNumber";

    private static final String EMAIL_ADDRESS = "address";

    private static final String EMAIL_ADDRESS_KEY = "EmailAddress";

    private static final String PARENT_KEY = "parent";

    private static final String PERSON_FIRST_NAME = "firstName";

    private static final String PERSON_MIDDLE_NAME = "middleName";

    private static final String PERSON_LAST_NAME = "lastName";

    private static final String PERSON_NAME_KEY = "PersonName";

    private static final String PERSON_KEY = "Person";

    private static final String ASSOCIATION_TYPE = "associationType";

    private static final String ASSOCIATION_SOURCE_OBJECT = "sourceObject";

    private static final String ASSOCIATION_TARGET_OBJECT = "targetObject";

    private static final String ASSOCIATION_KEY = "Association";

    private static final String EXT_ID_IDENTIFICATION_SCHEME = "identificationScheme";

    private static final String EXT_ID_REGISTRY_OBJECT = "registryObject";

    private static final String EXT_ID_VALUE = "value";

    private static final String EXTERNAL_IDENTIFIER_KEY = "ExternalIdentifier";

    private static final String EXTRINSIC_OBJECT_CONTENT_VERSION = "contentVersion";

    private static final String EXTRINSIC_OBJECT_OPAQUE = "opaque";

    private static final String EXTRINSIC_OBJECT_MIME_TYPE = "mimeType";

    private static final String EXTRINSIC_OBJECT_KEY = "ExtrinsicObject";

    private static final String LID_KEY = "Lid";

    private static final String STATUS_KEY = "Status";

    private static final ObjectFactory RIM_FACTORY = new ObjectFactory();

    private static final net.opengis.cat.wrs.v_1_0_2.ObjectFactory WRS_FACTORY =
            new net.opengis.cat.wrs.v_1_0_2.ObjectFactory();

    private static final net.opengis.gml.v_3_1_1.ObjectFactory GML_FACTORY =
            new net.opengis.gml.v_3_1_1.ObjectFactory();

    private static final String BINDING_ACCESS_URI = "accessUri";

    private static final String BINDING_SERVICE = "service";

    private static final String BINDING_TARGET_BINDING = "targetBinding";

    private static final String SPECIFICATION_LINK_SERVICE_BINDING = "serviceBinding";

    private static final String SPECIFICATION_LINK_SPECIFICATION_OBJECT = "specificationObject";

    private static final String SPECIFICATION_LINK_USAGE_DESCRIPTION = "usageDescription";

    private static final String SPECIFICATION_LINK_KEY = "SpecificationLink";

    private static final String SPECIFICATION_LINK_USAGE_PARAMETERS = "usageParameters";

    private static final String PRIMARY_CONTACT = "primaryContact";

    private static final String ADDRESS_STREET_NUMBER = "streetNumber";

    private static final String EMAIL_TYPE = "type";

    private static final String PHONE_COUNTRY_CODE = "countryCode";

    private static final String PHONE_TYPE = "phoneType";

    private static final String CLASSIFICATION_NODE = "classificationNode";

    private static final String CLASSIFIED_OBJECT = "classifiedObject";

    private static final String CLASSIFICATION_SCHEME = "classificationScheme";

    private static final String NODE_REPRESENTATION = "nodeRepresentation";

    private static final String CLASSIFICATION_KEY = "Classification";

    private RegistryPackageWebConverter() {
    }

    public static Map<String, Object> getRegistryObjectWebMap(RegistryObjectType registryObject) {
        Map<String, Object> registryObjectMap = new HashMap<>();

        putTopLevel(registryObject, registryObjectMap);

        if (registryObject instanceof RegistryPackageType) {
            putRegistryPackage((RegistryPackageType) registryObject, registryObjectMap);
        } else if (registryObject instanceof ExtrinsicObjectType) {
            putExtrinsicObject((ExtrinsicObjectType) registryObject, registryObjectMap);
        } else if (registryObject instanceof ServiceType) {
            putRegistryService((ServiceType) registryObject, registryObjectMap);
        } else if (registryObject instanceof OrganizationType) {
            putRegistryOrganization((OrganizationType) registryObject, registryObjectMap);
        } else if (registryObject instanceof PersonType) {
            putRegistryPerson((PersonType) registryObject, registryObjectMap);
        } else if (registryObject instanceof AssociationType1) {
            putRegistryAssociation((AssociationType1) registryObject, registryObjectMap);
        }

        return registryObjectMap;
    }

    public static RegistryPackageType getRegistryPackageFromWebMap(
            Map<String, Object> registryMap) {
        if (registryMap == null) {
            return null;
        }

        RegistryPackageType registryPackage = RIM_FACTORY.createRegistryPackageType();

        populateTopLevel(registryMap, registryPackage);

        if (registryMap.containsKey(REGISTRY_OBJECT_LIST_KEY)) {
            populateRegistryObjectList((Map<String, Object>) registryMap.get(
                    REGISTRY_OBJECT_LIST_KEY), registryPackage);
        }

        if (isRegistryPackageEmpty(registryPackage)) {
            registryPackage = null;
        }

        return registryPackage;
    }

    private static void putTopLevel(RegistryObjectType registryObject,
            Map<String, Object> registryMap) {

        putGeneralInfo(registryObject, registryMap);
    }

    private static void populateTopLevel(Map<String, Object> registryMap,
            RegistryObjectType registryPackage) {
        populateGeneralInfo(registryMap, registryPackage);
    }

    private static void putRegistryPackage(RegistryPackageType registryPackage,
            Map<String, Object> registryMap) {
        if (registryPackage.isSetRegistryObjectList()) {
            putRegistryObjectList(registryPackage.getRegistryObjectList(), registryMap);
        }

        if (registryPackage.isSetSlot()) {
            putSlotList(registryPackage.getSlot(), registryMap);
        }
    }

    private static void putRegistryObjectList(RegistryObjectListType registryObjects,
            Map<String, Object> registryObjectMap) {

        Map<String, Object> registryObjectListMap = new HashMap<>();

        for (JAXBElement identifiable : registryObjects.getIdentifiable()) {
            RegistryObjectType registryObject = (RegistryObjectType) identifiable.getValue();

            if (registryObject instanceof ExtrinsicObjectType) {
                putExtrinsicObject((ExtrinsicObjectType) registryObject, registryObjectListMap);
            } else if (registryObject instanceof ServiceType) {
                putRegistryService((ServiceType) registryObject, registryObjectListMap);
            } else if (registryObject instanceof OrganizationType) {
                putRegistryOrganization((OrganizationType) registryObject, registryObjectListMap);
            } else if (registryObject instanceof PersonType) {
                putRegistryPerson((PersonType) registryObject, registryObjectListMap);
            } else if (registryObject instanceof AssociationType1) {
                putRegistryAssociation((AssociationType1) registryObject, registryObjectListMap);
            }

            if (!registryObjectListMap.isEmpty()) {
                registryObjectMap.put(REGISTRY_OBJECT_LIST_KEY, registryObjectListMap);
            }
        }

    }

    private static void populateRegistryObjectList(Map<String, Object> rolMap,
            RegistryPackageType registryPackage) {
        if (rolMap.isEmpty()) {
            return;
        }

        RegistryObjectListType registryObjectList = RIM_FACTORY.createRegistryObjectListType();

        if (rolMap.containsKey(EXTRINSIC_OBJECT_KEY)) {
            populateROLWithExtrinsicObject((List<Map<String, Object>>) rolMap.get(
                    EXTRINSIC_OBJECT_KEY), registryObjectList);

        }

        if (rolMap.containsKey(SERVICE_KEY)) {
            populateROLWithService((List<Map<String, Object>>) rolMap.get(SERVICE_KEY),
                    registryObjectList);
        }

        if (rolMap.containsKey(ORGANIZATION_KEY)) {
            populateROLWithOrganization((List<Map<String, Object>>) rolMap.get(ORGANIZATION_KEY),
                    registryObjectList);
        }

        if (rolMap.containsKey(PERSON_KEY)) {
            populateROLWithPerson((List<Map<String, Object>>) rolMap.get(PERSON_KEY),
                    registryObjectList);
        }

        if (rolMap.containsKey(ASSOCIATION_KEY)) {
            populateROLWithAssociation((List<Map<String, Object>>) rolMap.get(ASSOCIATION_KEY),
                    registryObjectList);
        }

        if (CollectionUtils.isNotEmpty(registryObjectList.getIdentifiable())) {
            registryPackage.setRegistryObjectList(registryObjectList);
        }

    }

    private static void putExtrinsicObject(ExtrinsicObjectType extrinsicObject,
            Map<String, Object> registryObjectListMap) {
        if (extrinsicObject == null) {
            return;
        }

        Map<String, Object> extrinsicObjectMap = new HashMap<>();

        putGeneralInfo(extrinsicObject, extrinsicObjectMap);

        if (extrinsicObject.isSetContentVersionInfo()) {
            extrinsicObjectMap.put(EXTRINSIC_OBJECT_CONTENT_VERSION,
                    extrinsicObject.getContentVersionInfo()
                            .getVersionName());
        }

        if (extrinsicObject.isSetIsOpaque()) {
            extrinsicObjectMap.put(EXTRINSIC_OBJECT_OPAQUE, extrinsicObject.isIsOpaque());
        }

        if (extrinsicObject.isSetMimeType()) {
            extrinsicObjectMap.put(EXTRINSIC_OBJECT_MIME_TYPE, extrinsicObject.getMimeType());
        }

        if (extrinsicObject.isSetExternalIdentifier()) {
            putExternalIdentifier(extrinsicObject.getExternalIdentifier(), extrinsicObjectMap);
        }

        if (!extrinsicObjectMap.isEmpty()) {
            registryObjectListMap.putIfAbsent(EXTRINSIC_OBJECT_KEY,
                    new ArrayList<Map<String, Object>>());
            ((List) registryObjectListMap.get(EXTRINSIC_OBJECT_KEY)).add(extrinsicObjectMap);
        }
    }

    private static void populateROLWithExtrinsicObject(
            List<Map<String, Object>> extrinsicObjectsMapList,
            RegistryObjectListType registryObjectList) {
        for (Map<String, Object> extrinsicMap : extrinsicObjectsMapList) {
            ExtrinsicObjectType extrinsicObject = getExtrinsicObjectFromMap(extrinsicMap);

            if (extrinsicObject != null) {
                registryObjectList.getIdentifiable()
                        .add(RIM_FACTORY.createExtrinsicObject(extrinsicObject));
            }
        }
    }

    private static void populateROLWithService(List<Map<String, Object>> serviceMapList,
            RegistryObjectListType registryObjectList) {
        for (Map<String, Object> serviceMap : serviceMapList) {
            ServiceType service = getServiceFromMap(serviceMap);

            if (service != null) {
                registryObjectList.getIdentifiable()
                        .add(RIM_FACTORY.createService(service));
            }
        }
    }

    private static void populateROLWithOrganization(List<Map<String, Object>> organizationMapList,
            RegistryObjectListType registryObjectList) {
        for (Map<String, Object> organizationMap : organizationMapList) {
            OrganizationType organization = getOrganizationFromMap(organizationMap);

            if (organization != null) {
                registryObjectList.getIdentifiable()
                        .add(RIM_FACTORY.createOrganization(organization));
            }
        }
    }

    private static void populateROLWithPerson(List<Map<String, Object>> mapList,
            RegistryObjectListType registryObjectList) {
        for (Map<String, Object> personMap : mapList) {
            PersonType person = getPersonFromMap(personMap);

            if (person != null) {
                registryObjectList.getIdentifiable()
                        .add(RIM_FACTORY.createPerson(person));
            }
        }
    }

    private static void populateROLWithAssociation(List<Map<String, Object>> mapList,
            RegistryObjectListType registryObjectList) {
        for (Map<String, Object> associationMap : mapList) {
            AssociationType1 association = getAssociationFromMap(associationMap);

            if (association != null) {
                registryObjectList.getIdentifiable()
                        .add(RIM_FACTORY.createAssociation(association));
            }
        }
    }

    private static AssociationType1 getAssociationFromMap(Map<String, Object> map) {
        if (map == null) {
            return null;
        }

        AssociationType1 association = RIM_FACTORY.createAssociationType1();
        boolean populated = populateGeneralInfo(map, association);

        String valueToPopulate = getStringFromMap(ASSOCIATION_TYPE, map);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            association.setAssociationType(valueToPopulate);
            populated = true;
        }

        valueToPopulate = getStringFromMap(ASSOCIATION_SOURCE_OBJECT, map);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            association.setSourceObject(valueToPopulate);
            populated = true;
        }

        valueToPopulate = getStringFromMap(ASSOCIATION_TARGET_OBJECT, map);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            association.setTargetObject(valueToPopulate);
            populated = true;
        }

        if (!populated) {
            association = null;
        }

        return association;
    }

    private static PersonType getPersonFromMap(Map<String, Object> map) {
        if (map == null) {
            return null;
        }

        PersonType person = RIM_FACTORY.createPersonType();
        populateGeneralInfo(map, person);

        if (map.containsKey(ADDRESS_KEY)) {
            populateAddressList((List<Map<String, Object>>) map.get(ADDRESS_KEY),
                    person.getAddress());
        }

        if (map.containsKey(EMAIL_ADDRESS_KEY)) {
            populateEmailAddressList((List<Map<String, Object>>) map.get(EMAIL_ADDRESS_KEY),
                    person.getEmailAddress());
        }

        if (map.containsKey(PERSON_NAME_KEY)) {
            person.setPersonName(getPersonNameFromMap((Map<String, Object>) map.get(PERSON_NAME_KEY)));
        }

        if (map.containsKey(TELEPHONE_KEY)) {
            populateTelephoneList((List<Map<String, Object>>) map.get(TELEPHONE_KEY),
                    person.getTelephoneNumber());
        }

        return person;
    }

    private static ExtrinsicObjectType getExtrinsicObjectFromMap(Map<String, Object> map) {
        if (map == null) {
            return null;
        }

        ExtrinsicObjectType extrinsicObject = RIM_FACTORY.createExtrinsicObjectType();
        boolean populated = populateGeneralInfo(map, extrinsicObject);

        String valueToPopulate = getStringFromMap(EXTRINSIC_OBJECT_CONTENT_VERSION, map);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            VersionInfoType contentVersionInfo = RIM_FACTORY.createVersionInfoType();
            contentVersionInfo.setVersionName(valueToPopulate);

            extrinsicObject.setContentVersionInfo(contentVersionInfo);
            populated = true;
        }

        valueToPopulate = getStringFromMap(EXTRINSIC_OBJECT_MIME_TYPE, map);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            extrinsicObject.setMimeType(valueToPopulate);
            populated = true;
        }

        Boolean booleanToPopulate = getBooleanFromMap(EXTRINSIC_OBJECT_OPAQUE, map);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            extrinsicObject.setIsOpaque(booleanToPopulate);
            populated = true;
        }

        if (!populated) {
            extrinsicObject = null;
        }

        return extrinsicObject;
    }

    private static ServiceType getServiceFromMap(Map<String, Object> map) {
        if (map == null) {
            return null;
        }

        ServiceType service = RIM_FACTORY.createServiceType();
        boolean populated = populateGeneralInfo(map, service);

        if (map.containsKey(SERVICE_BINDING_KEY)) {
            populateServiceBindingList((List<Map<String, Object>>) map.get(SERVICE_BINDING_KEY),
                    service.getServiceBinding());
            if (!service.getServiceBinding()
                    .isEmpty()) {
                populated = true;
            }
        }

        if (!populated) {
            service = null;
        }

        return service;
    }

    private static OrganizationType getOrganizationFromMap(Map<String, Object> map) {
        if (map == null) {
            return null;
        }

        OrganizationType organization = RIM_FACTORY.createOrganizationType();
        populateGeneralInfo(map, organization);

        if (map.containsKey(ADDRESS_KEY)) {
            populateAddressList((List<Map<String, Object>>) map.get(ADDRESS_KEY),
                    organization.getAddress());
        }

        if (map.containsKey(EMAIL_ADDRESS_KEY)) {
            populateEmailAddressList((List<Map<String, Object>>) map.get(EMAIL_ADDRESS_KEY),
                    organization.getEmailAddress());
        }

        String valueToPopulate = getStringFromMap(PARENT_KEY, map);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            organization.setParent(valueToPopulate);
        }

        valueToPopulate = getStringFromMap(PRIMARY_CONTACT, map);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            organization.setPrimaryContact(valueToPopulate);
        }

        if (map.containsKey(TELEPHONE_KEY)) {
            populateTelephoneList((List<Map<String, Object>>) map.get(TELEPHONE_KEY),
                    organization.getTelephoneNumber());
        }

        return organization;
    }

    private static void populateTelephoneList(List<Map<String, Object>> mapList,
            List<TelephoneNumberType> telephoneNumberList) {
        for (Map<String, Object> map : mapList) {
            boolean populated = false;

            TelephoneNumberType telephoneNumber = RIM_FACTORY.createTelephoneNumberType();

            String valueToPopulate = getStringFromMap(PHONE_AREA_CODE, map);
            if (StringUtils.isNotBlank(valueToPopulate)) {
                telephoneNumber.setAreaCode(valueToPopulate);
                populated = true;
            }

            valueToPopulate = getStringFromMap(PHONE_COUNTRY_CODE, map);
            if (StringUtils.isNotBlank(valueToPopulate)) {
                telephoneNumber.setCountryCode(valueToPopulate);
                populated = true;
            }

            valueToPopulate = getStringFromMap(PHONE_EXTENSION, map);
            if (StringUtils.isNotBlank(valueToPopulate)) {
                telephoneNumber.setExtension(valueToPopulate);
                populated = true;
            }

            valueToPopulate = getStringFromMap(PHONE_NUMBER, map);
            if (StringUtils.isNotBlank(valueToPopulate)) {
                telephoneNumber.setNumber(valueToPopulate);
                populated = true;
            }

            valueToPopulate = getStringFromMap(PHONE_TYPE, map);
            if (StringUtils.isNotBlank(valueToPopulate)) {
                telephoneNumber.setPhoneType(valueToPopulate);
                populated = true;
            }

            if (populated) {
                telephoneNumberList.add(telephoneNumber);
            }
        }
    }

    private static void populateEmailAddressList(List<Map<String, Object>> mapList,
            List<EmailAddressType> emailAddressList) {
        for (Map<String, Object> map : mapList) {
            boolean populated = false;
            EmailAddressType emailAddress = RIM_FACTORY.createEmailAddressType();

            String valueToPopulate = getStringFromMap(EMAIL_ADDRESS, map);
            if (StringUtils.isNotBlank(valueToPopulate)) {
                emailAddress.setAddress(valueToPopulate);
                populated = true;
            }

            valueToPopulate = getStringFromMap(EMAIL_TYPE, map);
            if (StringUtils.isNotBlank(valueToPopulate)) {
                emailAddress.setType(valueToPopulate);
                populated = true;
            }

            if (populated) {
                emailAddressList.add(emailAddress);
            }
        }
    }

    private static void populateAddressList(List<Map<String, Object>> mapList,
            List<PostalAddressType> addressList) {
        for (Map<String, Object> map : mapList) {
            boolean populated = false;
            PostalAddressType address = RIM_FACTORY.createPostalAddressType();

            String valueToPopulate = getStringFromMap(ADDRESS_CITY, map);
            if (StringUtils.isNotBlank(valueToPopulate)) {
                address.setCity(valueToPopulate);
                populated = true;
            }

            valueToPopulate = getStringFromMap(ADDRESS_COUNTRY, map);
            if (StringUtils.isNotBlank(valueToPopulate)) {
                address.setCountry(valueToPopulate);
                populated = true;
            }

            valueToPopulate = getStringFromMap(ADDRESS_POSTAL_CODE, map);
            if (StringUtils.isNotBlank(valueToPopulate)) {
                address.setPostalCode(valueToPopulate);
                populated = true;
            }

            valueToPopulate = getStringFromMap(ADDRESS_STATE_OR_PROVINCE, map);
            if (StringUtils.isNotBlank(valueToPopulate)) {
                address.setStateOrProvince(valueToPopulate);
                populated = true;
            }

            valueToPopulate = getStringFromMap(ADDRESS_STREET, map);
            if (StringUtils.isNotBlank(valueToPopulate)) {
                address.setStreet(valueToPopulate);
                populated = true;
            }

            valueToPopulate = getStringFromMap(ADDRESS_STREET_NUMBER, map);
            if (StringUtils.isNotBlank(valueToPopulate)) {
                address.setStreetNumber(valueToPopulate);
                populated = true;
            }

            if (populated) {
                addressList.add(address);
            }
        }
    }

    private static void populateServiceBindingList(List<Map<String, Object>> mapList,
            List<ServiceBindingType> serviceBindings) {
        for (Map<String, Object> map : mapList) {
            ServiceBindingType serviceBinding = RIM_FACTORY.createServiceBindingType();

            boolean populated = populateGeneralInfo(map, serviceBinding);

            String valueToPopulate = getStringFromMap(BINDING_ACCESS_URI, map);
            if (StringUtils.isNotBlank(valueToPopulate)) {
                serviceBinding.setAccessURI(valueToPopulate);
                populated = true;
            }

            valueToPopulate = getStringFromMap(BINDING_SERVICE, map);
            if (StringUtils.isNotBlank(valueToPopulate)) {
                serviceBinding.setService(valueToPopulate);
                populated = true;
            }

            if (map.containsKey(SPECIFICATION_LINK_KEY)) {
                populateSpecificationLinkList((List<Map<String, Object>>) map.get(
                        SPECIFICATION_LINK_KEY), serviceBinding.getSpecificationLink());
                if (!serviceBinding.getSpecificationLink()
                        .isEmpty()) {
                    populated = true;
                }
            }

            valueToPopulate = getStringFromMap(BINDING_TARGET_BINDING, map);
            if (StringUtils.isNotBlank(valueToPopulate)) {
                serviceBinding.setTargetBinding(valueToPopulate);
                populated = true;
            }

            if (populated) {
                serviceBindings.add(serviceBinding);
            }
        }
    }

    private static void populateSpecificationLinkList(List<Map<String, Object>> mapList,
            List<SpecificationLinkType> specificationLinkList) {
        for (Map<String, Object> map : mapList) {
            boolean populated = false;
            SpecificationLinkType specificationLink = RIM_FACTORY.createSpecificationLinkType();
            populateGeneralInfo(map, specificationLink);

            String valueToPopulate = getStringFromMap(SPECIFICATION_LINK_SERVICE_BINDING, map);
            if (StringUtils.isNotBlank(valueToPopulate)) {
                specificationLink.setServiceBinding(valueToPopulate);
                populated = true;
            }

            valueToPopulate = getStringFromMap(SPECIFICATION_LINK_SPECIFICATION_OBJECT, map);
            if (StringUtils.isNotBlank(valueToPopulate)) {
                specificationLink.setSpecificationObject(valueToPopulate);
                populated = true;
            }

            valueToPopulate = getStringFromMap(SPECIFICATION_LINK_USAGE_DESCRIPTION, map);
            if (StringUtils.isNotBlank(valueToPopulate)) {
                InternationalStringType istToPopulate = getISTFromString(valueToPopulate);
                if (istToPopulate != null) {
                    specificationLink.setUsageDescription(istToPopulate);
                    populated = true;
                }
            }

            if (map.containsKey(SPECIFICATION_LINK_USAGE_PARAMETERS)) {
                specificationLink.getUsageParameter()
                        .addAll(getStringListFromMap(map, SPECIFICATION_LINK_USAGE_PARAMETERS));
            }

            if (populated) {
                specificationLinkList.add(specificationLink);
            }
        }
    }

    private static void populateExternalIdentifierList(List<Map<String, Object>> mapList,
            List<ExternalIdentifierType> externalIdentifierList) {
        if (mapList == null) {
            return;
        }

        for (Map<String, Object> extIdMap : mapList) {
            boolean populated = false;
            ExternalIdentifierType extId = RIM_FACTORY.createExternalIdentifierType();
            populateGeneralInfo(extIdMap, extId);

            String valueToPopulate = getStringFromMap(EXT_ID_IDENTIFICATION_SCHEME, extIdMap);
            if (StringUtils.isNotBlank(valueToPopulate)) {
                extId.setIdentificationScheme(valueToPopulate);
                populated = true;
            }

            valueToPopulate = getStringFromMap(EXT_ID_REGISTRY_OBJECT, extIdMap);
            if (StringUtils.isNotBlank(valueToPopulate)) {
                extId.setRegistryObject(valueToPopulate);
                populated = true;
            }

            valueToPopulate = getStringFromMap(EXT_ID_VALUE, extIdMap);
            if (StringUtils.isNotBlank(valueToPopulate)) {
                extId.setValue(valueToPopulate);
                populated = true;
            }

            if (populated) {
                externalIdentifierList.add(extId);
            }
        }
    }

    private static void populateClassificationList(List<Map<String, Object>> mapList,
            List<ClassificationType> classificationList) {
        if (mapList == null) {
            return;
        }

        for (Map<String, Object> map : mapList) {
            boolean populated = false;
            ClassificationType classification = RIM_FACTORY.createClassificationType();
            populateGeneralInfo(map, classification);

            String valueToPopulate = getStringFromMap(CLASSIFICATION_NODE, map);
            if (StringUtils.isNotBlank(valueToPopulate)) {
                classification.setClassificationNode(valueToPopulate);
                populated = true;
            }

            valueToPopulate = getStringFromMap(CLASSIFICATION_SCHEME, map);
            if (StringUtils.isNotBlank(valueToPopulate)) {
                classification.setClassificationScheme(valueToPopulate);
                populated = true;
            }

            valueToPopulate = getStringFromMap(CLASSIFIED_OBJECT, map);
            if (StringUtils.isNotBlank(valueToPopulate)) {
                classification.setClassifiedObject(valueToPopulate);
                populated = true;
            }

            valueToPopulate = getStringFromMap(NODE_REPRESENTATION, map);
            if (StringUtils.isNotBlank(valueToPopulate)) {
                classification.setNodeRepresentation(valueToPopulate);
                populated = true;
            }

            if (populated) {
                classificationList.add(classification);
            }
        }
    }

    private static void putRegistryAssociation(AssociationType1 association,
            Map<String, Object> registryObjectListMap) {
        if (association == null) {
            return;
        }
        Map<String, Object> associationMap = new HashMap<>();

        putGeneralInfo(association, associationMap);

        if (association.isSetAssociationType()) {
            associationMap.put(ASSOCIATION_TYPE, association.getAssociationType());
        }

        if (association.isSetSourceObject()) {
            associationMap.put(ASSOCIATION_SOURCE_OBJECT, association.getSourceObject());
        }

        if (association.isSetTargetObject()) {
            associationMap.put(ASSOCIATION_TARGET_OBJECT, association.getTargetObject());
        }

        if (!associationMap.isEmpty()) {
            registryObjectListMap.putIfAbsent(ASSOCIATION_KEY,
                    new ArrayList<Map<String, Object>>());
            ((List) registryObjectListMap.get(ASSOCIATION_KEY)).add(associationMap);
        }
    }

    private static void putRegistryService(ServiceType service,
            Map<String, Object> registryObjectListMap) {
        if (service == null) {
            return;
        }
        Map<String, Object> serviceMap = new HashMap<>();

        putGeneralInfo(service, serviceMap);

        if (service.isSetServiceBinding()) {
            putServiceBindings(service.getServiceBinding(), serviceMap);
        }

        if (!serviceMap.isEmpty()) {
            registryObjectListMap.putIfAbsent(SERVICE_KEY, new ArrayList<Map<String, Object>>());
            ((List) registryObjectListMap.get(SERVICE_KEY)).add(serviceMap);
        }

    }

    private static void putServiceBindings(List<ServiceBindingType> serviceBindings,
            Map<String, Object> serviceMap) {
        List<Map<String, Object>> webServiceBindings = new ArrayList<>();

        for (ServiceBindingType binding : serviceBindings) {
            Map<String, Object> bindingMap = new HashMap<>();

            putGeneralInfo(binding, bindingMap);

            if (binding.isSetAccessURI()) {
                bindingMap.put(BINDING_ACCESS_URI, binding.getAccessURI());
            }

            if (binding.isSetService()) {
                bindingMap.put(BINDING_SERVICE, binding.getService());
            }

            if (binding.isSetSpecificationLink()) {
                putSpecificationLinks(binding.getSpecificationLink(), bindingMap);
            }

            if (binding.isSetTargetBinding()) {
                bindingMap.put(BINDING_TARGET_BINDING, binding.getTargetBinding());
            }

            webServiceBindings.add(bindingMap);
        }

        if (CollectionUtils.isNotEmpty(webServiceBindings)) {
            serviceMap.put(SERVICE_BINDING_KEY, webServiceBindings);
        }
    }

    private static void putSpecificationLinks(List<SpecificationLinkType> specificationLinks,
            Map<String, Object> serviceBindingMap) {
        List<Map<String, Object>> webSpecificationLinks = new ArrayList<>();

        for (SpecificationLinkType specificationLink : specificationLinks) {
            Map<String, Object> specificationLinkMap = new HashMap<>();

            putGeneralInfo(specificationLink, specificationLinkMap);

            if (specificationLink.isSetServiceBinding()) {
                specificationLinkMap.put(SPECIFICATION_LINK_SERVICE_BINDING,
                        specificationLink.getServiceBinding());
            }

            if (specificationLink.isSetSpecificationObject()) {
                specificationLinkMap.put(SPECIFICATION_LINK_SPECIFICATION_OBJECT,
                        specificationLink.getSpecificationObject());

            }

            if (specificationLink.isSetUsageDescription()) {
                specificationLinkMap.put(SPECIFICATION_LINK_USAGE_DESCRIPTION,
                        getStringFromIST(specificationLink.getUsageDescription()));
            }

            if (specificationLink.isSetUsageParameter()) {
                specificationLinkMap.put(SPECIFICATION_LINK_USAGE_PARAMETERS,
                        specificationLink.getUsageParameter());
            }

            if (!specificationLinkMap.isEmpty()) {
                webSpecificationLinks.add(specificationLinkMap);
            }
        }

        if (CollectionUtils.isNotEmpty(webSpecificationLinks)) {
            serviceBindingMap.put(SPECIFICATION_LINK_KEY, webSpecificationLinks);
        }
    }

    private static void putRegistryOrganization(OrganizationType organization,
            Map<String, Object> registryObjectListMap) {
        if (organization == null) {
            return;
        }
        Map<String, Object> organizationMap = new HashMap<>();

        putGeneralInfo(organization, organizationMap);

        if (organization.isSetAddress()) {
            putAddress(organization.getAddress(), organizationMap);
        }

        if (organization.isSetEmailAddress()) {
            putEmailAddress(organization.getEmailAddress(), organizationMap);
        }

        if (organization.isSetParent()) {
            organizationMap.put(PARENT_KEY, organization.getParent());
        }

        if (organization.isSetPrimaryContact()) {
            organizationMap.put(PRIMARY_CONTACT, organization.getPrimaryContact());
        }

        if (organization.isSetTelephoneNumber()) {
            putTelephoneNumber(organization.getTelephoneNumber(), organizationMap);
        }

        if (!organizationMap.isEmpty()) {
            registryObjectListMap.putIfAbsent(ORGANIZATION_KEY,
                    new ArrayList<Map<String, Object>>());
            ((List) registryObjectListMap.get(ORGANIZATION_KEY)).add(organizationMap);
        }
    }

    private static void putRegistryPerson(PersonType person,
            Map<String, Object> registryObjectListMap) {
        if (person == null) {
            return;
        }

        Map<String, Object> personMap = new HashMap<>();

        putGeneralInfo(person, personMap);

        if (person.isSetAddress()) {
            putAddress(person.getAddress(), personMap);
        }

        if (person.isSetEmailAddress()) {
            putEmailAddress(person.getEmailAddress(), personMap);
        }

        if (person.isSetPersonName()) {
            putPersonName(person.getPersonName(), personMap);
        }

        if (person.isSetTelephoneNumber()) {
            putTelephoneNumber(person.getTelephoneNumber(), personMap);
        }

        if (!personMap.isEmpty()) {
            registryObjectListMap.putIfAbsent(PERSON_KEY, new ArrayList<Map<String, Object>>());
            ((List) registryObjectListMap.get(PERSON_KEY)).add(personMap);
        }
    }

    private static void putExternalIdentifier(List<ExternalIdentifierType> extIds,
            Map<String, Object> map) {
        List<Map<String, Object>> webExternalIds = new ArrayList<>();

        for (ExternalIdentifierType extId : extIds) {
            Map<String, Object> externalIdMap = new HashMap<>();

            putGeneralInfo(extId, externalIdMap);

            if (extId.isSetIdentificationScheme()) {
                externalIdMap.put(EXT_ID_IDENTIFICATION_SCHEME, extId.getIdentificationScheme());
            }

            if (extId.isSetRegistryObject()) {
                externalIdMap.put(EXT_ID_REGISTRY_OBJECT, extId.getRegistryObject());
            }

            if (extId.isSetValue()) {
                externalIdMap.put(EXT_ID_VALUE, extId.getValue());
            }

            if (!externalIdMap.isEmpty()) {
                webExternalIds.add(externalIdMap);
            }
        }

        if (CollectionUtils.isNotEmpty(webExternalIds)) {
            map.put(EXTERNAL_IDENTIFIER_KEY, webExternalIds);
        }
    }

    private static void putPersonName(PersonNameType personName, Map<String, Object> personMap) {
        Map<String, Object> personNameParts = new HashMap<>();

        if (personName.isSetFirstName()) {
            personNameParts.put(PERSON_FIRST_NAME, personName.getFirstName());
        }

        if (personName.isSetMiddleName()) {
            personNameParts.put(PERSON_MIDDLE_NAME, personName.getMiddleName());
        }

        if (personName.isSetLastName()) {
            personNameParts.put(PERSON_LAST_NAME, personName.getLastName());
        }

        if (!personNameParts.isEmpty()) {
            personMap.put(PERSON_NAME_KEY, personNameParts);
        }
    }

    private static PersonNameType getPersonNameFromMap(Map<String, Object> map) {
        if (map == null) {
            return null;
        }

        boolean populated = false;
        PersonNameType personName = RIM_FACTORY.createPersonNameType();

        String valueToPopulate = getStringFromMap(PERSON_FIRST_NAME, map);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            personName.setFirstName(valueToPopulate);
            populated = true;
        }

        valueToPopulate = getStringFromMap(PERSON_LAST_NAME, map);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            personName.setLastName(valueToPopulate);
            populated = true;
        }

        valueToPopulate = getStringFromMap(PERSON_MIDDLE_NAME, map);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            personName.setMiddleName(valueToPopulate);
            populated = true;
        }

        if (!populated) {
            personName = null;
        }

        return personName;
    }

    private static void putEmailAddress(List<EmailAddressType> emailAddresses,
            Map<String, Object> organizationMap) {
        List<Map<String, Object>> webEmailAddresses = new ArrayList<>();

        for (EmailAddressType emailAddress : emailAddresses) {
            Map<String, Object> emailAddressMap = new HashMap<>();

            if (emailAddress.isSetAddress()) {
                emailAddressMap.put(EMAIL_ADDRESS, emailAddress.getAddress());
            }

            if (emailAddress.isSetType()) {
                emailAddressMap.put(EMAIL_TYPE, emailAddress.getType());
            }

            if (!emailAddressMap.isEmpty()) {
                webEmailAddresses.add(emailAddressMap);
            }
        }

        if (CollectionUtils.isNotEmpty(webEmailAddresses)) {
            organizationMap.put(EMAIL_ADDRESS_KEY, webEmailAddresses);
        }
    }

    private static void putTelephoneNumber(List<TelephoneNumberType> phoneNumbers,
            Map<String, Object> organizationMap) {
        List<Map<String, Object>> webPhoneNumbers = new ArrayList<>();

        for (TelephoneNumberType phoneNumber : phoneNumbers) {
            Map<String, Object> phoneNumberMap = new HashMap<>();

            if (phoneNumber.isSetAreaCode()) {
                phoneNumberMap.put(PHONE_AREA_CODE, phoneNumber.getAreaCode());
            }

            if (phoneNumber.isSetCountryCode()) {
                phoneNumberMap.put(PHONE_COUNTRY_CODE, phoneNumber.getCountryCode());
            }

            if (phoneNumber.isSetExtension()) {
                phoneNumberMap.put(PHONE_EXTENSION, phoneNumber.getExtension());
            }

            if (phoneNumber.isSetNumber()) {
                phoneNumberMap.put(PHONE_NUMBER, phoneNumber.getNumber());
            }

            if (phoneNumber.isSetPhoneType()) {
                phoneNumberMap.putIfAbsent(PHONE_TYPE, phoneNumber.getPhoneType());
            }

            if (!phoneNumberMap.isEmpty()) {
                webPhoneNumbers.add(phoneNumberMap);
            }
        }

        if (CollectionUtils.isNotEmpty(webPhoneNumbers)) {
            organizationMap.put(TELEPHONE_KEY, webPhoneNumbers);
        }
    }

    private static void putAddress(List<PostalAddressType> addresses,
            Map<String, Object> organizationMap) {
        List<Map<String, Object>> webAddresses = new ArrayList<>();

        for (PostalAddressType postalAddress : addresses) {
            Map<String, Object> addressMap = new HashMap<>();

            if (postalAddress.isSetCity()) {
                addressMap.put(ADDRESS_CITY, postalAddress.getCity());
            }

            if (postalAddress.isSetCountry()) {
                addressMap.put(ADDRESS_COUNTRY, postalAddress.getCountry());
            }

            if (postalAddress.isSetPostalCode()) {
                addressMap.put(ADDRESS_POSTAL_CODE, postalAddress.getPostalCode());
            }

            if (postalAddress.isSetStateOrProvince()) {
                addressMap.put(ADDRESS_STATE_OR_PROVINCE, postalAddress.getStateOrProvince());
            }

            if (postalAddress.isSetStreet()) {
                addressMap.put(ADDRESS_STREET, postalAddress.getStreet());
            }

            if (postalAddress.isSetStreetNumber()) {
                addressMap.put(ADDRESS_STREET_NUMBER, postalAddress.getStreet());
            }

            if (!addressMap.isEmpty()) {
                webAddresses.add(addressMap);
            }
        }

        if (CollectionUtils.isNotEmpty(webAddresses)) {
            organizationMap.put(ADDRESS_KEY, webAddresses);
        }
    }

    private static void putClassification(List<ClassificationType> classifications,
            Map<String, Object> map) {
        List<Map<String, Object>> webClassifications = new ArrayList<>();

        for (ClassificationType classification : classifications) {
            Map<String, Object> classificationMap = new HashMap<>();

            putGeneralInfo(classification, classificationMap);

            if (classification.isSetClassificationNode()) {
                classificationMap.put(CLASSIFICATION_NODE, classification.getClassificationNode());
            }

            if (classification.isSetClassificationScheme()) {
                classificationMap.put(CLASSIFICATION_SCHEME,
                        classification.getClassificationScheme());
            }

            if (classification.isSetClassifiedObject()) {
                classificationMap.put(CLASSIFIED_OBJECT, classification.getClassifiedObject());
            }

            if (classification.isSetNodeRepresentation()) {
                classificationMap.put(NODE_REPRESENTATION, classification.getNodeRepresentation());
            }

            if (!classificationMap.isEmpty()) {
                webClassifications.add(classificationMap);
            }
        }

        if (CollectionUtils.isNotEmpty(webClassifications)) {
            map.put(CLASSIFICATION_KEY, webClassifications);
        }
    }

    private static void putGeneralInfo(RegistryObjectType registryObject, Map<String, Object> map) {
        if (registryObject.isSetClassification()) {
            putClassification(registryObject.getClassification(), map);
        }

        if (registryObject.isSetDescription()) {
            putStringValue(DESCRIPTION_KEY, getStringFromIST(registryObject.getDescription()), map);
        }

        if (registryObject.isSetExternalIdentifier()) {
            putExternalIdentifier(registryObject.getExternalIdentifier(), map);
        }

        if (registryObject.isSetHome()) {
            putStringValue(HOME_KEY, registryObject.getHome(), map);
        }

        if (registryObject.isSetId()) {
            putStringValue(ID_KEY, registryObject.getId(), map);
        }

        if (registryObject.isSetLid()) {
            putStringValue(LID_KEY, registryObject.getLid(), map);
        }

        if (registryObject.isSetName()) {
            putStringValue(NAME_KEY, getStringFromIST(registryObject.getName()), map);
        }

        if (registryObject.isSetObjectType()) {
            putStringValue(OBJECT_TYPE_KEY, registryObject.getObjectType(), map);
        }

        if (registryObject.isSetSlot()) {
            putSlotList(registryObject.getSlot(), map);
        }

        if (registryObject.isSetStatus()) {
            putStringValue(STATUS_KEY, registryObject.getStatus(), map);
        }

        if (registryObject.isSetVersionInfo()) {
            String versionName = registryObject.getVersionInfo()
                    .getVersionName();
            putStringValue(VERSION_INFO_KEY, versionName, map);

        }

    }

    private static boolean populateGeneralInfo(Map<String, Object> map,
            RegistryObjectType registryObject) {
        boolean populated = false;
        if (map.containsKey(CLASSIFICATION_KEY)) {
            populateClassificationList((List<Map<String, Object>>) map.get(CLASSIFICATION_KEY),
                    registryObject.getClassification());
            if (!registryObject.getClassification()
                    .isEmpty()) {
                populated = true;
            }
        }

        String valueToPopulate = getStringFromMap(DESCRIPTION_KEY, map);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            InternationalStringType istToPopulate = getISTFromString(valueToPopulate);
            if (istToPopulate != null) {
                registryObject.setDescription(istToPopulate);
                populated = true;
            }
        }

        if (map.containsKey(EXTERNAL_IDENTIFIER_KEY)) {
            populateExternalIdentifierList((List<Map<String, Object>>) map.get(
                    EXTERNAL_IDENTIFIER_KEY), registryObject.getExternalIdentifier());
            if (!registryObject.getExternalIdentifier()
                    .isEmpty()) {
                populated = true;
            }
        }

        valueToPopulate = getStringFromMap(HOME_KEY, map);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            registryObject.setHome(valueToPopulate);
            populated = true;
        }

        valueToPopulate = getStringFromMap(ID_KEY, map);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            registryObject.setId(valueToPopulate);
            populated = true;
        }

        valueToPopulate = getStringFromMap(LID_KEY, map);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            registryObject.setLid(valueToPopulate);
            populated = true;
        }

        valueToPopulate = getStringFromMap(NAME_KEY, map);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            InternationalStringType istToPopulate = getISTFromString(valueToPopulate);
            if (istToPopulate != null) {
                registryObject.setName(istToPopulate);
                populated = true;
            }
        }

        valueToPopulate = getStringFromMap(OBJECT_TYPE_KEY, map);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            registryObject.setObjectType(valueToPopulate);
            populated = true;
        }

        if (map.containsKey(SLOT)) {
            populateSlotList((List<Map<String, Object>>) map.get(SLOT), registryObject.getSlot());
            if (!registryObject.getSlot()
                    .isEmpty()) {
                populated = true;
            }
        }

        valueToPopulate = getStringFromMap(STATUS_KEY, map);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            registryObject.setStatus(valueToPopulate);
            populated = true;
        }

        valueToPopulate = getStringFromMap(VERSION_INFO_KEY, map);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            VersionInfoType versionInfo = RIM_FACTORY.createVersionInfoType();
            versionInfo.setVersionName(valueToPopulate);

            registryObject.setVersionInfo(versionInfo);
            populated = true;
        }

        return populated;
    }

    private static String getStringFromMap(String key, Map<String, Object> map) {
        String value = null;
        if (map.containsKey(key)) {
            value = (String) map.get(key);
        }

        return value;
    }

    private static BigInteger getBigIntFromMap(String key, Map<String, Object> map) {
        BigInteger value = null;
        if (map.containsKey(key)) {
            // Using String.valueOf so that it can handle Integer and Long the same
            value = new BigInteger(String.valueOf(map.get(key)));
        }

        return value;
    }

    private static Boolean getBooleanFromMap(String key, Map<String, Object> map) {
        Boolean value = null;
        if (map.containsKey(key)) {
            value = (Boolean) map.get(key);
        }

        return value;
    }

    private static void putSlotList(List<SlotType1> slots, Map<String, Object> map) {

        List<Map<String, Object>> slotList = new ArrayList<>();

        for (SlotType1 slot : slots) {
            addSlotMapToList(slot, slotList);
        }

        if (CollectionUtils.isNotEmpty(slotList)) {
            map.put(SLOT, slotList);
        }
    }

    private static void populateSlotList(List<Map<String, Object>> mapList, List<SlotType1> slots) {
        for (Map<String, Object> map : mapList) {
            boolean populated = false;

            SlotType1 slot = RIM_FACTORY.createSlotType1();

            String slotType = getStringFromMap(SLOT_TYPE, map);
            if (StringUtils.isNotBlank(slotType)) {
                if (slotType.contains(XML_GEO_TYPE)) {
                    populateSlotGMPointValue(map, slot);
                } else {
                    populateSlotStringValue(map, slot);
                }

                slot.setSlotType(slotType);
                populated = true;
            } else {
                populateSlotStringValue(map, slot);
                if (!slot.getValueList()
                        .getValue()
                        .getValue()
                        .isEmpty()) {
                    populated = true;
                }
            }

            String name = getStringFromMap(NAME, map);
            if (StringUtils.isNotBlank(name)) {
                slot.setName(name);
                populated = true;
            }

            if (populated) {
                slots.add(slot);
            }
        }
    }

    private static void populateSlotStringValue(Map<String, Object> map, SlotType1 slot) {
        ValueListType valueList = RIM_FACTORY.createValueListType();
        valueList.getValue()
                .addAll(getStringListFromMap(map, VALUE));

        slot.setValueList(RIM_FACTORY.createValueList(valueList));
    }

    private static boolean populateSlotGMPointValue(Map<String, Object> map, SlotType1 slot) {
        boolean populated = false;
        PointType point = GML_FACTORY.createPointType();

        if (map.containsKey(VALUE)) {
            Map<String, Object> valueMap = (Map<String, Object>) map.get(VALUE);

            if (valueMap.containsKey(POINT_KEY)) {
                Map<String, Object> pointMap = (Map<String, Object>) valueMap.get(POINT_KEY);

                BigInteger dimension = getBigIntFromMap(SRS_DIMENSION, pointMap);
                if (dimension != null) {
                    point.setSrsDimension(dimension);
                    populated = true;
                }

                String valueToPopulate = getStringFromMap(SRS_NAME, pointMap);
                if (StringUtils.isNotBlank(valueToPopulate)) {
                    point.setSrsName(valueToPopulate);
                    populated = true;
                }

                valueToPopulate = getStringFromMap(POSITION, pointMap);
                if (StringUtils.isNotBlank(valueToPopulate)) {
                    String[] values = StringUtils.split(valueToPopulate);

                    DirectPositionType directPosition = GML_FACTORY.createDirectPositionType();

                    for (String value : values) {
                        directPosition.getValue()
                                .add(Double.valueOf(value));
                    }

                    point.setPos(directPosition);
                    populated = true;
                }

                if (populated) {
                    AnyValueType anyValue = WRS_FACTORY.createAnyValueType();
                    anyValue.getContent()
                            .add(GML_FACTORY.createPoint(point));

                    net.opengis.cat.wrs.v_1_0_2.ValueListType valueList =
                            WRS_FACTORY.createValueListType();
                    valueList.getAnyValue()
                            .add(anyValue);

                    slot.setValueList(RIM_FACTORY.createValueList(valueList));
                }
            }

        }

        return populated;
    }

    private static List<String> getStringListFromMap(Map<String, Object> map, String key) {
        if (!map.containsKey(key)) {
            return null;
        }

        return (List<String>) map.get(key);
    }

    private static void addSlotMapToList(SlotType1 slot, List<Map<String, Object>> slotList) {
        if (slot.isSetSlotType()) {
            String slotType = slot.getSlotType();

            if (slotType.contains(XML_DATE_TIME_TYPE)) {
                putSlotDateValue(slot, slotList);
            } else if (slotType.contains(XML_GEO_TYPE)) {
                putSlotGeoValue(slot, slotList);
            } else {
                putSlotStringValue(slot, slotList);
            }
        } else {
            putSlotStringValue(slot, slotList);
        }

    }

    private static void putSlotDateValue(SlotType1 slot, List<Map<String, Object>> list) {
        List<Date> dates = getSlotDateValues(slot);

        if (CollectionUtils.isNotEmpty(dates)) {
            Map<String, Object> map = new HashMap<>();
            map.put(NAME, slot.getName());
            map.put(VALUE, dates);
            if (slot.isSetSlotType()) {
                map.put(SLOT_TYPE, slot.getSlotType());
            }

            list.add(map);
        }
    }

    private static List<Date> getSlotDateValues(SlotType1 slot) {
        List<Date> dates = new ArrayList<>();

        if (slot.isSetValueList()) {
            ValueListType valueList = slot.getValueList()
                    .getValue();

            if (valueList.isSetValue()) {
                List<String> values = valueList.getValue();

                for (String dateString : values) {
                    Date date = dateOptionalTimeParser().parseDateTime(dateString)
                            .toDate();
                    if (date != null) {
                        dates.add(date);
                    }
                }
            }
        }

        return dates;
    }

    private static List<String> getSlotStringValues(SlotType1 slot) {
        List<String> slotValues = new ArrayList<>();

        if (slot.isSetValueList()) {
            ValueListType valueList = slot.getValueList()
                    .getValue();
            if (valueList.isSetValue()) {
                slotValues = valueList.getValue();
            }
        }

        return slotValues;
    }

    private static void putSlotStringValue(SlotType1 slot, List<Map<String, Object>> list) {
        List<String> stringValues = getSlotStringValues(slot);

        if (CollectionUtils.isNotEmpty(stringValues)) {
            Map<String, Object> map = new HashMap<>();
            map.put(NAME, slot.getName());
            map.put(VALUE, stringValues);
            if (slot.isSetSlotType()) {
                map.put(SLOT_TYPE, slot.getSlotType());
            }

            list.add(map);
        }
    }

    private static void putSlotGeoValue(SlotType1 slot, List<Map<String, Object>> list) {
        if (slot.isSetValueList()) {
            net.opengis.cat.wrs.v_1_0_2.ValueListType valueList =
                    (net.opengis.cat.wrs.v_1_0_2.ValueListType) slot.getValueList()
                            .getValue();

            List<AnyValueType> anyValues = valueList.getAnyValue();

            for (AnyValueType anyValue : anyValues) {
                anyValue.getContent()
                        .stream()
                        .filter(content -> content instanceof JAXBElement)
                        .forEach(content -> {

                            JAXBElement jaxbElement = (JAXBElement) content;

                            if (jaxbElement.getValue() instanceof PointType) {

                                Map<String, Object> pointMap =
                                        getPointMapFromPointType((PointType) jaxbElement.getValue(),
                                                slot);

                                if (!pointMap.isEmpty()) {
                                    list.add(pointMap);
                                }
                            }

                        });
            }

        }
    }

    private static Map<String, Object> getPointMapFromPointType(PointType point, SlotType1 slot) {
        Map<String, Map<String, Object>> pointMap = new HashMap<>();
        Map<String, Object> map = new HashMap<>();
        pointMap.put(POINT_KEY, new HashMap<>());
        pointMap.get(POINT_KEY)
                .put(SRS_DIMENSION,
                        point.getSrsDimension()
                                .intValue());
        pointMap.get(POINT_KEY)
                .put(SRS_NAME, point.getSrsName());

        DirectPositionType directPosition = point.getPos();
        List<String> pointValues = new ArrayList<>();

        pointValues.addAll(directPosition.getValue()
                .stream()
                .map(String::valueOf)
                .collect(Collectors.toList()));

        String position = String.join(" ", pointValues);

        if (StringUtils.isNotBlank(position)) {
            pointMap.get(POINT_KEY)
                    .put(POSITION, position);
        }

        if (!pointMap.isEmpty()) {
            map.put(NAME, slot.getName());
            map.put(VALUE, pointMap);
            if (slot.isSetSlotType()) {
                map.put(SLOT_TYPE, slot.getSlotType());
            }
        }
        return map;
    }

    private static void putStringValue(String key, String value, Map<String, Object> map) {
        if (StringUtils.isNotBlank(value)) {
            map.put(key, value);
        }
    }

    private static String getStringFromIST(InternationalStringType internationalString) {
        String stringValue = "";
        List<LocalizedStringType> localizedStrings = internationalString.getLocalizedString();
        if (CollectionUtils.isNotEmpty(localizedStrings)) {
            LocalizedStringType localizedString = localizedStrings.get(0);
            if (localizedString != null) {
                stringValue = localizedString.getValue();
            }
        }

        return stringValue;
    }

    private static InternationalStringType getISTFromString(String internationalizeThis) {
        if (StringUtils.isBlank(internationalizeThis)) {
            return null;
        }

        InternationalStringType ist = new InternationalStringType();
        LocalizedStringType lst = new LocalizedStringType();

        lst.setValue(internationalizeThis);
        ist.setLocalizedString(Collections.singletonList(lst));

        return ist;
    }

    private static boolean isRegistryPackageEmpty(RegistryPackageType registryPackage) {
        if (registryPackage.isSetClassification()) {
            return false;
        }

        if (registryPackage.isSetId()) {
            return false;
        }

        if (registryPackage.isSetObjectType()) {
            return false;
        }

        if (registryPackage.isSetRegistryObjectList()) {
            return false;
        }

        if (registryPackage.isSetDescription()) {
            return false;
        }

        if (registryPackage.isSetExternalIdentifier()) {
            return false;
        }

        if (registryPackage.isSetHome()) {
            return false;
        }

        if (registryPackage.isSetLid()) {
            return false;
        }

        if (registryPackage.isSetName()) {
            return false;
        }

        if (registryPackage.isSetSlot()) {
            return false;
        }

        if (registryPackage.isSetStatus()) {
            return false;
        }

        if (registryPackage.isSetVersionInfo()) {
            return false;
        }

        return true;
    }

}
