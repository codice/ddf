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

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.codice.ddf.security.policy.context.ContextPolicy;
import org.codice.ddf.security.policy.context.ContextPolicyManager;

import ddf.common.test.AdminConfig;
import ddf.common.test.ServiceManager;

public class SecurityPolicyConfigurator implements AdminConfig.ConfigWaitable {
    private static final String SYMBOLIC_NAME = "security-policy-context";

    private static final String FACTORY_PID = "org.codice.ddf.security.policy.context.impl.PolicyManager";

    private static final String[] AUTH_TYPES = {"/=SAML|%s", "/admin=SAML|basic",
            "/jolokia=SAML|basic", "/system=SAML|basic", "/solr=SAML|PKI|basic"};

    private static final Lock LOCK = new ReentrantLock();

    private static SecurityPolicyConfigurator anonConfig;

    private static SecurityPolicyConfigurator basicConfig;

    private final ServiceManager serviceManager;

    private final String serviceRoot;

    private final String policyType;

    private final ContextPolicyManager ctxPolicyMgr;

    private SecurityPolicyConfigurator(ServiceManager serviceManager, String serviceRoot,
            String policyType) {
        this.serviceManager = serviceManager;
        this.serviceRoot = serviceRoot;
        this.policyType = policyType;
        ctxPolicyMgr = serviceManager.getService(ContextPolicyManager.class);
    }

    public static void configureRestForAnonymous(AdminConfig adminConfig,
            ServiceManager serviceManager, String serviceRoot)
            throws IOException, InterruptedException {
        LOCK.lock();
        try {
            if (anonConfig == null) {
                anonConfig = new SecurityPolicyConfigurator(serviceManager, serviceRoot, "ANON");
            }
        } finally {
            LOCK.unlock();
        }

        adminConfig.updateConfig(anonConfig);
    }

    public static void configureRestForBasic(AdminConfig adminConfig,
            ServiceManager serviceManager, String serviceRoot)
            throws IOException, InterruptedException {
        LOCK.lock();
        try {
            if (basicConfig == null) {
                basicConfig = new SecurityPolicyConfigurator(serviceManager, serviceRoot, "basic");
            }
        } finally {
            LOCK.unlock();
        }

        adminConfig.updateConfig(basicConfig);
    }

    @Override
    public String getPid() {
        return FACTORY_PID;
    }

    @Override
    public String getLocation() {
        return null;
    }

    @Override
    public Dictionary<String, ?> getConfigProps() {
        Map<String, Object> map = new HashMap<>();
        map.putAll(serviceManager.getMetatypeDefaults(SYMBOLIC_NAME, FACTORY_PID));
        map.put("whiteListContexts",
                "/services/SecurityTokenService,/services/internal,/proxy," + serviceRoot
                        + "/sdk/SoapService");
        map.put("authenticationTypes", getAuthTypes());

        return new Hashtable<>(map);
    }

    @Override
    public boolean isConfigured() {
        ContextPolicy contextPolicy = ctxPolicyMgr.getContextPolicy("/");

        return contextPolicy.getAuthenticationMethods().contains(policyType);
    }

    private String[] getAuthTypes() {
        String[] subbedAuths = new String[AUTH_TYPES.length];
        for (int i = 0; i < AUTH_TYPES.length; i++) {
            subbedAuths[i] = String.format(AUTH_TYPES[i], policyType);
        }

        return subbedAuths;
    }
}
