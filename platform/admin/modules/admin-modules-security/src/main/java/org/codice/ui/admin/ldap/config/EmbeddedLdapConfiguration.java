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

package org.codice.ui.admin.ldap.config;

import java.util.HashMap;
import java.util.Map;

import org.codice.ui.admin.wizard.config.Configuration;

public class EmbeddedLdapConfiguration extends Configuration {

    private int embeddedLdapPort;

    private int embeddedLdapsPort;

    private int embeddedLdapAdminPort;

    private String ldifPath;

    private String embeddedLdapStorageLocation;

    public Map<String, Object> toPropertiesMap() {
        Map props = new HashMap<>();
        props.put("embeddedLdapPort", embeddedLdapPort);
        props.put("embeddedLdapsPort", embeddedLdapsPort);
        props.put("embeddedLdapAdminPort", embeddedLdapAdminPort);
        props.put("ldifPath", ldifPath);
        props.put("embeddedLdapStorageLocation", embeddedLdapStorageLocation);
        return props;
    }

}
