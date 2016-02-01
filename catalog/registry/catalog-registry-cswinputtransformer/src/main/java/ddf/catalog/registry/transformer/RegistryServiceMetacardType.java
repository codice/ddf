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
package ddf.catalog.registry.transformer;

import ddf.catalog.data.Metacard;

/**
 * MetacardType for registry service metacards
 */
public class RegistryServiceMetacardType extends RegistryObjectMetacardType {
    public static final String SERVICE_REGISTRY_METACARD_TYPE_NAME = "registry.service";

    public static final String LIVE_DATE = "live-date";

    public static final String DATA_START_DATE = "data-start-date";

    public static final String DATA_END_DATE = "data-end-date";

    public static final String LINKS = "links";

    public static final String REGION = "region";

    public static final String DATA_BOUNDS = "data-bounds";

    public static final String DATA_REGION = "data-region";

    public static final String DATA_SOURCES = "input-data-sources";

    public static final String DATA_TYPES = "data-types";

    //list of all the service binding ids
    public static final String SERVICE_BINDINGS = "service-bindings";

    //list of bindingType fields from all the service bindings
    public static final String SERVICE_BINDING_TYPES = "service-binding-types";

    public RegistryServiceMetacardType() {
        super(SERVICE_REGISTRY_METACARD_TYPE_NAME, null);
        addServiceAttributes();
    }

    private void addServiceAttributes() {
        addQueryableDate(LIVE_DATE);
        addQueryableDate(DATA_START_DATE);
        addQueryableDate(DATA_END_DATE);
        addQueryableString(LINKS, true);
        addQueryableGeo(Metacard.GEOGRAPHY, false);
        addQueryableString(REGION, false);
        addQueryableGeo(DATA_BOUNDS, true);
        addQueryableString(DATA_REGION, false);
        addQueryableString(DATA_SOURCES, true);
        addQueryableString(DATA_TYPES, true);
        addQueryableString(SERVICE_BINDINGS, true);
        addQueryableString(SERVICE_BINDING_TYPES, true);
    }
}
