/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.security.handler.guest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.codice.ddf.platform.filter.AuthenticationException;
import org.codice.ddf.platform.filter.FilterChain;
import org.codice.ddf.security.handler.api.GuestAuthenticationToken;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.codice.ddf.security.handler.api.PKIAuthenticationTokenFactory;
import org.junit.Test;

public class GuestHandlerTest {

  /** This test ensures the proper functionality of GuestHandler's method, getNormalizedToken(). */
  @Test
  public void testGetNormalizedToken() throws AuthenticationException {
    GuestHandler handler = new GuestHandler();
    PKIAuthenticationTokenFactory tokenFactory = new PKIAuthenticationTokenFactory();
    handler.setTokenFactory(tokenFactory);

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    /** Note that the parameters are insignificant as GuestHandler does not use them. */
    HandlerResult result = handler.getNormalizedToken(request, response, chain, true);

    assertNotNull(result);
    assertEquals(HandlerResult.Status.COMPLETED, result.getStatus());
    assertTrue(result.getToken() instanceof GuestAuthenticationToken);
    assertEquals("Guest", result.getToken().getCredentials());
    assertEquals(null, result.getToken().getRealm());
    assertEquals("null-GuestHandler", result.getSource());
  }

  @Test
  public void testHandleError() throws IOException {
    GuestHandler handler = new GuestHandler();
    PKIAuthenticationTokenFactory tokenFactory = new PKIAuthenticationTokenFactory();
    handler.setTokenFactory(tokenFactory);
    StringWriter writer = new StringWriter(1024);
    PrintWriter printWriter = new PrintWriter(writer);
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getAttribute(anyString())).thenReturn("DDF");
    HttpServletResponse response = mock(HttpServletResponse.class);
    when(response.getWriter()).thenReturn(printWriter);

    FilterChain chain = mock(FilterChain.class);

    /** Note that the parameters are insignificant as GuestHandler does not use them. */
    HandlerResult result = handler.handleError(request, response, chain);

    assertNotNull(result);
    assertEquals(HandlerResult.Status.REDIRECTED, result.getStatus());
    assertNull(result.getToken());
    assertEquals("DDF-GuestHandler", result.getSource());
    assertEquals(GuestHandler.INVALID_MESSAGE, writer.toString());
  }
}
