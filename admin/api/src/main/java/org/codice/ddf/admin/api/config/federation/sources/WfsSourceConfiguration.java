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

package org.codice.ddf.admin.api.config.federation.sources;

import static org.codice.ddf.admin.api.handler.ConfigurationMessage.createInvalidFieldMsg;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.createMissingRequiredFieldMsg;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codice.ddf.admin.api.config.ConfigurationType;
import org.codice.ddf.admin.api.config.federation.SourceConfiguration;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;

public class WfsSourceConfiguration extends SourceConfiguration {

    public static final String CONFIGURATION_TYPE = "wfs-source";

    //** wfs Service Properties
    public static final String WFS_URL = "wfsUrl";
    public static final String WFS_SOURCE_DISPLAY_NAME = "WFS Source";
    public static final String WFS1_FACTORY_PID = "Wfs_v1_0_0_Federated_Source";
    public static final String WFS2_FACTORY_PID = "Wfs_v2_0_0_Federated_Source";
    static final List<String> WFS_FACTORY_PIDS = Arrays.asList(WFS1_FACTORY_PID, WFS2_FACTORY_PID);
    // ----

    //TODO: Needs WFS specific properties

    public WfsSourceConfiguration() {

    }

    public WfsSourceConfiguration(Map<String, Object> wfsProps) {
        factoryPid(wfsProps.get(FACTORY_PID_KEY) == null ?
                null :
                (String) wfsProps.get(FACTORY_PID_KEY));
        servicePid(wfsProps.get(SERVICE_PID_KEY) == null ?
                null :
                (String) wfsProps.get(SERVICE_PID_KEY));
        sourceName(wfsProps.get(SOURCE_NAME) == null ? null : (String) wfsProps.get(SOURCE_NAME));
        endpointUrl(wfsProps.get(WFS_URL) == null ? null : (String) wfsProps.get(WFS_URL));
        sourceUserName(wfsProps.get(USERNAME) == null ? null : (String) wfsProps.get(USERNAME));
        sourceUserPassword(wfsProps.get(PASSWORD) == null ? null : (String) wfsProps.get(PASSWORD));
    }

    public WfsSourceConfiguration(SourceConfiguration baseConfig) {
        factoryPid(baseConfig.factoryPid());
        sourceName(baseConfig.sourceName());
        sourceHostName(baseConfig.sourceHostName());
        sourcePort(baseConfig.sourcePort());
        sourceUserName(baseConfig.sourceUserName());
        sourceUserPassword(baseConfig.sourceUserPassword());
        endpointUrl(baseConfig.endpointUrl());
        trustedCertAuthority(baseConfig.trustedCertAuthority());
    }

    public List<ConfigurationMessage> validate(List<String> fields) {
        List<ConfigurationMessage> errors = super.validate(fields);
        for (String field : fields) {
            switch (field) {
            case FACTORY_PID:
                if (factoryPid() == null) {
                    errors.add(createMissingRequiredFieldMsg(FACTORY_PID));
                }
                if (!WFS_FACTORY_PIDS.contains(factoryPid())) {
                    errors.add(createInvalidFieldMsg("Unknown factory PID type.", FACTORY_PID));
                }
                break;
            }
        }
        return errors;
    }

    public Map<String, Object> configMap() {
        HashMap<String, Object> config = new HashMap<>();
        config.put(SOURCE_NAME, sourceName());
        config.put(WFS_URL, endpointUrl());
        if (sourceUserName() != null) {
            config.put(USERNAME, sourceUserName());
        }
        if (sourceUserPassword() != null) {
            config.put(PASSWORD, sourceUserPassword());
        }
        return config;
    }

    @Override
    public ConfigurationType getConfigurationType() {
        return new ConfigurationType(CONFIGURATION_TYPE, WfsSourceConfiguration.class);
    }
}
