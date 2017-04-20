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
import java.util.Map;

import org.codice.ddf.admin.api.config.sources.OpenSearchSourceConfiguration;

public class OpensearchServiceProperties {

    // --- Opeansearch service properties
    public static final String OPENSEARCH_FACTORY_PID = "OpenSearchSource";
    public static final String ID = "id";
    public static final String ENDPOINT_URL = "endpointUrl";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    // ---

    public static final OpenSearchSourceConfiguration servicePropsToOpenSearchConfig(Map<String, Object> props){
        OpenSearchSourceConfiguration config = new OpenSearchSourceConfiguration();
        config.factoryPid(props.get(FACTORY_PID_KEY) == null ? null : (String) props.get(FACTORY_PID_KEY));
        config.servicePid(props.get(SERVICE_PID_KEY) == null ? null : (String) props.get(SERVICE_PID_KEY));
        config.sourceName(props.get(ID) == null ? null : (String) props.get(ID));
        config.endpointUrl(props.get(ENDPOINT_URL) == null ? null : (String) props.get(ENDPOINT_URL));
        config.sourceUserName(props.get(USERNAME) == null ? null : (String) props.get(USERNAME));
        config.sourceUserPassword(props.get(PASSWORD) == null ? null : (String) props.get(PASSWORD));
        return config;
    }

    public static final Map<String, Object> openSearchConfigToServiceProps(OpenSearchSourceConfiguration config) {
        HashMap<String, Object> props = new HashMap<>();
        props.put(ID, config.sourceName());
        props.put(ENDPOINT_URL, config.endpointUrl());
        if (config.sourceUserName() != null) {
            props.put(USERNAME, config.sourceUserName());
        }
        if (config.sourceUserPassword() != null) {
            props.put(PASSWORD, config.sourceUserPassword());
        }
        return props;
    }
}
