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
 **/
package org.codice.ddf.registry.converter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBElement;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.schemabindings.helper.InternationalStringTypeHelper;
import org.codice.ddf.registry.schemabindings.helper.SlotTypeHelper;
import org.jvnet.jaxb2_commons.locator.DefaultRootObjectLocator;
import org.jvnet.ogc.gml.v_3_1_1.jts.ConversionFailedException;
import org.jvnet.ogc.gml.v_3_1_1.jts.GML311ToJTSGeometryConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import net.opengis.cat.wrs.v_1_0_2.AnyValueType;
import net.opengis.gml.v_3_1_1.AbstractGeometryType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.AssociationType1;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.EmailAddressType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExternalIdentifierType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExtrinsicObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.IdentifiableType;
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
import oasis.names.tc.ebxml_regrep.xsd.rim._3.TelephoneNumberType;

public class RegistryPackageConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryPackageConverter.class);

    private static final String URN_PATTERN_REGEX = "urn:(.*)";

    private static final Pattern URN_PATTERN = Pattern.compile(URN_PATTERN_REGEX);

    private static final Map<String, String> METACARD_XML_NAME_MAP;

    private static final InternationalStringTypeHelper INTERNATIONAL_STRING_TYPE_HELPER =
            new InternationalStringTypeHelper();

    private static final SlotTypeHelper SLOT_TYPE_HELPER = new SlotTypeHelper();

    static {
        METACARD_XML_NAME_MAP = new HashMap<>();
        METACARD_XML_NAME_MAP.put(RegistryObjectMetacardType.LIVE_DATE, "liveDate");
        METACARD_XML_NAME_MAP.put(Metacard.CREATED, "liveDate");
        METACARD_XML_NAME_MAP.put(RegistryObjectMetacardType.DATA_START_DATE, "dataStartDate");
        METACARD_XML_NAME_MAP.put(RegistryObjectMetacardType.DATA_END_DATE, "dataEndDate");
        METACARD_XML_NAME_MAP.put(Metacard.MODIFIED, "lastUpdated");
        METACARD_XML_NAME_MAP.put(RegistryObjectMetacardType.LINKS, "links");

        METACARD_XML_NAME_MAP.put(Metacard.GEOGRAPHY, "location");
        METACARD_XML_NAME_MAP.put(RegistryObjectMetacardType.REGION, "region");
        METACARD_XML_NAME_MAP.put(RegistryObjectMetacardType.DATA_SOURCES, "inputDataSources");
        METACARD_XML_NAME_MAP.put(RegistryObjectMetacardType.DATA_TYPES, "dataTypes");
        METACARD_XML_NAME_MAP.put(RegistryObjectMetacardType.SECURITY_LEVEL, "securityLevel");
        METACARD_XML_NAME_MAP.put(RegistryObjectMetacardType.SERVICE_BINDING_TYPES, "bindingType");
        METACARD_XML_NAME_MAP.put(RegistryObjectMetacardType.SERVICE_BINDINGS, "serviceType");

    }

    private RegistryPackageConverter() {
    }

    public static Metacard getRegistryObjectMetacard(RegistryObjectType registryObject)
            throws RegistryConversionException {
        MetacardImpl metacard = null;

        if (registryObject == null) {
            return metacard;
        }

        validateIdentifiable(registryObject);

        metacard = new MetacardImpl(new RegistryObjectMetacardType());

        parseTopLevel(registryObject, metacard);

        if (registryObject instanceof RegistryPackageType) {
            parseRegistryPackage((RegistryPackageType) registryObject, metacard);
        } else if (registryObject instanceof ExtrinsicObjectType) {
            parseNodeExtrinsicObject(registryObject, metacard);
        } else if (registryObject instanceof ServiceType) {
            parseRegistryService((ServiceType) registryObject, metacard);
        } else if (registryObject instanceof OrganizationType) {
            parseRegistryOrganization((OrganizationType) registryObject, metacard);
        } else if (registryObject instanceof PersonType) {
            parseRegistryPerson((PersonType) registryObject, metacard);
        } else {
            LOGGER.warn("Unexpected object found: {}", registryObject);
        }

        return metacard;
    }

    private static void parseRegistryObjectList(RegistryObjectListType registryObjects,
            MetacardImpl metacard) throws RegistryConversionException {
        Map<String, Set<String>> associations = new HashMap<>();
        Map<String, RegistryObjectType> registryIds = new HashMap<>();
        List<OrganizationType> orgs = new ArrayList<>();
        List<PersonType> contacts = new ArrayList<>();

        String nodeId = "";
        for (JAXBElement identifiable : registryObjects.getIdentifiable()) {
            RegistryObjectType registryObject = (RegistryObjectType) identifiable.getValue();
            registryIds.put(registryObject.getId(), registryObject);
            if (registryObject instanceof ExtrinsicObjectType
                    && RegistryConstants.REGISTRY_NODE_OBJECT_TYPE.equals(registryObject.getObjectType())) {
                nodeId = registryObject.getId();
                parseNodeExtrinsicObject(registryObject, metacard);
            } else if (registryObject instanceof ServiceType
                    && RegistryConstants.REGISTRY_SERVICE_OBJECT_TYPE.equals(registryObject.getObjectType())) {
                parseRegistryService((ServiceType) registryObject, metacard);
            } else if (registryObject instanceof OrganizationType) {
                orgs.add((OrganizationType) registryObject);
            } else if (registryObject instanceof PersonType) {
                contacts.add((PersonType) registryObject);
            } else if (registryObject instanceof AssociationType1) {
                AssociationType1 association = (AssociationType1) registryObject;
                if (associations.containsKey(association.getSourceObject())) {
                    associations.get(association.getSourceObject())
                            .add(association.getTargetObject());
                } else {
                    associations.put(association.getSourceObject(),
                            new HashSet<>(Collections.singleton(association.getTargetObject())));
                }
            }
        }
        boolean orgFound = false;
        boolean contactFound = false;
        if (associations.get(nodeId) != null) {
            Set<String> nodeAssociations = associations.get(nodeId);
            RegistryObjectType ro;
            for (String id : nodeAssociations) {
                ro = registryIds.get(id);
                if (!orgFound && ro != null && ro instanceof OrganizationType) {
                    parseRegistryOrganization((OrganizationType) ro, metacard);
                    orgFound = true;
                } else if (!contactFound && ro != null && ro instanceof PersonType) {
                    parseRegistryPerson((PersonType) ro, metacard);
                    contactFound = true;
                }
            }
        }

        if (!orgFound && !orgs.isEmpty()) {
            parseRegistryOrganization(orgs.get(0), metacard);
        }
        if (!contactFound && !contacts.isEmpty()) {
            parseRegistryPerson(contacts.get(0), metacard);
        }
    }

    private static void parseRegistryOrganization(OrganizationType organization,
            MetacardImpl metacard) throws RegistryConversionException {

        validateIdentifiable(organization);

        if (organization.isSetName()) {
            setMetacardStringAttribute(INTERNATIONAL_STRING_TYPE_HELPER.getString(organization.getName()),
                    RegistryObjectMetacardType.ORGANIZATION_NAME,
                    metacard);
        } else {
            unsetMetacardAttribute(RegistryObjectMetacardType.ORGANIZATION_NAME, metacard);
        }

        if (organization.isSetEmailAddress()) {
            setMetacardEmailAttribute(organization.getEmailAddress(),
                    RegistryObjectMetacardType.ORGANIZATION_EMAIL,
                    metacard);
        } else {
            unsetMetacardAttribute(RegistryObjectMetacardType.ORGANIZATION_EMAIL, metacard);
        }

        if (organization.isSetTelephoneNumber()) {
            setMetacardPhoneNumberAttribute(organization.getTelephoneNumber(),
                    RegistryObjectMetacardType.ORGANIZATION_PHONE_NUMBER,
                    metacard);
        } else {
            unsetMetacardAttribute(RegistryObjectMetacardType.ORGANIZATION_PHONE_NUMBER, metacard);
        }

        if (organization.isSetAddress()) {
            setMetacardAddressAttribute(organization.getAddress(),
                    RegistryObjectMetacardType.ORGANIZATION_ADDRESS,
                    metacard);
        } else {
            unsetMetacardAttribute(RegistryObjectMetacardType.ORGANIZATION_ADDRESS, metacard);
        }
    }

    private static void parseRegistryPackage(RegistryPackageType registryPackage,
            MetacardImpl metacard) throws RegistryConversionException {
        if (registryPackage.isSetRegistryObjectList()) {
            parseRegistryObjectList(registryPackage.getRegistryObjectList(), metacard);
        }
    }

    private static void parseRegistryPerson(PersonType person, MetacardImpl metacard)
            throws RegistryConversionException {

        validateIdentifiable(person);

        String name = "no name";
        String phone = "no telephone number";
        String email = "no email address";

        if (person.isSetPersonName()) {
            PersonNameType personName = person.getPersonName();
            List<String> nameParts = new ArrayList<>();
            if (StringUtils.isNotBlank(personName.getFirstName())) {
                nameParts.add(personName.getFirstName());
            }
            if (StringUtils.isNotBlank(personName.getLastName())) {
                nameParts.add(personName.getLastName());
            }

            if (CollectionUtils.isNotEmpty(nameParts)) {
                name = String.join(" ", nameParts);
            }
        }

        if (person.isSetTelephoneNumber()) {
            List<TelephoneNumberType> phoneNumbers = person.getTelephoneNumber();
            if (CollectionUtils.isNotEmpty(phoneNumbers)) {
                phone = getPhoneNumber(phoneNumbers.get(0));
            }
        }

        if (person.isSetEmailAddress()) {
            List<EmailAddressType> emailAddresses = person.getEmailAddress();

            if (CollectionUtils.isNotEmpty(emailAddresses)) {
                EmailAddressType emailAddress = emailAddresses.get(0);

                if (StringUtils.isNotBlank(emailAddress.getAddress())) {
                    email = emailAddress.getAddress();
                }
            }
        }

        String metacardPoc = String.format("%s, %s, %s", name, phone, email);
        metacard.setAttribute(Metacard.POINT_OF_CONTACT, metacardPoc);

    }

    private static void parseRegistryService(ServiceType service, MetacardImpl metacard)
            throws RegistryConversionException {

        validateIdentifiable(service);

        String xmlServiceBindingsTypesAttributeName = METACARD_XML_NAME_MAP.get(
                RegistryObjectMetacardType.SERVICE_BINDING_TYPES);
        String xmlServiceBindingsAttributeName = METACARD_XML_NAME_MAP.get(
                RegistryObjectMetacardType.SERVICE_BINDINGS);

        List<String> serviceBindings = new ArrayList<>();
        List<String> serviceBindingTypes = new ArrayList<>();

        List<String> bindings = new ArrayList<>();
        List<String> bindingTypes = new ArrayList<>();

        for (ServiceBindingType binding : service.getServiceBinding()) {
            bindings.clear();
            bindingTypes.clear();

            Map<String, List<SlotType1>> slotMap =
                    SLOT_TYPE_HELPER.getNameSlotMapDuplicateSlotNamesAllowed(binding.getSlot());

            if (slotMap.containsKey(xmlServiceBindingsTypesAttributeName)) {

                List<SlotType1> slots = slotMap.get(xmlServiceBindingsTypesAttributeName);

                for (SlotType1 slot : slots) {
                    bindingTypes.addAll(SLOT_TYPE_HELPER.getStringValues(slot));
                }
            }

            if (slotMap.containsKey(xmlServiceBindingsAttributeName)) {

                List<SlotType1> slots = slotMap.get(xmlServiceBindingsAttributeName);
                for (SlotType1 slot : slots) {
                    bindings.addAll(SLOT_TYPE_HELPER.getStringValues(slot));
                }
            }

            serviceBindingTypes.addAll(bindingTypes);
            serviceBindings.addAll(bindings);
        }
        metacard.setAttribute(RegistryObjectMetacardType.SERVICE_BINDING_TYPES,
                (Serializable) serviceBindingTypes);
        metacard.setAttribute(RegistryObjectMetacardType.SERVICE_BINDINGS,
                (Serializable) serviceBindings);
    }

    private static void parseNodeExtrinsicObject(RegistryObjectType registryObject,
            MetacardImpl metacard) throws RegistryConversionException {
        if (CollectionUtils.isNotEmpty(registryObject.getSlot())) {
            Map<String, SlotType1> slotMap =
                    SLOT_TYPE_HELPER.getNameSlotMap(registryObject.getSlot());

            setAttributeFromMap(RegistryObjectMetacardType.LIVE_DATE, slotMap, metacard);
            setAttributeFromMap(Metacard.CREATED, slotMap, metacard);
            setAttributeFromMap(RegistryObjectMetacardType.DATA_START_DATE, slotMap, metacard);
            setAttributeFromMap(RegistryObjectMetacardType.DATA_END_DATE, slotMap, metacard);
            setAttributeFromMap(Metacard.MODIFIED, slotMap, metacard);
            setAttributeFromMap(RegistryObjectMetacardType.LINKS, slotMap, metacard);
            setAttributeFromMap(Metacard.GEOGRAPHY, slotMap, metacard);
            setAttributeFromMap(RegistryObjectMetacardType.REGION, slotMap, metacard);
            setAttributeFromMap(RegistryObjectMetacardType.DATA_SOURCES, slotMap, metacard);
            setAttributeFromMap(RegistryObjectMetacardType.DATA_TYPES, slotMap, metacard);
            setAttributeFromMap(RegistryObjectMetacardType.SECURITY_LEVEL, slotMap, metacard);
        }

        if (registryObject.isSetName()) {
            setMetacardStringAttribute(INTERNATIONAL_STRING_TYPE_HELPER.getString(registryObject.getName()),
                    Metacard.TITLE,
                    metacard);
        }

        if (registryObject.isSetDescription()) {
            setMetacardStringAttribute(INTERNATIONAL_STRING_TYPE_HELPER.getString(registryObject.getDescription()),
                    Metacard.DESCRIPTION,
                    metacard);
        }

        if (registryObject.isSetVersionInfo()) {

            setMetacardStringAttribute(registryObject.getVersionInfo()
                    .getVersionName(), Metacard.CONTENT_TYPE_VERSION, metacard);
        }
    }

    private static void parseTopLevel(RegistryObjectType registryObject, MetacardImpl metacard)
            throws RegistryConversionException {

        if (registryObject.isSetId()) {
            metacard.setAttribute(RegistryObjectMetacardType.REGISTRY_ID, registryObject.getId());
        }

        if (registryObject.isSetObjectType()) {
            String objectType = registryObject.getObjectType();
            Matcher matcher = URN_PATTERN.matcher(objectType);
            if (matcher.find()) {
                objectType = matcher.group(1)
                        .replaceAll(":", ".");
                if (!objectType.startsWith(RegistryConstants.REGISTRY_TAG)) {
                    objectType = String.format("%s.%s", RegistryConstants.REGISTRY_TAG, objectType);
                }
            }
            metacard.setContentTypeName(objectType);
        }

        if (registryObject.isSetName()) {
            setMetacardStringAttribute(INTERNATIONAL_STRING_TYPE_HELPER.getString(registryObject.getName()),
                    Metacard.TITLE,
                    metacard);
        }

        if (registryObject.isSetDescription()) {
            setMetacardStringAttribute(INTERNATIONAL_STRING_TYPE_HELPER.getString(registryObject.getDescription()),
                    Metacard.DESCRIPTION,
                    metacard);
        }

        if (registryObject.isSetVersionInfo()) {

            setMetacardStringAttribute(registryObject.getVersionInfo()
                    .getVersionName(), Metacard.CONTENT_TYPE_VERSION, metacard);
        }

        if (registryObject.isSetExternalIdentifier()) {
            List<ExternalIdentifierType> extIds = registryObject.getExternalIdentifier();
            for (ExternalIdentifierType extId : extIds) {
                if (extId.getId()
                        .equals(RegistryConstants.REGISTRY_MCARD_LOCAL_ID)) {
                    metacard.setId(extId.getValue());
                    break;
                }
            }
        }

        if (registryObject.isSetHome()) {
            metacard.setAttribute(RegistryObjectMetacardType.REGISTRY_BASE_URL,
                    registryObject.getHome());
        }
    }

    private static boolean isAttributeMultiValued(String attribute, MetacardImpl metacard) {
        boolean multiValued = false;
        AttributeDescriptor descriptor = metacard.getMetacardType()
                .getAttributeDescriptor(attribute);

        if (descriptor != null) {
            multiValued = descriptor.isMultiValued();
        }

        return multiValued;
    }

    private static String getPhoneNumber(TelephoneNumberType digits) {
        StringBuilder phoneNumberBuilder = new StringBuilder();

        phoneNumberBuilder = buildNonNullString(phoneNumberBuilder,
                digits.getAreaCode(),
                "(%s)",
                "");
        phoneNumberBuilder = buildNonNullString(phoneNumberBuilder, digits.getNumber(), " ");
        phoneNumberBuilder = buildNonNullString(phoneNumberBuilder,
                digits.getExtension(),
                "extension %s",
                " ");
        return phoneNumberBuilder.toString();
    }

    private static void setMetacardAddressAttribute(List<PostalAddressType> addresses,
            String metacardAttribute, MetacardImpl metacard) {

        if (CollectionUtils.isNotEmpty(addresses)) {
            StringBuilder addressString = new StringBuilder();
            PostalAddressType address = addresses.get(0);

            addressString = buildNonNullString(addressString, address.getStreet(), " ");
            addressString = buildNonNullString(addressString, address.getCity(), ", ");
            addressString = buildNonNullString(addressString, address.getStateOrProvince(), ", ");
            addressString = buildNonNullString(addressString, address.getPostalCode(), " ");
            addressString = buildNonNullString(addressString, address.getCountry(), ", ");
            if (StringUtils.isNotBlank(addressString.toString())) {
                metacard.setAttribute(metacardAttribute, addressString.toString());
            }
        }

    }

    private static void setMetacardStringAttribute(String value, String metacardAttribute,
            MetacardImpl metacard) {
        if (StringUtils.isNotBlank(value)) {
            metacard.setAttribute(metacardAttribute, value);
        }
    }

    private static void setMetacardEmailAttribute(List<EmailAddressType> emailAddresses,
            String metacardAttribute, MetacardImpl metacard) {
        List<String> metacardEmailAddresses = emailAddresses.stream()
                .map(EmailAddressType::getAddress)
                .collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(metacardEmailAddresses)) {
            metacard.setAttribute(metacardAttribute, (Serializable) metacardEmailAddresses);
        }

    }

    private static void setMetacardPhoneNumberAttribute(List<TelephoneNumberType> phoneNumbers,
            String metacardAttribute, MetacardImpl metacard) {

        List<String> metacardPhoneNumbers = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(phoneNumbers)) {
            for (TelephoneNumberType digits : phoneNumbers) {
                metacardPhoneNumbers.add(getPhoneNumber(digits));

                if (CollectionUtils.isNotEmpty(metacardPhoneNumbers)) {
                    metacard.setAttribute(metacardAttribute, (Serializable) metacardPhoneNumbers);
                }
            }
        }
    }

    private static StringBuilder buildNonNullString(StringBuilder stringBuilder,
            String attributeValue, String format, String delimiterBefore) {
        if (StringUtils.isNotBlank(attributeValue)) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append(delimiterBefore);
            }
            if (StringUtils.isNotEmpty(format)) {
                stringBuilder.append(String.format(format, attributeValue));
            } else {
                stringBuilder.append(attributeValue);
            }
        }
        return stringBuilder;
    }

    private static StringBuilder buildNonNullString(StringBuilder stringBuilder,
            String attributeValue, String delimiterBefore) {

        return (buildNonNullString(stringBuilder, attributeValue, "", delimiterBefore));

    }

    private static void setAttributeFromMap(String metacardAttributeName,
            Map<String, SlotType1> map, MetacardImpl metacard) throws RegistryConversionException {
        String xmlAttributeName = METACARD_XML_NAME_MAP.get(metacardAttributeName);

        if (map.containsKey(xmlAttributeName)) {

            SlotType1 slot = map.get(xmlAttributeName);

            String slotType = slot.getSlotType();
            if (slotType.contains(RegistryConstants.XML_DATE_TIME_TYPE)) {
                setSlotDateAttribute(slot, metacardAttributeName, metacard);
            } else if (slotType.contains(RegistryConstants.XML_GEO_TYPE)) {
                setSlotGeoAttribute(slot, metacardAttributeName, metacard);
            } else {
                // default to string
                setSlotStringAttribute(slot, metacardAttributeName, metacard);
            }
        }
    }

    private static void setSlotStringAttribute(SlotType1 slot, String metacardAttributeName,
            MetacardImpl metacard) {
        List<String> stringAttributes = SLOT_TYPE_HELPER.getStringValues(slot);

        if (CollectionUtils.isEmpty(stringAttributes)) {
            return;
        }

        if (isAttributeMultiValued(metacardAttributeName, metacard)) {
            metacard.setAttribute(metacardAttributeName, (Serializable) stringAttributes);
        } else {
            metacard.setAttribute(metacardAttributeName, stringAttributes.get(0));
        }
    }

    private static void setSlotDateAttribute(SlotType1 slot, String metacardAttributeName,
            MetacardImpl metacard) {
        List<Date> dates = SLOT_TYPE_HELPER.getDateValues(slot);

        if (CollectionUtils.isNotEmpty(dates)) {
            if (isAttributeMultiValued(metacardAttributeName, metacard)) {
                metacard.setAttribute(metacardAttributeName, (Serializable) dates);
            } else {
                metacard.setAttribute(metacardAttributeName, dates.get(0));
            }
        }
    }

    private static String getWKTFromGeometry(AbstractGeometryType geometry, JAXBElement jaxbElement)
            throws RegistryConversionException {
        String convertedGeometry = null;

        if (geometry != null) {
            try {

                GML311ToJTSGeometryConverter geometryConverter = new GML311ToJTSGeometryConverter();
                Geometry jtsGeometry =
                        geometryConverter.createGeometry(new DefaultRootObjectLocator(jaxbElement),
                                geometry);

                if (jtsGeometry != null) {
                    WKTWriter writer = new WKTWriter();
                    convertedGeometry = writer.write(jtsGeometry);
                }

            } catch (ConversionFailedException e) {
                String message = "Error converting geometry. Caught an exception.";
                throw new RegistryConversionException(message, e);
            }
        }

        return convertedGeometry;
    }

    private static void setSlotGeoAttribute(SlotType1 slot, String metacardAttributeName,
            MetacardImpl metacard) throws RegistryConversionException {
        if (slot.isSetValueList()) {
            net.opengis.cat.wrs.v_1_0_2.ValueListType valueList =
                    (net.opengis.cat.wrs.v_1_0_2.ValueListType) slot.getValueList()
                            .getValue();

            List<AnyValueType> anyValues = valueList.getAnyValue();

            for (AnyValueType anyValue : anyValues) {

                if (anyValue.isSetContent()) {

                    for (Object content : anyValue.getContent()) {
                        if (content instanceof JAXBElement) {

                            JAXBElement jaxbElement = (JAXBElement) content;

                            AbstractGeometryType geometry =
                                    (AbstractGeometryType) jaxbElement.getValue();

                            String convertedGeometry = getWKTFromGeometry(geometry, jaxbElement);

                            if (StringUtils.isNotBlank(convertedGeometry)) {
                                metacard.setAttribute(metacardAttributeName, convertedGeometry);
                            }
                        }
                    }
                }
            }

        }
    }

    private static void unsetMetacardAttribute(String metacardAttribute, MetacardImpl metacard) {
        if (StringUtils.isNotBlank(metacardAttribute)) {
            metacard.setAttribute(metacardAttribute, null);
        }
    }

    private static void validateIdentifiable(IdentifiableType registryIdentifiable)
            throws RegistryConversionException {
        if (StringUtils.isBlank(registryIdentifiable.getId())) {
            String message = "Error converting registry object to metacard. "
                    + registryIdentifiable.getClass()
                    .getSimpleName() + " must have an ID set.";
            throw new RegistryConversionException(message);
        }
    }
}
