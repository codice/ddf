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

import static org.codice.ddf.registry.schemabindings.EbrimConstants.PHONE_AREA_CODE;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.PHONE_COUNTRY_CODE;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.PHONE_EXTENSION;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.PHONE_NUMBER;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.PHONE_TYPE;

import java.util.HashMap;
import java.util.Map;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.TelephoneNumberType;

public class TelephoneNumberWebConverter {

    /**
     * This method creates a Map<String, Object> representation of the TelephoneNumberType provided.
     * The following keys will be added to the map (Taken from EbrimConstants):
     * <p>
     * PHONE_COUNTRY_CODE = "countryCode";
     * PHONE_TYPE = "phoneType";
     * PHONE_AREA_CODE = "areaCode";
     * PHONE_NUMBER = "number";
     * PHONE_EXTENSION = "extension";
     *
     * @param phoneNumber the TelephoneNumberType to be converted into a map, null returns empty Map
     * @return Map<String, Object> representation of the TelephoneNumberType provided
     */
    public Map<String, Object> convert(TelephoneNumberType phoneNumber) {
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

        return phoneNumberMap;
    }
}
