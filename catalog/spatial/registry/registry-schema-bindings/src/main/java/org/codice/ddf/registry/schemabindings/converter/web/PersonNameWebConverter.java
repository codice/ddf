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

import oasis.names.tc.ebxml_regrep.xsd.rim._3.PersonNameType;

public class PersonNameWebConverter {
    public static final String FIRST_NAME = "firstName";

    public static final String LAST_NAME = "lastName";

    public static final String MIDDLE_NAME = "middleName";

    private WebMapHelper webMapHelper = new WebMapHelper();

    /**
     * This method creates a Map<String, Object> representation of the PersonNameType provided.
     * The following keys will be added to the map (Taken from EbrimConstants):
     * <p>
     * FIRST_NAME = "firstName";
     * MIDDLE_NAME = "middleName";
     * LAST_NAME = "lastName";
     *
     * @param personName the PersonNameType to be converted into a map, null returns empty Map
     * @return Map<String, Object> representation of the PersonNameType provided
     */
    public Map<String, Object> convert(PersonNameType personName) {
        Map<String, Object> personNameMap = new HashMap<>();
        if (personName == null) {
            return personNameMap;
        }

        webMapHelper.putIfNotEmpty(personNameMap, FIRST_NAME, personName.getFirstName());
        webMapHelper.putIfNotEmpty(personNameMap, LAST_NAME, personName.getLastName());
        webMapHelper.putIfNotEmpty(personNameMap, MIDDLE_NAME, personName.getMiddleName());

        return personNameMap;
    }
}
