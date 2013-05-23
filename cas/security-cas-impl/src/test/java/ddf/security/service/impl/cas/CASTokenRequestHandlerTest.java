/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.security.service.impl.cas;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.BasicConfigurator;
import org.apache.shiro.authc.AuthenticationToken;
import org.jasig.cas.client.authentication.AttributePrincipal;
import org.junit.BeforeClass;
import org.junit.Test;

import ddf.security.service.SecurityServiceException;


public class CASTokenRequestHandlerTest
{

    private static final String SAMPLE_TICKET = "ST-956-Lyg0BdLkgdrBO9W17bXS";
    private static final String SAMPLE_SERVICE = "http://localhost/test";
    private static final String DEFAULT_SERVICE = "https://server:8993/services/SecurityTokenService";

    @BeforeClass( )
    public static void setupLogging()
    {
        BasicConfigurator.configure();
    }

    /**
     * Tests that with no setting changes the ticket is returned.
     * 
     * @throws SecurityServiceException
     */
    @Test
    public void testDefaultAddress() throws SecurityServiceException
    {
        // setup mock classes
        AttributePrincipal principal = mock(AttributePrincipal.class);
        when(principal.getProxyTicketFor(anyString())).thenReturn(SAMPLE_TICKET);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getUserPrincipal()).thenReturn(principal);

        CASTokenRequestHandler handler = new CASTokenRequestHandler();
        Object token = handler.createToken(request);
        assertTrue(token instanceof AuthenticationToken);
        assertEquals(SAMPLE_TICKET, ((AuthenticationToken) token).getCredentials());
    }

    /**
     * Tests that an exception is thrown if there was no principal in the
     * request.
     * 
     * @throws SecurityServiceException
     */
    @Test( expected = SecurityServiceException.class )
    public void testNoAttribute() throws SecurityServiceException
    {
        HttpServletRequest request = mock(HttpServletRequest.class);
        CASTokenRequestHandler handler = new CASTokenRequestHandler();
        handler.createToken(request);
        fail("No Principal was added to request, code should throw an exception.");
    }

    /**
     * Tests that when the settings are updated, the new address is used. If the
     * default address is used by the code, the mock principal will throw a
     * runtime exception.
     * 
     * @throws SecurityServiceException
     */
    @Test
    public void testUpdatedAddress() throws SecurityServiceException
    {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("sts.address", SAMPLE_SERVICE);
        // setup mock classes
        AttributePrincipal principal = mock(AttributePrincipal.class);
        when(principal.getProxyTicketFor(SAMPLE_SERVICE)).thenReturn(SAMPLE_TICKET);
        when(principal.getProxyTicketFor(DEFAULT_SERVICE)).thenThrow(
            new IllegalArgumentException("Code should have used updated service instead of default service."));
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getUserPrincipal()).thenReturn(principal);

        CASTokenRequestHandler handler = new CASTokenRequestHandler();
        handler.ddfConfigurationUpdated(properties);
        Object token = handler.createToken(request);
        assertTrue(token instanceof AuthenticationToken);
        assertEquals(SAMPLE_TICKET, ((AuthenticationToken) token).getCredentials());
    }
}
