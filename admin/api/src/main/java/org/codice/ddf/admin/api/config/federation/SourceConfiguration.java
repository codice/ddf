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

package org.codice.ddf.admin.api.config.federation;

import static org.codice.ddf.admin.api.handler.ConfigurationMessage.createInvalidFieldMsg;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.createMissingRequiredFieldMsg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.codice.ddf.admin.api.commons.SourceUtils;
import org.codice.ddf.admin.api.config.Configuration;
import org.codice.ddf.admin.api.config.ConfigurationType;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;

public class SourceConfiguration extends Configuration {

    public static final String CONFIGURATION_TYPE = "sources";

    public static final String ID = "id";
    public static final String HOSTNAME = "hostname";
    public static final String PORT = "port";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String ENDPOINT_URL = "url";
    public static final String FACTORY_PID = "factoryPid";
    public static final String SERVICE_PID = "servicePid";

    private String sourceName;
    private String sourceHostName;
    private int sourcePort;
    private String sourceUserName;
    private String sourceUserPassword;
    private String endpointUrl;
    private String factoryPid;
    private String servicePid;

    private boolean certError;
    private boolean trustedCertAuthority;

    public List<ConfigurationMessage> validate(List<String> fields) {
        //TODO adimka Add subtypes to error messages once they are available
        List<ConfigurationMessage> errors = new ArrayList<>();
        for (String field : fields) {
            switch (field) {
            case ID:
                if (sourceName() == null || sourceName().isEmpty()) {
                    errors.add(createMissingRequiredFieldMsg(ID));
                    //TODO confirm ID being added is unique
                }
                break;
            case HOSTNAME:
                if (sourceHostName() == null) {
                    errors.add(createMissingRequiredFieldMsg(HOSTNAME));
                } else {
                    if(!SourceUtils.validHostnameFormat(sourceHostName())) {
                        errors.add(createInvalidFieldMsg("Hostname format is invalid.", HOSTNAME));
                    }
                }
                break;
            case PORT:
                if (!SourceUtils.validPortFormat(sourcePort())) {
                    errors.add(createInvalidFieldMsg("Port is not in valid range.", PORT));
                }
                break;
            case USERNAME:
                if (sourceUserName() == null) {
                    errors.add(createMissingRequiredFieldMsg(USERNAME));
                    // TODO: tbatie - 1/13/17 - If the username is specified but not password error msg, maybe in enforce this at the Handler level instead
                }
                break;
            case PASSWORD:
                if (sourceUserPassword() == null) {
                    errors.add(createMissingRequiredFieldMsg(PASSWORD));
                }
                break;
            case ENDPOINT_URL:
                if (endpointUrl() == null) {
                    errors.add(createInvalidFieldMsg("Configuration does not contain an endpoint URL.", ENDPOINT_URL));
                }
                if (SourceUtils.validUrlFormat(endpointUrl())) {
                    errors.add(createInvalidFieldMsg("Endpoint URL is not in a valid format.", ENDPOINT_URL));
                }
                break;
            case SERVICE_PID:
                if (servicePid() == null) {
                    errors.add(createMissingRequiredFieldMsg(SERVICE_PID));
                }
            }
        }
        return errors;
    }

    public boolean trustedCertAuthority() {
        return trustedCertAuthority;
    }

    public boolean certError() {
        return certError;
    }

    public SourceConfiguration certError(boolean certError) {
        this.certError = certError;
        return this;
    }

    public SourceConfiguration trustedCertAuthority(boolean trustedCertAuthority) {
        this.trustedCertAuthority = trustedCertAuthority;
        return this;
    }

    public String sourceName() {
        return sourceName;
    }

    public SourceConfiguration sourceName(String sourceName) {
        this.sourceName = sourceName;
        return this;
    }

    public String factoryPid() {
        return factoryPid;
    }

    public SourceConfiguration factoryPid(String factoryPid) {
        this.factoryPid = factoryPid;
        return this;
    }

    public String sourceUserPassword() {
        return sourceUserPassword;
    }

    public SourceConfiguration sourceUserPassword(String sourceUserPassword) {
        this.sourceUserPassword = sourceUserPassword;
        return this;
    }

    public String sourceUserName() {
        return sourceUserName;
    }

    public SourceConfiguration sourceUserName(String sourceUserName) {
        this.sourceUserName = sourceUserName;
        return this;
    }

    public String endpointUrl() {
        return endpointUrl;
    }

    public SourceConfiguration endpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
        return this;
    }

    public String sourceHostName() {
        return sourceHostName;
    }

    public SourceConfiguration sourceHostName(String sourceHostName) {
        this.sourceHostName = sourceHostName;
        return this;
    }

    public int sourcePort() {
        return sourcePort;
    }

    public SourceConfiguration sourcePort(int sourcePort) {
        this.sourcePort = sourcePort;
        return this;
    }

    public String servicePid() {
        return servicePid;
    }

    public SourceConfiguration servicePid(String servicePid) {
        this.servicePid = servicePid;
        return this;
    }

    public Map<String, Object> configMap() {
        return null;
    }

    @Override
    public ConfigurationType getConfigurationType() {
        return new ConfigurationType(CONFIGURATION_TYPE, SourceConfiguration.class);
    }
}
