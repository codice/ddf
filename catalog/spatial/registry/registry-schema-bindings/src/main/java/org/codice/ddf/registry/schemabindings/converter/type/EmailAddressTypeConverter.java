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

import static org.codice.ddf.registry.schemabindings.EbrimConstants.ADDRESS;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.RIM_FACTORY;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.TYPE;

import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.EmailAddressType;

public class EmailAddressTypeConverter {

    /**
     * This method creates an EmailAddressType from the values in the provided map.
     * The following keys are expected in the provided map (Taken from EbrimConstants):
     * <p>
     * ADDRESS = "address";
     * TYPE = "type";
     *
     * @param map the Map representation of the EmailAddressType to generate, null returns empty Optional
     * @return Optional EmailAddressType created from the values in the map
     */
    public Optional<EmailAddressType> convert(Map<String, Object> map) {
        Optional<EmailAddressType> optionalEmailAddress = Optional.empty();

        String valueToPopulate = MapUtils.getString(map, ADDRESS);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            if (!optionalEmailAddress.isPresent()) {
                optionalEmailAddress = Optional.of(RIM_FACTORY.createEmailAddressType());
            }
            optionalEmailAddress.get()
                    .setAddress(valueToPopulate);
        }

        valueToPopulate = MapUtils.getString(map, TYPE);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            if (!optionalEmailAddress.isPresent()) {
                optionalEmailAddress = Optional.of(RIM_FACTORY.createEmailAddressType());
            }
            optionalEmailAddress.get()
                    .setType(valueToPopulate);
        }

        return optionalEmailAddress;
    }
}
