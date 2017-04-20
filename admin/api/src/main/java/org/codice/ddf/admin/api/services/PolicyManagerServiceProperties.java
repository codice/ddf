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
package org.codice.ddf.admin.api.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.codice.ddf.admin.api.config.context.ContextPolicyBin;
import org.codice.ddf.admin.api.config.context.ContextPolicyConfiguration;
import org.codice.ddf.admin.api.configurator.Configurator;
import org.codice.ddf.security.policy.context.ContextPolicy;
import org.codice.ddf.security.policy.context.ContextPolicyManager;
import org.codice.ddf.security.policy.context.impl.PolicyManager;

public class PolicyManagerServiceProperties {

    public static final String STS_CLAIMS_CONFIGURATION_CONFIG_ID = "ddf.security.sts.client.configuration";
    public static final String STS_CLAIMS_PROPS_KEY_CLAIMS = "claims";

    public static final String IDP_CLIENT_BUNDLE_NAME = "security-idp-client";
    public static final String IDP_SERVER_BUNDLE_NAME = "security-idp-server";


    public static final ContextPolicyConfiguration contextPolicyServiceToContextPolicyConfig(){
        ContextPolicyManager ref = new Configurator().getServiceReference(ContextPolicyManager.class);
        PolicyManager policyManager = ((PolicyManager) ref);
        return new ContextPolicyConfiguration().contextPolicyBins(
                policyManagerSettingsToBins(policyManager))
                .whiteListContexts(policyManager.getWhiteListContexts());
    }


    // TODO: tbatie - 1/17/17 - (Ticket) Get rid of this PolicyManager reference and break this dependency.
    public static List<ContextPolicyBin> policyManagerSettingsToBins(PolicyManager policyManager) {
        List<ContextPolicyBin> bins = new ArrayList<>();

        Collection<ContextPolicy> allPolicies = policyManager.getAllContextPolicies();
        for (ContextPolicy policy : allPolicies) {
            boolean foundBin = false;
            Map<String, String> policyRequiredAttributes = policy.getAllowedAttributes()
                    .stream()
                    .collect(Collectors.toMap(map -> map.getAttributeName(),
                            map -> map.getAttributeValue()));

            //Check if bin containing an identical context policy exists already, if so add the context path to it
            for (ContextPolicyBin bin : bins) {
                if (bin.realm()
                        .equals(policy.getRealm()) && bin.authenticationTypes()
                        .equals(policy.getAuthenticationMethods()) && hasSameRequiredAttributes(bin, policyRequiredAttributes)) {
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

    public static boolean hasSameRequiredAttributes(ContextPolicyBin bin, Map<String, String> mappingsToCheck) {
        if (!(bin.requiredAttributes().keySet()
                .containsAll(mappingsToCheck.keySet()) && mappingsToCheck.keySet()
                .containsAll(bin.requiredAttributes().keySet()))) {
            return false;
        }

        return !bin.requiredAttributes().entrySet()
                .stream()
                .filter(binMapping -> !mappingsToCheck.get(binMapping.getKey())
                        .equals(binMapping.getValue()))
                .findFirst()
                .isPresent();
    }
}
