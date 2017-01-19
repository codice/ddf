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

package org.codice.ddf.admin.api.services;

import static org.codice.ddf.admin.api.validation.ValidationUtils.FACTORY_PID_KEY;
import static org.codice.ddf.admin.api.validation.ValidationUtils.SERVICE_PID_KEY;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codice.ddf.admin.api.config.sources.CswSourceConfiguration;

import com.google.common.collect.ImmutableList;

public class CswServiceProperties {

    // --- Csw service properties
    public static final String ID = "id";
    public static final String SOURCE_NAME = "sourceName";
    public static final String CSW_URL = "cswUrl";
    public static final String EVENT_SERVICE_ADDRESS = "eventServiceAddress";
    public static final String OUTPUT_SCHEMA = "outputSchema";
    public static final String FORCE_SPATIAL_FILTER = "forceSpatialFilter";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String CSW_PROFILE_FACTORY_PID = "Csw_Federation_Profile_Source";
    public static final String CSW_GMD_FACTORY_PID = "Gmd_Csw_Federated_Source";
    public static final String CSW_SPEC_FACTORY_PID = "Csw_Federated_Source";
    public static final List<String> CSW_FACTORY_PIDS = ImmutableList.of(CSW_PROFILE_FACTORY_PID, CSW_GMD_FACTORY_PID, CSW_SPEC_FACTORY_PID);
    // ---


    public static final CswSourceConfiguration servicePropsToCswConfig(Map<String, Object> cswSourceProps) {
        CswSourceConfiguration cswConfig = new CswSourceConfiguration();
        cswConfig.factoryPid(cswSourceProps.get(FACTORY_PID_KEY) == null ? null : (String) cswSourceProps.get(FACTORY_PID_KEY));
        cswConfig.servicePid(cswSourceProps.get(SERVICE_PID_KEY) == null ? null : (String) cswSourceProps.get(SERVICE_PID_KEY));
        cswConfig.sourceName(cswSourceProps.get(SOURCE_NAME) == null ? null : (String) cswSourceProps.get(SOURCE_NAME));
        cswConfig.endpointUrl(cswSourceProps.get(CSW_URL) == null ? null : (String) cswSourceProps.get(CSW_URL));
        cswConfig.outputSchema(cswSourceProps.get(OUTPUT_SCHEMA) == null ? null : (String) cswSourceProps.get(OUTPUT_SCHEMA));
        return cswConfig;
    }

    public static final Map<String, Object> cswConfigToServiceProps(CswSourceConfiguration config) {
        HashMap<String, Object> props = new HashMap<>();
        props.put(ID, config.sourceName());
        props.put(CSW_URL, config.endpointUrl());
        if (!config.factoryPid().equals(CSW_GMD_FACTORY_PID)) {
            props.put(EVENT_SERVICE_ADDRESS, config.endpointUrl() + "/subscription");
        }
        if (config.sourceUserName() != null) {
            props.put(USERNAME, config.sourceUserName());
        }
        if (config.sourceUserPassword() != null) {
            props.put(PASSWORD, config.sourceUserPassword());
        }
        if (config.outputSchema() != null) {
            props.put(OUTPUT_SCHEMA, config.outputSchema());
        }
        if (config.forceSpatialFilter() != null) {
            props.put(FORCE_SPATIAL_FILTER, config.forceSpatialFilter());
        }
        return props;
    }
}
