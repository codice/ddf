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
package ddf.catalog.security.plugin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.ThreadContext;
import org.junit.Test;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.UpdateRequest;
import ddf.security.SecurityConstants;
import ddf.security.Subject;

public class SecurityPluginTest {

    public static final String TEST_USER = "test-user";

    @Test
    public void testNominalCaseCreate() throws Exception {
        Subject mockSubject = mock(Subject.class);
        PrincipalCollection mockPrincipals = mock(PrincipalCollection.class);
        when(mockPrincipals.getPrimaryPrincipal()).thenReturn(TEST_USER);
        when(mockSubject.getPrincipals()).thenReturn(mockPrincipals);

        ThreadContext.bind(mockSubject);
        CreateRequest request = new MockCreateRequest();
        SecurityPlugin plugin = new SecurityPlugin();

        request = plugin.processPreCreate(request);

        assertThat(request.getPropertyValue(SecurityConstants.SECURITY_SUBJECT),
                equalTo(mockSubject));
        assertThat(request.getMetacards()
                .size(), is(2));
        request.getMetacards()
                .forEach(metacard -> assertThat(metacard.getAttribute(Metacard.POINT_OF_CONTACT)
                        .getValue(), equalTo(TEST_USER)));
    }

    @Test
    public void testNominalCaseUpdate() throws Exception {
        Subject mockSubject = mock(Subject.class);
        ThreadContext.bind(mockSubject);
        UpdateRequest request = new MockUpdateRequest();
        SecurityPlugin plugin = new SecurityPlugin();
        request = plugin.processPreUpdate(request, new HashMap<>());
        assertThat(request.getPropertyValue(SecurityConstants.SECURITY_SUBJECT),
                equalTo(mockSubject));
    }

    @Test
    public void testNominalCaseDelete() throws Exception {
        Subject mockSubject = mock(Subject.class);
        ThreadContext.bind(mockSubject);
        DeleteRequest request = new MockDeleteRequest();
        SecurityPlugin plugin = new SecurityPlugin();
        request = plugin.processPreDelete(request);
        request = plugin.processPreDelete(request);
        assertThat(request.getPropertyValue(SecurityConstants.SECURITY_SUBJECT),
                equalTo(mockSubject));
    }

    @Test
    public void testNominalCaseQuery() throws Exception {
        Subject mockSubject = mock(Subject.class);
        ThreadContext.bind(mockSubject);
        QueryRequest request = new MockQueryRequest();
        SecurityPlugin plugin = new SecurityPlugin();
        request = plugin.processPreQuery(request);
        assertThat(request.getPropertyValue(SecurityConstants.SECURITY_SUBJECT),
                equalTo(mockSubject));
    }

    @Test
    public void testNominalCaseResource() throws Exception {
        Subject mockSubject = mock(Subject.class);
        ThreadContext.bind(mockSubject);
        ResourceRequest request = new MockResourceRequest();
        SecurityPlugin plugin = new SecurityPlugin();
        request = plugin.processPreResource(request);
        assertThat(request.getPropertyValue(SecurityConstants.SECURITY_SUBJECT),
                equalTo(mockSubject));
    }

    @Test
    public void testSubjectExists() throws Exception {
        Subject mockSubject = mock(Subject.class);
        CreateRequest request = new MockCreateRequest();
        request.getProperties()
                .put(SecurityConstants.SECURITY_SUBJECT, mockSubject);
        SecurityPlugin plugin = new SecurityPlugin();
        request = plugin.processPreCreate(request);
        assertThat(request.getPropertyValue(SecurityConstants.SECURITY_SUBJECT),
                equalTo(mockSubject));
    }

    @Test
    public void testBadSubjectCase() throws Exception {
        Subject mockSubject = mock(Subject.class);
        ThreadContext.bind(mockSubject);
        CreateRequest request = new MockCreateRequest();
        request.getProperties()
                .put(SecurityConstants.SECURITY_SUBJECT, new HashMap<>());
        SecurityPlugin plugin = new SecurityPlugin();
        request = plugin.processPreCreate(request);
        assertThat(request.getPropertyValue(SecurityConstants.SECURITY_SUBJECT),
                equalTo(mockSubject));
    }

