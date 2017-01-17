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

public class CswSourceConfiguration extends SourceConfiguration {

    public static final String CONFIGURATION_TYPE = "csw-source";

    //** Csw Service Properties
    public static final String ID = "id";
    public static final String CSW_SOURCE_DISPLAY_NAME = "CSW Source";
    public static final String CSW_PROFILE_FACTORY_PID = "Csw_Federation_Profile_Source";
    public static final String CSW_GMD_FACTORY_PID = "Gmd_Csw_Federated_Source";
    public static final String CSW_SPEC_FACTORY_PID = "Csw_Federated_Source";

    static final List<String> CSW_FACTORY_PIDS = Arrays.asList(CSW_PROFILE_FACTORY_PID, CSW_GMD_FACTORY_PID, CSW_SPEC_FACTORY_PID);

    public static final String CSW_URL = "cswUrl";
    public static final String EVENT_SERVICE_ADDRESS = "eventServiceAddress";
    public static final String OUTPUT_SCHEMA = "outputSchema";
    public static final String FORCE_SPATIAL_FILTER = "forceSpatialFilter";
    // TODO: tbatie - 12/20/16 - Include service properties for registering for events and the even service address

    private String outputSchema;
    private String forceSpatialFilter;

    public CswSourceConfiguration() {

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
        trustedCertAuthority(baseConfig.trustedCertAuthority());
        if (baseConfig instanceof CswSourceConfiguration) {
            outputSchema(((CswSourceConfiguration) baseConfig).outputSchema());
            forceSpatialFilter(((CswSourceConfiguration) baseConfig).forceSpatialFilter());
        }
    }

    public CswSourceConfiguration(Map<String, Object> cswSourceProps) {
        factoryPid(cswSourceProps.get(FACTORY_PID_KEY) == null ?
                null :
                (String) cswSourceProps.get(FACTORY_PID_KEY));
        servicePid(cswSourceProps.get(SERVICE_PID_KEY) == null ?
                null :
                (String) cswSourceProps.get(SERVICE_PID_KEY));
        sourceName(cswSourceProps.get(SOURCE_NAME) == null ? null : (String) cswSourceProps.get(
                SOURCE_NAME));
        endpointUrl(
                cswSourceProps.get(CSW_URL) == null ? null : (String) cswSourceProps.get(CSW_URL));
        outputSchema(cswSourceProps.get(OUTPUT_SCHEMA) == null ?
                null :
                (String) cswSourceProps.get(OUTPUT_SCHEMA));
    }

    public List<ConfigurationMessage> validate(List<String> fields) {
        List<ConfigurationMessage> errors = super.validate(fields);
        for(String field : fields) {
            switch (field) {
            case FACTORY_PID:
                if (factoryPid() == null) {
                    errors.add(createMissingRequiredFieldMsg(FACTORY_PID));
                }
                if (!CSW_FACTORY_PIDS.contains(factoryPid())) {
                    errors.add(createInvalidFieldMsg("Configuration factory PID does not belong to a CSW Source factory.", FACTORY_PID));
                }
                break;
            }
        }
        return errors;
    }

    // TODO: tbatie - 1/11/17 - Let's do this in the probe method or source utils instead, this is external to the configuration class since these keys a specific to the Csw Source MSF and hopefully we can pass structured data instead of maps of strings one day
    public Map<String, Object> configMap() {
        HashMap<String, Object> config = new HashMap<>();
        config.put(ID, sourceName());
        config.put(CSW_URL, endpointUrl());
        if (!factoryPid().equals(CSW_GMD_FACTORY_PID)) {
            config.put(EVENT_SERVICE_ADDRESS, endpointUrl() + "/subscription");
        }
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

    @Override
    public ConfigurationType getConfigurationType() {
        return new ConfigurationType(CONFIGURATION_TYPE, CswSourceConfiguration.class);
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


}
