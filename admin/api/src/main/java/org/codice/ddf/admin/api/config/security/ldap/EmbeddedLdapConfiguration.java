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

package org.codice.ddf.admin.api.config.security.ldap;

import org.codice.ddf.admin.api.config.ConfigurationType;

public class EmbeddedLdapConfiguration extends LdapConfiguration {

    public static final String CONFIGURATION_TYPE = "embedded-ldap";

    private int embeddedLdapPort;
    private int embeddedLdapsPort;
    private int embeddedLdapAdminPort;
    private String ldifPath;
    private String embeddedLdapStorageLocation;

    public void embeddedLdapPort(int embeddedLdapPort) {
        this.embeddedLdapPort = embeddedLdapPort;
    }

    public void embeddedLdapsPort(int embeddedLdapsPort) {
        this.embeddedLdapsPort = embeddedLdapsPort;
    }

    public void embeddedLdapAdminPort(int embeddedLdapAdminPort) {
        this.embeddedLdapAdminPort = embeddedLdapAdminPort;
    }

    public void ldifPath(String ldifPath) {
        this.ldifPath = ldifPath;
    }

    public void embeddedLdapStorageLocation(String embeddedLdapStorageLocation) {
        this.embeddedLdapStorageLocation = embeddedLdapStorageLocation;
    }

    @Override
    public ConfigurationType getConfigurationType() {
        return new ConfigurationType(CONFIGURATION_TYPE, EmbeddedLdapConfiguration.class);
    }
}
