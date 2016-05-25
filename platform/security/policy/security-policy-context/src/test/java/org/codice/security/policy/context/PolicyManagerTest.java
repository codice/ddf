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
package org.codice.security.policy.context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.shiro.authz.Permission;
import org.codice.ddf.security.policy.context.ContextPolicy;
import org.codice.ddf.security.policy.context.impl.Policy;
import org.codice.ddf.security.policy.context.impl.PolicyManager;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import junit.framework.Assert;

/**
 * Test for PolicyManager
 */
public class PolicyManagerTest {

    private static final String REALMS = "realms";

    private static final String DEFAULT_REALM_CONTEXT_VALUE = "karaf";

    private static final String AUTH_TYPES = "authenticationTypes";

    private static final String REQ_ATTRS = "requiredAttributes";

    private static final List<String> DEFAULT_AUTH_TYPES = Arrays.asList(new String[] {"SAML",
            "GUEST"});

    private PolicyManager manager;

    private PolicyManager rollBackTestManager;

    private String[] rollBackRealmValues =
            {"/=" + DEFAULT_REALM_CONTEXT_VALUE, "/A=a", "/A/B/C/testContext4=abcTestContext4",
                    "/testContext5=karaf", "/1/2/3/testContext6=" + DEFAULT_REALM_CONTEXT_VALUE,
                    "/A/B/C/testContext7=" + DEFAULT_REALM_CONTEXT_VALUE};

    private String[] rollBackAuthTypesValues =
            {"/=SAML|GUEST", "/A=a", "/A/B/C/testContext4=abcTestContext4"};

    private String[] rollBackReqAttrValues =
            {"/=", "/A={A=a}", "/testContext=", "/1/2/3/testContext2=", "/A/B/C/testContext3=",
                    "/A/B/C/testContext4=",
                    "/A/B/C/testContext8={AbcTestContext8=abcTestContext8}"};

    private final Map<String, String> expectedRollBackRealms =
            new ImmutableMap.Builder<String, String>().put("/testContext",
                    DEFAULT_REALM_CONTEXT_VALUE)
                    .put("/1/2/3/testContext2", DEFAULT_REALM_CONTEXT_VALUE)
                    .put("/A/B/C/testContext3", "a")
                    .put("/A/B/C/testContext4", "abcTestContext4")
                    .build();

    private final Map<String, List<String>> expectedRollBackAuthTypes =
            new ImmutableMap.Builder<String, List<String>>().put("/testContext", DEFAULT_AUTH_TYPES)
                    .put("/1/2/3/testContext2", Arrays.asList("SAML", "GUEST"))
                    .put("/A/B/C/testContext3", Arrays.asList("a"))
                    .put("/A/B/C/testContext4", Arrays.asList("abcTestContext4"))
                    .build();

    private final Map<String, List<String>> expectedRollBackReqAttrs =
            new ImmutableMap.Builder<String, List<String>>().put("/testContext5",
                    Arrays.asList(new String[] {}))
                    .put("/1/2/3/testContext6", Arrays.asList(new String[] {}))
                    .put("/A/B/C/testContext7", Arrays.asList("A"))
                    .put("/A/B/C/testContext8", Arrays.asList("AbcTestContext8"))
                    .build();

    @Before
    public void setup() {
        manager = new PolicyManager();
        manager.setContextPolicy("/", new Policy("/", null, new ArrayList<String>(), null));
        manager.setContextPolicy("/search",
                new Policy("/search", null, new ArrayList<String>(), null));
        manager.setContextPolicy("/admin",
                new Policy("/admin", null, new ArrayList<String>(), null));
        manager.setContextPolicy("/search/standard",
                new Policy("/search/standard", null, new ArrayList<String>(), null));
        manager.setContextPolicy("/search/cometd",
                new Policy("/search/cometd", null, new ArrayList<String>(), null));
        manager.setContextPolicy("/search/simple",
                new Policy("/search/simple", null, new ArrayList<String>(), null));
        manager.setContextPolicy("/aaaaaa",
                new Policy("/aaaaaa", null, new ArrayList<String>(), null));
        manager.setContextPolicy("/aaa", new Policy("/aaa", null, new ArrayList<String>(), null));
        manager.setContextPolicy("/aaa/aaa",
                new Policy("/aaa/aaa", null, new ArrayList<String>(), null));
        manager.setContextPolicy("/foo/bar",
                new Policy("/foo/bar", null, new ArrayList<String>(), null));
        manager.setWhiteListContexts(Arrays.asList("/foo"));

        Map<String, Object> contextPolicies = new HashMap<>();
        contextPolicies.put(REALMS, rollBackRealmValues);
        contextPolicies.put(AUTH_TYPES, rollBackAuthTypesValues);
        contextPolicies.put(REQ_ATTRS, rollBackReqAttrValues);

        rollBackTestManager = new PolicyManager();
        rollBackTestManager.setPolicies(contextPolicies);
    }

