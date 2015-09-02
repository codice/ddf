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
package ddf.catalog.test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.codice.ddf.security.policy.context.ContextPolicy;
import org.codice.ddf.security.policy.context.ContextPolicyManager;

import ddf.common.test.AdminConfig;
import ddf.common.test.ServiceManager;
import ddf.common.test.SynchronizedConfiguration;

public class SecurityPolicyConfigurator {
    private static final String SYMBOLIC_NAME = "security-policy-context";

    private static final String FACTORY_PID = "org.codice.ddf.security.policy.context.impl.PolicyManager";

    private static final String[] AUTH_TYPES = {"/=SAML|%s", "/admin=SAML|basic",
            "/jolokia=SAML|basic", "/system=SAML|basic", "/solr=SAML|PKI|basic"};

    public static void configureRestForAnonymous(AdminConfig adminConfig,
            ServiceManager serviceManager, String serviceRoot) throws Exception {
        configure(adminConfig, serviceManager, serviceRoot, "ANON");
    }

    public static void configureRestForBasic(AdminConfig adminConfig, ServiceManager serviceManager,
            String serviceRoot) throws Exception {
        configure(adminConfig, serviceManager, serviceRoot, "basic");
    }

    private static void configure(AdminConfig adminConfig, ServiceManager serviceManager,
            String serviceRoot, String policyType) throws Exception {
        ContextPolicyManager ctxPolicyMgr = serviceManager.getService(ContextPolicyManager.class);

        new SynchronizedConfiguration(FACTORY_PID, null,
                getConfigProps(serviceManager, serviceRoot, policyType),
                createChecker(policyType, ctxPolicyMgr)).updateConfig(adminConfig);

    }

    private static Callable<Boolean> createChecker(final String policyType,
            final ContextPolicyManager ctxPolicyMgr) {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                ContextPolicy contextPolicy = ctxPolicyMgr.getContextPolicy("/");

                return contextPolicy.getAuthenticationMethods().contains(policyType);
            }
        };
    }

    private static Map<String, Object> getConfigProps(ServiceManager serviceManager,
            String serviceRoot, String policyType) {
        Map<String, Object> map = new HashMap<>();
        map.putAll(serviceManager.getMetatypeDefaults(SYMBOLIC_NAME, FACTORY_PID));
        map.put("whiteListContexts",
                "/services/SecurityTokenService,/services/internal,/proxy," + serviceRoot
                        + "/sdk/SoapService");
        map.put("authenticationTypes", getAuthTypes(policyType));
        return map;

    }

    private static String[] getAuthTypes(String policyType) {
        String[] subbedAuths = new String[AUTH_TYPES.length];
        for (int i = 0; i < AUTH_TYPES.length; i++) {
            subbedAuths[i] = String.format(AUTH_TYPES[i], policyType);
        }

        return subbedAuths;
    }
}
