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

package org.codice.ddf.admin.security.context.probe;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.codice.ddf.admin.api.config.security.context.ContextPolicyConfiguration;
import org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration;
import org.codice.ddf.admin.api.handler.ConfigurationHandler;
import org.codice.ddf.admin.api.handler.method.ProbeMethod;
import org.codice.ddf.admin.api.handler.report.ProbeReport;
import org.codice.ddf.admin.api.persist.Configurator;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class AvaliableOptionsProbeMethod extends ProbeMethod<ContextPolicyConfiguration>{

    public static final String KARAF = "karaf";
    public static final String LDAP = "ldap";
    public static final String IDP = "IdP";
    public static final List<String> REALMS = ImmutableList.of(KARAF, LDAP, IDP);
    public static final String REALMS_KEY = "realms";

    public static final String SAML = "saml";
    public static final String BASIC = "basic";
    public static final String PKI = "PKI";
    public static final String CAS = "CAS"; // TODO: tbatie - 1/12/17 - Ignoring CAS for now
    public static final String GUEST = "guest";
    public static final List<String> AUTH_TYPES = ImmutableList.of(SAML, BASIC, PKI, GUEST);
    public static final String AUTH_TYPES_KEY = "authenticationTypes";

    public static final String CLAIMS_KEY = "claims";

    public static final String ID = "options";
    public static final String DESCRIPTION = "Returns the web context policy options available for configuration based on the system's state.";

    public static final Map<String, String> RETURN_TYPES = ImmutableMap.of(
            REALMS_KEY, "A list of realms that are setup to be used. Will contain: " + String.join(", ", REALMS_KEY),
            CLAIMS_KEY, "Configured STS claims.",
            AUTH_TYPES_KEY, "List of auth types currently configured.");

    Configurator configurator = new Configurator();
    ConfigurationHandler ldapConfigHandler;

    public AvaliableOptionsProbeMethod(ConfigurationHandler ldapConfigHandler) {
        super(ID,
                DESCRIPTION,
                null,
                null,
                null,
                null,
                null,
                RETURN_TYPES);
        this.ldapConfigHandler = ldapConfigHandler;
    }

    @Override
    public ProbeReport probe(ContextPolicyConfiguration config) {
        // TODO: tbatie - 1/12/17 - Validate fields
        return new ProbeReport().addProbeResult(AUTH_TYPES_KEY, getAuthTypes())
                .addProbeResult(REALMS_KEY, getRealms())
                .addProbeResult(CLAIMS_KEY, getClaims());
    }

    public List<String> getAuthTypes() {
        // TODO: tbatie - 1/12/17 - Is there a preference order we should apply with these auth types?
        // TODO: tbatie - 1/12/17 - Could do a check for these, all should be running by default though
        List<String> authTypes = Arrays.asList(BASIC, SAML, PKI, GUEST);

        if(configurator.isBundleStarted("security-idp-client")) {
            authTypes.add(IDP);
        }

        return authTypes;
    }

    public List<String> getRealms() {
        List<String> realms = Arrays.asList(KARAF);

        // TODO: tbatie - 1/12/17 - If a IdpConfigurationHandler exists replace this with a service reference
        if(configurator.isBundleStarted("security-idp-server")) {
            realms.add(IDP);
        }

        if (ldapConfigHandler == null && ldapConfigHandler.getConfigurations()
                .stream()
                .anyMatch(config -> ((LdapConfiguration) config).ldapUseCase()
                        .equals(LdapConfiguration.LOGIN)
                        || ((LdapConfiguration) config).ldapUseCase()
                        .equals(LdapConfiguration.LOGIN_AND_CREDENTIAL_STORE))) {
            realms.add(LDAP);
        }

        if(configurator.isBundleStarted("security-idp-client")) {
            realms.add(IDP);
        }

        return realms;
    }

    public Object getClaims() {
        Map<String, Object> stsConfig = new Configurator().getConfig(
                "ddf.security.sts.client.configuration");
        return stsConfig == null ?  null : stsConfig.get("claims");
    }
}
