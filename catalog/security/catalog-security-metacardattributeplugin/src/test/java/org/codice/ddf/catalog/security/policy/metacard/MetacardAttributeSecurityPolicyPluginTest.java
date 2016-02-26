/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package org.codice.ddf.catalog.security.policy.metacard;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.plugin.PolicyResponse;
import ddf.catalog.plugin.StopProcessingException;

public class MetacardAttributeSecurityPolicyPluginTest {

    MetacardAttributeSecurityPolicyPlugin plugin;

    Metacard metacard;

    Metacard metacard1;

    @Before
    public void setup() {
        metacard = new MetacardImpl();
        metacard.setAttribute(new AttributeImpl("parsed.security", Arrays.asList("A", "B", "C")));
        metacard.setAttribute(new AttributeImpl("parsed.countries", Arrays.asList("USA", "CAN")));
        metacard.setAttribute(new AttributeImpl("parsed.other", Arrays.asList("X", "Y")));
        metacard1 = new MetacardImpl();
        metacard1.setAttribute(new AttributeImpl("parsed.security", Arrays.asList("X", "Y", "Z")));
        metacard1.setAttribute(new AttributeImpl("parsed.countries", Arrays.asList("GBR", "CAN")));
        metacard1.setAttribute(new AttributeImpl("parsed.other", Arrays.asList("E", "F")));
        plugin = new MetacardAttributeSecurityPolicyPlugin();
        plugin.setMetacardAttributes(Arrays.asList("parsed.security", "parsed.countries"));
    }

    @Test
    public void testProcessPreCreate() throws StopProcessingException {
        PolicyResponse policyResponse = plugin.processPreCreate(metacard, new HashMap<>());
        assertThat(policyResponse.itemPolicy()
                .size(), is(2));

        assertTrue(policyResponse.itemPolicy()
                .get("parsed.security")
                .contains("A"));
        assertTrue(policyResponse.itemPolicy()
                .get("parsed.security")
                .contains("B"));
        assertTrue(policyResponse.itemPolicy()
                .get("parsed.security")
                .contains("C"));

        assertTrue(policyResponse.itemPolicy()
                .get("parsed.countries")
                .contains("USA"));
        assertTrue(policyResponse.itemPolicy()
                .get("parsed.countries")
                .contains("CAN"));

        assertNull(policyResponse.itemPolicy()
                .get("parsed.other"));
    }

    @Test
    public void testProcessPreUpdate() throws StopProcessingException {
        PolicyResponse policyResponse = plugin.processPreUpdate(metacard, new HashMap<>());
        assertThat(policyResponse.itemPolicy()
                .size(), is(2));

        assertTrue(policyResponse.itemPolicy()
                .get("parsed.security")
                .contains("A"));
        assertTrue(policyResponse.itemPolicy()
                .get("parsed.security")
                .contains("B"));
        assertTrue(policyResponse.itemPolicy()
                .get("parsed.security")
                .contains("C"));

        assertTrue(policyResponse.itemPolicy()
                .get("parsed.countries")
                .contains("USA"));
        assertTrue(policyResponse.itemPolicy()
                .get("parsed.countries")
                .contains("CAN"));

        assertNull(policyResponse.itemPolicy()
                .get("parsed.other"));
    }

    @Test
    public void testProcessPreDelete() throws StopProcessingException {
        PolicyResponse policyResponse = plugin.processPreDelete(Arrays.asList(metacard, metacard1),
                new HashMap<>());
        assertThat(policyResponse.operationPolicy()
                .size(), is(2));

        assertTrue(policyResponse.operationPolicy()
                .get("parsed.security")
                .contains("A"));
        assertTrue(policyResponse.operationPolicy()
                .get("parsed.security")
                .contains("B"));
        assertTrue(policyResponse.operationPolicy()
                .get("parsed.security")
                .contains("C"));
        assertTrue(policyResponse.operationPolicy()
                .get("parsed.security")
                .contains("X"));
        assertTrue(policyResponse.operationPolicy()
                .get("parsed.security")
                .contains("Y"));
        assertTrue(policyResponse.operationPolicy()
                .get("parsed.security")
                .contains("Z"));

        assertTrue(policyResponse.operationPolicy()
                .get("parsed.countries")
                .contains("USA"));
        assertTrue(policyResponse.operationPolicy()
                .get("parsed.countries")
                .contains("CAN"));
        assertTrue(policyResponse.operationPolicy()
                .get("parsed.countries")
                .contains("GBR"));
    }

    @Test
    public void testProcessPostDelete() throws StopProcessingException {
        PolicyResponse policyResponse = plugin.processPostDelete(metacard, new HashMap<>());
        assertThat(policyResponse.itemPolicy()
                .size(), is(2));

        assertTrue(policyResponse.itemPolicy()
                .get("parsed.security")
                .contains("A"));
        assertTrue(policyResponse.itemPolicy()
                .get("parsed.security")
                .contains("B"));
        assertTrue(policyResponse.itemPolicy()
                .get("parsed.security")
                .contains("C"));

        assertTrue(policyResponse.itemPolicy()
                .get("parsed.countries")
                .contains("USA"));
        assertTrue(policyResponse.itemPolicy()
                .get("parsed.countries")
                .contains("CAN"));

        assertNull(policyResponse.itemPolicy()
                .get("parsed.other"));
    }

    @Test
    public void testProcessPostQuery() throws StopProcessingException {
        Result result = mock(Result.class);
        when(result.getMetacard()).thenReturn(metacard);
        PolicyResponse policyResponse = plugin.processPostQuery(result, new HashMap<>());
        assertThat(policyResponse.itemPolicy()
                .size(), is(2));

        assertTrue(policyResponse.itemPolicy()
                .get("parsed.security")
                .contains("A"));
        assertTrue(policyResponse.itemPolicy()
                .get("parsed.security")
                .contains("B"));
        assertTrue(policyResponse.itemPolicy()
                .get("parsed.security")
                .contains("C"));

        assertTrue(policyResponse.itemPolicy()
                .get("parsed.countries")
                .contains("USA"));
        assertTrue(policyResponse.itemPolicy()
                .get("parsed.countries")
                .contains("CAN"));

        assertNull(policyResponse.itemPolicy()
                .get("parsed.other"));
    }

    @Test
    public void testProcessPostResource() throws StopProcessingException {
        PolicyResponse policyResponse = plugin.processPostResource(mock(ResourceResponse.class),
                metacard);
        assertThat(policyResponse.itemPolicy()
                .size(), is(2));

        assertTrue(policyResponse.itemPolicy()
                .get("parsed.security")
                .contains("A"));
        assertTrue(policyResponse.itemPolicy()
                .get("parsed.security")
                .contains("B"));
        assertTrue(policyResponse.itemPolicy()
                .get("parsed.security")
                .contains("C"));

        assertTrue(policyResponse.itemPolicy()
                .get("parsed.countries")
                .contains("USA"));
        assertTrue(policyResponse.itemPolicy()
                .get("parsed.countries")
                .contains("CAN"));

        assertNull(policyResponse.itemPolicy()
                .get("parsed.other"));
    }

    @Test
    public void testUnusedMethods() throws StopProcessingException {
        PolicyResponse policyResponse = plugin.processPreQuery(mock(Query.class), new HashMap<>());
        assertThat(policyResponse.itemPolicy().size(), is(0));

        policyResponse = plugin.processPreResource(mock(ResourceRequest.class));
        assertThat(policyResponse.itemPolicy().size(), is(0));
    }
}
