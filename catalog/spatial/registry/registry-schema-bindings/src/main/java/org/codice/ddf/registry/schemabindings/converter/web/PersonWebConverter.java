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

import org.apache.commons.collections.MapUtils;
import org.codice.ddf.registry.schemabindings.helper.WebMapHelper;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.EmailAddressType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.PersonType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.PostalAddressType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.TelephoneNumberType;

public class PersonWebConverter extends RegistryObjectWebConverter {
    public static final String ADDRESS_KEY = "Address";

    public static final String EMAIL_ADDRESS_KEY = "EmailAddress";

    public static final String PERSON_NAME_KEY = "PersonName";

    public static final String TELEPHONE_KEY = "TelephoneNumber";

    private WebMapHelper webMapHelper = new WebMapHelper();

    /**
     * This method creates a Map<String, Object> representation of the PersonType provided.
     * The following keys will be added to the map (Taken from EbrimConstants):
     * <p>
     * PERSON_NAME_KEY = "PersonName"
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
     * PersonNameWebConverter
     *
     * @param person the PersonType to be converted into a map, null returns empty Map
     * @return Map<String, Object> representation of the PersonType provided
     */
    public Map<String, Object> convert(PersonType person) {
        Map<String, Object> personMap = new HashMap<>();
        if (person == null) {
            return personMap;
        }

        webMapHelper.putAllIfNotEmpty(personMap, super.convertRegistryObject(person));

        if (person.isSetAddress()) {
            List<Map<String, Object>> addresses = new ArrayList<>();
            PostalAddressWebConverter addressConverter = new PostalAddressWebConverter();

            for (PostalAddressType address : person.getAddress()) {
                Map<String, Object> addressMap = addressConverter.convert(address);

                if (MapUtils.isNotEmpty(addressMap)) {
                    addresses.add(addressMap);
                }
            }

            webMapHelper.putIfNotEmpty(personMap, ADDRESS_KEY, addresses);
        }

        if (person.isSetEmailAddress()) {
            List<Map<String, Object>> emailAddresses = new ArrayList<>();
            EmailAddressWebConverter emailConverter = new EmailAddressWebConverter();

            for (EmailAddressType email : person.getEmailAddress()) {
                Map<String, Object> emailMap = emailConverter.convert(email);

                if (MapUtils.isNotEmpty(emailMap)) {
                    emailAddresses.add(emailMap);
                }
            }

            webMapHelper.putIfNotEmpty(personMap, EMAIL_ADDRESS_KEY, emailAddresses);
        }

        if (person.isSetPersonName()) {
            PersonNameWebConverter personNameConverter = new PersonNameWebConverter();
            Map<String, Object> personNameMap = personNameConverter.convert(person.getPersonName());

            webMapHelper.putIfNotEmpty(personMap, PERSON_NAME_KEY, personNameMap);
        }

        if (person.isSetTelephoneNumber()) {
            List<Map<String, Object>> telephoneNumbers = new ArrayList<>();
            TelephoneNumberWebConverter telephoneConverter = new TelephoneNumberWebConverter();

            for (TelephoneNumberType telephone : person.getTelephoneNumber()) {
                Map<String, Object> telephoneMap = telephoneConverter.convert(telephone);

                if (MapUtils.isNotEmpty(telephoneMap)) {
                    telephoneNumbers.add(telephoneMap);
                }
            }

            webMapHelper.putIfNotEmpty(personMap, TELEPHONE_KEY, telephoneNumbers);
        }

        return personMap;
    }
}
