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

import static junit.framework.TestCase.assertNull;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.io.CharStreams;
import com.nimbusds.oauth2.sdk.id.State;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.codice.ddf.security.handler.api.HandlerResult.Status;
import org.codice.ddf.security.handler.api.OidcHandlerConfiguration;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.redirect.RedirectAction;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.credentials.OidcCredentials;

@RunWith(MockitoJUnitRunner.Silent.class)
public class OidcHandlerTest {
  private static String accessTokenString;
  private static String authorizationCodeString;
  private static String idTokenString;
  private static String stateString;
  private static State state;

  private OidcHandler handler;
  private HandlerResult result;

  @Mock private OidcHandlerConfiguration mockConfiguration;
  @Mock private OidcConfiguration mockOidcConfiguration;
  private OidcClient mockOidcClient;
  @Mock private HttpServletRequest mockRequest;
  @Mock private HttpServletResponse mockResponse;
  @Mock private HttpSession mockSession;

  private Map<String, String[]> parameterMap = new HashMap<>();

  @BeforeClass
  public static void setupClass() throws Exception {
    accessTokenString =
        CharStreams.toString(
            new InputStreamReader(
                OidcHandlerTest.class.getClassLoader().getResourceAsStream("accessToken.jwt"),
                StandardCharsets.UTF_8));
    authorizationCodeString =
        CharStreams.toString(
            new InputStreamReader(
                OidcHandlerTest.class
                    .getClassLoader()
                    .getResourceAsStream("authorizationCode.txt")));
    idTokenString =
        CharStreams.toString(
            new InputStreamReader(
                OidcHandlerTest.class.getClassLoader().getResourceAsStream("idToken.jwt"),
                StandardCharsets.UTF_8));
    stateString =
        CharStreams.toString(
            new InputStreamReader(
                OidcHandlerTest.class.getClassLoader().getResourceAsStream("state.txt"),
                StandardCharsets.UTF_8));
    state = new State(stateString);
  }

  @Before
  public void setup() throws Exception {
    // oidc client
    mockOidcClient = new MockOidcClient();

    // oidc configuration
    when(mockConfiguration.getOidcConfiguration()).thenReturn(mockOidcConfiguration);
    when(mockConfiguration.getOidcClient(anyString())).thenReturn(mockOidcClient);

    // session
    when(mockSession.getAttribute("oidcStateAttribute")).thenReturn(state);

    // request
    when(mockRequest.getMethod()).thenReturn("POST");
    when(mockRequest.getRequestURL()).thenReturn(new StringBuffer("https://request/url"));
    when(mockRequest.getRemoteAddr()).thenReturn("127.0.0.1");
    when(mockRequest.getHeader("User-Agent")).thenReturn("Mozilla");
    when(mockRequest.getSession()).thenReturn(mockSession);
    when(mockRequest.getSession(any(Boolean.class))).thenReturn(mockSession);
    parameterMap.put("state", new String[] {stateString});
    when(mockRequest.getParameterMap()).thenReturn(parameterMap);

    handler = new OidcHandler(mockConfiguration);
  }

  @Test
  public void constructWithNullConfiguration() {
    handler = new OidcHandler(null);

    assertNull(handler.getConfiguration());
  }

  @Test
  public void constructWithEmptyConfiguration() {
    OidcHandlerConfigurationImpl emptyConfiguration = new OidcHandlerConfigurationImpl();
    handler = new OidcHandler(emptyConfiguration);

    assertThat(handler.getConfiguration(), is(emptyConfiguration));
  }

  @Test
  public void getNormalizedTokenHeadRequest() throws Exception {
    when(mockRequest.getMethod()).thenReturn("HEAD");

    result = handler.getNormalizedToken(mockRequest, mockResponse, null, false);

    verify(mockResponse, times(1)).setStatus(HttpServletResponse.SC_OK);
    verify(mockResponse, times(1)).flushBuffer();
    assertThat(result.getStatus(), is(Status.NO_ACTION));
  }

  @Test
  public void getNormalizedTokenNonUserAgentRequest() throws Exception {
    when(mockRequest.getHeader("User-Agent")).thenReturn("");

    handler = new OidcHandler(mockConfiguration);
    result = handler.getNormalizedToken(mockRequest, mockResponse, null, false);

    assertThat(result.getStatus(), is(Status.NO_ACTION));
  }

  @Test
  public void getNormalizedTokenNoCredentialsOnRequest() throws Exception {
    result = handler.getNormalizedToken(mockRequest, mockResponse, null, false);

    assertThat(result.getStatus(), is(Status.REDIRECTED));
  }

  @Test
  public void getNormalizedTokenWithAuthorizationCodeInQueryParameters() throws Exception {
    parameterMap.put("code", new String[] {authorizationCodeString});
    parameterMap.put("client_name", new String[] {"ddf-client"});

    when(mockRequest.getParameterMap()).thenReturn(parameterMap);

    result = handler.getNormalizedToken(mockRequest, mockResponse, null, false);

    assertThat(result.getStatus(), is(Status.COMPLETED));
    assertThat(result.getToken().getCredentials(), instanceOf(OidcCredentials.class));
    assertThat(
        ((OidcCredentials) result.getToken().getCredentials()).getCode().toString(),
        is(authorizationCodeString));
  }

  @Test
  public void getNormalizedTokenWithAccessTokenInQueryParameters() throws Exception {
    parameterMap.put("access_token", new String[] {accessTokenString});
    parameterMap.put("token_type", new String[] {"Bearer"});
    when(mockRequest.getParameterMap()).thenReturn(parameterMap);

    result = handler.getNormalizedToken(mockRequest, mockResponse, null, false);

    assertThat(result.getStatus(), is(Status.COMPLETED));
    assertThat(result.getToken().getCredentials(), instanceOf(OidcCredentials.class));
    assertThat(
        ((OidcCredentials) result.getToken().getCredentials()).getAccessToken().toString(),
        is(accessTokenString));
  }

  @Test
  public void getNormalizedTokenWithIdTokenInQueryParameters() throws Exception {
    parameterMap.put("id_token", new String[] {idTokenString});
    when(mockRequest.getParameterMap()).thenReturn(parameterMap);

    result = handler.getNormalizedToken(mockRequest, mockResponse, null, false);

    assertThat(result.getStatus(), is(Status.COMPLETED));
    assertThat(result.getToken().getCredentials(), instanceOf(OidcCredentials.class));
    assertThat(
        ((OidcCredentials) result.getToken().getCredentials()).getIdToken().getParsedString(),
        is(idTokenString));
  }

  // have to do a manual mock here in order to stub methods from the parent class
  private class MockOidcClient extends OidcClient {
    @Override
    public RedirectAction getRedirectAction(final WebContext context) {
      RedirectAction mockRedirectAction = mock(RedirectAction.class);
      when(mockRedirectAction.perform(any(WebContext.class))).thenReturn(null);

      return mockRedirectAction;
    }

    @Override
    public String computeFinalCallbackUrl(final WebContext context) {
      return "https://final.callback.url";
    }

    @Override
    public OidcConfiguration getConfiguration() {
      return mockOidcConfiguration;
    }
  }
}
