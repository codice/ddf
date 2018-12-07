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
package org.codice.ddf.security.filter.websso;

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import java.io.IOException;
import java.util.Collections;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.codice.ddf.platform.filter.AuthenticationException;
import org.codice.ddf.platform.filter.FilterChain;
import org.codice.ddf.security.handler.api.AuthenticationHandler;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.codice.ddf.security.handler.api.HandlerResult.Status;
import org.codice.ddf.security.policy.context.ContextPolicy;
import org.codice.ddf.security.policy.context.ContextPolicyManager;
import org.junit.Test;
import org.slf4j.LoggerFactory;

public class WebSSOFilterTest {

  private static final String DDF_AUTHENTICATION_TOKEN = "ddf.security.token";

  private static final String MOCK_CONTEXT = "/test";

  @Test
  public void testInit() {
    final Logger logger = (Logger) LoggerFactory.getLogger(WebSSOFilter.class);
    logger.setLevel(Level.DEBUG);

    AuthenticationHandler handler = mock(AuthenticationHandler.class);
    when(handler.getAuthenticationType()).thenReturn("basic");

    WebSSOFilter webSSOFilter = new WebSSOFilter();
    webSSOFilter.setHandlerList(Collections.singletonList(handler));
    webSSOFilter.init();

    logger.setLevel(Level.OFF);
  }

  @Test
  public void testDoFilterWhiteListed() throws IOException, AuthenticationException {
    ContextPolicy testPolicy = mock(ContextPolicy.class);
    when(testPolicy.getRealm()).thenReturn("TestRealm");
    ContextPolicyManager policyManager = mock(ContextPolicyManager.class);
    when(policyManager.getContextPolicy(nullable(String.class))).thenReturn(testPolicy);
    when(policyManager.isWhiteListed(nullable(String.class))).thenReturn(true);

    WebSSOFilter filter = new WebSSOFilter();
    // set handlers
    AuthenticationHandler handler1 = mock(AuthenticationHandler.class);
    HandlerResult noActionResult = mock(HandlerResult.class);
    when(noActionResult.getStatus()).thenReturn(Status.NO_ACTION);
    HandlerResult completedResult = mock(HandlerResult.class);
    when(completedResult.getStatus()).thenReturn(Status.COMPLETED);
    when(completedResult.getToken()).thenReturn(null);
    when(handler1.getNormalizedToken(
            any(ServletRequest.class),
            any(ServletResponse.class),
            any(FilterChain.class),
            eq(true)))
        .thenReturn(completedResult);
    when(handler1.getNormalizedToken(
            any(ServletRequest.class),
            any(ServletResponse.class),
            any(FilterChain.class),
            eq(false)))
        .thenReturn(noActionResult);

    filter.setHandlerList(Collections.singletonList(handler1));
    filter.setContextPolicyManager(policyManager);

    FilterChain filterChain = mock(FilterChain.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getContextPath()).thenReturn(MOCK_CONTEXT);
    HttpServletResponse response = mock(HttpServletResponse.class);

    filter.doFilter(request, response, filterChain);

    verify(request, times(1)).setAttribute(ContextPolicy.ACTIVE_REALM, "TestRealm");
    verify(request, times(1)).setAttribute(ContextPolicy.NO_AUTH_POLICY, true);
    verify(filterChain).doFilter(request, response);
    verify(handler1, never())
        .getNormalizedToken(
            any(HttpServletRequest.class),
            any(HttpServletResponse.class),
            any(FilterChain.class),
            anyBoolean());
  }

