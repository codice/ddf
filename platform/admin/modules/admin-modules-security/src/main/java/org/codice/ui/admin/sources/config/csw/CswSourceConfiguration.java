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

package org.codice.ui.admin.sources.config.csw;

import java.util.HashMap;
import java.util.Map;

import org.codice.ui.admin.sources.config.SourceConfiguration;

public class CswSourceConfiguration extends SourceConfiguration {
    private static final String CSW_SOURCE_DISPLAY_NAME = "CSW Source";

    private String outputSchema;

    private String forceSpatialFilter;

    public CswSourceConfiguration(SourceConfiguration baseConfig) {
        displayName(CSW_SOURCE_DISPLAY_NAME);
        factoryPid(baseConfig.factoryPid());
        sourceName(baseConfig.sourceName());
        sourceHostName(baseConfig.sourceHostName());
        sourcePort(baseConfig.sourcePort());
        sourceUserName(baseConfig.sourceUserName());
        sourceUserPassword(baseConfig.sourceUserPassword());
        endpointUrl(baseConfig.endpointUrl());
        if (baseConfig instanceof CswSourceConfiguration) {
            outputSchema(((CswSourceConfiguration) baseConfig).outputSchema());
            forceSpatialFilter(((CswSourceConfiguration) baseConfig).forceSpatialFilter());
        }
    }

    public CswSourceConfiguration outputSchema(String outputSchema) {
        this.outputSchema = outputSchema;
        return this;
    }

    public String outputSchema() {
        return outputSchema;
    }

    public CswSourceConfiguration forceSpatialFilter(String forceSpatialFilter) {
        this.forceSpatialFilter = forceSpatialFilter;
        return this;
    }

    public String forceSpatialFilter() {
        return forceSpatialFilter;
    }

    public Map<String, String> configMap() {
        HashMap<String, String> config = new HashMap<>();
        config.put("id", sourceName());
        config.put("cswUrl", endpointUrl());
        if (sourceUserName() != null) {
            config.put("username", sourceUserName());
        }
        if (sourceUserPassword() != null) {
            config.put("password", sourceUserPassword());
        }
        if (outputSchema() != null) {
            config.put("outputSchema", outputSchema());
        }
        if (forceSpatialFilter() != null) {
            config.put("forceSpatialFilter", forceSpatialFilter());
        }
        return config;
    }
}
