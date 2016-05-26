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

import static org.codice.ddf.registry.schemabindings.EbrimConstants.CITY;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.COUNTRY;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.POSTAL_CODE;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.RIM_FACTORY;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.STATE_OR_PROVINCE;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.STREET;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.STREET_NUMBER;

import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.PostalAddressType;

public class PostalAddressTypeConverter {

    /**
     * This method creates an PostalAddressType from the values in the provided map.
     * The following keys are expected in the provided map (Taken from EbrimConstants):
     * <p>
     * CITY = "city";
     * COUNTRY = "country";
     * POSTAL_CODE = "postalCode";
     * STATE_OR_PROVINCE = "stateOrProvince";
     * STREET = "street";
     * STREET_NUMBER = "streetNumber";
     *
     * @param map the Map representation of the PostalAddressType to generate, null returns empty Optional
     * @return Optional PostalAddressType created from the values in the map
     */
    public Optional<PostalAddressType> convert(Map<String, Object> map) {
        Optional<PostalAddressType> optionalAddress = Optional.empty();
        if (MapUtils.isEmpty(map)) {
            return optionalAddress;
        }

        String valueToPopulate = MapUtils.getString(map, CITY);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            if (!optionalAddress.isPresent()) {
                optionalAddress = Optional.of(RIM_FACTORY.createPostalAddressType());
            }
            optionalAddress.get()
                    .setCity(valueToPopulate);
        }

        valueToPopulate = MapUtils.getString(map, COUNTRY);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            if (!optionalAddress.isPresent()) {
                optionalAddress = Optional.of(RIM_FACTORY.createPostalAddressType());
            }
            optionalAddress.get()
                    .setCountry(valueToPopulate);
        }

        valueToPopulate = MapUtils.getString(map, POSTAL_CODE);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            if (!optionalAddress.isPresent()) {
                optionalAddress = Optional.of(RIM_FACTORY.createPostalAddressType());
            }
            optionalAddress.get()
                    .setPostalCode(valueToPopulate);
        }

        valueToPopulate = MapUtils.getString(map, STATE_OR_PROVINCE);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            if (!optionalAddress.isPresent()) {
                optionalAddress = Optional.of(RIM_FACTORY.createPostalAddressType());
            }
            optionalAddress.get()
                    .setStateOrProvince(valueToPopulate);
        }

        valueToPopulate = MapUtils.getString(map, STREET);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            if (!optionalAddress.isPresent()) {
                optionalAddress = Optional.of(RIM_FACTORY.createPostalAddressType());
            }
            optionalAddress.get()
                    .setStreet(valueToPopulate);
        }

        valueToPopulate = MapUtils.getString(map, STREET_NUMBER);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            if (!optionalAddress.isPresent()) {
                optionalAddress = Optional.of(RIM_FACTORY.createPostalAddressType());
            }
            optionalAddress.get()
                    .setStreetNumber(valueToPopulate);
        }

        return optionalAddress;
    }

}
