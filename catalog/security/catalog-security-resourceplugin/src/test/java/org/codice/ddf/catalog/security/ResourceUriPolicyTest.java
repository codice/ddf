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
package org.codice.ddf.catalog.security;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.plugin.PolicyPlugin;
import ddf.catalog.plugin.PolicyResponse;
import ddf.catalog.plugin.StopProcessingException;

public class ResourceUriPolicyTest {

    private String key;

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testTwoEmptyUris() throws URISyntaxException, StopProcessingException {

        PolicyPlugin policyPlugin = getPolicyPlugin("",
                new String[] {"role=admin", "fizzle=bang"},
                new String[] {"role=admin", "fizzle=bang"});

        PolicyResponse response = policyPlugin.processPreUpdate(getMockMetacard(""), null);

        assertEmptyResponse(
                "If metacard has resource URI and it matches update resource URI, no policy is needed",
                response);
    }

    @Test
    public void testInputUriNotEmptyAndMatchesCatalogUri()
            throws URISyntaxException, StopProcessingException {

        PolicyPlugin policyPlugin = getPolicyPlugin("sampleURI",
                new String[] {"role=admin", "fizzle=bang"},
                new String[] {"role=admin", "fizzle=bang"});

        PolicyResponse response = policyPlugin.processPreUpdate(getMockMetacard("sampleURI"), null);

        assertEmptyResponse(
                "If metacard has resource URI and it matches update resource URI, no policy is needed",
                response);
    }

    @Test
    public void testInputUriEmptyButCatalogUriNotEmpty()
            throws URISyntaxException, StopProcessingException {

        PolicyPlugin policyPlugin = getPolicyPlugin("sampleURI",
                new String[] {"role=admin", "fizzle=bang"},
                new String[] {"role=admin", "fizzle=bang"});

        PolicyResponse response = policyPlugin.processPreUpdate(getMockMetacard(""), null);
        Map<String, Set<String>> itemPolicy = response.itemPolicy();

        assertThat(
                "If metacard has resource URI, but update does not, policy needed to ensure no overwriting occurs",
                itemPolicy.isEmpty(),
                is(false));
    }

    @Test
    public void testInputUriNotEmptyButCatalogUriEmpty()
            throws URISyntaxException, StopProcessingException {

        PolicyPlugin policyPlugin = getPolicyPlugin("",
                new String[] {"role=admin", "fizzle=bang"},
                new String[] {"role=admin", "fizzle=bang"});

        PolicyResponse response = policyPlugin.processPreUpdate(getMockMetacard("sampleURI"), null);
        Map<String, Set<String>> itemPolicy = response.itemPolicy();

        assertThat(
                "If metacard has no resource URI, but update does, policy needed to ensure no overwriting occurs",
                itemPolicy.isEmpty(),
                is(false));
    }

    @Test
    public void testInputUriNotEmptyAndDifferentThanCatalogUri()
            throws URISyntaxException, StopProcessingException {

        PolicyPlugin policyPlugin = getPolicyPlugin("sampleURI",
                new String[] {"role=admin", "fizzle=bang"},
                new String[] {"role=admin", "fizzle=bang"});

        PolicyResponse response = policyPlugin.processPreUpdate(getMockMetacard("differentURI"),
                null);
        Map<String, Set<String>> itemPolicy = response.itemPolicy();

        assertThat(
                "If metacard and update each has resource URI, but differ, policy needed to ensure no overwriting occurs",
                itemPolicy.isEmpty(),
                is(false));
    }

    @Test
    public void testCreatePermission() throws URISyntaxException, StopProcessingException {
        String key = "baz";
        String value = "foo";

        PolicyPlugin policyPlugin = getPolicyPlugin("",
                new String[] {"role=admin", key + "=" + value},
                new String[] {"role=admin", "fizzle=bang"});

        PolicyResponse response = policyPlugin.processPreCreate(getMockMetacard("sampleURI"), null);
        Map<String, Set<String>> itemPolicy = response.itemPolicy();

        assertThat("Creating a metacard with a resource URI requires special permissions",
                itemPolicy.containsKey(key),
                is(true));

        assertThat(itemPolicy.get(key), containsInAnyOrder(value));

    }

    private ResourceUriPolicy getPolicyPlugin(String catalogResourceUri,
            String[] createPermissionsArray, String[] updatePermissionsArray)
            throws URISyntaxException, StopProcessingException {

        Metacard catalogMetacard = mock(MetacardImpl.class);
        when(catalogMetacard.getResourceURI()).thenReturn(new URI(catalogResourceUri));

        ResourceUriPolicy policyPlugin = new ResourceUriPolicy() {
            protected Metacard getMetacardFromCatalog(String id) {
                return catalogMetacard;
            }
        };

        policyPlugin.setCreatePermissions(createPermissionsArray);
        policyPlugin.setUpdatePermissions(updatePermissionsArray);
        return policyPlugin;
    }

    private Metacard getMockMetacard(String inputResourceUri) throws URISyntaxException {
        Metacard inputMetacard = mock(MetacardImpl.class);
        when(inputMetacard.getId()).thenReturn("id");
        when(inputMetacard.getResourceURI()).thenReturn(new URI(inputResourceUri));

        return inputMetacard;
    }

    private void assertEmptyResponse(String message, PolicyResponse response) {
        assertThat(message,
                response.itemPolicy()
                        .isEmpty(),
                is(equalTo(true)));
    }
}