    @Test
    public void testWrongSubjectCase() throws Exception {
        org.apache.shiro.subject.Subject wrongSubject =
                mock(org.apache.shiro.subject.Subject.class);
        ThreadContext.bind(wrongSubject);
        CreateRequest request = new MockCreateRequest();
        SecurityPlugin plugin = new SecurityPlugin();
        request = plugin.processPreCreate(request);
        assertThat(request.getPropertyValue(SecurityConstants.SECURITY_SUBJECT), equalTo(null));
    }

    public static class MockCreateRequest implements CreateRequest {
        private Map<String, Serializable> props = new HashMap<>();

        private List<Metacard> metacards;

        @Override
        public List<Metacard> getMetacards() {
            if (metacards == null) {
                metacards = new ArrayList<>();
                metacards.add(new MetacardImpl());
                metacards.add(new MetacardImpl());
            }

            return metacards;
        }

        @Override
        public Set<String> getPropertyNames() {
            return props.keySet();
        }

        @Override
        public Serializable getPropertyValue(String name) {
            return props.get(name);
        }

        @Override
        public boolean containsPropertyName(String name) {
            return props.containsKey(name);
        }

        @Override
        public boolean hasProperties() {
            return true;
        }

        @Override
        public Map<String, Serializable> getProperties() {
            return props;
        }
    }

    public static class MockUpdateRequest implements UpdateRequest {
        private Map<String, Serializable> props = new HashMap<>();

        @Override
        public Set<String> getPropertyNames() {
            return props.keySet();
        }

        @Override
        public Serializable getPropertyValue(String name) {
            return props.get(name);
        }

        @Override
        public boolean containsPropertyName(String name) {
            return props.containsKey(name);
        }

        @Override
        public boolean hasProperties() {
            return true;
        }

        @Override
        public Map<String, Serializable> getProperties() {
            return props;
        }

        @Override
        public String getAttributeName() {
            return "";
        }

        @Override
        public List<Map.Entry<Serializable, Metacard>> getUpdates() {
            return new ArrayList<>();
        }
    }

    public static class MockDeleteRequest implements DeleteRequest {
        private Map<String, Serializable> props = new HashMap<>();

        @Override
        public Set<String> getPropertyNames() {
            return props.keySet();
        }

        @Override
        public Serializable getPropertyValue(String name) {
            return props.get(name);
        }

        @Override
        public boolean containsPropertyName(String name) {
            return props.containsKey(name);
        }

        @Override
        public boolean hasProperties() {
            return true;
        }

        @Override
        public Map<String, Serializable> getProperties() {
            return props;
        }

        @Override
        public String getAttributeName() {
            return "";
        }

        @Override
        public List<? extends Serializable> getAttributeValues() {
            return null;
        }
    }

    public static class MockQueryRequest implements QueryRequest {
        private Map<String, Serializable> props = new HashMap<>();

        @Override
        public Set<String> getPropertyNames() {
            return props.keySet();
        }

        @Override
        public Serializable getPropertyValue(String name) {
            return props.get(name);
        }

        @Override
        public boolean containsPropertyName(String name) {
            return props.containsKey(name);
        }

        @Override
        public boolean hasProperties() {
            return true;
        }

        @Override
        public Map<String, Serializable> getProperties() {
            return props;
        }

        @Override
        public Query getQuery() {
            return mock(Query.class);
        }

        @Override
        public Set<String> getSourceIds() {
            return new HashSet<>();
        }

        @Override
        public boolean isEnterprise() {
            return false;
        }
    }

    public static class MockResourceRequest implements ResourceRequest {
        private Map<String, Serializable> props = new HashMap<>();

        @Override
        public Set<String> getPropertyNames() {
            return props.keySet();
        }

        @Override
        public Serializable getPropertyValue(String name) {
            return props.get(name);
        }

        @Override
        public boolean containsPropertyName(String name) {
            return props.containsKey(name);
        }

        @Override
        public boolean hasProperties() {
            return true;
        }

        @Override
        public Map<String, Serializable> getProperties() {
            return props;
        }

        @Override
        public String getAttributeName() {
            return "";
        }

        @Override
        public Serializable getAttributeValue() {
            return "";
        }
    }
}
