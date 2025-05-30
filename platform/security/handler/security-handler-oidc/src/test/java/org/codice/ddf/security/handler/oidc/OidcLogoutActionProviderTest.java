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

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import ddf.action.Action;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.assertion.jwt.impl.SecurityAssertionJwt;
import ddf.security.common.PrincipalHolder;
import ddf.security.service.impl.SubjectUtils;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.codice.ddf.security.handler.api.OidcHandlerConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.pac4j.core.exception.http.FoundAction;
import org.pac4j.oidc.credentials.OidcCredentials;
import org.pac4j.oidc.logout.OidcLogoutActionBuilder;
import org.pac4j.oidc.profile.OidcProfile;

public class OidcLogoutActionProviderTest {

  private static final String PREVIOUS_URL = "https://localhost:8993/admin";
  private static final String PREVIOUS_URL_ENCODED = "https%3A%2F%2Flocalhost%3A8993%2Fadmin";

  private OidcLogoutActionBuilder oidcLogoutActionBuilder;
  private OidcLogoutActionProvider oidcLogoutActionProvider;
  private HttpServletRequest request;
  private HttpServletResponse response;
  private Subject subject;

  @Before
  public void setup() {
    oidcLogoutActionBuilder = mock(OidcLogoutActionBuilder.class);
    OidcHandlerConfiguration handlerConfiguration = mock(OidcHandlerConfiguration.class);
    when(handlerConfiguration.getOidcLogoutActionBuilder()).thenReturn(oidcLogoutActionBuilder);

    oidcLogoutActionProvider = new OidcLogoutActionProvider(handlerConfiguration);
    oidcLogoutActionProvider.setSubjectOperations(new SubjectUtils());

    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);
    subject = mock(Subject.class);
    HttpSession session = mock(HttpSession.class);
    PrincipalHolder principalHolderMock = mock(PrincipalHolder.class);
    SimplePrincipalCollection principalCollection = new SimplePrincipalCollection();
    SecurityAssertion securityAssertion = mock(SecurityAssertion.class);
    OidcProfile profile = mock(OidcProfile.class);

    when(securityAssertion.getToken()).thenReturn(profile);
    when(securityAssertion.getTokenType()).thenReturn(SecurityAssertionJwt.JWT_TOKEN_TYPE);
    when(subject.getPrincipals()).thenReturn(principalCollection);
    when(principalHolderMock.getPrincipals()).thenReturn(principalCollection);
    principalCollection.add(securityAssertion, "oidc");
    when(session.getAttribute(SecurityConstants.SECURITY_TOKEN_KEY))
        .thenReturn(principalHolderMock);
    when(request.getSession(false)).thenReturn(session);
  }

  @Test
  public void testGetAction() {
    String actionUrl = "https://localhost:8993/services/oidc/logout";

    FoundAction foundAction = mock(FoundAction.class);
    when(foundAction.getLocation()).thenReturn(actionUrl);
    when(oidcLogoutActionBuilder.getLogoutAction(any(), any(), any()))
        .thenReturn(Optional.of(foundAction));

    Action action =
        oidcLogoutActionProvider.getAction(
            ImmutableMap.of(
                SecurityConstants.SECURITY_SUBJECT,
                subject,
                "http_request",
                request,
                "http_response",
                response));

    assertEquals(actionUrl, action.getUrl().toString());
  }

  @Test
  public void testGetActionUnencodedReferer() {
    when(request.getHeader("Referer")).thenReturn("http://foo.bar?prevurl=" + PREVIOUS_URL);

    oidcLogoutActionProvider.getAction(
        ImmutableMap.of(
            SecurityConstants.SECURITY_SUBJECT,
            subject,
            "http_request",
            request,
            "http_response",
            response));

    ArgumentCaptor<String> callbackUri = ArgumentCaptor.forClass(String.class);
    verify(oidcLogoutActionBuilder).getLogoutAction(any(), any(), callbackUri.capture());
    assertThat(callbackUri.getValue(), containsString("prevurl=" + PREVIOUS_URL_ENCODED));
  }

  @Test
  public void testGetActionEncodedReferer() {
    when(request.getHeader("Referer")).thenReturn("http://foo.bar?prevurl=" + PREVIOUS_URL_ENCODED);

    oidcLogoutActionProvider.getAction(
        ImmutableMap.of(
            SecurityConstants.SECURITY_SUBJECT,
            subject,
            "http_request",
            request,
            "http_response",
            response));

    ArgumentCaptor<String> callbackUri = ArgumentCaptor.forClass(String.class);
    verify(oidcLogoutActionBuilder).getLogoutAction(any(), any(), callbackUri.capture());
    assertThat(callbackUri.getValue(), containsString("prevurl=" + PREVIOUS_URL_ENCODED));
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
