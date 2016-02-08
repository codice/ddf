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
package ddf.catalog.plugin.resource.usage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.shiro.authz.Permission;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.support.DelegatingSubject;
import org.codice.ddf.persistence.attributes.AttributesStore;
import org.junit.Before;
import org.junit.Test;

import ddf.catalog.data.Metacard;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.StopProcessingException;
import ddf.security.SecurityConstants;
import ddf.security.Subject;

public class TestResourceUsagePlugin {

    ResourceUsagePlugin plugin;

    AttributesStore attributeStore;

    AttributesStore emptyAttributeStore;

    private static final String TEST_USER = "testuser";

    private static final String RESOURCE_SIZE = "100";

    Subject subject;

    @Before
    public void setup() {
        attributeStore = spy(new AttributesStore() {
            @Override
            public long getCurrentDataUsageByUser(String username) {
                assertEquals(TEST_USER, username);
                return 500L;
            }

            @Override
            public void updateUserDataUsage(String username, long dataUsage) {

                assertEquals(TEST_USER, username);
                assertEquals(Long.valueOf(RESOURCE_SIZE) + 500L, dataUsage);

                return;
            }
        });

        emptyAttributeStore = spy(new AttributesStore() {
            @Override
            public long getCurrentDataUsageByUser(String username) {
                assertEquals(TEST_USER, username);
                return 0L;
            }

            @Override
            public void updateUserDataUsage(String username, long dataUsage) {

                assertEquals(TEST_USER, username);
                long resourceSize = Long.valueOf(RESOURCE_SIZE);
                assertEquals(resourceSize, dataUsage);

                return;
            }
        });
        plugin = new ResourceUsagePlugin(attributeStore);
    }

    @Test
    public void testNullInput() throws StopProcessingException, PluginExecutionException {
        assertNull(plugin.process(null));
    }

    @Test
    public void testValidResourceSize() throws StopProcessingException, PluginExecutionException {

        ResourceRequest origRequest = getMockResourceRequest(RESOURCE_SIZE, TEST_USER);
        ResourceRequest request = plugin.process(origRequest);
        assertNotNull(plugin.process(origRequest));
        assertEquals(origRequest, request);

    }

    @Test
    public void testResourceSizeNotFound()
            throws StopProcessingException, PluginExecutionException {
        ResourceRequest origRequest = getMockResourceRequest(null, TEST_USER);
        ResourceRequest request = plugin.process(origRequest);
        assertNotNull(plugin.process(origRequest));
        assertEquals(origRequest, request);
    }

    @Test
    public void testInvalidResourceSize() throws StopProcessingException, PluginExecutionException {
        ResourceRequest origRequest = getMockResourceRequest("47 bytes", TEST_USER);
        ResourceRequest request = plugin.process(origRequest);
        assertNotNull(plugin.process(origRequest));
        assertEquals(origRequest, request);
    }

    @Test
    public void testNoSubject() throws StopProcessingException, PluginExecutionException {
        ResourceRequest origRequest = getMockResourceRequestNoSubject(RESOURCE_SIZE);
        ResourceRequest request = plugin.process(origRequest);
        assertNotNull(plugin.process(origRequest));
        assertEquals(origRequest, request);

    }

    private ResourceRequest getMockResourceRequest(String resourceSize, String expectedUsername) {

        AuthorizingRealm realm = mock(AuthorizingRealm.class);

        when(realm.getName()).thenReturn("mockRealm");
        when(realm.isPermitted(any(PrincipalCollection.class), any(Permission.class))).thenReturn(
                true);

        Collection<Realm> realms = new ArrayList<Realm>();
        realms.add(realm);

        DefaultSecurityManager manager = new DefaultSecurityManager();
        manager.setRealms(realms);

        SimplePrincipalCollection principalCollection =
                new SimplePrincipalCollection(new Principal() {
                    @Override
                    public String getName() {
                        return expectedUsername;
                    }

                    @Override
                    public String toString() {
                        return expectedUsername;
                    }
                }, realm.getName());

        subject = new MockSubject(manager, principalCollection);

        ResourceRequest resourceRequest = mock(ResourceRequest.class);
        Map<String, Serializable> requestProperties = new HashMap<String, Serializable>();

        requestProperties.put(SecurityConstants.SECURITY_SUBJECT, subject);

        requestProperties.put(Metacard.RESOURCE_SIZE, resourceSize);
        when(resourceRequest.getPropertyNames()).thenReturn(requestProperties.keySet());
        when(resourceRequest.getPropertyValue(SecurityConstants.SECURITY_SUBJECT)).thenReturn(
                subject);
        when(resourceRequest.getPropertyValue(Metacard.RESOURCE_SIZE)).thenReturn(resourceSize);
        return resourceRequest;
    }

    private ResourceRequest getMockResourceRequestNoSubject(String resourceSize) {

        ResourceRequest resourceRequest = mock(ResourceRequest.class);
        Map<String, Serializable> requestProperties = new HashMap<String, Serializable>();

        requestProperties.put(SecurityConstants.SECURITY_SUBJECT, subject);

        requestProperties.put(Metacard.RESOURCE_SIZE, resourceSize);
        when(resourceRequest.getPropertyNames()).thenReturn(requestProperties.keySet());

        when(resourceRequest.getPropertyValue(Metacard.RESOURCE_SIZE)).thenReturn(resourceSize);
        return resourceRequest;
    }

    private class MockSubject extends DelegatingSubject implements Subject {

        public MockSubject(SecurityManager manager, PrincipalCollection principals) {
            super(principals,
                    true,
                    null,
                    new SimpleSession(UUID.randomUUID()
                            .toString()),
                    manager);
        }

        @Override
        public boolean isGuest() {
            return false;
        }
    }

}
