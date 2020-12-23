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

import static ddf.security.SecurityConstants.AUTHENTICATION_TOKEN_KEY;
import static ddf.security.SecurityConstants.SECURITY_TOKEN_KEY;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ddf.security.audit.SecurityLogger;
import ddf.security.common.PrincipalHolder;
import java.io.IOException;
import java.util.Collections;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.shiro.subject.PrincipalCollection;
import org.codice.ddf.platform.filter.AuthenticationException;
import org.codice.ddf.platform.filter.SecurityFilterChain;
import org.codice.ddf.security.handler.BaseAuthenticationToken;
import org.codice.ddf.security.handler.GuestAuthenticationToken;
import org.codice.ddf.security.handler.api.AuthenticationHandler;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.codice.ddf.security.handler.api.HandlerResult.Status;
import org.codice.ddf.security.policy.context.ContextPolicy;
import org.codice.ddf.security.policy.context.ContextPolicyManager;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;

public class WebSSOFilterTest {

  private static final String DDF_AUTHENTICATION_TOKEN = "ddf.security.token";

  private static final String MOCK_CONTEXT = "/test";

  @Test
  public void testInit() {
    final Logger logger = (Logger) LoggerFactory.getLogger(WebSSOFilter.class);
    logger.setLevel(Level.DEBUG);

    AuthenticationHandler handlerMock = mock(AuthenticationHandler.class);
    when(handlerMock.getAuthenticationType()).thenReturn("basic");

    ContextPolicyManager policyManager = mock(ContextPolicyManager.class);
    when(policyManager.getSessionAccess()).thenReturn(true);

    WebSSOFilter webSSOFilter = new WebSSOFilter();
    webSSOFilter.setHandlerList(Collections.singletonList(handlerMock));
    webSSOFilter.setContextPolicyManager(policyManager);
    webSSOFilter.init();

    logger.setLevel(Level.OFF);
  }

