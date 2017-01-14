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
package org.codice.ddf.admin.security.ldap.probe;


import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.LDAP_TYPE;

import java.util.ArrayList;
import java.util.Map;

import org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration;
import org.codice.ddf.admin.api.handler.method.ProbeMethod;
import org.codice.ddf.admin.api.handler.report.ProbeReport;

public class BindUserExampleProbe extends ProbeMethod<LdapConfiguration> {
    private static final String BIND_USER_EXAMPLE = "bindUserExample";

    private static final String DESCRIPTION =
            "Returns sample username formats for the specific LDAP server type.";

    private static final Map<String, String> REQUIRED_FIELDS = LdapConfiguration.buildFieldMap(
            LDAP_TYPE);

    public BindUserExampleProbe() {
        super(BIND_USER_EXAMPLE, DESCRIPTION, REQUIRED_FIELDS, null, null, null, null);
    }

    @Override
    public ProbeReport probe(LdapConfiguration configuration) {
        switch (configuration.ldapType()) {
        case "activeDirectory":
            return new ProbeReport(new ArrayList<>()).probeResult("bindUserDn", "user@domain");
        default:
            return new ProbeReport(new ArrayList<>()).probeResult("bindUserDn", "cn=admin");
        }
    }
}
