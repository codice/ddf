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

package org.codice.ddf.admin.security.ldap.embedded;

import java.util.HashMap;
import java.util.Map;

import org.codice.ddf.admin.security.ldap.commons.LdapConfiguration;

public class EmbeddedLdapConfiguration extends LdapConfiguration {

    private int embeddedLdapPort;
    private int embeddedLdapsPort;
    private int embeddedLdapAdminPort;
    private String ldifPath;
    private String embeddedLdapStorageLocation;

    public static EmbeddedLdapConfiguration fromProperties(Map<String, Object> props) {
        EmbeddedLdapConfiguration config = new EmbeddedLdapConfiguration();
        config.embeddedLdapPort = (int) props.get("embeddedLdapPort");
        config.embeddedLdapsPort = (int) props.get("embeddedLdapsPort");
        config.embeddedLdapAdminPort = (int) props.get("embeddedLdapAdminPort");
        config.ldifPath = (String) props.get("ldifPath");
        config.embeddedLdapStorageLocation = (String) props.get("embeddedLdapStorageLocation");

        return config;
    }

    public Map<String, Object> toPropertiesMap() {
        Map<String, Object> props = new HashMap<>();
        props.put("embeddedLdapPort", embeddedLdapPort);
        props.put("embeddedLdapsPort", embeddedLdapsPort);
        props.put("embeddedLdapAdminPort", embeddedLdapAdminPort);
        props.put("ldifPath", ldifPath);
        props.put("embeddedLdapStorageLocation", embeddedLdapStorageLocation);
        return props;
    }
}
