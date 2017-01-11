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

import java.util.HashMap;
import java.util.Map;

import org.codice.ddf.admin.api.config.federation.SourceConfiguration;

public class OpenSearchSourceConfiguration extends SourceConfiguration {

    // Open Search Service Properties
    public static final String ID = "id";

    public static final String ENDPOINT_URL = "endpointUrl";

    public static final String USERNAME = "username";

    public static final String PASSWORD = "password";
    // ----

    public OpenSearchSourceConfiguration(Map<String, Object> props) {
        factoryPid(props.get(FACTORY_PID_KEY) == null ? null : (String) props.get(FACTORY_PID_KEY));
        servicePid(props.get(SERVICE_PID_KEY) == null ? null : (String) props.get(SERVICE_PID_KEY));
        sourceName(props.get(ID) == null ? null : (String) props.get(ID));
        endpointUrl(props.get(ENDPOINT_URL) == null ? null : (String) props.get(ENDPOINT_URL));
        sourceUserName(props.get(USERNAME) == null ? null : (String) props.get(USERNAME));
        sourceUserPassword(props.get(PASSWORD) == null ? null : (String) props.get(PASSWORD));
    }

    public OpenSearchSourceConfiguration(SourceConfiguration baseConfig) {
        factoryPid(baseConfig.factoryPid());
        sourceName(baseConfig.sourceName());
        sourceHostName(baseConfig.sourceHostName());
        sourcePort(baseConfig.sourcePort());
        sourceUserName(baseConfig.sourceUserName());
        sourceUserPassword(baseConfig.sourceUserPassword());
        endpointUrl(baseConfig.endpointUrl());
    }

    public Map<String, Object> configMap() {
        HashMap<String, Object> config = new HashMap<>();
        config.put(ID, sourceName());
        config.put(ENDPOINT_URL, endpointUrl());
        if (sourceUserName() != null) {
            config.put(USERNAME, sourceUserName());
        }
        if (sourceUserPassword() != null) {
            config.put(PASSWORD, sourceUserPassword());
        }
        return config;
    }
}
