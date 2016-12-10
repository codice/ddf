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
 **/
package ddf.test.itests.common;

import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.security.policy.context.ContextPolicy;
import org.codice.ddf.security.policy.context.ContextPolicyManager;
import org.codice.ddf.security.policy.context.impl.PolicyManager;
import org.osgi.service.cm.ConfigurationAdmin;

import ddf.common.test.ServiceManager;
import ddf.common.test.SynchronizedConfiguration;

public class SecurityPolicyConfigurator {

    private static final String SYMBOLIC_NAME = "security-policy-context";

    private static final String FACTORY_PID =
            "org.codice.ddf.security.policy.context.impl.PolicyManager";

    public static final String BASIC_AUTH_TYPES = "/=SAML|basic,/solr=SAML|PKI|basic";

    public static final String GUEST_AUTH_TYPES =
            "/=SAML|GUEST,/admin=SAML|basic,/system=SAML|basic,/solr=SAML|PKI|basic";

    public static final String DEFAULT_WHITELIST =
            "/services/SecurityTokenService,/services/internal,/proxy";

    private ServiceManager services;

    private ConfigurationAdmin configAdmin;

    public SecurityPolicyConfigurator(ServiceManager services, ConfigurationAdmin configAdmin) {
        this.services = services;
        this.configAdmin = configAdmin;
    }

    public void configureRestForGuest() throws Exception {
        configureRestForGuest(null);
    }

    public void configureRestForGuest(String whitelist) throws Exception {
        configureWebContextPolicy(null, null, null, createWhitelist(whitelist));
    }

    public void configureRestForBasic() throws Exception {
        configureRestForBasic(null);
    }

    public void configureRestForBasic(String whitelist) throws Exception {
        configureWebContextPolicy(null, BASIC_AUTH_TYPES, null, createWhitelist(whitelist));
    }

    public static String createWhitelist(String whitelist) {
        return DEFAULT_WHITELIST + (StringUtils.isNotBlank(whitelist) ? "," + whitelist : "");
    }

    public void configureWebContextPolicy(String realms, String authTypes,
            String requiredAttributes, String whitelist) throws Exception {

        Map<String, Object> policyProperties = services.getMetatypeDefaults(SYMBOLIC_NAME,
                FACTORY_PID);

        putPolicyValues(policyProperties, "realms", realms);
        putPolicyValues(policyProperties, "authenticationTypes", authTypes);
        putPolicyValues(policyProperties, "requiredAttributes", requiredAttributes);
        putPolicyValues(policyProperties, "whiteListContexts", whitelist);

        new SynchronizedConfiguration(FACTORY_PID,
                null,
                policyProperties,
                createChecker(policyProperties),
                configAdmin).updateConfig();

        services.waitForAllBundles();
    }

    private void putPolicyValues(Map<String, Object> properties, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            properties.put(key, StringUtils.split(value, ","));
        }
    }

    private Callable<Boolean> createChecker(final Map<String, Object> policyProperties) {

        final ContextPolicyManager ctxPolicyMgr = services.getService(ContextPolicyManager.class);

        final PolicyManager targetPolicies = new PolicyManager();
        targetPolicies.setPolicies(policyProperties);

        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                for (ContextPolicy policy : ctxPolicyMgr.getAllContextPolicies()) {
                    ContextPolicy targetPolicy =
                            targetPolicies.getContextPolicy(policy.getContextPath());

                    if (targetPolicy == null || !targetPolicy.getContextPath()
                            .equals(policy.getContextPath()) || (targetPolicy.getRealm() != null
                            && !targetPolicy.getRealm()
                            .equals(policy.getRealm())) || !targetPolicy.getAuthenticationMethods()
                            .containsAll(policy.getAuthenticationMethods())
                            || !targetPolicy.getAllowedAttributeNames()
                            .containsAll(policy.getAllowedAttributeNames())) {
                        return false;
                    }
                }

                return true;
            }
        };
    }

}
