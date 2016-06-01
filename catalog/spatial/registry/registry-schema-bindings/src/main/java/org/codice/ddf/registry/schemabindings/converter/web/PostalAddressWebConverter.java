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

import org.codice.ddf.registry.schemabindings.helper.WebMapHelper;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.PostalAddressType;

public class PostalAddressWebConverter {
    public static final String CITY = "city";

    public static final String COUNTRY = "country";

    public static final String POSTAL_CODE = "postalCode";

    public static final String STATE_OR_PROVINCE = "stateOrProvince";

    public static final String STREET = "street";

    public static final String STREET_NUMBER = "streetNumber";

    private WebMapHelper webMapHelper = new WebMapHelper();

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

        webMapHelper.putIfNotEmpty(addressMap, CITY, postalAddress.getCity());
        webMapHelper.putIfNotEmpty(addressMap, COUNTRY, postalAddress.getCountry());
        webMapHelper.putIfNotEmpty(addressMap, POSTAL_CODE, postalAddress.getPostalCode());
        webMapHelper.putIfNotEmpty(addressMap,
                STATE_OR_PROVINCE,
                postalAddress.getStateOrProvince());
        webMapHelper.putIfNotEmpty(addressMap, STREET, postalAddress.getStreet());
        webMapHelper.putIfNotEmpty(addressMap, STREET_NUMBER, postalAddress.getStreetNumber());

        return addressMap;
    }
}
