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

package org.codice.ui.admin.security.policy.context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codice.ddf.security.policy.context.ContextPolicy;
import org.codice.ddf.security.policy.context.ContextPolicyManager;
import org.codice.ddf.security.policy.context.attributes.ContextAttributeMapping;
import org.codice.ui.admin.security.policy.context.container.ContextPolicyBin;
import org.codice.ui.admin.wizard.api.CapabilitiesReport;
import org.codice.ui.admin.wizard.api.ConfigurationHandler;
import org.codice.ui.admin.wizard.api.ProbeReport;
import org.codice.ui.admin.wizard.api.TestReport;
import org.codice.ui.admin.wizard.config.Configurator;

public class ContextPolicyManagerHandler
        implements ConfigurationHandler<ContextPolicyConfiguration> {

    public static final String SAML = "SAML";

    public static final String PKI = "PKI";

    public static final String BASIC = "basic";

    public static final String GUEST = "guest";

    private List<String> authenticationTypes = Arrays.asList(SAML, PKI, BASIC, GUEST);

    public static final String KARAF = "karaf";

    public static final String LDAP = "ldap";

    public static final String IDP = "idp";

    private List<String> realms = Arrays.asList(KARAF, LDAP, IDP);

    public static final String CONTEXT_POLICY_MANAGER_HANDLER_ID = "contextPolicyManager";

    public static final String POLICY_OPTIONS_ID = "options";

    private ContextPolicyManager policyManager;

    @Override
    public ProbeReport probe(String probeId, ContextPolicyConfiguration configuration) {

        switch (probeId) {
        case POLICY_OPTIONS_ID:
            Object claims = new Configurator().getConfig("ddf.security.sts.client.configuration")
                    .get("claims");
            return new ProbeReport().addProbeResult("authTypes", authenticationTypes)
                    .addProbeResult("realms", realms)
                    .addProbeResult("claims", claims);
        }

        return null;
    }

    @Override
    public TestReport test(String testId, ContextPolicyConfiguration configuration) {
        return null;
    }

    @Override
    public TestReport persist(ContextPolicyConfiguration configuration) {
        Configurator configurator = new Configurator();
        // TODO: tbatie - 12/10/16
        return null;
    }

    @Override
    public List<ContextPolicyConfiguration> getConfigurations() {
        return Arrays.asList(new ContextPolicyConfiguration().contextPolicyBins(
                contextPolicyManagerSettingsToBins()));
    }

    @Override
    public CapabilitiesReport getCapabilities() {
        return null;
    }

    @Override
    public String getConfigurationHandlerId() {
        return CONTEXT_POLICY_MANAGER_HANDLER_ID;
    }

    @Override
    public Class getConfigClass() {
        return ContextPolicyConfiguration.class;
    }

    public List<ContextPolicyBin> contextPolicyManagerSettingsToBins() {

        // TODO: tbatie - 12/10/16 - Probably should match terminology used by the policy manager
        List<ContextPolicyBin> bins = new ArrayList<>();
        Collection<ContextPolicy> allPolicies = policyManager.getAllContextPolicies();
        for (ContextPolicy policy : allPolicies) {
            boolean foundBin = false;
            Map<String, Set<String>> policyRequiredAttributes =
                    convertAttributeMappingToMap(policy.getAllowedAttributes());

            for (ContextPolicyBin bin : bins) {
                if (bin.realm()
                        .equals(policy.getRealm()) && bin.authenticationTypes()
                        .equals(policy.getAuthenticationMethods()) && bin.hasSameRequiredAttributes(
                        policyRequiredAttributes)) {
                    bin.addContextPath(policy.getContextPath());
                    foundBin = true;
                }
            }

            if (!foundBin) {
                // TODO: tbatie - 12/10/16 - ???? auth types from ContextPolicy interface should maintain order, not be a collection
                bins.add(new ContextPolicyBin().realm(policy.getRealm())
                        .requiredAttributes(policyRequiredAttributes)
                        .authenticationTypes(new ArrayList<>(policy.getAuthenticationMethods()))
                        .addContextPath(policy.getContextPath()));
            }
        }

        return bins;
    }

    public Map<String, Set<String>> convertAttributeMappingToMap(
            Collection<ContextAttributeMapping> mappingToConvert) {
        Map<String, Set<String>> convertedMapping = new HashMap<>();

        for (ContextAttributeMapping mapping : mappingToConvert) {
            String name = mapping.getAttributeName();
            String value = mapping.getAttributeValue();

            if (convertedMapping.containsKey(name)) {
                convertedMapping.get(name)
                        .add(value);
            } else {
                convertedMapping.put(name, new HashSet<>(Arrays.asList(value)));
            }
        }

        return convertedMapping;
    }

    public void setPolicyManager(ContextPolicyManager policyManager) {
        this.policyManager = policyManager;
    }
}
