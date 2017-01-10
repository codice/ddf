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

package org.codice.ddf.admin.sources.csw;

import java.util.HashMap;
import java.util.Map;

import org.codice.ddf.admin.api.sources.SourceConfiguration;

public class CswSourceConfiguration extends SourceConfiguration {
    //** Csw Service Properties
    public static final String ID = "id";
    public static final String CSW_URL = "cswUrl";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String OUTPUT_SCHEMA = "outputSchema";
    public static final String FORCE_SPATIAL_FILTER = "forceSpatialFilter";

    // TODO: tbatie - 12/20/16 - Include service properties for registering for events and the even service address
    // TODO: tbatie - 12/20/16 - Do we want to add the insane amount of extra fields offered in the CSW Specification Profile Federated Source?
    // TODO: tbatie - 12/20/16 - Do we want to add the insane amount of extra fields offered in the GMD CSW ISO Federated Source?
    //----

    private String outputSchema;

    private String forceSpatialFilter;

    public CswSourceConfiguration(Map<String, Object> cswSourceProps) {
        factoryPid(cswSourceProps.get(FACTORY_PID_KEY) == null ? null : (String)cswSourceProps.get(FACTORY_PID_KEY));
        servicePid(cswSourceProps.get(SERVICE_PID_KEY) == null ? null : (String)cswSourceProps.get(SERVICE_PID_KEY));
        sourceName(cswSourceProps.get(ID) == null ? null : (String) cswSourceProps.get(ID));
        endpointUrl(cswSourceProps.get(CSW_URL) == null ? null : (String) cswSourceProps.get(CSW_URL));
        outputSchema(cswSourceProps.get(OUTPUT_SCHEMA) == null ? null : (String) cswSourceProps.get(OUTPUT_SCHEMA));
    }

    public CswSourceConfiguration(SourceConfiguration baseConfig) {
        factoryPid(baseConfig.factoryPid());
        servicePid(baseConfig.servicePid());
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

    public Map<String, Object> configMap() {
        HashMap<String, Object> config = new HashMap<>();
        config.put(ID, sourceName());
        config.put(CSW_URL, endpointUrl());
        if (sourceUserName() != null) {
            config.put(USERNAME, sourceUserName());
        }
        if (sourceUserPassword() != null) {
            config.put(PASSWORD, sourceUserPassword());
        }
        if (outputSchema() != null) {
            config.put(OUTPUT_SCHEMA, outputSchema());
        }
        if (forceSpatialFilter() != null) {
            config.put(FORCE_SPATIAL_FILTER, forceSpatialFilter());
        }
        return config;
    }
}
