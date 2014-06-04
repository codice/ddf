/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package org.codice.security.policy.context;

import ddf.security.permission.CollectionPermission;
import junit.framework.Assert;
import org.codice.ddf.security.policy.context.ContextPolicy;
import org.codice.ddf.security.policy.context.impl.Policy;
import org.codice.ddf.security.policy.context.impl.PolicyManager;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Test for PolicyManager
 */
public class PolicyManagerTest {

    private PolicyManager manager;

    @Before
    public void setup() {
        manager = new PolicyManager();
        manager.setContextPolicy("/", new Policy("/", null, null));
        manager.setContextPolicy("/search", new Policy("/search", null, null));
        manager.setContextPolicy("/admin", new Policy("/admin", null, null));
        manager.setContextPolicy("/search/standard", new Policy("/search/standard", null, null));
        manager.setContextPolicy("/cometd", new Policy("/cometd", null, null));
        manager.setContextPolicy("/search/simple", new Policy("/search/simple", null, null));
        manager.setContextPolicy("/aaaaaa", new Policy("/aaaaaa", null, null));
        manager.setContextPolicy("/aaa", new Policy("/aaa", null, null));
        manager.setContextPolicy("/aaa/aaa", new Policy("/aaa/aaa", null, null));
        manager.setContextPolicy("/foo/bar", new Policy("/foo/bar", null, null));
        manager.setWhiteListContexts("/foo");
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
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("authenticationTypes", "/=SAML|BASIC,/search=SAML|BASIC|ANON,/admin=SAML|BASIC,/foo=BASIC,/blah=ANON,/bleh=ANON,/unprotected=,/unprotected2=");
        properties.put("requiredAttributes", "/={},/blah=,/search={role=user;control=foo|bar},/admin={role=admin|supervisor}");
        manager.setPolicies(properties);
        testAllPolicies();
    }

    @Test
    public void testMangledConfiguration() {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("authenticationTypes", new String[] {"/=SAML|BASIC,/search=SAML|BASIC|ANON,/admin=SAML|BASIC,/foo=BASIC,/blah=ANON", "/unprotected=,/unprotected2=", "/bleh=ANON"});
        properties.put("requiredAttributes", new String[] {"/={},/blah=,/search={role=user;control=foo|bar}", "/admin={role=admin|supervisor}"});
        manager.setPolicies(properties);
        testAllPolicies();
    }

    private void testAllPolicies() {
        //check search policy
        ContextPolicy policy = manager.getContextPolicy("/search");
        Assert.assertEquals("/search", policy.getContextPath());
        Iterator<String> authIter = policy.getAuthenticationMethods().iterator();
        int i = 0;
        while(authIter.hasNext()) {
            if(i == 0) {
                Assert.assertEquals("SAML", authIter.next());
            } else if(i == 1) {
                Assert.assertEquals("BASIC", authIter.next());
            } else if(i == 2) {
                Assert.assertEquals("ANON", authIter.next());
            }

            i++;
        }

        Iterator<CollectionPermission> permIter = policy.getAllowedAttributePermissions().iterator();
        i = 0;
        while(authIter.hasNext()) {
            if(i == 0) {
                Assert.assertEquals("role : user", permIter.next().getPermissionList().get(0).toString());
            } else if(i == 1) {
                Assert.assertEquals("control : foo", permIter.next().getPermissionList().get(0).toString());
                Assert.assertEquals("control : bar", permIter.next().getPermissionList().get(1).toString());
            }

            i++;
        }

        //check admin policy
        policy = manager.getContextPolicy("/admin");
        Assert.assertEquals("/admin", policy.getContextPath());
        authIter = policy.getAuthenticationMethods().iterator();
        i = 0;
        while(authIter.hasNext()) {
            if(i == 0) {
                Assert.assertEquals("SAML", authIter.next());
            } else if(i == 1) {
                Assert.assertEquals("BASIC", authIter.next());
            }

            i++;
        }

        //check foo policy
        policy = manager.getContextPolicy("/foo");
        Assert.assertEquals("/foo", policy.getContextPath());
        authIter = policy.getAuthenticationMethods().iterator();
        i = 0;
        while(authIter.hasNext()) {
            if(i == 0) {
                Assert.assertEquals("BASIC", authIter.next());
            }

            i++;
        }

        //make sure some random context points to /
        policy = manager.getContextPolicy("/random");
        Assert.assertEquals("/", policy.getContextPath());
        authIter = policy.getAuthenticationMethods().iterator();
        i = 0;
        while(authIter.hasNext()) {
            if(i == 0) {
                Assert.assertEquals("SAML", authIter.next());
            } else if(i == 1) {
                Assert.assertEquals("BASIC", authIter.next());
            }

            i++;
        }

        //check unprotected contexts
        policy = manager.getContextPolicy("/unprotected");
        Assert.assertEquals("/unprotected", policy.getContextPath());
        authIter = policy.getAuthenticationMethods().iterator();
        Assert.assertEquals(false, authIter.hasNext());

        policy = manager.getContextPolicy("/unprotected2");
        Assert.assertEquals("/unprotected2", policy.getContextPath());
        authIter = policy.getAuthenticationMethods().iterator();
        Assert.assertEquals(false, authIter.hasNext());
    }
}
