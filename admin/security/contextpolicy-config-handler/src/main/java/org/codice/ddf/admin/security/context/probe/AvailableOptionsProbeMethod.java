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

import static org.codice.ddf.admin.api.config.services.PolicyManagerServiceProperties.IDP_CLIENT_BUNDLE_NAME;
import static org.codice.ddf.admin.api.config.services.PolicyManagerServiceProperties.IDP_SERVER_BUNDLE_NAME;
import static org.codice.ddf.admin.api.config.services.PolicyManagerServiceProperties.STS_CLAIMS_CONFIGURATION_CONFIG_ID;
import static org.codice.ddf.admin.api.config.services.PolicyManagerServiceProperties.STS_CLAIMS_PROPS_KEY_CLAIMS;
import static org.codice.ddf.admin.api.config.validation.LdapValidationUtils.LOGIN;
import static org.codice.ddf.admin.api.config.validation.LdapValidationUtils.LOGIN_AND_CREDENTIAL_STORE;
import static org.codice.ddf.admin.api.config.validation.SecurityValidationUtils.BASIC;
import static org.codice.ddf.admin.api.config.validation.SecurityValidationUtils.GUEST;
import static org.codice.ddf.admin.api.config.validation.SecurityValidationUtils.IDP;
import static org.codice.ddf.admin.api.config.validation.SecurityValidationUtils.KARAF;
import static org.codice.ddf.admin.api.config.validation.SecurityValidationUtils.LDAP;
import static org.codice.ddf.admin.api.config.validation.SecurityValidationUtils.PKI;
import static org.codice.ddf.admin.api.config.validation.SecurityValidationUtils.SAML;
import static org.codice.ddf.admin.api.handler.commons.HandlerCommons.FAILED_PROBE;
import static org.codice.ddf.admin.api.handler.commons.HandlerCommons.SUCCESSFUL_PROBE;
import static org.codice.ddf.admin.api.handler.report.ProbeReport.createProbeReport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codice.ddf.admin.api.config.context.ContextPolicyConfiguration;
import org.codice.ddf.admin.api.config.ldap.LdapConfiguration;
import org.codice.ddf.admin.api.configurator.Configurator;
import org.codice.ddf.admin.api.handler.ConfigurationHandler;
import org.codice.ddf.admin.api.handler.method.ProbeMethod;
import org.codice.ddf.admin.api.handler.report.ProbeReport;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class AvailableOptionsProbeMethod extends ProbeMethod<ContextPolicyConfiguration>{

    public static final String ID = "options";
    public static final String DESCRIPTION = "Returns the web context policy options available for configuration based on the system's state.";

    public static final String REALMS_KEY = "realms";
    public static final String AUTH_TYPES_KEY = "authenticationTypes";
    public static final String CLAIMS_KEY = "claims";

    public static final Map<String, String> FAILED_TYPES = ImmutableMap.of(FAILED_PROBE, "Failed to retrieve context policy manager options.");
    public static final Map<String, String> SUCCESS_TYPES = ImmutableMap.of(SUCCESSFUL_PROBE, "Successfully retrieved context policy options.");
    public static final List<String> RETURN_TYPES = ImmutableList.of(REALMS_KEY, AUTH_TYPES_KEY, CLAIMS_KEY);

    Configurator configurator = new Configurator();
    ConfigurationHandler ldapConfigHandler;

    public AvailableOptionsProbeMethod(ConfigurationHandler ldapConfigHandler) {
        super(ID,
                DESCRIPTION,
                null,
                null,
                SUCCESS_TYPES,
                FAILED_TYPES,
                null,
                RETURN_TYPES);
        this.ldapConfigHandler = ldapConfigHandler;
    }

    @Override
    public ProbeReport probe(ContextPolicyConfiguration config) {
        Map<String, Object> probeResults = new HashMap();
        probeResults.put(AUTH_TYPES_KEY, getAuthTypes());
        probeResults.put(REALMS_KEY, getRealms());
        probeResults.put(CLAIMS_KEY, getClaims());

        return createProbeReport(SUCCESS_TYPES, FAILED_TYPES, null, probeResults.isEmpty() ? FAILED_PROBE : SUCCESSFUL_PROBE).probeResults(probeResults);
    }

    public List<String> getAuthTypes() {
        // TODO: tbatie - 1/12/17 - Is there a preference order we should apply with these auth types?
        // TODO: tbatie - 1/12/17 - (Ticket) need to eventually check if these handlers are running for these auth types instead of hardcoding
        List<String> authTypes = new ArrayList<>(Arrays.asList(BASIC, SAML, PKI, GUEST));

        if(configurator.isBundleStarted(IDP_CLIENT_BUNDLE_NAME)) {
            authTypes.add(IDP);
        }

        return authTypes;
    }

    public List<String> getRealms() {
        List<String> realms = new ArrayList<>(Arrays.asList(KARAF));
        // TODO: tbatie - 1/12/17 - If a IdpConfigurationHandler exists replace this with a service reference
        if(configurator.isBundleStarted(IDP_SERVER_BUNDLE_NAME)) {
            realms.add(IDP);
        }

        if (ldapConfigHandler == null || ldapConfigHandler.getConfigurations()
                .stream()
                .anyMatch(config -> ((LdapConfiguration) config).ldapUseCase()
                        .equals(LOGIN)
                        || ((LdapConfiguration) config).ldapUseCase()
                        .equals(LOGIN_AND_CREDENTIAL_STORE))) {
            realms.add(LDAP);
        }

        return realms;
    }

    public Object getClaims() {
        Map<String, Object> stsConfig = new Configurator().getConfig(STS_CLAIMS_CONFIGURATION_CONFIG_ID);
        return stsConfig == null ?  null : stsConfig.get(STS_CLAIMS_PROPS_KEY_CLAIMS);
    }
}
