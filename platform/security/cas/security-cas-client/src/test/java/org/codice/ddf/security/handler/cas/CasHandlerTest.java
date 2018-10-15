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
package org.codice.ddf.security.handler.cas;

import static org.junit.Assert.assertEquals;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.security.sts.client.configuration.STSClientConfiguration;
import java.io.IOException;
import java.util.Collections;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.codice.ddf.platform.filter.AuthenticationException;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.codice.ddf.security.handler.cas.filter.ProxyFilter;
import org.jasig.cas.client.authentication.AttributePrincipal;
import org.jasig.cas.client.util.AbstractCasFilter;
import org.jasig.cas.client.validation.Assertion;
import org.junit.Test;

public class CasHandlerTest {

  private static final String STS_ADDRESS = "http://localhost:8181/sts";

  private static final String MOCK_TICKET = "ST-956-Lyg0BdLkgdrBO9W17bXS";

  private static final String SESSION_ID = "12345678910";

  /**
   * Tests that the handler properly returns a NO_ACTION result if no assertion is available in the
   * request and resolve is false.
   *
   * @throws ServletException
   */
  @Test
  public void testNoPrincipalNoResolve() throws AuthenticationException {
    CasHandler handler = createHandler();
    HandlerResult result =
        handler.getNormalizedToken(
            createServletRequest(false), mock(HttpServletResponse.class), null, false);
    // NO_ACTION due to resolve being false
    assertEquals(HandlerResult.Status.NO_ACTION, result.getStatus());
  }

  /**
   * Tests that the handler properly returns a COMPLETED result if the assertion is in the session.
   *
   * @throws ServletException
   */
  @Test
  public void testPrincipalNoResolve() throws AuthenticationException {
    CasHandler handler = createHandler();
    HandlerResult result =
        handler.getNormalizedToken(
            createServletRequest(true), mock(HttpServletResponse.class), null, false);
    assertEquals(HandlerResult.Status.COMPLETED, result.getStatus());
  }

  /**
   * Tests that the handler properly returns a REDIRECTED result if the assertion is not in the
   * session and resolve is true.
   *
   * @throws ServletException
   * @throws IOException
   */
  @Test
  public void testNoPrincipalResolve()
      throws AuthenticationException, IOException, ServletException {
    CasHandler handler = createHandler();
    AbstractCasFilter testFilter = mock(AbstractCasFilter.class);
    handler.setProxyFilter(new ProxyFilter(Collections.singletonList(testFilter)));
    HandlerResult result =
        handler.getNormalizedToken(
            createServletRequest(false), mock(HttpServletResponse.class), null, true);
    assertEquals(HandlerResult.Status.REDIRECTED, result.getStatus());
    // verify that the filter was called once
    verify(testFilter)
        .doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
  }

  /**
   * Tests that the handler properly returns a COMPLETED result if the assertion is in the session
   * and resolve is true.
   *
   * @throws ServletException
   * @throws IOException
   */
  @Test
  public void testPrincipalResolve() throws AuthenticationException {
    CasHandler handler = createHandler();
    HandlerResult result =
        handler.getNormalizedToken(
            createServletRequest(true), mock(HttpServletResponse.class), null, true);
    assertEquals(HandlerResult.Status.COMPLETED, result.getStatus());
  }

  /**
   * Tests that the handler properly returns a COMPLETED result from having a cached session that
   * contains the CAS assertion.
   *
   * @throws ServletException
   * @throws IOException
   */
  @Test
  public void testCachedPrincipalResolve() throws AuthenticationException {
    CasHandler handler = createHandler();
    HttpServletRequest servletRequest = createServletRequest(true);
    HttpSession session = servletRequest.getSession();
    HandlerResult result =
        handler.getNormalizedToken(servletRequest, mock(HttpServletResponse.class), null, true);
    assertEquals(HandlerResult.Status.COMPLETED, result.getStatus());

    // now check for caching sessions
    servletRequest = createServletRequest(false);
    when(servletRequest.getSession()).thenReturn(session);
    when(servletRequest.getSession(any(Boolean.class))).thenReturn(session);
    result =
        handler.getNormalizedToken(servletRequest, mock(HttpServletResponse.class), null, true);
    assertEquals(HandlerResult.Status.COMPLETED, result.getStatus());
  }

  private CasHandler createHandler() {
    CasHandler handler = new CasHandler();
    STSClientConfiguration clientConfiguration = mock(STSClientConfiguration.class);
    when(clientConfiguration.getAddress()).thenReturn(STS_ADDRESS);
    handler.setClientConfiguration(clientConfiguration);
    AbstractCasFilter testFilter = mock(AbstractCasFilter.class);
    handler.setProxyFilter(new ProxyFilter(Collections.singletonList(testFilter)));
    return handler;
  }

  private HttpServletRequest createServletRequest(boolean shouldAddCas) {
    HttpServletRequest servletRequest = mock(HttpServletRequest.class);
    HttpSession session = mock(HttpSession.class);
    when(session.getId()).thenReturn(SESSION_ID);
    when(servletRequest.getSession()).thenReturn(session);
    when(servletRequest.getSession(any(Boolean.class))).thenReturn(session);
    if (shouldAddCas) {
      // Mock CAS items
      Assertion assertion = mock(Assertion.class);
      when(session.getAttribute(AbstractCasFilter.CONST_CAS_ASSERTION)).thenReturn(assertion);
      AttributePrincipal principal = mock(AttributePrincipal.class);
      when(principal.getProxyTicketFor(STS_ADDRESS)).thenReturn(MOCK_TICKET);
      when(principal.getProxyTicketFor(not(eq(STS_ADDRESS))))
          .thenThrow(new RuntimeException("Tried to create ticket for incorrect service."));
      when(assertion.getPrincipal()).thenReturn(principal);
    }
    return servletRequest;
  }
}
