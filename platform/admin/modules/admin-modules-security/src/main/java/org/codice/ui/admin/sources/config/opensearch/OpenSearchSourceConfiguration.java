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

package org.codice.ui.admin.sources.config.opensearch;

import java.util.HashMap;
import java.util.Map;

import org.codice.ui.admin.sources.config.SourceConfiguration;

public class OpenSearchSourceConfiguration extends SourceConfiguration {

    private static final String OPENSEARCH_SOURCE_DISPLAY_NAME = "OpenSearch Source";

    public OpenSearchSourceConfiguration(SourceConfiguration baseConfig) {
        displayName(OPENSEARCH_SOURCE_DISPLAY_NAME);
        factoryPid(baseConfig.factoryPid());
        sourceName(baseConfig.sourceName());
        sourceHostName(baseConfig.sourceHostName());
        sourcePort(baseConfig.sourcePort());
        sourceUserName(baseConfig.sourceUserName());
        sourceUserPassword(baseConfig.sourceUserPassword());
        endpointUrl(baseConfig.endpointUrl());
    }

    public Map<String, String> configMap() {
        HashMap<String, String> config = new HashMap<>();
        config.put("id", sourceName());
        config.put("endpointUrl", endpointUrl());
        if (sourceUserName() != null) {
            config.put("username", sourceUserName());
        }
        if (sourceUserPassword() != null) {
            config.put("password", sourceUserPassword());
        }
        return config;
    }
}
