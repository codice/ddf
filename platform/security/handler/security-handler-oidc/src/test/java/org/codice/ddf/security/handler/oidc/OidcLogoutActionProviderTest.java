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
package org.codice.ddf.security.handler.oidc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import ddf.action.Action;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.assertion.jwt.impl.SecurityAssertionJwt;
import ddf.security.common.SecurityTokenHolder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.codice.ddf.security.handler.api.OidcHandlerConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.pac4j.core.redirect.RedirectAction;
import org.pac4j.oidc.credentials.OidcCredentials;
import org.pac4j.oidc.logout.OidcLogoutActionBuilder;
import org.pac4j.oidc.profile.OidcProfile;

public class OidcLogoutActionProviderTest {

  private static final String LOCATION = "https://localhost:8993/services/oidc/logout";

  private OidcLogoutActionProvider oidcLogoutActionProvider;

  @Before
  public void setup() {
    OidcLogoutActionBuilder oidcLogoutActionBuilder = mock(OidcLogoutActionBuilder.class);
    RedirectAction redirectAction = mock(RedirectAction.class);
    when(redirectAction.getLocation()).thenReturn(LOCATION);
    when(oidcLogoutActionBuilder.getLogoutAction(any(), any(), any())).thenReturn(redirectAction);

    OidcHandlerConfiguration handlerConfiguration = mock(OidcHandlerConfiguration.class);
    when(handlerConfiguration.getOidcLogoutActionBuilder()).thenReturn(oidcLogoutActionBuilder);

    oidcLogoutActionProvider = new OidcLogoutActionProvider(handlerConfiguration);
  }

  @Test
  public void testGetAction() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    HttpSession session = mock(HttpSession.class);
    Subject subject = mock(Subject.class);
    SecurityTokenHolder tokenHolder = mock(SecurityTokenHolder.class);
    SimplePrincipalCollection principalCollection = new SimplePrincipalCollection();
    SecurityAssertion securityAssertion = mock(SecurityAssertion.class);
    OidcProfile profile = mock(OidcProfile.class);

    when(securityAssertion.getToken()).thenReturn(profile);
    when(securityAssertion.getTokenType()).thenReturn(SecurityAssertionJwt.JWT_TOKEN_TYPE);
    when(subject.getPrincipals()).thenReturn(principalCollection);
    when(tokenHolder.getPrincipals()).thenReturn(principalCollection);
    principalCollection.add(securityAssertion, "oidc");
    when(session.getAttribute(SecurityConstants.SECURITY_TOKEN_KEY)).thenReturn(tokenHolder);
    when(request.getSession(false)).thenReturn(session);

    Action action =
        oidcLogoutActionProvider.getAction(
            ImmutableMap.of(
                SecurityConstants.SECURITY_SUBJECT,
                subject,
                "http_request",
                request,
                "http_response",
                response));
    assertEquals(LOCATION, action.getUrl().toString());
  }

  @Test
  public void testGetActionFailure() {
    Object notSubjectMap = new Object();
    Action action = oidcLogoutActionProvider.getAction(notSubjectMap);
    assertNull(action);

    action =
        oidcLogoutActionProvider.getAction(
            ImmutableMap.of(SecurityConstants.SECURITY_SUBJECT, notSubjectMap));
    assertNull(action);
  }

  @Test
  public void testGetActionFailureWrongKey() {
    OidcCredentials credentials = mock(OidcCredentials.class);
    Action action = oidcLogoutActionProvider.getAction(ImmutableMap.of("wrong key", credentials));
    assertNull(action);
  }

  @Test
  public void testGetActionFailsWithoutRequestAndResponse() {
    OidcCredentials credentials = mock(OidcCredentials.class);
    Action action =
        oidcLogoutActionProvider.getAction(
            ImmutableMap.of(SecurityConstants.SECURITY_SUBJECT, credentials));
    assertNull(action);
  }
}
