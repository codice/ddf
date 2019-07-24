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
package org.codice.ddf.security.filter.login;

import static ddf.security.SecurityConstants.AUTHENTICATION_TOKEN_KEY;
import static ddf.security.SecurityConstants.SECURITY_TOKEN_KEY;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.common.SecurityTokenHolder;
import ddf.security.http.SessionFactory;
import ddf.security.impl.SubjectImpl;
import ddf.security.service.SecurityManager;
import java.util.Arrays;
import java.util.Collections;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.codice.ddf.platform.filter.FilterChain;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.codice.ddf.security.policy.context.ContextPolicy;
import org.codice.ddf.security.policy.context.ContextPolicyManager;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class LoginFilterTest {
  private static final FilterChain FAIL_FILTER_CHAIN =
      (request, response) ->
          fail("Should not have continued down the filter chain without a valid Subject.");

  private LoginFilter loginFilter;
  private Subject subject;
  private SecurityTokenHolder securityTokenHolder;

  // mocks
  @Mock private HttpServletRequest requestMock;
  @Mock private HttpServletResponse responseMock;
  @Mock private HttpSession sessionMock;
  @Mock private FilterChain filterChainMock;
  @Mock private SecurityManager securityManagerMock;
  @Mock private SecurityAssertion securityAssertionMock;
  @Mock private PrincipalCollection principalCollectionMock;
  @Mock private BaseAuthenticationToken goodAuthenticationTokenMock;
  @Mock private BaseAuthenticationToken badAuthenticationTokenMock;
  @Mock private SecurityToken goodSecurityTokenMock;
  @Mock private SecurityToken badSecurityTokenMock;
  @Mock private BaseAuthenticationToken referenceTokenMock;
  @Mock private SessionFactory sessionFactory;
  @Mock private ContextPolicyManager contextPolicyManager;

  @BeforeClass
  public static void init() {
    OpenSAMLUtil.initSamlEngine();
  }

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);

    SimplePrincipalCollection principalCollection = new SimplePrincipalCollection();
    securityTokenHolder = new SecurityTokenHolder();
    securityTokenHolder.setPrincipals(principalCollection);
    loginFilter = new LoginFilter();
    loginFilter.setSecurityManager(securityManagerMock);
    loginFilter.setSessionFactory(sessionFactory);
    loginFilter.setContextPolicyManager(contextPolicyManager);
    loginFilter.init();

    subject =
        new SubjectImpl(
            principalCollectionMock, true, null, mock(org.apache.shiro.mgt.SecurityManager.class));

    when(securityAssertionMock.getToken()).thenReturn(goodSecurityTokenMock);

    when(principalCollectionMock.byType(SecurityAssertion.class))
        .thenReturn(Collections.singletonList(securityAssertionMock));
    when(principalCollectionMock.asList()).thenReturn(Arrays.asList(goodSecurityTokenMock));

    when(securityManagerMock.getSubject(goodAuthenticationTokenMock)).thenReturn(subject);
    when(securityManagerMock.getSubject(badAuthenticationTokenMock)).thenReturn(null);

    when(sessionMock.getId()).thenReturn("sessionId");

    when(requestMock.getSession(any(boolean.class))).thenReturn(sessionMock);

    when(sessionFactory.getOrCreateSession(any())).thenReturn(sessionMock);

    when(sessionMock.getAttribute(SECURITY_TOKEN_KEY)).thenReturn(securityTokenHolder);

    when(sessionFactory.getOrCreateSession(any())).thenReturn(sessionMock);

    when(sessionMock.getAttribute(SECURITY_TOKEN_KEY)).thenReturn(securityTokenHolder);

    when(contextPolicyManager.getSessionAccess()).thenReturn(true);
  }

  @Test
  public void testNoAuth() throws Exception {

    when(requestMock.getAttribute(ContextPolicy.NO_AUTH_POLICY)).thenReturn("true");

    loginFilter.doFilter(requestMock, responseMock, filterChainMock);

    verify(requestMock, times(0)).getAttribute(AUTHENTICATION_TOKEN_KEY);
  }

  @Test
  public void testNoToken() throws Exception {

    loginFilter.doFilter(requestMock, responseMock, FAIL_FILTER_CHAIN);

    verifyZeroInteractions(securityManagerMock);
  }

  @Test
  public void testBadToken() throws Exception {
    HandlerResult result =
        new HandlerResult(HandlerResult.Status.COMPLETED, badAuthenticationTokenMock);
    when(requestMock.getAttribute(AUTHENTICATION_TOKEN_KEY)).thenReturn(result);

    loginFilter.doFilter(requestMock, responseMock, FAIL_FILTER_CHAIN);

    verify(requestMock, times(0)).setAttribute(any(), any());
  }

  @Test
  public void testGoodToken() throws Exception {
    HandlerResult result =
        new HandlerResult(HandlerResult.Status.COMPLETED, goodAuthenticationTokenMock);
    when(requestMock.getAttribute(AUTHENTICATION_TOKEN_KEY)).thenReturn(result);

    loginFilter.doFilter(requestMock, responseMock, filterChainMock);

    verify(filterChainMock, times(1)).doFilter(any(), any());
  }

  @Test
  public void testValidReference() throws Exception {
    HandlerResult result = new HandlerResult(HandlerResult.Status.COMPLETED, referenceTokenMock);
    when(requestMock.getAttribute(AUTHENTICATION_TOKEN_KEY)).thenReturn(result);

    SecurityTokenHolder securityTokenHolder = new SecurityTokenHolder();
    securityTokenHolder.setPrincipals(principalCollectionMock);
    when(sessionMock.getAttribute(SECURITY_TOKEN_KEY)).thenReturn(securityTokenHolder);

    when(securityManagerMock.getSubject(referenceTokenMock)).thenReturn(subject);

    loginFilter.doFilter(requestMock, responseMock, filterChainMock);

    verify(filterChainMock, times(1)).doFilter(any(), any());
  }

  @Test
  public void testInvalidReference() throws Exception {
    HandlerResult result = new HandlerResult(HandlerResult.Status.COMPLETED, referenceTokenMock);
    when(requestMock.getAttribute(AUTHENTICATION_TOKEN_KEY)).thenReturn(result);

    when(sessionMock.getAttribute(SECURITY_TOKEN_KEY)).thenReturn(badSecurityTokenMock);

    loginFilter.doFilter(requestMock, responseMock, FAIL_FILTER_CHAIN);

    verify(requestMock, times(0)).setAttribute(any(), any());
  }
}
