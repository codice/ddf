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
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.shiro.util.ThreadContext;
import org.junit.Test;

import ddf.catalog.data.Metacard;
import ddf.catalog.operation.CreateRequest;
import ddf.security.SecurityConstants;
import ddf.security.Subject;

public class SecurityPluginTest {

    @Test
    public void testNominalCase() throws Exception {
        Subject subject = mock(Subject.class);
        ThreadContext.bind(subject);
        CreateRequest request = new MockCreateRequest();
        SecurityPlugin plugin = new SecurityPlugin();
        request = plugin.processPreCreate(request);
        assertThat(request.getPropertyValue(SecurityConstants.SECURITY_SUBJECT), equalTo(subject));
    }

    @Test
    public void testSubjectExists() throws Exception {
        Subject subject = mock(Subject.class);
        CreateRequest request = new MockCreateRequest();
        request.getProperties()
                .put(SecurityConstants.SECURITY_SUBJECT, subject);
        SecurityPlugin plugin = new SecurityPlugin();
        request = plugin.processPreCreate(request);
        assertThat(request.getPropertyValue(SecurityConstants.SECURITY_SUBJECT), equalTo(subject));
    }

    @Test
    public void testBadSubjectCase() throws Exception {
        Subject subject = mock(Subject.class);
        ThreadContext.bind(subject);
        CreateRequest request = new MockCreateRequest();
        request.getProperties()
                .put(SecurityConstants.SECURITY_SUBJECT, new HashMap<>());
        SecurityPlugin plugin = new SecurityPlugin();
        request = plugin.processPreCreate(request);
        assertThat(request.getPropertyValue(SecurityConstants.SECURITY_SUBJECT), equalTo(subject));
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

        @Override
        public List<Metacard> getMetacards() {
            return null;
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
}
