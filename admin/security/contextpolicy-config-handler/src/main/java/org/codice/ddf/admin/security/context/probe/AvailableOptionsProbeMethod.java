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

import static org.codice.ddf.admin.api.config.security.context.ContextPolicyBin.BASIC;
import static org.codice.ddf.admin.api.config.security.context.ContextPolicyBin.GUEST;
import static org.codice.ddf.admin.api.config.security.context.ContextPolicyBin.IDP;
import static org.codice.ddf.admin.api.config.security.context.ContextPolicyBin.KARAF;
import static org.codice.ddf.admin.api.config.security.context.ContextPolicyBin.LDAP;
import static org.codice.ddf.admin.api.config.security.context.ContextPolicyBin.PKI;
import static org.codice.ddf.admin.api.config.security.context.ContextPolicyBin.SAML;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.codice.ddf.admin.api.config.security.context.ContextPolicyConfiguration;
import org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration;
import org.codice.ddf.admin.api.handler.ConfigurationHandler;
import org.codice.ddf.admin.api.handler.method.ProbeMethod;
import org.codice.ddf.admin.api.handler.report.ProbeReport;
import org.codice.ddf.admin.api.configurator.Configurator;

import com.google.common.collect.ImmutableList;

import ddf.security.sts.client.configuration.STSClientConfiguration;

public class AvailableOptionsProbeMethod extends ProbeMethod<ContextPolicyConfiguration>{

    public static final String ID = "options";
    public static final String DESCRIPTION = "Returns the web context policy options available for configuration based on the system's state.";

    public static final String REALMS_KEY = "realms";
    public static final String AUTH_TYPES_KEY = "authenticationTypes";
    public static final String CLAIMS_KEY = "claims";


    public static final List<String> RETURN_TYPES = ImmutableList.of(REALMS_KEY, CLAIMS_KEY, AUTH_TYPES_KEY);

    Configurator configurator = new Configurator();
    ConfigurationHandler ldapConfigHandler;

    public AvailableOptionsProbeMethod(ConfigurationHandler ldapConfigHandler) {
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
        return new ProbeReport().probeResult(AUTH_TYPES_KEY, getAuthTypes())
                .probeResult(REALMS_KEY, getRealms())
                .probeResult(CLAIMS_KEY, getClaims());
    }

    public List<String> getAuthTypes() {
        // TODO: tbatie - 1/12/17 - Is there a preference order we should apply with these auth types?
        List<String> authTypes = new ArrayList<>(Arrays.asList(BASIC, SAML, PKI, GUEST));

        if(configurator.isBundleStarted("security-idp-client")) {
            authTypes.add(IDP);
        }

        return authTypes;
    }

    public List<String> getRealms() {
        List<String> realms = new ArrayList<>(Arrays.asList(KARAF));
        // TODO: tbatie - 1/12/17 - If a IdpConfigurationHandler exists replace this with a service reference
        if(configurator.isBundleStarted("security-idp-server")) {
            realms.add(IDP);
        }

        if (ldapConfigHandler == null || ldapConfigHandler.getConfigurations()
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

    public STSClientConfiguration getStsClientConfig() {
        return new Configurator().getServiceReference(STSClientConfiguration.class);
    }
}
