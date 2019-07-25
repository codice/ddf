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

import static com.github.scribejava.core.model.OAuthConstants.ACCESS_TOKEN;
import static junit.framework.TestCase.assertNull;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import com.google.common.io.CharStreams;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import java.io.InputStreamReader;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.pac4j.core.context.WebContext;
import org.pac4j.oidc.credentials.OidcCredentials;

@RunWith(MockitoJUnitRunner.class)
public class CustomOAuthCredentialsExtractorTest {
  private static final String CODE = "code";
  private static final String AUTHORIZATION = "Authorization";

  private static String authorizationCode;
  private static AccessToken accessToken;
  private static String authorizationHeader;

  private CustomOAuthCredentialsExtractor extractor;
  private OidcCredentials credentials;

  @Mock private WebContext mockWebContext;

  @BeforeClass
  public static void setupClass() throws Exception {
    authorizationCode =
        CharStreams.toString(
            new InputStreamReader(
                CustomOAuthCredentialsExtractorTest.class
                    .getClassLoader()
                    .getResourceAsStream("authorizationCode.txt")));
    String accessTokenString =
        CharStreams.toString(
            new InputStreamReader(
                CustomOAuthCredentialsExtractorTest.class
                    .getClassLoader()
                    .getResourceAsStream("accessToken.jwt")));

    accessToken = new BearerAccessToken(accessTokenString);
    authorizationHeader = "Bearer " + accessToken;
  }

  @Before
  public void setup() {
    extractor = new CustomOAuthCredentialsExtractor();
  }

  @Test(expected = NullPointerException.class)
  public void extractNullWebContext() {
    extractor.getOauthCredentialsAsOidcCredentials(null);
  }

  @Test
  public void extractNoCredentialsOnWebContext() {
    when(mockWebContext.getRequestParameter(CODE)).thenReturn(null);
    when(mockWebContext.getRequestParameter(ACCESS_TOKEN)).thenReturn(null);
    when(mockWebContext.getRequestHeader(AUTHORIZATION)).thenReturn(null);

    credentials = extractor.getOauthCredentialsAsOidcCredentials(mockWebContext);

    assertNull(credentials.getCode());
    assertNull(credentials.getAccessToken());
  }

  @Test
  public void extractCodeParameterOnWebContext() {
    when(mockWebContext.getRequestParameter(CODE)).thenReturn(authorizationCode);
    when(mockWebContext.getRequestParameter(ACCESS_TOKEN)).thenReturn(null);
    when(mockWebContext.getRequestHeader(AUTHORIZATION)).thenReturn(null);

    credentials = extractor.getOauthCredentialsAsOidcCredentials(mockWebContext);

    assertThat(credentials.getCode().getValue(), is(authorizationCode));
    assertNull(credentials.getAccessToken());
  }

  @Test
  public void extractAccessTokenParameterOnWebContext() {
    when(mockWebContext.getRequestParameter(CODE)).thenReturn(null);
    when(mockWebContext.getRequestParameter(ACCESS_TOKEN)).thenReturn(accessToken.toString());
    when(mockWebContext.getRequestHeader(AUTHORIZATION)).thenReturn(null);

    credentials = extractor.getOauthCredentialsAsOidcCredentials(mockWebContext);

    assertNull(credentials.getCode());
    assertThat(credentials.getAccessToken().getValue(), is(accessToken.toString()));
  }

  @Test
  public void extractAccessTokenHeaderOnWebContext() {
    when(mockWebContext.getRequestParameter(CODE)).thenReturn(null);
    when(mockWebContext.getRequestParameter(ACCESS_TOKEN)).thenReturn(null);
    when(mockWebContext.getRequestHeader(AUTHORIZATION)).thenReturn(authorizationHeader);

    credentials = extractor.getOauthCredentialsAsOidcCredentials(mockWebContext);

    assertNull(credentials.getCode());
    assertThat(credentials.getAccessToken().getValue(), is(accessToken.toString()));
  }

  @Test
  public void extractEverythingOnWebContext() {
    when(mockWebContext.getRequestParameter(CODE)).thenReturn(authorizationCode);
    when(mockWebContext.getRequestParameter(ACCESS_TOKEN)).thenReturn(accessToken.toString());
    when(mockWebContext.getRequestHeader(AUTHORIZATION)).thenReturn(authorizationHeader);

    credentials = extractor.getOauthCredentialsAsOidcCredentials(mockWebContext);

    assertThat(credentials.getCode().getValue(), is(authorizationCode));
    assertThat(credentials.getAccessToken().getValue(), is(accessToken.toString()));
  }

  @Test
  public void extractBadHeaderOnWebContext() {
    when(mockWebContext.getRequestParameter(CODE)).thenReturn(null);
    when(mockWebContext.getRequestParameter(ACCESS_TOKEN)).thenReturn(null);
    when(mockWebContext.getRequestHeader(AUTHORIZATION)).thenReturn("badHeader");

    credentials = extractor.getOauthCredentialsAsOidcCredentials(mockWebContext);

    assertNull(credentials.getCode());
    assertNull(credentials.getAccessToken());
  }
}
