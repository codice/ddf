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
package org.codice.ddf.security.handler.basic;

import org.apache.geronimo.mail.util.Base64;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.junit.Test;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import java.security.Principal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BasicAuthenticationHandlerTest {

    private static final String USERNAME = "admin";

    private static final String PASSWORD = "admin";

    private static final String CREDENTIALS = USERNAME + ":" + PASSWORD;

    /**
     * This test case handles the scenario in which the credentials should be
     * obtained (i.e. resolve flag is set) but UsernameTokenType could not be
     * created with the HTTP request.
     */
    @Test
    public void testGetNormalizedTokenResolveRedirected() {
        BasicAuthenticationHandler handler = new BasicAuthenticationHandler();

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        HandlerResult result = handler.getNormalizedToken(request, response, chain, true);

        assertNotNull(result);
        assertEquals(HandlerResult.Status.REDIRECTED, result.getStatus());
    }

    /**
     * This test case handles the scenario in which the credentials should be
     * obtained (i.e. resolve flag is set) and UsernameTokenType was created
     * from the HTTP request.
     */
    @Test
    public void testGetNormalizedTokenResolveCompleted() {
        BasicAuthenticationHandler handler = new BasicAuthenticationHandler();

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader(HttpHeaders.AUTHORIZATION))
                .thenReturn("Basic " + new String(Base64.encode(CREDENTIALS.getBytes())));

        HandlerResult result = handler.getNormalizedToken(request, response, chain, true);

        assertNotNull(result);
        assertEquals(HandlerResult.Status.COMPLETED, result.getStatus());
        assertEquals("admin", ((Principal) result.getPrincipal()).getName());
    }

    /**
     * This test case handles the scenario in which the credentials are not to
     * be obtained (i.e. resolve flag is not set) and the UsernameTokenType
     * could not be created with the HTTP request.
     */
    @Test
    public void testGetNormalizedTokenNoResolveNoAction() {
        BasicAuthenticationHandler handler = new BasicAuthenticationHandler();

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        HandlerResult result = handler.getNormalizedToken(request, response, chain, false);

        assertNotNull(result);
        assertEquals(HandlerResult.Status.NO_ACTION, result.getStatus());
    }

    /**
     * This test case handles the scenario in which the credentials are not to
     * be obtained (i.e. resolve flag is not set) and the UsernameTokenType was
     * successfully created from the HTTP request.
     */
    @Test
    public void testGetNormalizedTokenNoResolveCompleted() {
        BasicAuthenticationHandler handler = new BasicAuthenticationHandler();

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(
                "Basic " + new String(Base64.encode(CREDENTIALS.getBytes())));

        HandlerResult result = handler.getNormalizedToken(request, response, chain, false);

        assertNotNull(result);
        assertEquals(HandlerResult.Status.COMPLETED, result.getStatus());
        assertEquals("admin", ((Principal) result.getPrincipal()).getName());
    }

}
