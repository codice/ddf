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

import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.FAILURE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codice.ddf.admin.api.config.federation.SourceConfiguration;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;

public class WfsSourceConfiguration extends SourceConfiguration {

    //** wfs Service Properties
    public static final String WFS_URL = "wfsUrl";
    public static final String WFS_SOURCE_DISPLAY_NAME = "WFS Source";
    public static final String WFS1_FACTORY_PID = "Wfs_v1_0_0_Federated_Source";
    public static final String WFS2_FACTORY_PID = "Wfs_v2_0_0_Federated_Source";
    // ----

    //TODO: Needs WFS specific properties

    public WfsSourceConfiguration(Map<String, Object> wfsProps) {
        factoryPid(wfsProps.get(FACTORY_PID_KEY) == null ?
                null :
                (String) wfsProps.get(FACTORY_PID_KEY));
        servicePid(wfsProps.get(SERVICE_PID_KEY) == null ?
                null :
                (String) wfsProps.get(SERVICE_PID_KEY));
        sourceName(wfsProps.get(ID) == null ? null : (String) wfsProps.get(ID));
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
    }

    public List<ConfigurationMessage> validate(List<String> fields) {
        List<ConfigurationMessage> errors = super.validate(fields);
        for (String field : fields) {
            switch (field) {
            case FACTORY_PID:
                if (factoryPid() == null) {
                    errors.add(new ConfigurationMessage("Configuration does not contain a factory PID.", FAILURE));
                }
                if (!(factoryPid().equals(WFS1_FACTORY_PID) || factoryPid().equals(WFS2_FACTORY_PID))) {
                    errors.add(new ConfigurationMessage("Configuration does not contain a factory PID.", FAILURE));
                }
                break;
            }
        }
        return errors;
    }

    public Map<String, Object> configMap() {
        HashMap<String, Object> config = new HashMap<>();
        config.put(ID, sourceName());
        config.put(WFS_URL, endpointUrl());
        if (sourceUserName() != null) {
            config.put(USERNAME, sourceUserName());
        }
        if (sourceUserPassword() != null) {
            config.put(PASSWORD, sourceUserPassword());
        }
        return config;
    }
}
