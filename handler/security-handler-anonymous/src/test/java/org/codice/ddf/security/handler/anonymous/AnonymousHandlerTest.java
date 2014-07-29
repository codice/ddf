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
package org.codice.ddf.security.handler.anonymous;

import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.codice.ddf.security.handler.api.UPAuthenticationToken;
import org.junit.Test;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AnonymousHandlerTest {

    /**
     * This test ensures the proper functionality of AnonymousHandler's method,
     * getNormalizedToken().
     */
    @Test
    public void testGetNormalizedToken() {
        AnonymousHandler handler = new AnonymousHandler();

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        /**
         * Note that the parameters are insignificant as AnonymousHandler
         * does not use them.
         */
        HandlerResult result = handler.getNormalizedToken(request, response, chain, true);

        assertNotNull(result);
        assertEquals(HandlerResult.Status.COMPLETED, result.getStatus());
        assertTrue(result.getToken() instanceof UPAuthenticationToken);
        assertEquals("guest", ((UPAuthenticationToken) result.getToken()).getUsername());
        assertEquals("guest", ((UPAuthenticationToken) result.getToken()).getPassword());
        assertEquals(BaseAuthenticationToken.DEFAULT_REALM, ((UPAuthenticationToken) result.getToken()).getRealm());
        assertEquals("DDF-AnonymousHandler", result.getSource());
    }

    @Test
    public void testHandleError() throws ServletException, IOException {
        AnonymousHandler handler = new AnonymousHandler();
        StringWriter writer = new StringWriter(1024);
        PrintWriter printWriter = new PrintWriter(writer);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(printWriter);

        FilterChain chain = mock(FilterChain.class);

        /**
         * Note that the parameters are insignificant as AnonymousHandler
         * does not use them.
         */
        HandlerResult result = handler.handleError(request, response, chain);

        assertNotNull(result);
        assertEquals(HandlerResult.Status.COMPLETED, result.getStatus());
        assertNull(result.getToken());
        assertEquals("DDF-AnonymousHandler", result.getSource());
        assertEquals(AnonymousHandler.INVALID_MESSAGE, writer.toString());
    }
}
