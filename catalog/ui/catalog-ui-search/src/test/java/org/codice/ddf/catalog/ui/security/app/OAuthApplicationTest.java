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
package org.codice.ddf.catalog.ui.security.app;

import static org.codice.ddf.security.token.storage.api.TokenStorage.CLIENT_ID;
import static org.codice.ddf.security.token.storage.api.TokenStorage.DISCOVERY_URL;
import static org.codice.ddf.security.token.storage.api.TokenStorage.SECRET;
import static org.codice.ddf.security.token.storage.api.TokenStorage.SOURCE_ID;
import static org.codice.ddf.security.token.storage.api.TokenStorage.USER_ID;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pac4j.core.context.HttpConstants.APPLICATION_JSON;
import static org.pac4j.oidc.profile.OidcProfileDefinition.AUTH_TIME;
import static org.pac4j.oidc.profile.OidcProfileDefinition.AZP;
import static org.pac4j.oidc.profile.OidcProfileDefinition.EMAIL_VERIFIED;
import static org.pac4j.oidc.profile.OidcProfileDefinition.PREFERRED_USERNAME;

import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.impl.PublicClaims;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Resource;
import com.nimbusds.jose.util.ResourceRetriever;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.security.token.storage.api.TokenInformation;
import org.codice.ddf.security.token.storage.api.TokenInformationImpl;
import org.codice.ddf.security.token.storage.api.TokenStorage;
import org.junit.Before;
import org.junit.Test;
import spark.Response;

public class OAuthApplicationTest {

  private static final String USERNAME = "username";
  private static final String CSW_SOURCE = "CSW";
  private static final String ACCESS_TOKEN_VAL = "myAccessToken";
  private static final String REFRESH_TOKEN_VAL = "myRefreshToken";
  private static final String DDF_CLIENT_ID = "ddf-client";
  private static final String DDF_CLIENT_SECRET = "secret";
  private static final String METADATA_ENDPOINT = "http://localhost:8080/auth/master/metadata";
  private static final String JWK_ENDPOINT =
      "http://localhost:8080/auth/realms/master/protocol/openid-connect/certs";

