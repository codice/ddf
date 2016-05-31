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

import java.util.HashMap;
import java.util.Map;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.PostalAddressType;

public class PostalAddressWebConverter {
    public static final String CITY = "city";

    public static final String COUNTRY = "country";

    public static final String POSTAL_CODE = "postalCode";

    public static final String STATE_OR_PROVINCE = "stateOrProvince";

    public static final String STREET = "street";

    public static final String STREET_NUMBER = "streetNumber";

    /**
     * This method creates a Map<String, Object> representation of the PostalAddressType provided.
     * The following keys will be added to the map (Taken from EbrimConstants):
     * <p>
     * CITY = "city";
     * COUNTRY = "country";
     * POSTAL_CODE = "postalCode";
     * STATE_OR_PROVINCE = "stateOrProvince";
     * STREET = "street";
     * STREET_NUMBER = "streetNumber";
     *
     * @param postalAddress the PostalAddressType to be converted into a map, null returns empty Map
     * @return Map<String, Object> representation of the PostalAddressType provided
     */
    public Map<String, Object> convert(PostalAddressType postalAddress) {
        Map<String, Object> addressMap = new HashMap<>();
        if (postalAddress == null) {
            return addressMap;
        }

        if (postalAddress.isSetCity()) {
            addressMap.put(CITY, postalAddress.getCity());
        }

        if (postalAddress.isSetCountry()) {
            addressMap.put(COUNTRY, postalAddress.getCountry());
        }

        if (postalAddress.isSetPostalCode()) {
            addressMap.put(POSTAL_CODE, postalAddress.getPostalCode());
        }

        if (postalAddress.isSetStateOrProvince()) {
            addressMap.put(STATE_OR_PROVINCE, postalAddress.getStateOrProvince());
        }

        if (postalAddress.isSetStreet()) {
            addressMap.put(STREET, postalAddress.getStreet());
        }

        if (postalAddress.isSetStreetNumber()) {
            addressMap.put(STREET_NUMBER, postalAddress.getStreetNumber());
        }

        return addressMap;
    }
}
