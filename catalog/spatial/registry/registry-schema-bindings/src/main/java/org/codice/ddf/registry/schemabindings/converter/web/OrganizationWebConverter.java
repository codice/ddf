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

import static org.codice.ddf.registry.schemabindings.EbrimConstants.ADDRESS_KEY;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.EMAIL_ADDRESS_KEY;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.PARENT;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.PRIMARY_CONTACT;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.TELEPHONE_KEY;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.EmailAddressType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.OrganizationType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.PostalAddressType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.TelephoneNumberType;

public class OrganizationWebConverter extends RegistryObjectWebConverter {

    /**
     * This method creates a Map<String, Object> representation of the OrganizationType provided.
     * The following keys will be added to the map (Taken from EbrimConstants):
     * <p>
     * PARENT = "parent";
     * PRIMARY_CONTACT = "primaryContact";
     * ADDRESS_KEY = "Address";
     * EMAIL_ADDRESS_KEY = "EmailAddress";
     * TELEPHONE_KEY = "TelephoneNumber";
     * <p>
     * This will also try to parse RegistryObjectType values to the map.
     * <p>
     * Uses:
     * PostalAddressWebConverter
     * EmailAddressWebConverter
     * TelephoneNumberWebConverter
     *
     * @param organization the OrganizationType to be converted into a map, null returns empty Map
     * @return Map<String, Object> representation of the OrganizationType provided
     */
    public Map<String, Object> convert(OrganizationType organization) {
        Map<String, Object> organizationMap = new HashMap<>();
        if (organization == null) {
            return organizationMap;
        }

        Map<String, Object> registryObjectMap = super.convertRegistryObject(organization);
        if (MapUtils.isNotEmpty(registryObjectMap)) {
            organizationMap.putAll(registryObjectMap);
        }

        if (organization.isSetAddress()) {
            List<Map<String, Object>> addresses = new ArrayList<>();
            PostalAddressWebConverter addressConverter = new PostalAddressWebConverter();

            for (PostalAddressType address : organization.getAddress()) {
                Map<String, Object> addressMap = addressConverter.convert(address);

                if (MapUtils.isNotEmpty(addressMap)) {
                    addresses.add(addressMap);
                }
            }

            if (CollectionUtils.isNotEmpty(addresses)) {
                organizationMap.put(ADDRESS_KEY, addresses);
            }
        }

        if (organization.isSetEmailAddress()) {
            List<Map<String, Object>> emailAddresses = new ArrayList<>();
            EmailAddressWebConverter emailConverter = new EmailAddressWebConverter();

            for (EmailAddressType email : organization.getEmailAddress()) {
                Map<String, Object> emailMap = emailConverter.convert(email);

                if (MapUtils.isNotEmpty(emailMap)) {
                    emailAddresses.add(emailMap);
                }
            }

            if (CollectionUtils.isNotEmpty(emailAddresses)) {
                organizationMap.put(EMAIL_ADDRESS_KEY, emailAddresses);
            }
        }

        if (organization.isSetParent()) {
            organizationMap.put(PARENT, organization.getParent());
        }

        if (organization.isSetPrimaryContact()) {
            organizationMap.put(PRIMARY_CONTACT, organization.getPrimaryContact());
        }

        if (organization.isSetTelephoneNumber()) {
            List<Map<String, Object>> telephoneNumbers = new ArrayList<>();
            TelephoneNumberWebConverter telephoneConverter = new TelephoneNumberWebConverter();

            for (TelephoneNumberType telephone : organization.getTelephoneNumber()) {
                Map<String, Object> telephoneMap = telephoneConverter.convert(telephone);

                if (MapUtils.isNotEmpty(telephoneMap)) {
                    telephoneNumbers.add(telephoneMap);
                }
            }

            if (CollectionUtils.isNotEmpty(telephoneNumbers)) {
                organizationMap.put(TELEPHONE_KEY, telephoneNumbers);
            }
        }

        return organizationMap;
    }
}
