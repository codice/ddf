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

package org.codice.ui.admin.sources.config;

import java.util.Map;

import org.codice.ui.admin.wizard.config.Configuration;

public class SourceConfiguration extends Configuration {

    private String sourceHostName;

    private int sourcePort;

    private String sourceUserName;

    private String sourceUserPassword;

    private String endpointUrl;

    private String factoryPid;

    private String sourceName;

    private String displayName;

    private boolean trustedCertAuthority;

    public boolean trustedCertAuthority() {
        return trustedCertAuthority;
    }

    public SourceConfiguration trustedCertAuthority(boolean trustedCertAuthority) {
        this.trustedCertAuthority = trustedCertAuthority;
        return this;
    }

    public String displayName() {
        return displayName;
    }

    public SourceConfiguration displayName(String displayName) {
        this.displayName = displayName;
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

    public Map<String, String> configMap() {
        return null;
    }

    // TODO: tbatie - 11/29/16 - Do we need this method still?
    public SourceConfiguration copy() {
        return new SourceConfiguration().factoryPid(factoryPid)
                .sourceUserName(sourceUserName)
                .sourceUserPassword(sourceUserPassword)
                .sourceHostName(sourceHostName)
                .sourcePort(sourcePort)
                .endpointUrl(endpointUrl)
                .displayName(displayName)
                .trustedCertAuthority(trustedCertAuthority);
    }
}
