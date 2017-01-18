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
package org.codice.ddf.admin.api.config.services;

import static org.codice.ddf.admin.api.config.validation.ValidationUtils.FACTORY_PID_KEY;
import static org.codice.ddf.admin.api.config.validation.ValidationUtils.SERVICE_PID_KEY;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codice.ddf.admin.api.config.sources.WfsSourceConfiguration;

import com.google.common.collect.ImmutableList;

public class WfsServiceProperties {
    // --- Wfs Service Properties
    public static final String ID = "id";
    public static final String WFS_URL = "wfsUrl";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String WFS1_FACTORY_PID = "Wfs_v1_0_0_Federated_Source";
    public static final String WFS2_FACTORY_PID = "Wfs_v2_0_0_Federated_Source";
    public static final List<String> WFS_FACTORY_PIDS = ImmutableList.of(WFS1_FACTORY_PID, WFS2_FACTORY_PID);
    // ----

    public static final WfsSourceConfiguration servicePropsToWfsConfig(Map<String, Object> props){
        WfsSourceConfiguration wfsConfig = new WfsSourceConfiguration();
        wfsConfig.factoryPid(props.get(FACTORY_PID_KEY) == null ?
                null :
                (String) props.get(FACTORY_PID_KEY));
        wfsConfig.servicePid(props.get(SERVICE_PID_KEY) == null ?
                null :
                (String) props.get(SERVICE_PID_KEY));
        wfsConfig.sourceName(props.get(ID) == null ? null : (String) props.get(ID));
        wfsConfig.endpointUrl(props.get(WFS_URL) == null ? null : (String) props.get(WFS_URL));
        wfsConfig.sourceUserName(props.get(USERNAME) == null ? null : (String) props.get(USERNAME));
        wfsConfig.sourceUserPassword(props.get(PASSWORD) == null ? null : (String) props.get(PASSWORD));
        return wfsConfig;
    }

    public static final Map<String, Object> wfsConfigToServiceProps(WfsSourceConfiguration configuration) {
        HashMap<String, Object> props = new HashMap<>();
        props.put(ID, configuration.sourceName());
        props.put(WFS_URL, configuration.endpointUrl());
        if (configuration.sourceUserName() != null) {
            props.put(USERNAME, configuration.sourceUserName());
        }
        if (configuration.sourceUserPassword() != null) {
            props.put(PASSWORD, configuration.sourceUserPassword());
        }
        return props;
    }
}
