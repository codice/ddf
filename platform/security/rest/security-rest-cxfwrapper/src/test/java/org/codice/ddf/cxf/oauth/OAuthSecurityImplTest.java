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
package org.codice.ddf.cxf.oauth;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.codice.ddf.cxf.oauth.OAuthSecurity.OAUTH;
import static org.codice.ddf.cxf.oauth.OAuthSecurityImpl.GSON;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pac4j.core.context.HttpConstants.APPLICATION_JSON;
import static org.pac4j.oidc.profile.OidcProfileDefinition.AZP;
import static org.pac4j.oidc.profile.OidcProfileDefinition.EMAIL_VERIFIED;
import static org.pac4j.oidc.profile.OidcProfileDefinition.PREFERRED_USERNAME;

import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.common.collect.ImmutableMap;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Resource;
import com.nimbusds.jose.util.ResourceRetriever;
import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Principal;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.shiro.subject.PrincipalCollection;
import org.codice.ddf.security.token.storage.api.TokenInformation;
import org.codice.ddf.security.token.storage.api.TokenInformationImpl;
import org.codice.ddf.security.token.storage.api.TokenStorage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class OAuthSecurityImplTest {

  private static final String METADATA_ENDPOINT = "http://localhost:8080/auth/master/metadata";
  private static final String JWK_ENDPOINT =
      "http://localhost:8080/auth/realms/master/protocol/openid-connect/certs";

  private static final String SOURCE_ID = "CSW";
  private static final String SECRET = "secret";
  private static final String USERNAME = "username";
  private static final String DDF_CLIENT = "ddf-client";
  private static final String ENCODED_CRED = "Basic ZGRmLWNsaWVudDpzZWNyZXQ=";

  private OAuthSecurityWithMockWebclient oauthSecurity;
  private TokenStorage tokenStorage;
  private Algorithm validAlgorithm;
  private Algorithm invalidAlgorithm;

  @Before
  public void setUp() throws Exception {
    // Generate the RSA key pair to sign tokens
    KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
    gen.initialize(2048);
    KeyPair keyPair = gen.generateKeyPair();
    RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
    RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

    JWK sigJwk =
        new RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyUse(KeyUse.SIGNATURE)
            .keyID(UUID.randomUUID().toString())
            .build();

    String jwk = "{\"keys\": [" + sigJwk.toPublicJWK().toJSONString() + "] }";
    validAlgorithm = Algorithm.RSA256(publicKey, privateKey);
    invalidAlgorithm = Algorithm.HMAC256("WRONG");

    ResourceRetriever resourceRetriever = mock(ResourceRetriever.class);
    Resource jwkResource = new Resource(jwk, APPLICATION_JSON);
    when(resourceRetriever.retrieveResource(eq(new URL(JWK_ENDPOINT)))).thenReturn(jwkResource);

    String content =
        IOUtils.toString(
            Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream("metadata.json")),
            StandardCharsets.UTF_8);
    Resource metadataResource = new Resource(content, APPLICATION_JSON);
    when(resourceRetriever.retrieveResource(eq(new URL(METADATA_ENDPOINT))))
        .thenReturn(metadataResource);

    tokenStorage = mock(TokenStorage.class);
    oauthSecurity = new OAuthSecurityWithMockWebclient(tokenStorage);
    oauthSecurity.setResourceRetriever(resourceRetriever);
  }

  @Test
  public void testSettingUserTokenOnClient() throws Exception {
    Client client = mock(Client.class);
    URI uri = new URI("https://localhost:8993/search/catalog");
    when(client.getCurrentURI()).thenReturn(uri);
    Subject subject = getSubject();

    String accessToken = getAccessTokenBuilder().sign(validAlgorithm);
    TokenInformation.TokenEntry tokenEntry =
        new TokenInformationImpl.TokenEntryImpl(accessToken, "refresh_token", METADATA_ENDPOINT);
    when(tokenStorage.read(USERNAME, SOURCE_ID)).thenReturn(tokenEntry);

    oauthSecurity.setUserTokenOnClient(
        client, subject, SOURCE_ID, DDF_CLIENT, SECRET, METADATA_ENDPOINT);
    verify(client, times(1)).header(OAUTH, "Bearer " + accessToken);
  }

  @Test
  public void testSettingSystemTokensOnClient() throws Exception {
    Client client = mock(Client.class);
    URI uri = new URI("https://localhost:8993/search/catalog");
    when(client.getCurrentURI()).thenReturn(uri);
    Subject subject = getSubject();

    String accessToken = getAccessTokenBuilder().sign(validAlgorithm);
    TokenInformation.TokenEntry tokenEntry =
        new TokenInformationImpl.TokenEntryImpl(accessToken, "refresh_token", METADATA_ENDPOINT);
    String id = Base64.getEncoder().encodeToString((DDF_CLIENT + ":" + SECRET).getBytes(UTF_8));
    when(tokenStorage.read(id, "client_credentials")).thenReturn(tokenEntry);

    oauthSecurity.setSystemTokenOnClient(client, DDF_CLIENT, SECRET, METADATA_ENDPOINT);
    verify(client, times(1)).header(OAUTH, "Bearer " + accessToken);
  }

  @Test
  public void testSettingSystemTokensOnClientNoSavedTokens() {
    Client client = mock(Client.class);
    Response response = mock(Response.class);

    when(tokenStorage.read(anyString(), anyString())).thenReturn(null);
    String accessToken = getAccessTokenBuilder().sign(validAlgorithm);
    when(response.getStatus()).thenReturn(200);
    when(response.getEntity())
        .thenReturn(new ByteArrayInputStream(getResponse(accessToken).getBytes()));

    WebClient webClient = oauthSecurity.webClient;
    when(webClient.form(any(Form.class))).thenReturn(response);

    ArgumentCaptor<Form> captor = ArgumentCaptor.forClass(Form.class);
    oauthSecurity.setSystemTokenOnClient(client, DDF_CLIENT, SECRET, METADATA_ENDPOINT);

    verify(client, times(1)).header(OAUTH, "Bearer " + accessToken);
    verify(webClient, times(1)).header(AUTHORIZATION, ENCODED_CRED);
    verify(webClient, times(1)).accept(MediaType.APPLICATION_JSON);
    verify(webClient, times(1)).form(captor.capture());
    Form form = captor.getValue();
    assertTrue(form.asMap().get("grant_type").contains("client_credentials"));
  }

  @Test
  public void testSettingSystemTokensOnClientInvalidAccessToken() {
    Client client = mock(Client.class);
    Response response = mock(Response.class);
    when(tokenStorage.read(anyString(), anyString())).thenReturn(null);
    String accessToken = getAccessTokenBuilder().sign(invalidAlgorithm);
    when(response.getStatus()).thenReturn(200);
    when(response.getEntity())
        .thenReturn(new ByteArrayInputStream(getResponse(accessToken).getBytes()));

    WebClient webClient = oauthSecurity.webClient;
    when(webClient.form(any(Form.class))).thenReturn(response);

    ArgumentCaptor<Form> captor = ArgumentCaptor.forClass(Form.class);
    oauthSecurity.setSystemTokenOnClient(client, DDF_CLIENT, SECRET, METADATA_ENDPOINT);

    verify(client, times(0)).header(any(), any());

    verify(webClient, times(1)).header(AUTHORIZATION, ENCODED_CRED);
    verify(webClient, times(1)).accept(MediaType.APPLICATION_JSON);
    verify(webClient, times(1)).form(captor.capture());
    Form form = captor.getValue();
    assertTrue(form.asMap().get("grant_type").contains("client_credentials"));
  }

  @Test
  public void testSettingSystemTokensOnClientErrorResponse() {
    Client client = mock(Client.class);
    Response response = mock(Response.class);
    when(tokenStorage.read(anyString(), anyString())).thenReturn(null);
    when(response.getStatus()).thenReturn(400);
    when(response.getEntity()).thenReturn(new ByteArrayInputStream("".getBytes()));

    WebClient webClient = oauthSecurity.webClient;
    when(webClient.form(any(Form.class))).thenReturn(response);

    ArgumentCaptor<Form> captor = ArgumentCaptor.forClass(Form.class);
    oauthSecurity.setSystemTokenOnClient(client, DDF_CLIENT, SECRET, METADATA_ENDPOINT);

    verify(client, times(0)).header(any(), any());

    verify(webClient, times(1)).header(AUTHORIZATION, ENCODED_CRED);
    verify(webClient, times(1)).accept(MediaType.APPLICATION_JSON);
    verify(webClient, times(1)).form(captor.capture());
    Form form = captor.getValue();
    assertTrue(form.asMap().get("grant_type").contains("client_credentials"));
  }

  private JWTCreator.Builder getAccessTokenBuilder() {
    String[] audience = {"master-realm", "account"};
    String[] roles = {"create-realm", "offline_access", "admin", "uma_authorization"};

    return com.auth0
        .jwt
        .JWT
        .create()
        .withJWTId(UUID.randomUUID().toString())
        .withExpiresAt(new Date(Instant.now().plus(Duration.ofDays(3)).toEpochMilli()))
        .withNotBefore(new Date(0))
        .withIssuedAt(new Date())
        .withIssuer("http://localhost:8080/auth/realms/master")
        .withArrayClaim("aud", audience)
        .withSubject("subject")
        .withClaim("typ", "Bearer")
        .withClaim(AZP, DDF_CLIENT)
        .withClaim("auth_time", new Date())
        .withArrayClaim("roles", roles)
        .withClaim(EMAIL_VERIFIED, false)
        .withClaim(PREFERRED_USERNAME, "admin");
  }

  private String getResponse(String accessToken) {
    return GSON.toJson(
        ImmutableMap.of(
            "access_token",
            accessToken,
            "token_type",
            "Bearer",
            "refresh_token",
            "refreshToken",
            "expires_in",
            3600));
  }

  private Subject getSubject() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn(USERNAME);

    SecurityAssertion securityAssertion = mock(SecurityAssertion.class);
    when(securityAssertion.getWeight()).thenReturn(10);
    when(securityAssertion.getPrincipal()).thenReturn(principal);

    PrincipalCollection principalCollection = mock(PrincipalCollection.class);
    when(principalCollection.byType(SecurityAssertion.class))
        .thenReturn(Collections.singletonList(securityAssertion));

    Subject subject = mock(Subject.class);
    when(subject.getPrincipals()).thenReturn(principalCollection);
    return subject;
  }

  private static class OAuthSecurityWithMockWebclient extends OAuthSecurityImpl {
    private WebClient webClient = mock(WebClient.class);

    OAuthSecurityWithMockWebclient(TokenStorage tokenStorage) {
      super(tokenStorage);
    }

    @Override
    WebClient createWebClient(URI uri) {
      return webClient;
    }
  }
}