  @Test
  public void testDoFilterResolvingOnSecondCall() throws IOException, AuthenticationException {
    ContextPolicy testPolicy = mock(ContextPolicy.class);
    when(testPolicy.getRealm()).thenReturn("TestRealm");
    ContextPolicyManager policyManager = mock(ContextPolicyManager.class);
    when(policyManager.getContextPolicy(anyString())).thenReturn(testPolicy);
    when(policyManager.isWhiteListed(anyString())).thenReturn(false);

    WebSSOFilter filter = new WebSSOFilter();

    // set handlers
    AuthenticationHandler handler1 = mock(AuthenticationHandler.class);
    HandlerResult noActionResult = mock(HandlerResult.class);
    when(noActionResult.getStatus()).thenReturn(Status.NO_ACTION);
    HandlerResult completedResult = mock(HandlerResult.class);
    when(completedResult.getStatus()).thenReturn(Status.COMPLETED);
    when(completedResult.getToken()).thenReturn(null);
    when(handler1.getNormalizedToken(
            any(ServletRequest.class),
            any(ServletResponse.class),
            any(FilterChain.class),
            eq(true)))
        .thenReturn(completedResult);
    when(handler1.getNormalizedToken(
            any(ServletRequest.class),
            any(ServletResponse.class),
            any(FilterChain.class),
            eq(false)))
        .thenReturn(noActionResult);

    filter.setHandlerList(Collections.singletonList(handler1));

    FilterChain filterChain = mock(FilterChain.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getContextPath()).thenReturn(MOCK_CONTEXT);
    HttpServletResponse response = mock(HttpServletResponse.class);

    try {
      filter.doFilter(request, response, filterChain);
    } catch (AuthenticationException e) {

    }

    verify(handler1, times(2))
        .getNormalizedToken(
            any(HttpServletRequest.class),
            any(HttpServletResponse.class),
            any(FilterChain.class),
            anyBoolean());
    // the next filter should NOT be called
    verify(filterChain, never()).doFilter(request, response);
    verify(request, never()).setAttribute(eq(DDF_AUTHENTICATION_TOKEN), any(HandlerResult.class));
  }

  @Test
  public void testDoFilterWithRedirected() throws AuthenticationException, IOException {
    ContextPolicy testPolicy = mock(ContextPolicy.class);
    when(testPolicy.getRealm()).thenReturn("TestRealm");
    ContextPolicyManager policyManager = mock(ContextPolicyManager.class);
    when(policyManager.getContextPolicy(anyString())).thenReturn(testPolicy);
    when(policyManager.isWhiteListed(anyString())).thenReturn(false);

    WebSSOFilter filter = new WebSSOFilter();

    // set handlers
    AuthenticationHandler handler1 = mock(AuthenticationHandler.class);
    HandlerResult noActionResult = mock(HandlerResult.class);
    when(noActionResult.getStatus()).thenReturn(Status.NO_ACTION);
    HandlerResult redirectedResult = mock(HandlerResult.class);
    when(redirectedResult.getStatus()).thenReturn(Status.REDIRECTED);
    when(redirectedResult.getToken()).thenReturn(null);
    when(handler1.getNormalizedToken(
            any(ServletRequest.class),
            any(ServletResponse.class),
            any(FilterChain.class),
            eq(false)))
        .thenReturn(noActionResult);
    when(handler1.getNormalizedToken(
            any(ServletRequest.class),
            any(ServletResponse.class),
            any(FilterChain.class),
            eq(true)))
        .thenReturn(redirectedResult);

    filter.setHandlerList(Collections.singletonList(handler1));

    FilterChain filterChain = mock(FilterChain.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getContextPath()).thenReturn(MOCK_CONTEXT);
    HttpServletResponse response = mock(HttpServletResponse.class);

    try {
      filter.doFilter(request, response, filterChain);
    } catch (AuthenticationException e) {

    }

    // the next filter should NOT be called
    verify(filterChain, never()).doFilter(request, response);
    verify(request, never()).setAttribute(eq(DDF_AUTHENTICATION_TOKEN), any(HandlerResult.class));
  }

  @Test
  public void testDoFilterReturnsStatusCode503WhenNoHandlersRegistered()
      throws IOException, AuthenticationException {
    WebSSOFilter filter = new WebSSOFilter();
    FilterChain filterChain = mock(FilterChain.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    filter.doFilter(request, response, filterChain);

    verify(response).setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    verify(response).flushBuffer();
    verify(filterChain, never()).doFilter(request, response);
  }
}
