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
package org.codice.ddf.catalog.security.policy.metacard;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Collections;

import org.junit.Test;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.impl.ResourceRequestById;
import ddf.catalog.plugin.PolicyResponse;

public class UploaderPolicyPluginTest {

    public static final String ADMIN_LOCALHOST = "admin@localhost";

    private UploaderPolicyPlugin uploaderPolicyPlugin = new UploaderPolicyPlugin();

    @Test
    public void processPreCreateDoesNothing() throws Exception {
        PolicyResponse response = uploaderPolicyPlugin.processPreCreate(new MetacardImpl(),
                Collections.emptyMap());

        responseIsEmpty(response);
    }

    @Test
    public void processPreUpdateWithNoPointOfContactDoesNothing() throws Exception {
        PolicyResponse response = uploaderPolicyPlugin.processPreUpdate(new MetacardImpl(),
                Collections.emptyMap());

        responseIsEmpty(response);
    }

    @Test
    public void processPostUpdateWithPointOfContactReturnsMap() throws Exception {
        Metacard metacard = new MetacardImpl();
        metacard.setAttribute(new AttributeImpl(Metacard.POINT_OF_CONTACT, ADMIN_LOCALHOST));
        PolicyResponse response = uploaderPolicyPlugin.processPreUpdate(metacard, null);

        responseHasPointOfContactSecurityMap(response);
    }

    @Test
    public void processPreDeleteDoesNothing() throws Exception {
        PolicyResponse response = uploaderPolicyPlugin.processPreDelete(Collections.emptyList(),
                Collections.emptyMap());

        responseIsEmpty(response);
    }

    @Test
    public void processPostDeleteDoesNothing() throws Exception {
        PolicyResponse response = uploaderPolicyPlugin.processPostDelete(new MetacardImpl(),
                Collections.emptyMap());

        responseIsEmpty(response);
    }

    @Test
    public void processPreQueryDoesNothing() throws Exception {
        PolicyResponse response = uploaderPolicyPlugin.processPreQuery(mock(Query.class),
                Collections.emptyMap());

        responseIsEmpty(response);
    }

    @Test
    public void processPostQueryWithNoPointOfContactDoesNothing() throws Exception {
        PolicyResponse response =
                uploaderPolicyPlugin.processPostQuery(new ResultImpl(new MetacardImpl()), null);

        responseIsEmpty(response);
    }

    @Test
    public void processPostQueryWithPointOfContactReturnsMap() throws Exception {
        Metacard metacard = new MetacardImpl();
        metacard.setAttribute(new AttributeImpl(Metacard.POINT_OF_CONTACT, ADMIN_LOCALHOST));
        PolicyResponse response = uploaderPolicyPlugin.processPostQuery(new ResultImpl(metacard),
                null);

        responseHasPointOfContactSecurityMap(response);
    }

    @Test
    public void processPreResourceDoesNothing() throws Exception {
        PolicyResponse response = uploaderPolicyPlugin.processPreResource(new ResourceRequestById(
                "test-id"));

        responseIsEmpty(response);
    }

    @Test
    public void processPostResourceDoesNothing() throws Exception {
        PolicyResponse response =
                uploaderPolicyPlugin.processPostResource(mock(ResourceResponse.class),
                        new MetacardImpl());

        responseIsEmpty(response);
    }

    private void responseIsEmpty(PolicyResponse response) {
        assertThat(response.itemPolicy()
                .entrySet(), hasSize(0));
        assertThat(response.operationPolicy()
                .entrySet(), hasSize(0));
    }

    private void responseHasPointOfContactSecurityMap(PolicyResponse response) {
        assertThat(response.itemPolicy()
                .entrySet(), hasSize(3));
        assertTrue(response.itemPolicy()
                .get(Metacard.POINT_OF_CONTACT + "-all")
                .contains("admin@localhost"));
        assertTrue(response.itemPolicy()
                .get(Metacard.POINT_OF_CONTACT + "-one")
                .contains("admin@localhost"));
        assertTrue(response.itemPolicy()
                .get(Metacard.POINT_OF_CONTACT + "-xacml")
                .contains("admin@localhost"));
        assertThat(response.operationPolicy()
                .entrySet(), hasSize(0));
    }
}
