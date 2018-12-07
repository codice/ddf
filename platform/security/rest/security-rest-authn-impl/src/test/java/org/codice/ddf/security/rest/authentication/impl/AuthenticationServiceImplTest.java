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
package org.codice.ddf.security.rest.authentication.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.common.SecurityTokenHolder;
import ddf.security.http.impl.HttpSessionFactory;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.shiro.subject.PrincipalCollection;
import org.codice.ddf.security.handler.api.UPAuthenticationToken;
import org.codice.ddf.security.policy.context.ContextPolicy;
import org.codice.ddf.security.policy.context.ContextPolicyManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

public class AuthenticationServiceImplTest {
  private static final String URL = "https://localhost/path";

  private static final String PATH = "/path";

  private static final String REALM = "test";

  private static final String USER_NAME = "test";

  private static final String PASSWORD = "test";

  private ContextPolicyManager policyManager;

  private SecurityManager securityManager;

  private AuthenticationServiceImpl authenticationService;

  @Before
  public void setup() throws SecurityServiceException, URISyntaxException {
    HttpSessionFactory sessionFactory = mock(HttpSessionFactory.class);
    HttpSession session = mock(HttpSession.class);
    when(session.getAttribute(SecurityConstants.SAML_ASSERTION))
        .thenReturn(mock(SecurityTokenHolder.class));
    when(sessionFactory.getOrCreateSession(any())).thenReturn(session);

    policyManager = mock(ContextPolicyManager.class);
    securityManager = mock(SecurityManager.class);

    authenticationService =
        new AuthenticationServiceImpl(policyManager, securityManager, sessionFactory);

    UriInfo uriInfo = mock(UriInfo.class);
    UriBuilder uriBuilder = mock(UriBuilder.class);
    when(uriInfo.getBaseUriBuilder()).thenReturn(uriBuilder);
    when(uriBuilder.replacePath(anyString())).thenReturn(uriBuilder);
    when(uriBuilder.build()).thenReturn(new URI(URL));

    mockUser(USER_NAME, PASSWORD, REALM);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNoSSLFailure() throws SecurityServiceException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    authenticationService.login(request, USER_NAME, PASSWORD, PATH);
  }

  @Test(expected = SecurityServiceException.class)
  public void testUnauthorized() throws SecurityServiceException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.isSecure()).thenReturn(true);

    ContextPolicy policy = mock(ContextPolicy.class);
    when(policy.getRealm()).thenReturn(REALM);
    when(policyManager.getContextPolicy(PATH)).thenReturn(policy);

    authenticationService.login(request, "bad", "bad", PATH);
  }

  @Test
  public void testDefaultRealm() throws SecurityServiceException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.isSecure()).thenReturn(true);

    mockUser(USER_NAME, PASSWORD, "karaf");
    authenticationService.login(request, USER_NAME, PASSWORD, PATH);
  }

  @Test
  public void testSingleRealm() throws SecurityServiceException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.isSecure()).thenReturn(true);

    ContextPolicy policy = mock(ContextPolicy.class);
    when(policy.getRealm()).thenReturn(REALM);
    when(policyManager.getContextPolicy(PATH)).thenReturn(policy);

    authenticationService.login(request, USER_NAME, PASSWORD, PATH);
  }

  @Test
  public void testMultiRealm() throws SecurityServiceException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.isSecure()).thenReturn(true);

    ContextPolicy policy = mock(ContextPolicy.class);
    ContextPolicy anotherPolicy = mock(ContextPolicy.class);

    when(policy.getRealm()).thenReturn(REALM);
    when(anotherPolicy.getRealm()).thenReturn("ANOTHER_REALM");
    when(policyManager.getContextPolicy(PATH)).thenReturn(policy);
    when(policyManager.getContextPolicy("/anotherPath")).thenReturn(anotherPolicy);

    mockUser("another", "another", "ANOTHER_REALM");

    authenticationService.login(request, USER_NAME, PASSWORD, PATH);
    authenticationService.login(request, "another", "another", "/anotherPath");
  }

  private void mockUser(String username, String password, String realm)
      throws SecurityServiceException {
    Subject subject = mock(Subject.class);
    SecurityAssertion securityAssertion = mock(SecurityAssertion.class);
    SecurityToken securityToken = mock(SecurityToken.class);
    when(securityAssertion.getSecurityToken()).thenReturn(securityToken);

    PrincipalCollection collection = mock(PrincipalCollection.class);
    Iterator iter = mock(Iterator.class);
    when(iter.hasNext()).thenReturn(true, false);
    when(iter.next()).thenReturn(securityAssertion);
    when(collection.iterator()).thenReturn(iter);

    when(subject.getPrincipals()).thenReturn(collection);

    UPAuthenticationToken token = new UPAuthenticationToken(username, password, realm);
    when(securityManager.getSubject(argThat(new UsernamePasswordTokenMatcher(token))))
        .thenReturn(subject);
  }

  private class UsernamePasswordTokenMatcher implements ArgumentMatcher<UPAuthenticationToken> {

    private UPAuthenticationToken left;

    UsernamePasswordTokenMatcher(UPAuthenticationToken token) {
      this.left = token;
    }

    @Override
    public boolean matches(UPAuthenticationToken token) {
      if (left == null) {
        return token == null;
      }

      if (token == null) {
        return false;
      }

      return left.getUsername().equals(token.getUsername())
          && left.getPassword().equals(token.getPassword())
          && left.getRealm().equals(token.getRealm());
    }
  }
}
