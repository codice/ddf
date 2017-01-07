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

package org.codice.ddf.admin.api.sources;

import java.util.Map;

import org.codice.ddf.admin.api.handler.Configuration;

public class SourceConfiguration extends Configuration {

    private String sourceHostName;

    private int sourcePort;

    private String sourceUserName;

    private String sourceUserPassword;

    private String endpointUrl;

    private String factoryPid;

    private String servicePid;

    private String sourceName;

    private boolean certError;

    private boolean trustedCertAuthority;

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
}