    /**
     * Tests context rollbacks to the specified realms
     * / <- /testContext - single rollback to default
     * / <- /1/2/3/testContext2 - several rollbacks to default
     * /A <- /A/B/C/testContext3 - several rollback to parent
     * /A/B/C/testContext4 <- /A/B/C/testContext4 - no rollback
     */
    @Test
    public void testContextRealmRollBack() {
        for (String contextPath : expectedRollBackRealms.keySet()) {
            Assert.assertEquals(expectedRollBackRealms.get(contextPath),
                    rollBackTestManager.getContextPolicy(contextPath)
                            .getRealm());
        }

    }

    /**
     * Tests context rollbacks to the specified authorization types
     * / <- /testContext - single rollback to default
     * / <- /1/2/3/testContext2 - several rollbacks to default
     * /A <- /A/B/C/testContext3 - several rollback to parent
     * /A/B/C/testContext4 <- /A/B/C/testContext4 - no rollback
     */
    @Test
    public void testAuthTypesRollBack() {
        for (String contextPath : expectedRollBackAuthTypes.keySet()) {
            for (String authType : rollBackTestManager.getContextPolicy(contextPath)
                    .getAuthenticationMethods()) {
                Assert.assertTrue(expectedRollBackAuthTypes.get(contextPath)
                        .contains(authType));
            }
        }
    }

    /**
     * Tests context rollbacks to the specified required attributes
     * / <- /testContext5 - single rollback to default
     * / <- /1/2/3/testContext6 - several rollbacks to default
     * /A <- /A/B/C/testContext7 - several rollback to parent
     * /A/B/C/testContext8 <- /A/B/C/testContext8 - no rollback
     */
    @Test
    public void testReqAttrRollBack() {
        for (String contextPath : expectedRollBackReqAttrs.keySet()) {
            for (String authType : rollBackTestManager.getContextPolicy(contextPath)
                    .getAllowedAttributeNames()) {
                Assert.assertTrue(expectedRollBackReqAttrs.get(contextPath)
                        .contains(authType));
            }
        }
    }

    @Test
    public void testInvalidEntry() {
        Assert.assertEquals(rollBackTestManager.getContextPolicy("invalidContextPathEntry")
                .getRealm(), PolicyManager.DEFAULT_REALM_CONTEXT_VALUE);
        Assert.assertEquals(rollBackTestManager.getContextPolicy("invalidContextPathEntry")
                .getAllowedAttributeNames(), Arrays.asList(new String[] {}));
        Assert.assertEquals(rollBackTestManager.getContextPolicy("invalidContextPathEntry")
                .getAuthenticationMethods(), DEFAULT_AUTH_TYPES);
    }

    @Test
    public void testFindContextPaths() {
        ContextPolicy policy = manager.getContextPolicy("/search/standard/user");

        Assert.assertEquals("/search/standard", policy.getContextPath());

        policy = manager.getContextPolicy("/search/standard");

        Assert.assertEquals("/search/standard", policy.getContextPath());

        policy = manager.getContextPolicy("/search/endpoint");

        Assert.assertEquals("/search", policy.getContextPath());

        policy = manager.getContextPolicy("/random/other/endpoint");

        Assert.assertEquals("/", policy.getContextPath());

        policy = manager.getContextPolicy("/aaaaab");

        Assert.assertEquals("/", policy.getContextPath());

        policy = manager.getContextPolicy("/aaa/aab");

        Assert.assertEquals("/aaa", policy.getContextPath());

        policy = manager.getContextPolicy("/");

        Assert.assertEquals("/", policy.getContextPath());

        policy = manager.getContextPolicy("blah");

        Assert.assertEquals("/", policy.getContextPath());

        policy = manager.getContextPolicy("/foo/bar");

        Assert.assertEquals("/foo/bar", policy.getContextPath());

        policy = manager.getContextPolicy("/foo/bar/foobar");

        Assert.assertEquals("/foo/bar", policy.getContextPath());

        policy = manager.getContextPolicy("/foo");

        Assert.assertEquals(null, policy);

        Assert.assertTrue(manager.isWhiteListed("/foo"));

        Assert.assertTrue(!manager.isWhiteListed("/foo/bar"));
    }

