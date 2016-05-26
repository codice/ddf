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

import static org.codice.ddf.registry.schemabindings.EbrimConstants.ADDRESS_KEY;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.EMAIL_ADDRESS_KEY;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.PARENT;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.PRIMARY_CONTACT;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.RIM_FACTORY;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.TELEPHONE_KEY;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.EmailAddressType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.OrganizationType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.PostalAddressType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.TelephoneNumberType;

public class OrganizationTypeConverter extends RegistryObjectTypeConverter {

    @Override
    protected RegistryObjectType createObjectInstance() {
        return RIM_FACTORY.createOrganizationType();
    }

    /**
     * This method creates an OrganizationType from the values in the provided map.
     * The following keys are expected in the provided map (Taken from EbrimConstants):
     * <p>
     * PARENT = "parent";
     * PRIMARY_CONTACT = "primaryContact";
     * ADDRESS_KEY = "Address";
     * EMAIL_ADDRESS_KEY = "EmailAddress";
     * TELEPHONE_KEY = "TelephoneNumber";
     * <p>
     * This will also try to populate the RegistryObjectType values also looked for in the map.
     * <p>
     * Uses:
     * PostalAddressTypeConverter
     * EmailAddressTypeConverter
     * TelephoneNumberTypeConverter
     *
     * @param map the Map representation of the OrganizationType to generate, null returns empty Optional
     * @return Optional OrganizationType created from the values in the map
     */
    public Optional<OrganizationType> convert(Map<String, Object> map) {
        Optional<OrganizationType> optionalOrganization = Optional.empty();
        if (MapUtils.isEmpty(map)) {
            return optionalOrganization;
        }

        Optional<RegistryObjectType> optionalRot = super.convertRegistryObject(map);
        if (optionalRot.isPresent()) {
            optionalOrganization = Optional.of((OrganizationType) optionalRot.get());
        }

        if (map.containsKey(ADDRESS_KEY)) {
            Optional<PostalAddressType> optionalAddress;
            PostalAddressTypeConverter addressConverter = new PostalAddressTypeConverter();

            for (Map<String, Object> addressMap : (List<Map<String, Object>>) map.get(ADDRESS_KEY)) {
                optionalAddress = addressConverter.convert(addressMap);
                if (optionalAddress.isPresent()) {
                    if (!optionalOrganization.isPresent()) {
                        optionalOrganization = Optional.of(RIM_FACTORY.createOrganizationType());
                    }

                    optionalOrganization.get()
                            .getAddress()
                            .add(optionalAddress.get());
                }
            }
        }

        if (map.containsKey(EMAIL_ADDRESS_KEY)) {
            Optional<EmailAddressType> optionalEmailAddress;
            EmailAddressTypeConverter emailConverter = new EmailAddressTypeConverter();

            for (Map<String, Object> emailAddressMap : (List<Map<String, Object>>) map.get(
                    EMAIL_ADDRESS_KEY)) {
                optionalEmailAddress = emailConverter.convert(emailAddressMap);
                if (optionalEmailAddress.isPresent()) {
                    if (!optionalOrganization.isPresent()) {
                        optionalOrganization = Optional.of(RIM_FACTORY.createOrganizationType());
                    }

                    optionalOrganization.get()
                            .getEmailAddress()
                            .add(optionalEmailAddress.get());
                }
            }
        }

        String valueToPopulate = MapUtils.getString(map, PARENT);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            if (!optionalOrganization.isPresent()) {
                optionalOrganization = Optional.of(RIM_FACTORY.createOrganizationType());
            }
            optionalOrganization.get()
                    .setParent(valueToPopulate);
        }

        valueToPopulate = MapUtils.getString(map, PRIMARY_CONTACT);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            if (!optionalOrganization.isPresent()) {
                optionalOrganization = Optional.of(RIM_FACTORY.createOrganizationType());
            }
            optionalOrganization.get()
                    .setPrimaryContact(valueToPopulate);
        }

        if (map.containsKey(TELEPHONE_KEY)) {
            Optional<TelephoneNumberType> optionalTelephone;
            TelephoneNumberTypeConverter telephoneConverter = new TelephoneNumberTypeConverter();

            for (Map<String, Object> telephoneMap : (List<Map<String, Object>>) map.get(
                    TELEPHONE_KEY)) {
                optionalTelephone = telephoneConverter.convert(telephoneMap);
                if (optionalTelephone.isPresent()) {
                    if (!optionalOrganization.isPresent()) {
                        optionalOrganization = Optional.of(RIM_FACTORY.createOrganizationType());
                    }

                    optionalOrganization.get()
                            .getTelephoneNumber()
                            .add(optionalTelephone.get());
                }
            }
        }

        return optionalOrganization;
    }
}
