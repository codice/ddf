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
package org.codice.ddf.security.handler.oauth;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.io.CharStreams;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.codice.ddf.security.handler.api.HandlerResult.Status;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.pac4j.oidc.credentials.OidcCredentials;

@RunWith(MockitoJUnitRunner.class)
public class OAuthHandlerTest {
  private static String accessTokenString;
  private static String authorizationCodeString;

  private OAuthHandler handler;
  private HandlerResult result;

  @Mock private HttpServletRequest mockRequest;
  @Mock private HttpServletResponse mockResponse;
  @Mock private Enumeration<String> mockHeaderNames;

  @BeforeClass
  public static void setupClass() throws Exception {
    accessTokenString =
        CharStreams.toString(
            new InputStreamReader(
                OAuthHandlerTest.class.getClassLoader().getResourceAsStream("accessToken.jwt"),
                StandardCharsets.UTF_8));
    authorizationCodeString =
        CharStreams.toString(
            new InputStreamReader(
                OAuthHandlerTest.class
                    .getClassLoader()
                    .getResourceAsStream("authorizationCode.txt")));
  }

  @Before
  public void setup() throws Exception {
    // request
    when(mockRequest.getMethod()).thenReturn("POST");
    when(mockRequest.getRequestURL()).thenReturn(new StringBuffer("https://request/url"));
    when(mockRequest.getRemoteAddr()).thenReturn("127.0.0.1");

    handler = new OAuthHandler();
  }

  @Test
  public void constructWithNoParameters() {
    assertNotNull(handler);
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
  public void getNormalizedTokenUserAgentRequest() throws Exception {
    when(mockRequest.getHeader("User-Agent")).thenReturn("Mozilla");
    result = handler.getNormalizedToken(mockRequest, mockResponse, null, false);

    assertThat(result.getStatus(), is(Status.NO_ACTION));
  }

  @Test
  public void getNormalizedTokenNoCredentialsOnRequest() throws Exception {
    result = handler.getNormalizedToken(mockRequest, mockResponse, null, false);

    assertThat(result.getStatus(), is(Status.NO_ACTION));
  }

  @Test
  public void getNormalizedTokenWithAuthorizationCode() throws Exception {
    when(mockRequest.getParameter(eq("code"))).thenReturn(authorizationCodeString);

    result = handler.getNormalizedToken(mockRequest, mockResponse, null, false);

    assertThat(result.getStatus(), is(Status.COMPLETED));
    assertThat(result.getToken().getCredentials(), instanceOf(OidcCredentials.class));
    assertThat(
        ((OidcCredentials) result.getToken().getCredentials()).getCode().toString(),
        is(authorizationCodeString));
  }

  @Test
  public void getNormalizedTokenWithAccessTokenInHeader() throws Exception {
    when(mockHeaderNames.hasMoreElements()).thenReturn(true, false);
    when(mockHeaderNames.nextElement()).thenReturn("Authorization", null);

    when(mockRequest.getHeaderNames()).thenReturn(mockHeaderNames);
    when(mockRequest.getHeader("Authorization")).thenReturn("Bearer " + accessTokenString);

    result = handler.getNormalizedToken(mockRequest, mockResponse, null, false);

    assertThat(result.getStatus(), is(Status.COMPLETED));
    assertThat(result.getToken().getCredentials(), instanceOf(OidcCredentials.class));
    assertThat(
        ((OidcCredentials) result.getToken().getCredentials()).getAccessToken().toString(),
        is(accessTokenString));
  }

  @Test
  public void getNormalizedTokenWithAccessTokenInQueryParameters() throws Exception {
    when(mockRequest.getParameter(eq("access_token"))).thenReturn(accessTokenString);

    result = handler.getNormalizedToken(mockRequest, mockResponse, null, false);

    assertThat(result.getStatus(), is(Status.COMPLETED));
    assertThat(result.getToken().getCredentials(), instanceOf(OidcCredentials.class));
    assertThat(
        ((OidcCredentials) result.getToken().getCredentials()).getAccessToken().toString(),
        is(accessTokenString));
  }
}
