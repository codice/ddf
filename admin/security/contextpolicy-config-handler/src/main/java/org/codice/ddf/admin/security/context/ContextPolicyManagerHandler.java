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

package org.codice.ddf.admin.security.context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.codice.ddf.admin.api.config.ConfigurationType;
import org.codice.ddf.admin.api.config.security.context.ContextPolicyBin;
import org.codice.ddf.admin.api.config.security.context.ContextPolicyConfiguration;
import org.codice.ddf.admin.api.handler.ConfigurationHandler;
import org.codice.ddf.admin.api.handler.DefaultConfigurationHandler;
import org.codice.ddf.admin.api.handler.method.PersistMethod;
import org.codice.ddf.admin.api.handler.method.ProbeMethod;
import org.codice.ddf.admin.api.handler.method.TestMethod;
import org.codice.ddf.admin.api.persist.Configurator;
import org.codice.ddf.admin.security.context.persist.EditContextPolicyMethod;
import org.codice.ddf.admin.security.context.probe.AvailableOptionsProbeMethod;
import org.codice.ddf.security.policy.context.ContextPolicy;
import org.codice.ddf.security.policy.context.ContextPolicyManager;
import org.codice.ddf.security.policy.context.impl.PolicyManager;

public class ContextPolicyManagerHandler extends DefaultConfigurationHandler<ContextPolicyConfiguration> {

    private ConfigurationHandler ldapConfigHandler;
    public static final String CONTEXT_POLICY_MANAGER_HANDLER_ID = ContextPolicyConfiguration.CONFIGURATION_TYPE;

    @Override
    public String getConfigurationHandlerId() {
        return CONTEXT_POLICY_MANAGER_HANDLER_ID;
    }

    @Override
    public List<ProbeMethod> getProbeMethods() {
        return Arrays.asList(new AvailableOptionsProbeMethod(ldapConfigHandler));
    }

    @Override
    public List<TestMethod> getTestMethods() {
        return null;
    }

    @Override
    public List<PersistMethod> getPersistMethods() {
        return Arrays.asList(new EditContextPolicyMethod());
    }

    @Override
    public List<ContextPolicyConfiguration> getConfigurations() {
        ContextPolicyManager ref = new Configurator().getServiceReference(ContextPolicyManager.class);
        if(ref == null) {
            return new ArrayList<>();
        }

        PolicyManager policyManager = ((PolicyManager) ref);
        return Arrays.asList(new ContextPolicyConfiguration().contextPolicyBins(
                policyManagerSettingsToBins(policyManager))
                .whiteListContexts(policyManager.getWhiteListContexts()));
    }

    public List<ContextPolicyBin> policyManagerSettingsToBins(PolicyManager policyManager) {
        List<ContextPolicyBin> bins = new ArrayList<>();

        Collection<ContextPolicy> allPolicies = policyManager.getAllContextPolicies();
        for (ContextPolicy policy : allPolicies) {
            boolean foundBin = false;
            Map<String, String> policyRequiredAttributes = policy.getAllowedAttributes()
                    .stream()
                    .collect(Collectors.toMap(map -> map.getAttributeName(),
                            map -> map.getAttributeValue()));

            for (ContextPolicyBin bin : bins) {
                if (bin.realm()
                        .equals(policy.getRealm()) && bin.authenticationTypes()
                        .equals(policy.getAuthenticationMethods()) && bin.hasSameRequiredAttributes(
                        policyRequiredAttributes)) {
                    bin.contextPaths(policy.getContextPath());
                    foundBin = true;
                }
            }

            if (!foundBin) {
                bins.add(new ContextPolicyBin().realm(policy.getRealm())
                        .requiredAttributes(policyRequiredAttributes)
                        .authenticationTypes(new ArrayList<>(policy.getAuthenticationMethods()))
                        .contextPaths(policy.getContextPath()));
            }
        }

        return bins;
    }

    @Override
    public ConfigurationType getConfigurationType() {
        return new ContextPolicyConfiguration().getConfigurationType();
    }

    public void setLdapConfigHandler(ConfigurationHandler ldapConfigHandler) {
        this.ldapConfigHandler = ldapConfigHandler;
    }
}
