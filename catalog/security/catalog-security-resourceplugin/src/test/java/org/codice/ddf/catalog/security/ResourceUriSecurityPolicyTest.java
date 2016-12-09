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
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.Test;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.plugin.PolicyResponse;
import ddf.catalog.plugin.StopProcessingException;

public class ResourceUriSecurityPolicyTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testTwoEmptyUris() throws URISyntaxException, StopProcessingException {
        Metacard input = mock(MetacardImpl.class);
        Metacard catalogMetacard = mock(MetacardImpl.class);
        when(catalogMetacard.getResourceURI()).thenReturn(new URI(""));
        when(input.getId()).thenReturn("id");
        when(input.getResourceURI()).thenReturn(new URI(""));
        ResourceUriSecurityPolicy policyPlugin = new ResourceUriSecurityPolicy() {
            protected Metacard getMetacardFromCatalog(String id) {
                return catalogMetacard;
            }
        };

        policyPlugin.setCreatePermissions(new String[] {"role=admin", "fizzle=bang"});
        PolicyResponse response = policyPlugin.processPreUpdate(input, null);
        assertEmptyResponse(
                "If metacard has no resource URI, and update does not have a resource URI, no policy is needed",
                response);
    }

    @Test
    public void testInputUriEmptyButCatalogUriNotEmpty() {

    }

    @Test
    public void testInputUriNotEmtpyButCatalogUriEmpty() {

    }

    @Test
    public void testInputUriNotEmptyAndSameAsCatalogUri() {

    }

    @Test
    public void testInputUriNotEmptyAndDifferentThanCatalogUri() {

    }

    private void assertEmptyResponse(String message, PolicyResponse response) {
        assertThat(message,
                response.itemPolicy()
                        .isEmpty(),
                is(equalTo(true)));
    }

}