  @Test
  public void testDoFilterWhiteListed() throws IOException, AuthenticationException {
    ContextPolicy testPolicy = mock(ContextPolicy.class);
    ContextPolicyManager policyManager = mock(ContextPolicyManager.class);
    when(policyManager.getContextPolicy(anyString())).thenReturn(testPolicy);
    when(policyManager.isWhiteListed(anyString())).thenReturn(true);
    when(policyManager.getSessionAccess()).thenReturn(false);

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
            any(SecurityFilterChain.class),
            eq(true)))
        .thenReturn(completedResult);
    when(handler1.getNormalizedToken(
            any(ServletRequest.class),
            any(ServletResponse.class),
            any(SecurityFilterChain.class),
            eq(false)))
        .thenReturn(noActionResult);

    filter.setHandlerList(Collections.singletonList(handler1));
    filter.setContextPolicyManager(policyManager);

    SecurityFilterChain filterChain = mock(SecurityFilterChain.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn(MOCK_CONTEXT);
    HttpServletResponse response = mock(HttpServletResponse.class);

    filter.doFilter(request, response, filterChain);

    verify(request, times(1)).setAttribute(ContextPolicy.NO_AUTH_POLICY, true);
    verify(filterChain).doFilter(request, response);
    verify(handler1, never())
        .getNormalizedToken(
            any(HttpServletRequest.class),
            any(HttpServletResponse.class),
            any(SecurityFilterChain.class),
            anyBoolean());
  }

  @Test
  public void testDoFilterSessionStorageDisabled() throws Exception {
    PrincipalCollection principalCollectionMock = mock(PrincipalCollection.class);

    PrincipalHolder principalHolderMock = mock(PrincipalHolder.class);
    when(principalHolderMock.getPrincipals()).thenReturn(principalCollectionMock);

    HttpSession sessionMock = mock(HttpSession.class);
    when(sessionMock.getAttribute(SECURITY_TOKEN_KEY)).thenReturn(principalHolderMock);

    HttpServletRequest requestMock = mock(HttpServletRequest.class);
    when(requestMock.getSession(any(Boolean.class))).thenReturn(sessionMock);
    when(requestMock.getRequestURI()).thenReturn(MOCK_CONTEXT);

    HttpServletResponse responseMock = mock(HttpServletResponse.class);

    ContextPolicyManager policyManager = mock(ContextPolicyManager.class);
    when(policyManager.getSessionAccess()).thenReturn(false);
    when(policyManager.isWhiteListed(MOCK_CONTEXT)).thenReturn(false);
    ContextPolicy testPolicy = mock(ContextPolicy.class);
    when(testPolicy.getAuthenticationMethods()).thenReturn(Collections.singletonList("basic"));
    when(policyManager.getContextPolicy(MOCK_CONTEXT)).thenReturn(testPolicy);

    AuthenticationHandler handlerMock = mock(AuthenticationHandler.class);
    when(handlerMock.getAuthenticationType()).thenReturn("basic");
    HandlerResult completedResult = mock(HandlerResult.class);
    when(completedResult.getStatus()).thenReturn(Status.COMPLETED);
    when(completedResult.getToken()).thenReturn(mock(BaseAuthenticationToken.class));
    when(handlerMock.getNormalizedToken(
            any(ServletRequest.class),
            any(ServletResponse.class),
            any(SecurityFilterChain.class),
            anyBoolean()))
        .thenReturn(completedResult);

    SecurityFilterChain filterChain = mock(SecurityFilterChain.class);

    WebSSOFilter filter = new WebSSOFilter();
    filter.setContextPolicyManager(policyManager);
    filter.setHandlerList(Collections.singletonList(handlerMock));

    filter.doFilter(requestMock, responseMock, filterChain);

    verify(sessionMock, times(0)).getAttribute(SECURITY_TOKEN_KEY);
    verify(handlerMock, times(1)).getNormalizedToken(any(), any(), any(), anyBoolean());
    verify(requestMock, times(1)).setAttribute(eq(AUTHENTICATION_TOKEN_KEY), any());
  }

  @Test
  public void testDoFilterGetResultFromSession() throws Exception {
    PrincipalCollection principalCollectionMock = mock(PrincipalCollection.class);
    when(principalCollectionMock.byType(any())).thenReturn(Collections.singletonList("principal"));

    PrincipalHolder principalHolderMock = mock(PrincipalHolder.class);
    when(principalHolderMock.getPrincipals()).thenReturn(principalCollectionMock);

    HttpSession sessionMock = mock(HttpSession.class);
    when(sessionMock.getAttribute(SECURITY_TOKEN_KEY)).thenReturn(principalHolderMock);

    HttpServletRequest requestMock = mock(HttpServletRequest.class);
    when(requestMock.getSession(any(Boolean.class))).thenReturn(sessionMock);
    when(requestMock.getRequestURI()).thenReturn(MOCK_CONTEXT);
    when(requestMock.getRequestedSessionId()).thenReturn("JSESSIONID");

    HttpServletResponse responseMock = mock(HttpServletResponse.class);

    ContextPolicyManager policyManager = mock(ContextPolicyManager.class);
    when(policyManager.getSessionAccess()).thenReturn(true);
    when(policyManager.isWhiteListed(MOCK_CONTEXT)).thenReturn(false);
    ContextPolicy testPolicy = mock(ContextPolicy.class);
    when(testPolicy.getAuthenticationMethods()).thenReturn(Collections.singletonList("basic"));
    when(policyManager.getContextPolicy(MOCK_CONTEXT)).thenReturn(testPolicy);

    AuthenticationHandler handlerMock = mock(AuthenticationHandler.class);
    when(handlerMock.getAuthenticationType()).thenReturn("basic");
    HandlerResult completedResult = mock(HandlerResult.class);
    when(completedResult.getStatus()).thenReturn(Status.COMPLETED);
    when(completedResult.getToken()).thenReturn(mock(BaseAuthenticationToken.class));
    when(handlerMock.getNormalizedToken(
            any(ServletRequest.class),
            any(ServletResponse.class),
            any(SecurityFilterChain.class),
            anyBoolean()))
        .thenReturn(completedResult);

    SecurityFilterChain filterChain = mock(SecurityFilterChain.class);

    WebSSOFilter filter = new WebSSOFilter();
    filter.setContextPolicyManager(policyManager);
    filter.setHandlerList(Collections.singletonList(handlerMock));

    filter.doFilter(requestMock, responseMock, filterChain);

    verify(sessionMock, times(1)).getAttribute(SECURITY_TOKEN_KEY);
    verify(handlerMock, times(0)).getNormalizedToken(any(), any(), any(), anyBoolean());
    verify(requestMock, times(1)).setAttribute(eq(AUTHENTICATION_TOKEN_KEY), any());
  }

  @Test
  public void testDoFilterResolvingOnSecondCall() throws IOException, AuthenticationException {
    ContextPolicy testPolicy = mock(ContextPolicy.class);
    when(testPolicy.getAuthenticationMethods()).thenReturn(Collections.singletonList("basic"));
    ContextPolicyManager policyManager = mock(ContextPolicyManager.class);
    when(policyManager.getContextPolicy(MOCK_CONTEXT)).thenReturn(testPolicy);
    when(policyManager.isWhiteListed(MOCK_CONTEXT)).thenReturn(false);
    when(policyManager.getSessionAccess()).thenReturn(false);
    WebSSOFilter filter = new WebSSOFilter();

    // set handlers
    AuthenticationHandler handler1 = mock(AuthenticationHandler.class);
    when(handler1.getAuthenticationType()).thenReturn("basic");
    HandlerResult noActionResult = mock(HandlerResult.class);
    when(noActionResult.getStatus()).thenReturn(Status.NO_ACTION);
    HandlerResult completedResult = mock(HandlerResult.class);
    when(completedResult.getStatus()).thenReturn(Status.COMPLETED);
    when(completedResult.getToken()).thenReturn(null);
    when(handler1.getNormalizedToken(
            any(ServletRequest.class),
            any(ServletResponse.class),
            any(SecurityFilterChain.class),
            eq(true)))
        .thenReturn(completedResult);
    when(handler1.getNormalizedToken(
            any(ServletRequest.class),
            any(ServletResponse.class),
            any(SecurityFilterChain.class),
            eq(false)))
        .thenReturn(noActionResult);

    filter.setContextPolicyManager(policyManager);
    filter.setHandlerList(Collections.singletonList(handler1));
    filter.setSecurityLogger(mock(SecurityLogger.class));

    SecurityFilterChain filterChain = mock(SecurityFilterChain.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn(MOCK_CONTEXT);
    HttpServletResponse response = mock(HttpServletResponse.class);

    try {
      filter.doFilter(request, response, filterChain);
    } catch (AuthenticationException e) {

    }

    verify(handler1, times(2))
        .getNormalizedToken(
            any(HttpServletRequest.class),
            any(HttpServletResponse.class),
            any(SecurityFilterChain.class),
            anyBoolean());
    // the next filter should NOT be called
    verify(filterChain, never()).doFilter(request, response);
    verify(request, never()).setAttribute(eq(DDF_AUTHENTICATION_TOKEN), any(HandlerResult.class));
  }

  @Test
  public void testDoFilterWithRedirected() throws AuthenticationException, IOException {
    ContextPolicy testPolicy = mock(ContextPolicy.class);
    ContextPolicyManager policyManager = mock(ContextPolicyManager.class);
    when(policyManager.getContextPolicy(MOCK_CONTEXT)).thenReturn(testPolicy);
    when(policyManager.isWhiteListed(MOCK_CONTEXT)).thenReturn(false);
    when(policyManager.getSessionAccess()).thenReturn(false);

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
            any(SecurityFilterChain.class),
            eq(false)))
        .thenReturn(noActionResult);
    when(handler1.getNormalizedToken(
            any(ServletRequest.class),
            any(ServletResponse.class),
            any(SecurityFilterChain.class),
            eq(true)))
        .thenReturn(redirectedResult);

    filter.setContextPolicyManager(policyManager);
    filter.setHandlerList(Collections.singletonList(handler1));

    SecurityFilterChain filterChain = mock(SecurityFilterChain.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn(MOCK_CONTEXT);
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
  public void testDoFilterReturnsStatusCode503WhenNoHandlersRegisteredAndGuestAccessDisabled()
      throws IOException, AuthenticationException {
    ContextPolicyManager policyManager = mock(ContextPolicyManager.class);
    when(policyManager.isWhiteListed(MOCK_CONTEXT)).thenReturn(false);
    when(policyManager.getGuestAccess()).thenReturn(false);
    when(policyManager.getSessionAccess()).thenReturn(true);
    WebSSOFilter filter = new WebSSOFilter();
    filter.setContextPolicyManager(policyManager);

    SecurityFilterChain filterChain = mock(SecurityFilterChain.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn(MOCK_CONTEXT);
    HttpServletResponse response = mock(HttpServletResponse.class);
    filter.doFilter(request, response, filterChain);

    verify(response).setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    verify(response).flushBuffer();
    verify(filterChain, never()).doFilter(request, response);
  }

  @Test
  public void testDoFilterReturnsGuestTokenWhenNoHandlersRegisteredAndGuestAccessEnabled()
      throws IOException, AuthenticationException {
    ContextPolicyManager policyManager = mock(ContextPolicyManager.class);
    when(policyManager.isWhiteListed(MOCK_CONTEXT)).thenReturn(false);
    when(policyManager.getGuestAccess()).thenReturn(true);
    when(policyManager.getSessionAccess()).thenReturn(true);
    WebSSOFilter filter = new WebSSOFilter();
    filter.setContextPolicyManager(policyManager);

    SecurityFilterChain filterChain = mock(SecurityFilterChain.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn(MOCK_CONTEXT);
    HttpServletResponse response = mock(HttpServletResponse.class);
    filter.doFilter(request, response, filterChain);

    ArgumentCaptor<HandlerResult> handlerResult = ArgumentCaptor.forClass(HandlerResult.class);
    verify(request).setAttribute(eq(DDF_AUTHENTICATION_TOKEN), handlerResult.capture());
    assertTrue(handlerResult.getValue().getToken() instanceof GuestAuthenticationToken);
  }
}
