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
package org.codice.ddf.catalog.plugin.oauth;

import static ddf.security.SecurityConstants.SECURITY_SUBJECT;
import static ddf.security.SubjectUtils.EMAIL_ADDRESS_CLAIM_URI;
import static org.codice.ddf.security.token.storage.api.TokenStorage.CLIENT_ID;
import static org.codice.ddf.security.token.storage.api.TokenStorage.DISCOVERY_URL;
import static org.codice.ddf.security.token.storage.api.TokenStorage.EXPIRES_AT;
import static org.codice.ddf.security.token.storage.api.TokenStorage.SECRET;
import static org.codice.ddf.security.token.storage.api.TokenStorage.SOURCE_ID;
import static org.codice.ddf.security.token.storage.api.TokenStorage.USER_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pac4j.core.context.HttpConstants.APPLICATION_JSON;
import static org.pac4j.oidc.profile.OidcProfileDefinition.AZP;
import static org.pac4j.oidc.profile.OidcProfileDefinition.EMAIL_VERIFIED;
import static org.pac4j.oidc.profile.OidcProfileDefinition.PREFERRED_USERNAME;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Resource;
import com.nimbusds.jose.util.ResourceRetriever;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.plugin.OAuthPluginException;
import ddf.catalog.source.OAuthFederatedSource;
import ddf.catalog.source.Source;
import ddf.security.Subject;
import ddf.security.assertion.Attribute;
import ddf.security.assertion.AttributeStatement;
import ddf.security.assertion.SecurityAssertion;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;
import net.minidev.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.shiro.subject.PrincipalCollection;
import org.codice.ddf.security.token.storage.api.TokenInformation;
import org.codice.ddf.security.token.storage.api.TokenInformationImpl;
import org.codice.ddf.security.token.storage.api.TokenStorage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class OAuthPluginTest {

  private static final String METADATA_ENDPOINT = "http://localhost:8080/auth/master/metadata";
  private static final String JWK_ENDPOINT =
      "http://localhost:8080/auth/realms/master/protocol/openid-connect/certs";

  private static final String USER_EMAIL = "username@localhost.com";
  private static final String CSW_SOURCE = "CSW";
  private static final String DDF_CLIENT = "ddf-client";
  private static final String DDF_SECRET = "secret";

  private OAuthPluginWithMockWebClient oauthPlugin;
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
    oauthPlugin = new OAuthPluginWithMockWebClient(tokenStorage);
    oauthPlugin.setResourceRetriever(resourceRetriever);
  }

  @Test
  public void testProcess() throws Exception {
    OAuthFederatedSource source = oauthPlugin.oauthSource;
    Subject subject = getSubject();
    QueryRequest input = mock(QueryRequest.class);
    when(input.getProperties()).thenReturn(ImmutableMap.of(SECURITY_SUBJECT, subject));

    String accessToken = getAccessTokenBuilder().sign(validAlgorithm);
    TokenInformation.TokenEntry tokenEntry =
        new TokenInformationImpl.TokenEntryImpl(accessToken, "refresh_token", METADATA_ENDPOINT);
    when(tokenStorage.read(USER_EMAIL, CSW_SOURCE)).thenReturn(tokenEntry);

    QueryRequest output = oauthPlugin.process(source, input);
    assertEquals(input, output);
  }

  @Test
  public void testProcessExpiredAccessToken() throws Exception {
    OAuthFederatedSource source = oauthPlugin.oauthSource;
    Subject subject = getSubject();
    QueryRequest input = mock(QueryRequest.class);
    when(input.getProperties()).thenReturn(ImmutableMap.of(SECURITY_SUBJECT, subject));

    String accessToken =
        getAccessTokenBuilder()
            .withExpiresAt(new Date(Instant.now().minus(1, ChronoUnit.MINUTES).toEpochMilli()))
            .sign(validAlgorithm);
    String refreshToken = getRefreshTokenBuilder().sign(validAlgorithm);
    TokenInformation.TokenEntry tokenEntry =
        new TokenInformationImpl.TokenEntryImpl(accessToken, refreshToken, METADATA_ENDPOINT);
    when(tokenStorage.read(USER_EMAIL, CSW_SOURCE)).thenReturn(tokenEntry);

    String validAccessToken = getAccessTokenBuilder().sign(validAlgorithm);
    Response response = mock(Response.class);
    when(response.getStatus()).thenReturn(200);
    when(response.getEntity()).thenReturn(getResponse(validAccessToken));
    when(oauthPlugin.webClient.form(any(Form.class))).thenReturn(response);

    QueryRequest output = oauthPlugin.process(source, input);

    assertEquals(input, output);
    verify(tokenStorage, times(1))
        .create(anyString(), anyString(), anyString(), anyString(), anyString());
  }

  @Test(expected = OAuthPluginException.class)
  public void testDifferentDiscoveryUrl() throws Exception {
    OAuthFederatedSource source = oauthPlugin.oauthSource;
    Subject subject = getSubject();
    QueryRequest input = mock(QueryRequest.class);
    when(input.getProperties()).thenReturn(ImmutableMap.of(SECURITY_SUBJECT, subject));

    Map<String, Map<String, Object>> stateMap = mock(Map.class);
    String accessToken = getAccessTokenBuilder().sign(validAlgorithm);
    TokenInformation.TokenEntry tokenEntry =
        new TokenInformationImpl.TokenEntryImpl(accessToken, "refresh_token", "http://example.com");
    when(tokenStorage.read(USER_EMAIL, CSW_SOURCE)).thenReturn(tokenEntry);
    when(tokenStorage.getStateMap()).thenReturn(stateMap);

    try {
      oauthPlugin.process(source, input);
    } catch (OAuthPluginException e) {
      verify(tokenStorage, times(1)).delete(USER_EMAIL, CSW_SOURCE);
      verify(tokenStorage, times(1)).getStateMap();

      ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
      verify(stateMap, times(1)).put(anyString(), captor.capture());

      assertUrl(e, captor.getValue());
      throw e;
    }
  }

  @Test(expected = OAuthPluginException.class)
  public void testNoStoredTokensButExistingUnderDifferentSource() throws Exception {
    OAuthFederatedSource source = oauthPlugin.oauthSource;
    Subject subject = getSubject();
    QueryRequest input = mock(QueryRequest.class);
    when(input.getProperties()).thenReturn(ImmutableMap.of(SECURITY_SUBJECT, subject));

    String accessToken =
        getAccessTokenBuilder()
            .withExpiresAt(new Date(Instant.now().plus(1, ChronoUnit.MINUTES).toEpochMilli()))
            .sign(validAlgorithm);
    TokenInformation.TokenEntry tokenEntry =
        new TokenInformationImpl.TokenEntryImpl(accessToken, "myRefreshToken", METADATA_ENDPOINT);

    TokenInformation tokenInformation = mock(TokenInformation.class);
    when(tokenInformation.getDiscoveryUrls()).thenReturn(Collections.singleton(METADATA_ENDPOINT));
    when(tokenInformation.getTokenEntries()).thenReturn(Collections.singletonMap("OS", tokenEntry));

    when(tokenStorage.read(USER_EMAIL, SOURCE_ID)).thenReturn(null);
    when(tokenStorage.read(USER_EMAIL)).thenReturn(tokenInformation);

    try {
      oauthPlugin.process(source, input);
    } catch (OAuthPluginException e) {
      assertEquals(e.getSourceId(), CSW_SOURCE);
      assertEquals(e.getErrorType().getStatusCode(), 412);

      String url = e.getUrl();
      Map<String, String> urlParams =
          URLEncodedUtils.parse(new URI(url), StandardCharsets.UTF_8)
              .stream()
              .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
      assertEquals(urlParams.get(USER_ID), USER_EMAIL);
      assertEquals(urlParams.get(SOURCE_ID), CSW_SOURCE);
      assertEquals(urlParams.get(DISCOVERY_URL), METADATA_ENDPOINT);
      throw e;
    }
  }

  @Test(expected = OAuthPluginException.class)
  public void testNoStoredTokensExistingTokenUnderDifferentSourceExpiredTokens() throws Exception {
    OAuthFederatedSource source = oauthPlugin.oauthSource;
    Subject subject = getSubject();
    QueryRequest input = mock(QueryRequest.class);
    when(input.getProperties()).thenReturn(ImmutableMap.of(SECURITY_SUBJECT, subject));

    String accessToken =
        getAccessTokenBuilder()
            .withExpiresAt(new Date(Instant.now().minus(1, ChronoUnit.MINUTES).toEpochMilli()))
            .sign(validAlgorithm);
    String refreshToken =
        getRefreshTokenBuilder()
            .withExpiresAt(new Date(Instant.now().minus(1, ChronoUnit.MINUTES).toEpochMilli()))
            .sign(validAlgorithm);
    TokenInformation.TokenEntry tokenEntry =
        new TokenInformationImpl.TokenEntryImpl(accessToken, refreshToken, METADATA_ENDPOINT);

    TokenInformation tokenInformation = mock(TokenInformation.class);
    when(tokenInformation.getDiscoveryUrls()).thenReturn(Collections.singleton(METADATA_ENDPOINT));
    when(tokenInformation.getTokenEntries()).thenReturn(Collections.singletonMap("OS", tokenEntry));

    Map<String, Map<String, Object>> stateMap = mock(Map.class);
    when(tokenStorage.getStateMap()).thenReturn(stateMap);
    when(tokenStorage.read(USER_EMAIL, SOURCE_ID)).thenReturn(null);
    when(tokenStorage.read(USER_EMAIL)).thenReturn(tokenInformation);
    try {
      oauthPlugin.process(source, input);
    } catch (OAuthPluginException e) {
      assertEquals(e.getSourceId(), CSW_SOURCE);
      assertEquals(e.getErrorType().getStatusCode(), 401);

      ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
      verify(stateMap, times(1)).put(anyString(), captor.capture());

      assertUrl(e, captor.getValue());
      throw e;
    }
  }

  @Test(expected = OAuthPluginException.class)
  public void testInvalidRefreshError() throws Exception {
    OAuthFederatedSource source = oauthPlugin.oauthSource;
    Subject subject = getSubject();
    QueryRequest input = mock(QueryRequest.class);
    when(input.getProperties()).thenReturn(ImmutableMap.of(SECURITY_SUBJECT, subject));

    String accessToken =
        getAccessTokenBuilder()
            .withExpiresAt(new Date(Instant.now().minus(1, ChronoUnit.MINUTES).toEpochMilli()))
            .sign(validAlgorithm);
    String refreshToken = getRefreshTokenBuilder().sign(validAlgorithm);
    Map<String, Map<String, Object>> stateMap = mock(Map.class);
    TokenInformation.TokenEntry tokenEntry =
        new TokenInformationImpl.TokenEntryImpl(accessToken, refreshToken, METADATA_ENDPOINT);
    when(tokenStorage.read(USER_EMAIL, CSW_SOURCE)).thenReturn(tokenEntry);
    when(tokenStorage.getStateMap()).thenReturn(stateMap);

    Response response = mock(Response.class);
    when(response.getStatus()).thenReturn(400);
    when(response.getEntity()).thenReturn(new ByteArrayInputStream("".getBytes()));
    when(oauthPlugin.webClient.form(any(Form.class))).thenReturn(response);

    try {
      oauthPlugin.process(source, input);
    } catch (OAuthPluginException e) {
      ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
      verify(stateMap, times(1)).put(anyString(), captor.capture());
      verify(tokenStorage, times(1)).getStateMap();

      assertUrl(e, captor.getValue());
      throw e;
    }
  }

  @Test(expected = OAuthPluginException.class)
  public void testInvalidRefreshedAccessToken() throws Exception {
    OAuthFederatedSource source = oauthPlugin.oauthSource;
    Subject subject = getSubject();
    QueryRequest input = mock(QueryRequest.class);
    when(input.getProperties()).thenReturn(ImmutableMap.of(SECURITY_SUBJECT, subject));

    String accessToken =
        getAccessTokenBuilder()
            .withExpiresAt(new Date(Instant.now().minus(1, ChronoUnit.MINUTES).toEpochMilli()))
            .sign(validAlgorithm);
    String refreshToken = getRefreshTokenBuilder().sign(validAlgorithm);
    Map<String, Map<String, Object>> stateMap = mock(Map.class);
    TokenInformation.TokenEntry tokenEntry =
        new TokenInformationImpl.TokenEntryImpl(accessToken, refreshToken, METADATA_ENDPOINT);
    when(tokenStorage.read(USER_EMAIL, CSW_SOURCE)).thenReturn(tokenEntry);
    when(tokenStorage.getStateMap()).thenReturn(stateMap);

    String invalidAccessToken = getAccessTokenBuilder().sign(invalidAlgorithm);
    Response response = mock(Response.class);
    when(response.getStatus()).thenReturn(200);
    when(response.getEntity()).thenReturn(getResponse(invalidAccessToken));
    when(oauthPlugin.webClient.form(any(Form.class))).thenReturn(response);

    try {
      oauthPlugin.process(source, input);
    } catch (OAuthPluginException e) {
      ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
      verify(stateMap, times(1)).put(anyString(), captor.capture());
      verify(tokenStorage, times(0))
          .create(anyString(), anyString(), anyString(), anyString(), anyString());
      verify(tokenStorage, times(1)).getStateMap();

      assertUrl(e, captor.getValue());
      throw e;
    }
  }

  private InputStream getResponse(String accessToken) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("access_token", accessToken);
    jsonObject.put("token_type", "bearer");
    jsonObject.put("refresh_token", getRefreshTokenBuilder().sign(validAlgorithm));
    jsonObject.put("expires_in", 60);
    jsonObject.put("scope", "openid profile email");
    return new ByteArrayInputStream(jsonObject.toJSONString().getBytes(StandardCharsets.UTF_8));
  }

  private void assertUrl(OAuthPluginException e, Map<String, Object> stateValue)
      throws URISyntaxException {
    assertEquals(e.getSourceId(), CSW_SOURCE);
    assertEquals(e.getErrorType().getStatusCode(), 401);

    String url = e.getUrl();
    Map<String, String> urlParams =
        URLEncodedUtils.parse(
                new URI(url.substring(0, url.indexOf("&state"))), StandardCharsets.UTF_8)
            .stream()
            .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
    assertEquals(urlParams.get("response_type"), "code");
    assertEquals(urlParams.get("client_id"), DDF_CLIENT);
    assertEquals(urlParams.get("scope"), "openid");
    assertEquals(
        urlParams.get("redirect_uri"), "https://localhost:8993/search/catalog/internal/oauth");

    assertEquals(stateValue.get(USER_ID), USER_EMAIL);
    assertEquals(stateValue.get(SOURCE_ID), CSW_SOURCE);
    assertEquals(stateValue.get(CLIENT_ID), DDF_CLIENT);
    assertEquals(stateValue.get(SECRET), DDF_SECRET);
    assertEquals(stateValue.get(DISCOVERY_URL), METADATA_ENDPOINT);
    assertTrue(
        Instant.now()
            .plus(5, ChronoUnit.MINUTES)
            .isAfter(Instant.ofEpochSecond((Long) stateValue.get(EXPIRES_AT))));
    assertTrue(
        Instant.now()
            .plus(4, ChronoUnit.MINUTES)
            .isBefore(Instant.ofEpochSecond((Long) stateValue.get(EXPIRES_AT))));
  }

  private Subject getSubject() {
    Attribute attribute = mock(Attribute.class);
    when(attribute.getName()).thenReturn(EMAIL_ADDRESS_CLAIM_URI);
    when(attribute.getValues()).thenReturn(Collections.singletonList(USER_EMAIL));

    AttributeStatement attributeStatement = mock(AttributeStatement.class);
    when(attributeStatement.getAttributes()).thenReturn(Collections.singletonList(attribute));

    SecurityAssertion assertion = mock(SecurityAssertion.class);
    when(assertion.getWeight()).thenReturn(5);
    when(assertion.getAttributeStatements())
        .thenReturn(Collections.singletonList(attributeStatement));

    PrincipalCollection principals = mock(PrincipalCollection.class);
    when(principals.byType(SecurityAssertion.class))
        .thenReturn(Collections.singletonList(assertion));

    Subject subject = mock(Subject.class);
    when(subject.getPrincipals()).thenReturn(principals);
    return subject;
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

  private JWTCreator.Builder getRefreshTokenBuilder() {
    String[] audience = {"master-realm", "account"};
    JSONObject realmAccess = new JSONObject();
    realmAccess.put(
        "roles", ImmutableList.of("create-realm", "offline_access", "admin", "uma_authorization"));

    return JWT.create()
        .withJWTId(UUID.randomUUID().toString())
        .withExpiresAt(new Date(Instant.now().plus(Duration.ofDays(3)).toEpochMilli()))
        .withNotBefore(new Date(0))
        .withIssuedAt(new Date())
        .withIssuer("http://localhost:8080/auth/realms/master")
        .withAudience("http://localhost:8080/auth/realms/master")
        .withArrayClaim("aud", audience)
        .withSubject("subject")
        .withClaim("typ", "Refresh")
        .withClaim(AZP, DDF_CLIENT)
        .withClaim("auth_time", 0)
        .withClaim("realm_access", realmAccess.toString())
        .withClaim("scope", "openid profile email");
  }

  static class OAuthPluginWithMockWebClient extends OAuthPlugin {
    WebClient webClient = mock(WebClient.class);
    OAuthFederatedSource oauthSource;

    OAuthPluginWithMockWebClient(TokenStorage tokenStorage) {
      super(tokenStorage);
      oauthSource = mock(OAuthFederatedSource.class);
      when(oauthSource.getId()).thenReturn(CSW_SOURCE);
      when(oauthSource.getOauthClientId()).thenReturn(DDF_CLIENT);
      when(oauthSource.getOauthClientSecret()).thenReturn(DDF_SECRET);
      when(oauthSource.getOauthDiscoveryUrl()).thenReturn(METADATA_ENDPOINT);
    }

    @Override
    WebClient createWebclient(String url) {
      return webClient;
    }

    @Override
    OAuthFederatedSource getSource(Source source) {
      return oauthSource;
    }
  }
}