  private OAuthApplicationWithMockCredentialResolver oauthApplication;
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
    oauthApplication = new OAuthApplicationWithMockCredentialResolver(tokenStorage);
    oauthApplication.setResourceRetriever(resourceRetriever);
  }

  @Test
  public void testGetCodeFlowTokens() throws Exception {
    Response response = mock(Response.class);
    String idToken = getIdTokenBuilder().sign(validAlgorithm);
    String accessToken = getAccessTokenBuilder().sign(validAlgorithm);

    oauthApplication.oidcTokens =
        new OIDCTokens(
            SignedJWT.parse(idToken),
            new BearerAccessToken(accessToken),
            new RefreshToken(REFRESH_TOKEN_VAL));

    oauthApplication.processCodeFlow(
        response,
        "code",
        ImmutableMap.of(
            USER_ID,
            USERNAME,
            SOURCE_ID,
            CSW_SOURCE,
            DISCOVERY_URL,
            METADATA_ENDPOINT,
            CLIENT_ID,
            DDF_CLIENT_ID,
            SECRET,
            DDF_CLIENT_SECRET));
    verify(tokenStorage, times(1))
        .create(USERNAME, CSW_SOURCE, accessToken, REFRESH_TOKEN_VAL, METADATA_ENDPOINT);
  }

  @Test
  public void testGetCodeFlowTokensInvalidToken() throws Exception {
    Response response = mock(Response.class);

    String idToken = getIdTokenBuilder().sign(validAlgorithm);
    String accessToken = getAccessTokenBuilder().sign(invalidAlgorithm);
    oauthApplication.oidcTokens =
        new OIDCTokens(
            SignedJWT.parse(idToken),
            new BearerAccessToken(accessToken),
            new RefreshToken(REFRESH_TOKEN_VAL));

    oauthApplication.processCodeFlow(
        response,
        "code",
        ImmutableMap.of(
            USER_ID,
            USERNAME,
            SOURCE_ID,
            CSW_SOURCE,
            DISCOVERY_URL,
            METADATA_ENDPOINT,
            CLIENT_ID,
            DDF_CLIENT_ID,
            SECRET,
            DDF_CLIENT_SECRET));
    verify(tokenStorage, times(0))
        .create(anyString(), anyString(), anyString(), anyString(), anyString());
  }

  @Test
  public void testUpdateAuthorizedSource() {
    TokenInformation.TokenEntry tokenEntry =
        new TokenInformationImpl.TokenEntryImpl(
            ACCESS_TOKEN_VAL, REFRESH_TOKEN_VAL, METADATA_ENDPOINT);
    TokenInformation.TokenEntry tokenEntry2 =
        new TokenInformationImpl.TokenEntryImpl(
            "ACCESS_TOKEN", "REFRESH_TOKEN", "https://example.com/");

    TokenInformation tokenInformation = mock(TokenInformation.class);
    when(tokenInformation.getDiscoveryUrls())
        .thenReturn(ImmutableSet.of("https://localhost:8993/", "https://example.com/"));
    when(tokenInformation.getTokenEntries())
        .thenReturn(ImmutableMap.of(CSW_SOURCE, tokenEntry, "OpenSearch", tokenEntry2));

    when(tokenStorage.read(USERNAME)).thenReturn(tokenInformation);
    oauthApplication.updateAuthorizedSource(USERNAME, "MySource", METADATA_ENDPOINT);
    verify(tokenStorage, times(1))
        .create(USERNAME, "MySource", ACCESS_TOKEN_VAL, REFRESH_TOKEN_VAL, METADATA_ENDPOINT);
  }

  @Test
  public void testUpdateAuthorizedSourceWithNoMatchingUrls() {
    TokenInformation.TokenEntry tokenEntry =
        new TokenInformationImpl.TokenEntryImpl(
            ACCESS_TOKEN_VAL, REFRESH_TOKEN_VAL, "https://localhost:8993/");
    TokenInformation.TokenEntry tokenEntry2 =
        new TokenInformationImpl.TokenEntryImpl(
            ACCESS_TOKEN_VAL, REFRESH_TOKEN_VAL, "https://example.com/");

    TokenInformation tokenInformation = mock(TokenInformation.class);
    when(tokenInformation.getDiscoveryUrls())
        .thenReturn(ImmutableSet.of("https://localhost:8993/", "https://example.com/"));
    when(tokenInformation.getTokenEntries())
        .thenReturn(ImmutableMap.of(CSW_SOURCE, tokenEntry, "OpenSearch", tokenEntry2));

    when(tokenStorage.read(USERNAME)).thenReturn(tokenInformation);
    oauthApplication.updateAuthorizedSource(USERNAME, "MySource", METADATA_ENDPOINT);
    verify(tokenStorage, times(0))
        .create(anyString(), anyString(), anyString(), anyString(), anyString());
  }

  private JWTCreator.Builder getIdTokenBuilder() {
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
        .withAudience("ddf-client")
        .withSubject("subject")
        .withClaim(PublicClaims.TYPE, "ID")
        .withClaim(AUTH_TIME, new Date())
        .withArrayClaim("roles", roles)
        .withClaim(EMAIL_VERIFIED, false)
        .withClaim(PREFERRED_USERNAME, "admin");
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
        .withClaim(AZP, "ddf-client")
        .withClaim("auth_time", new Date())
        .withArrayClaim("roles", roles)
        .withClaim(EMAIL_VERIFIED, false)
        .withClaim(PREFERRED_USERNAME, "admin");
  }

  /**
   * {@link OAuthApplication} which overrides the getTokens method to return a mock instead of
   * trying to make a call to get the tokens for testing purposes purposes
   */
  private static class OAuthApplicationWithMockCredentialResolver extends OAuthApplication {
    OIDCTokens oidcTokens;

    OAuthApplicationWithMockCredentialResolver(TokenStorage tokenStorage) {
      super(tokenStorage);
    }

    @Override
    OIDCTokens getTokens(
        String code, String clientId, String clientSecret, OIDCProviderMetadata metadata) {
      return oidcTokens;
    }
  }
}
