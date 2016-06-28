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

package org.codice.ddf.registry.report.viewer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.federationadmin.service.FederationAdminException;
import org.codice.ddf.registry.federationadmin.service.FederationAdminService;
import org.codice.ddf.registry.schemabindings.helper.InternationalStringTypeHelper;
import org.codice.ddf.registry.schemabindings.helper.RegistryPackageTypeHelper;
import org.codice.ddf.registry.schemabindings.helper.SlotTypeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.EmailAddressType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExtrinsicObjectType;
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

/**
 * Generates a registry report for a given registry metacard.
 * Displays the Name, General, Collections,
 * Services, Organizations, and Contacts information for the node.
 */
@Path("/")
public class RegistryReportViewer {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryReportViewer.class);

    private static final String BINDING_TYPE = "bindingType";

    private static final String NAME = "Name";

    private static final String VERSION_INFO = "Version Info";

    private static final String DESCRIPTION = "Description";

    private static final String GENERAL = "General";

    private static final String COLLECTIONS = "Collections";

    private static final String REGISTRY_NAME = "RegistryName";

    private static final String SERVICES = "Services";

    private static final String ORGANIZATIONS = "Organizations";

    private static final String CONTACTS = "Contacts";

    private static final String ACCESS_URL = "Access Url";

    private static final String SERVICE = "Service";

    private static final String TARGET_BINDING = "Target binding";

    private static final String SPECIFICATION_LINK = "Specification link";

    private static final String PROPERTIES = "Properties";

    private static final String BINDINGS = "Bindings";

    private static final String ADDRESSES = "Addresses";

    private static final String PHONE_NUMBERS = "Phone Numbers";

    private static final String EMAIL_ADDRESSES = "Email Addresses";

    private static final String CONTACT_INFO = "ContactInfo";

    private static final String CUSTOM_SLOTS = "CustomSlots";

    private FederationAdminService federationAdminService;

    private ClassPathTemplateLoader templateLoader;

    private InternationalStringTypeHelper internationalStringTypeHelper =
            new InternationalStringTypeHelper();

    private RegistryPackageTypeHelper registryPackageTypeHelper = new RegistryPackageTypeHelper();

    private SlotTypeHelper slotTypeHelper = new SlotTypeHelper();

    public RegistryReportViewer() {
        templateLoader = new ClassPathTemplateLoader();
        templateLoader.setPrefix("/templates");
        templateLoader.setSuffix(".hbt");
    }

    @Path("/{registryId}/report")
    @Produces(MediaType.TEXT_HTML)
    @GET
    public Response viewRegInfoHtml(@PathParam("registryId") final String registryId,
            @QueryParam("sourceId") final List<String> sourceIds) {
        String html = "";
        if (StringUtils.isBlank(registryId)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Registry Id cannot be blank")
                    .build();
        }

        RegistryPackageType registryPackage;
        try {
            registryPackage = federationAdminService.getRegistryObjectByRegistryId(registryId,
                    sourceIds);
        } catch (FederationAdminException e) {
            String message = "Error getting registry package.";
            LOGGER.error("{} For metacard id: '{}', optional sources: {}",
                    message,
                    registryId,
                    sourceIds);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(message)
                    .build();
        }

        if (registryPackage == null) {
            String message = "No registry package was found.";
            LOGGER.error("{} For metacard id: '{}', optional source ids: {}.",
                    message,
                    registryId,
                    sourceIds);
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(message)
                    .build();
        }

        Map<String, Object> registryMap = buildRegistryMap(registryPackage);

        try {
            Handlebars handlebars = new Handlebars(templateLoader);
            Template template = handlebars.compile("report");
            html = template.apply(registryMap);
        } catch (IOException e) {
            LOGGER.error("Error compiling and applying report template.");
        }

        return Response.ok(html)
                .build();
    }

    protected Map<String, Object> buildRegistryMap(RegistryPackageType registryPackage) {
        Map<String, Object> registryMap = new HashMap<>();

        Map<String, Object> extrinsicInfo = getExtrinsicInfo(registryPackage);
        Map<String, Object> generalMap = (Map<String, Object>) extrinsicInfo.get(GENERAL);
        Map<String, Object> collectionMap = (Map<String, Object>) extrinsicInfo.get(COLLECTIONS);

        registryMap.put(NAME, generalMap.get(REGISTRY_NAME));
        generalMap.remove(REGISTRY_NAME);

        registryMap.put(GENERAL, generalMap);
        registryMap.put(COLLECTIONS, collectionMap);

        Map<String, Object> serviceMap = getServiceInfo(registryPackage.getRegistryObjectList());
        registryMap.put(SERVICES, serviceMap);

        Map<String, Object> organizationMap = getOrganizationInfo(registryPackage);
        registryMap.put(ORGANIZATIONS, organizationMap);

        Map<String, Object> contactMap = getContactInfo(registryPackage);
        registryMap.put(CONTACTS, contactMap);

        return registryMap;
    }

    private Map<String, Object> getServiceInfo(RegistryObjectListType registryObjectListType) {
        Map<String, Object> serviceInfo = new HashMap<>();
        for (JAXBElement id : registryObjectListType.getIdentifiable()) {
            RegistryObjectType registryObjectType = (RegistryObjectType) id.getValue();
            if (registryObjectType instanceof ServiceType
                    && RegistryConstants.REGISTRY_SERVICE_OBJECT_TYPE.equals(registryObjectType.getObjectType())) {
                ServiceType service = (ServiceType) registryObjectType;

                String serviceName = SERVICE;
                if (service.isSetName()) {
                    serviceName = internationalStringTypeHelper.getString(service.getName());
                }

                Map<String, Object> serviceProperties = new HashMap<>();
                Map<String, Object> serviceBindings = new HashMap<>();

                Map<String, Object> customSlots = new HashMap<>();
                customSlots.putAll(getCustomSlots(service.getSlot()));
                if (service.isSetName()) {
                    customSlots.put(NAME,
                            internationalStringTypeHelper.getString(service.getName()));
                }
                if (service.isSetVersionInfo()) {
                    customSlots.put(VERSION_INFO,
                            service.getVersionInfo()
                                    .getVersionName());
                }
                if (service.isSetDescription()) {
                    customSlots.put(DESCRIPTION,
                            internationalStringTypeHelper.getString(service.getDescription()));
                }

                for (ServiceBindingType binding : service.getServiceBinding()) {
                    Map<String, Object> bindingProperties;
                    String bindingType;
                    bindingProperties = getCustomSlots(binding.getSlot());
                    if (!bindingProperties.containsKey(BINDING_TYPE) || bindingProperties.get(
                            BINDING_TYPE) == null) {
                        continue;
                    }

                    if (binding.isSetAccessURI()) {
                        bindingProperties.put(ACCESS_URL, binding.getAccessURI());
                    }
                    if (binding.isSetService()) {
                        bindingProperties.put(SERVICE, binding.getService());
                    }
                    if (binding.isSetTargetBinding()) {
                        bindingProperties.put(TARGET_BINDING, binding.getTargetBinding());
                    }
                    bindingType = bindingProperties.get(BINDING_TYPE)
                            .toString();
                    bindingProperties.remove(BINDING_TYPE);
                    serviceBindings.put(bindingType, bindingProperties);
                }
                serviceProperties.put(PROPERTIES, customSlots);
                serviceProperties.put(BINDINGS, serviceBindings);
                serviceInfo.put(serviceName, serviceProperties);
            }
        }
        return serviceInfo;
    }

    private Map<String, Object> getExtrinsicInfo(RegistryPackageType registryPackage) {
        Map<String, Object> generalMap = new HashMap<>();
        Map<String, Object> collectionMap = new HashMap<>();

        List<ExtrinsicObjectType> extrinsicObjectTypes =
                registryPackageTypeHelper.getExtrinsicObjects(registryPackage);
        for (ExtrinsicObjectType extrinsicObjectType : extrinsicObjectTypes) {
            if (extrinsicObjectType.isSetObjectType()) {
                if (extrinsicObjectType.getObjectType()
                        .equals(RegistryConstants.REGISTRY_NODE_OBJECT_TYPE)) {
                    if (extrinsicObjectType.isSetName()) {
                        generalMap.put(REGISTRY_NAME,
                                internationalStringTypeHelper.getString(extrinsicObjectType.getName()));

                    } else {
                        generalMap.put(REGISTRY_NAME, "");
                    }
                    if (extrinsicObjectType.isSetVersionInfo()) {
                        generalMap.put(VERSION_INFO,
                                extrinsicObjectType.getVersionInfo()
                                        .getVersionName());
                    }
                    if (extrinsicObjectType.isSetDescription()) {
                        generalMap.put(DESCRIPTION,
                                internationalStringTypeHelper.getString(extrinsicObjectType.getDescription()));
                    }
                    Map<String, Object> customSlots = getCustomSlots(extrinsicObjectType.getSlot());
                    generalMap.putAll(customSlots);
                } else {
                    Map<String, Object> collectionValues = new HashMap<>();

                    collectionValues.putAll(getCustomSlots(extrinsicObjectType.getSlot()));

                    String collectionName = "";
                    if (extrinsicObjectType.isSetName()) {
                        collectionName =
                                internationalStringTypeHelper.getString(extrinsicObjectType.getName());
                    }

                    collectionMap.put(collectionName, collectionValues);
                }
            } else {
                Map<String, Object> collectionValues = new HashMap<>();

                collectionValues.putAll(getCustomSlots(extrinsicObjectType.getSlot()));

                String collectionName = "";
                if (extrinsicObjectType.isSetName()) {
                    collectionName =
                            internationalStringTypeHelper.getString(extrinsicObjectType.getName());
                }

                collectionMap.put(collectionName, collectionValues);
            }
        }

        Map<String, Object> extrinsicInfoMap = new HashMap<>();
        extrinsicInfoMap.put(GENERAL, generalMap);
        extrinsicInfoMap.put(COLLECTIONS, collectionMap);

        return extrinsicInfoMap;
    }

    private Map<String, Object> getOrganizationInfo(RegistryPackageType registryPackage) {
        Map<String, Object> organizationMap = new HashMap<>();

        List<OrganizationType> organizationTypes = registryPackageTypeHelper.getOrganizations(
                registryPackage);
        for (OrganizationType organizationType : organizationTypes) {
            Map<String, Object> organizationInfo = new HashMap<>();
            Map<String, Object> contactInfo = new HashMap<>();

            String orgName = "";

            if (organizationType.isSetName()) {
                orgName = internationalStringTypeHelper.getString(organizationType.getName());
            }

            addNonEmptyKeyValue(contactInfo,
                    ADDRESSES,
                    getAddresses(organizationType.getAddress()));
            addNonEmptyKeyValue(contactInfo,
                    PHONE_NUMBERS,
                    getPhoneNumbers(organizationType.getTelephoneNumber()));
            addNonEmptyKeyValue(contactInfo,
                    EMAIL_ADDRESSES,
                    getEmailAddresses(organizationType.getEmailAddress()));

            organizationInfo.put(CONTACT_INFO, contactInfo);
            organizationInfo.put(CUSTOM_SLOTS, getCustomSlots(organizationType.getSlot()));
            organizationMap.put(orgName, organizationInfo);
        }

        return organizationMap;
    }

    private Map<String, Object> getContactInfo(RegistryPackageType registryPackage) {
        Map<String, Object> contactMap = new HashMap<>();
        List<PersonType> persons = registryPackageTypeHelper.getPersons(registryPackage);
        for (PersonType person : persons) {
            PersonNameType personName = person.getPersonName();

            if (personName == null) {
                continue;
            }
            ArrayList<String> nameArray = new ArrayList<>();

            addNonEmptyValue(nameArray, personName.getFirstName());
            addNonEmptyValue(nameArray, personName.getMiddleName());
            addNonEmptyValue(nameArray, personName.getLastName());

            String name = StringUtils.join(nameArray, " ");

            if (StringUtils.isEmpty(name)) {
                continue;
            }

            Map<String, Object> personMap = new HashMap<>();
            Map<String, Object> contactInfo = new HashMap<>();

            addNonEmptyKeyValue(contactInfo,
                    EMAIL_ADDRESSES,
                    getEmailAddresses(person.getEmailAddress()));
            addNonEmptyKeyValue(contactInfo,
                    PHONE_NUMBERS,
                    getPhoneNumbers(person.getTelephoneNumber()));
            addNonEmptyKeyValue(contactInfo, ADDRESSES, getAddresses(person.getAddress()));

            personMap.put(CONTACT_INFO, contactInfo);
            personMap.put(CUSTOM_SLOTS, getCustomSlots(person.getSlot()));
            contactMap.put(name, personMap);
        }

        return contactMap;
    }

    private List<String> getAddresses(List<PostalAddressType> addressTypes) {
        List<String> addresses = new ArrayList<>();
        for (PostalAddressType address : addressTypes) {
            ArrayList<String> addressArray = new ArrayList<>();

            addNonEmptyValue(addressArray, address.getStreet());
            addNonEmptyValue(addressArray, address.getCity());
            addNonEmptyValue(addressArray, address.getStateOrProvince());
            addNonEmptyValue(addressArray, address.getPostalCode());
            addNonEmptyValue(addressArray, address.getCountry());

            String addressString = StringUtils.join(addressArray, " ");
            addresses.add(addressString);
        }

        return addresses;
    }

    private List<String> getPhoneNumbers(List<TelephoneNumberType> telephoneNumberTypes) {
        List<String> phoneNumbers = new ArrayList<>();
        for (TelephoneNumberType telephoneNumberType : telephoneNumberTypes) {
            StringBuilder phoneNumber = new StringBuilder();

            if (telephoneNumberType.isSetCountryCode()) {
                phoneNumber.append(telephoneNumberType.getCountryCode())
                        .append("-");
            }
            if (telephoneNumberType.isSetAreaCode()) {
                phoneNumber.append(telephoneNumberType.getAreaCode())
                        .append("-");
            }
            if (telephoneNumberType.isSetNumber()) {
                phoneNumber.append(telephoneNumberType.getNumber());
            }
            if (telephoneNumberType.isSetExtension()) {
                phoneNumber.append(" ext. ");
                phoneNumber.append(telephoneNumberType.getExtension());
            }
            if (telephoneNumberType.isSetPhoneType()) {
                phoneNumber.append(" (");
                phoneNumber.append(telephoneNumberType.getPhoneType())
                        .append(")");
            }
            phoneNumbers.add(phoneNumber.toString());
        }
        return phoneNumbers;
    }

    private List<String> getEmailAddresses(List<EmailAddressType> emailAddressTypes) {
        List<String> emailAddresses = new ArrayList<>();

        for (EmailAddressType emailAddressType : emailAddressTypes) {
            emailAddresses.add(emailAddressType.getAddress());
        }
        return emailAddresses;
    }

    private Map<String, Object> getCustomSlots(List<SlotType1> slots) {
        Map<String, Object> customSlotMap = new HashMap<>();

        for (SlotType1 slot : slots) {
            String key = slot.getName();
            List<String> values = slotTypeHelper.getStringValues(slot);
            if (CollectionUtils.isEmpty(values)) {
                continue;
            }
            customSlotMap.put(key, StringUtils.join(values, ", "));
        }
        return customSlotMap;
    }

    private void addNonEmptyValue(List<String> list, String value) {
        if (StringUtils.isEmpty(value)) {
            return;
        }
        list.add(value);
    }

    private void addNonEmptyKeyValue(Map<String, Object> map, String key, List<String> list) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        map.put(key, list);
    }

    public void setFederationAdminService(FederationAdminService federationAdminService) {
        this.federationAdminService = federationAdminService;
    }
}
