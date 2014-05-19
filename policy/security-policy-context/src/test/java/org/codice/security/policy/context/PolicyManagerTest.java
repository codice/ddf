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

import junit.framework.Assert;
import org.codice.security.policy.context.impl.Policy;
import org.codice.security.policy.context.impl.PolicyManager;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by tustisos on 5/16/14.
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
    }

    @Test
    public void testConfiguration() {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("authenticationTypes", "/=SAML|BASIC,/search=SAML|BASIC|ANON,/admin=SAML|BASIC,/foo=BASIC,/blah=ANON,/bleh=ANON");
        properties.put("requiredAttributes", "/={},/blah=,/search={role=user;control=foo|bar},/admin={role=admin|supervisor}");
        manager.setPolicies(properties);

        //check search policy
        ContextPolicy policy = manager.getContextPolicy("/search");
        Assert.assertEquals("/search", policy.getContextPath());
        Assert.assertEquals("SAML", policy.getAuthenticationMethods().get(0));
        Assert.assertEquals("BASIC", policy.getAuthenticationMethods().get(1));
        Assert.assertEquals("ANON", policy.getAuthenticationMethods().get(2));

        String permission = policy.getAllowedAttributePermissions().get(0).getPermissionList().get(0).toString();
        Assert.assertEquals("role : user", permission);
        String permission2 = policy.getAllowedAttributePermissions().get(1).getPermissionList().get(0).toString();
        Assert.assertEquals("control : foo", permission2);
        permission2 = policy.getAllowedAttributePermissions().get(1).getPermissionList().get(1).toString();
        Assert.assertEquals("control : bar", permission2);

        //check admin policy
        policy = manager.getContextPolicy("/admin");
        Assert.assertEquals("/admin", policy.getContextPath());
        Assert.assertEquals("SAML", policy.getAuthenticationMethods().get(0));
        Assert.assertEquals("BASIC", policy.getAuthenticationMethods().get(1));

        //check foo policy
        policy = manager.getContextPolicy("/foo");
        Assert.assertEquals("/foo", policy.getContextPath());
        Assert.assertEquals("BASIC", policy.getAuthenticationMethods().get(0));

        //make sure some random context points to /
        policy = manager.getContextPolicy("/random");
        Assert.assertEquals("/", policy.getContextPath());
        Assert.assertEquals("SAML", policy.getAuthenticationMethods().get(0));
        Assert.assertEquals("BASIC", policy.getAuthenticationMethods().get(1));
    }
}
