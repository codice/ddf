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

import static org.codice.ddf.admin.api.handler.ConfigurationMessage.FAILED_PERSIST;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.FAILURE;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.SUCCESS;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.NO_METHOD_FOUND;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.SUCCESSFUL_PERSIST;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.buildMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.admin.api.config.security.context.ContextPolicyBin;
import org.codice.ddf.admin.api.config.security.context.ContextPolicyConfiguration;
import org.codice.ddf.admin.api.handler.ConfigurationHandler;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;
import org.codice.ddf.admin.api.handler.method.ProbeMethod;
import org.codice.ddf.admin.api.handler.report.CapabilitiesReport;
import org.codice.ddf.admin.api.handler.report.ProbeReport;
import org.codice.ddf.admin.api.handler.report.TestReport;
import org.codice.ddf.admin.api.persist.ConfigReport;
import org.codice.ddf.admin.api.persist.Configurator;
import org.codice.ddf.admin.security.context.probe.AvaliableOptionsProbeMethod;
import org.codice.ddf.security.policy.context.ContextPolicy;
import org.codice.ddf.security.policy.context.ContextPolicyManager;
import org.codice.ddf.security.policy.context.impl.PolicyManager;

import com.google.common.collect.ImmutableMap;

import ddf.security.sts.client.configuration.STSClientConfiguration;

public class ContextPolicyManagerHandler
        implements ConfigurationHandler<ContextPolicyConfiguration> {

    private ConfigurationHandler ldapConfigHandler;

    public static final String CONTEXT_POLICY_MANAGER_HANDLER_ID = "contextPolicyManager";

    @Override
    public ProbeReport probe(String probeId, ContextPolicyConfiguration configuration) {
        Optional<ProbeMethod> probeMethod = getProbeMethods(ldapConfigHandler).stream()
                .filter(method -> method.id()
                        .equals(probeId))
                .findFirst();

        return probeMethod.isPresent() ?
                probeMethod.get()
                        .probe(configuration) :
                new ProbeReport(new ConfigurationMessage(FAILURE, NO_METHOD_FOUND, null));
    }

    @Override
    public TestReport test(String testId, ContextPolicyConfiguration configuration) {
        return null;
    }

    @Override
    public TestReport persist(ContextPolicyConfiguration config, String persistId) {
        if (config.contextPolicyBins()
                .stream()
                .filter(bin -> bin.contextPaths()
                        .isEmpty() || StringUtils.isEmpty(bin.realm()) || bin.authenticationTypes()
                        .isEmpty())
                .findFirst()
                .isPresent()) {
            return new TestReport(buildMessage(FAILURE, FAILED_PERSIST,
                    "Context paths, realm and authentication types cannot be empty"));
        }

        Configurator configurator = new Configurator();
        configurator.updateConfigFile("org.codice.ddf.security.policy.context.impl.PolicyManager",
                configToPolicyManagerSettings(config),
                true);
        ConfigReport configReport = configurator.commit();
        if (!configReport.getFailedResults()
                .isEmpty()) {
            return new TestReport(buildMessage(FAILURE, FAILED_PERSIST,
                    "Unable to persist changes"));
        } else {
            return new TestReport(buildMessage(SUCCESS, SUCCESSFUL_PERSIST,
                    "Successfully saved Web Context Policy Manager settings"));
        }
    }

    @Override
    public List<ContextPolicyConfiguration> getConfigurations() {
        return Arrays.asList(new ContextPolicyConfiguration().contextPolicyBins(
                policyManagerSettingsToBins())
                .whiteListContexts(getPolicyManager().getWhiteListContexts()));
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

    public List<ContextPolicyBin> policyManagerSettingsToBins() {
        // TODO: tbatie - 12/10/16 - Should match terminology used by the policy manager
        List<ContextPolicyBin> bins = new ArrayList<>();
        Collection<ContextPolicy> allPolicies = getPolicyManager().getAllContextPolicies();
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

    public Map<String, Object> configToPolicyManagerSettings(ContextPolicyConfiguration config){
        List<String> realmsProps = new ArrayList<>();
        List<String> authTypesProps = new ArrayList<>();
        List<String> reqAttrisProps = new ArrayList<>();

        for (ContextPolicyBin bin : config.contextPolicyBins()) {
            bin.contextPaths()
                    .stream()
                    .forEach(context -> {
                        realmsProps.add(context + "=" + bin.realm());
                        authTypesProps.add(
                                context + "=" + String.join("|", bin.authenticationTypes()));
                        if (bin.requiredAttributes()
                                .isEmpty()) {
                            reqAttrisProps.add(context + "=");
                        } else {
                            reqAttrisProps.add(context + "={" + String.join(";",
                                    bin.requiredAttributes()
                                            .entrySet()
                                            .stream()
                                            .map(entry -> entry.getKey() + "=" + entry.getValue())
                                            .collect(Collectors.toList())) + "}");
                        }
                    });
        }

        return ImmutableMap.of("authenticationTypes",
                authTypesProps,
                "realms",
                realmsProps,
                "requiredAttributes",
                reqAttrisProps,
                "whiteListContexts",
                config.whiteListContexts());
    }

    public void setLdapConfigHandler(ConfigurationHandler ldapConfigHandler) {
        this.ldapConfigHandler = ldapConfigHandler;
    }

    public PolicyManager getPolicyManager() {
        // TODO: tbatie - 12/16/16 - Throw exception? Something is seriously wrong if service isnt available
        ContextPolicyManager manager =
                new Configurator().getServiceReference(ContextPolicyManager.class);
        return manager == null ? null : (PolicyManager) manager;
    }

    public STSClientConfiguration getStsClientConfig() {
        // TODO: tbatie - 12/16/16 - Throw exception? Something is seriously wrong if this service isnt available
        return new Configurator().getServiceReference(STSClientConfiguration.class);
    }

    public List<ProbeMethod> getProbeMethods(ConfigurationHandler ldapConfigHandler){
        return Arrays.asList(new AvaliableOptionsProbeMethod(ldapConfigHandler));
    }
}