    @Test
    public void testConfiguration() {
        Map<String, Object> properties = new HashMap<>();

        String[] authTypes =
                new String[] {"/=SAML|BASIC", "/search=SAML|BASIC|GUEST", "/admin=SAML|BASIC",
                        "/foo=BASIC", "/blah=GUEST", "/bleh=GUEST", "/unprotected=",
                        "/unprotected2="};
        String[] requiredAttributes =
                new String[] {"/={}", "/blah=", "/search={role=user;control=foo;control=bar}",
                        "/admin={role=admin}"};

        properties.put("authenticationTypes", authTypes);
        properties.put("requiredAttributes", requiredAttributes);
        manager.setPolicies(properties);
        testAllPolicies();
    }

    @Test
    public void testSetPropertiesIgnoresNullMap() {
        String[] authTypes =
                new String[] {"/=SAML|BASIC", "/search=SAML|BASIC|GUEST", "/admin=SAML|BASIC",
                        "/foo=BASIC", "/blah=GUEST", "/unprotected=", "/unprotected2=",
                        "/bleh=GUEST"};
        String[] requiredAttributes =
                new String[] {"/={}", "/search={role=user;control=foo;control=bar}",
                        "/admin={role=admin|supervisor}"};
        manager.setAuthenticationTypes(Arrays.asList(authTypes));
        manager.setRequiredAttributes(Arrays.asList(requiredAttributes));
        manager.configure();
        manager.setPolicies(null);
        testAllPolicies();
    }

    @Test
    public void testWhiteListWithProperties() {
        System.setProperty("org.codice.security.policy.context.test.bar", "/baz");
        manager.setWhiteListContexts(Arrays.asList("/foo",
                "${org.codice.security.policy.context.test.bar}"));
        Assert.assertTrue(manager.getWhiteListContexts()
                .contains("/baz"));
    }

    private void testAllPolicies() {
        //check search policy
        ContextPolicy policy = manager.getContextPolicy("/search");
        Assert.assertEquals("/search", policy.getContextPath());
        Iterator<String> authIter = policy.getAuthenticationMethods()
                .iterator();
        int i = 0;
        while (authIter.hasNext()) {
            if (i == 0) {
                Assert.assertEquals("SAML", authIter.next());
            } else if (i == 1) {
                Assert.assertEquals("BASIC", authIter.next());
            } else if (i == 2) {
                Assert.assertEquals("GUEST", authIter.next());
            }

            i++;
        }

        List<Permission> permissionList = policy.getAllowedAttributePermissions()
                .getPermissionList();
        Assert.assertEquals("role : user",
                permissionList.get(0)
                        .toString());
        Assert.assertEquals("control : foo",
                permissionList.get(1)
                        .toString());
        Assert.assertEquals("control : bar",
                permissionList.get(2)
                        .toString());

        //check admin policy
        policy = manager.getContextPolicy("/admin");
        Assert.assertEquals("/admin", policy.getContextPath());
        authIter = policy.getAuthenticationMethods()
                .iterator();
        i = 0;
        while (authIter.hasNext()) {
            if (i == 0) {
                Assert.assertEquals("SAML", authIter.next());
            } else if (i == 1) {
                Assert.assertEquals("BASIC", authIter.next());
            }

            i++;
        }

        //check foo policy
        policy = manager.getContextPolicy("/foo");
        Assert.assertEquals("/foo", policy.getContextPath());
        authIter = policy.getAuthenticationMethods()
                .iterator();
        i = 0;
        while (authIter.hasNext()) {
            if (i == 0) {
                Assert.assertEquals("BASIC", authIter.next());
            }

            i++;
        }

        //make sure some random context points to /
        policy = manager.getContextPolicy("/random");
        Assert.assertEquals("/", policy.getContextPath());
        authIter = policy.getAuthenticationMethods()
                .iterator();
        i = 0;
        while (authIter.hasNext()) {
            if (i == 0) {
                Assert.assertEquals("SAML", authIter.next());
            } else if (i == 1) {
                Assert.assertEquals("BASIC", authIter.next());
            }

            i++;
        }

        //check unprotected contexts
        policy = manager.getContextPolicy("/unprotected");
        Assert.assertEquals("/unprotected", policy.getContextPath());
        authIter = policy.getAuthenticationMethods()
                .iterator();
        Assert.assertEquals(false, authIter.hasNext());

        policy = manager.getContextPolicy("/unprotected2");
        Assert.assertEquals("/unprotected2", policy.getContextPath());
        authIter = policy.getAuthenticationMethods()
                .iterator();
        Assert.assertEquals(false, authIter.hasNext());
    }
}
