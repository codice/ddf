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

import static org.codice.ddf.registry.schemabindings.EbrimConstants.PHONE_AREA_CODE;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.PHONE_COUNTRY_CODE;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.PHONE_EXTENSION;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.PHONE_NUMBER;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.PHONE_TYPE;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.RIM_FACTORY;

import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.TelephoneNumberType;

public class TelephoneNumberTypeConverter {

    /**
     * This method creates an OrganizationType from the values in the provided map.
     * The following keys are expected in the provided map (Taken from EbrimConstants):
     * <p>
     * PHONE_COUNTRY_CODE = "countryCode";
     * PHONE_TYPE = "phoneType";
     * PHONE_AREA_CODE = "areaCode";
     * PHONE_NUMBER = "number";
     * PHONE_EXTENSION = "extension";
     *
     * @param map the Map representation of the OrganizationType to generate, null returns empty Optional
     * @return Optional OrganizationType created from the values in the map
     */
    public Optional<TelephoneNumberType> convert(Map<String, Object> map) {
        Optional<TelephoneNumberType> optionalTelephone = Optional.empty();

        String valueToPopulate = MapUtils.getString(map, PHONE_AREA_CODE);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            if (!optionalTelephone.isPresent()) {
                optionalTelephone = Optional.of(RIM_FACTORY.createTelephoneNumberType());
            }
            optionalTelephone.get()
                    .setAreaCode(valueToPopulate);
        }

        valueToPopulate = MapUtils.getString(map, PHONE_COUNTRY_CODE);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            if (!optionalTelephone.isPresent()) {
                optionalTelephone = Optional.of(RIM_FACTORY.createTelephoneNumberType());
            }
            optionalTelephone.get()
                    .setCountryCode(valueToPopulate);
        }

        valueToPopulate = MapUtils.getString(map, PHONE_EXTENSION);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            if (!optionalTelephone.isPresent()) {
                optionalTelephone = Optional.of(RIM_FACTORY.createTelephoneNumberType());
            }
            optionalTelephone.get()
                    .setExtension(valueToPopulate);
        }

        valueToPopulate = MapUtils.getString(map, PHONE_NUMBER);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            if (!optionalTelephone.isPresent()) {
                optionalTelephone = Optional.of(RIM_FACTORY.createTelephoneNumberType());
            }
            optionalTelephone.get()
                    .setNumber(valueToPopulate);
        }

        valueToPopulate = MapUtils.getString(map, PHONE_TYPE);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            if (!optionalTelephone.isPresent()) {
                optionalTelephone = Optional.of(RIM_FACTORY.createTelephoneNumberType());
            }
            optionalTelephone.get()
                    .setPhoneType(valueToPopulate);
        }

        return optionalTelephone;
    }
}
