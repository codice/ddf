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
import static org.codice.ddf.registry.schemabindings.converter.web.PersonWebConverter.ADDRESS_KEY;
import static org.codice.ddf.registry.schemabindings.converter.web.PersonWebConverter.EMAIL_ADDRESS_KEY;
import static org.codice.ddf.registry.schemabindings.converter.web.PersonWebConverter.PERSON_NAME_KEY;
import static org.codice.ddf.registry.schemabindings.converter.web.PersonWebConverter.TELEPHONE_KEY;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections.MapUtils;
import org.codice.ddf.registry.schemabindings.helper.MapToSchemaElement;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.EmailAddressType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.PersonNameType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.PersonType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.PostalAddressType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.TelephoneNumberType;

public class PersonTypeConverter extends AbstractRegistryObjectTypeConverter<PersonType> {

    public PersonTypeConverter(MapToSchemaElement<PersonType> mapToSchemaElement) {
        super(mapToSchemaElement);
    }

    public PersonTypeConverter() {
        this(new MapToSchemaElement<>(RIM_FACTORY::createPersonType));

    }

    /**
     * This method creates an PersonType from the values in the provided map.
     * The following keys are expected in the provided map (Taken from EbrimConstants):
     * <p>
     * PERSON_NAME_KEY = "PersonName"
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
     * PersonNameTypeConverter
     *
     * @param map the Map representation of the PersonType to generate, null returns empty Optional
     * @return Optional PersonType created from the values in the map
     */
    public Optional<PersonType> convert(Map<String, Object> map) {
        Optional<PersonType> optionalPerson = Optional.empty();
        if (MapUtils.isEmpty(map)) {
            return optionalPerson;
        }

        optionalPerson = super.convert(map);

        if (map.containsKey(ADDRESS_KEY)) {
            Optional<PostalAddressType> optionalAddress;
            PostalAddressTypeConverter addressConverter = new PostalAddressTypeConverter();

            for (Map<String, Object> addressMap : (List<Map<String, Object>>) map.get(ADDRESS_KEY)) {
                optionalAddress = addressConverter.convert(addressMap);
                if (optionalAddress.isPresent()) {
                    if (!optionalPerson.isPresent()) {
                        optionalPerson = Optional.of(mapToSchemaElement.getObjectFactory()
                                .get());
                    }

                    optionalPerson.get()
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
                    if (!optionalPerson.isPresent()) {
                        optionalPerson = Optional.of(mapToSchemaElement.getObjectFactory()
                                .get());
                    }

                    optionalPerson.get()
                            .getEmailAddress()
                            .add(optionalEmailAddress.get());
                }
            }
        }

        if (map.containsKey(PERSON_NAME_KEY)) {
            PersonNameTypeConverter nameConverter = new PersonNameTypeConverter();
            Optional<PersonNameType> optionalPersonName =
                    nameConverter.convert((Map<String, Object>) map.get(PERSON_NAME_KEY));

            if (optionalPersonName.isPresent()) {
                if (!optionalPerson.isPresent()) {
                    optionalPerson = Optional.of(mapToSchemaElement.getObjectFactory()
                            .get());
                }
                optionalPerson.get()
                        .setPersonName(optionalPersonName.get());
            }
        }

        if (map.containsKey(TELEPHONE_KEY)) {
            Optional<TelephoneNumberType> optionalTelephone;
            TelephoneNumberTypeConverter telephoneConverter = new TelephoneNumberTypeConverter();

            for (Map<String, Object> telephoneMap : (List<Map<String, Object>>) map.get(
                    TELEPHONE_KEY)) {
                optionalTelephone = telephoneConverter.convert(telephoneMap);
                if (optionalTelephone.isPresent()) {
                    if (!optionalPerson.isPresent()) {
                        optionalPerson = Optional.of(mapToSchemaElement.getObjectFactory()
                                .get());
                    }

                    optionalPerson.get()
                            .getTelephoneNumber()
                            .add(optionalTelephone.get());
                }
            }
        }

        return optionalPerson;
    }
}
