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
package org.codice.ddf.security.oidc.realm;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.codice.ddf.security.oidc.realm.TokenBuilder.getAccessTokenBuilder;
import static org.codice.ddf.security.oidc.realm.TokenBuilder.getIdTokenBuilder;
import static org.codice.ddf.security.oidc.realm.TokenBuilder.getWebContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.pac4j.oidc.profile.OidcProfileDefinition.AUTH_TIME;

import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.impl.PublicClaims;
import com.google.common.collect.ImmutableList;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Resource;
import com.nimbusds.jose.util.ResourceRetriever;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.codice.ddf.security.handler.api.OidcAuthenticationToken;
import org.codice.ddf.security.handler.api.OidcHandlerConfiguration;
import org.codice.ddf.security.handler.api.SAMLAuthenticationToken;
import org.junit.Before;
import org.junit.Test;
import org.pac4j.core.context.WebContext;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.credentials.OidcCredentials;

public class OidcRealmTest {

  private OidcRealm realm;
  private OidcAuthenticationToken authenticationToken;
  private OidcCredentials oidcCredentials;

  private Algorithm validAlgorithm;
  private Algorithm invalidAlgorithm;

  @Before
  public void setup() throws Exception {
    realm = new OidcRealm();

    // Generate the RSA key pair
    KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
    gen.initialize(2048);
    KeyPair keyPair = gen.generateKeyPair();
    RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
    RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
    validAlgorithm = Algorithm.RSA256(publicKey, privateKey);
    invalidAlgorithm = Algorithm.HMAC256("WRONG");

    JWK sigJwk =
        new RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyUse(KeyUse.SIGNATURE)
            .keyID(UUID.randomUUID().toString())
            .build();

    String jwk = "{\"keys\": [" + sigJwk.toPublicJWK().toJSONString() + "] }";

    OIDCProviderMetadata oidcProviderMetadata = mock(OIDCProviderMetadata.class);
    when(oidcProviderMetadata.getIDTokenJWSAlgs()).thenReturn(ImmutableList.of(JWSAlgorithm.RS256));
    when(oidcProviderMetadata.getIssuer())
        .thenReturn(new Issuer("http://localhost:8080/auth/realms/master"));
    when(oidcProviderMetadata.getJWKSetURI())
        .thenReturn(
            new URI("http://localhost:8080/auth/realms/master/protocol/openid-connect/certs"));

    ResourceRetriever resourceRetriever = mock(ResourceRetriever.class);
    Resource resource = new Resource(jwk, APPLICATION_JSON);
    when(resourceRetriever.retrieveResource(any())).thenReturn(resource);

    OidcConfiguration configuration = mock(OidcConfiguration.class);
    when(configuration.getClientId()).thenReturn("ddf-client");
    when(configuration.getSecret()).thenReturn("secret");
    when(configuration.isUseNonce()).thenReturn(true);
    when(configuration.getResponseType()).thenReturn("code");
    when(configuration.findProviderMetadata()).thenReturn(oidcProviderMetadata);
    when(configuration.findResourceRetriever()).thenReturn(resourceRetriever);

    OidcHandlerConfiguration handlerConfiguration = mock(OidcHandlerConfiguration.class);
    when(handlerConfiguration.getOidcConfiguration()).thenReturn(configuration);
    when(handlerConfiguration.getOidcClient(any())).thenReturn(mock(OidcClient.class));

    realm.setOidcHandlerConfiguration(handlerConfiguration);

    realm.setUsernameAttributeList(Collections.singletonList("preferred_username"));

    JWT jwt = mock(JWT.class);
    AccessToken accessToken = new BearerAccessToken(getAccessTokenBuilder().sign(validAlgorithm));
    AuthorizationCode authorizationCode = new AuthorizationCode();

    WebContext webContext = getWebContext();
    oidcCredentials = mock(OidcCredentials.class);
    when(oidcCredentials.getIdToken()).thenReturn(jwt);
    when(oidcCredentials.getIdToken()).thenReturn(jwt);
    when(oidcCredentials.getAccessToken()).thenReturn(accessToken);
    when(oidcCredentials.getCode()).thenReturn(authorizationCode);

    authenticationToken = mock(OidcAuthenticationToken.class);
    when(authenticationToken.getCredentials()).thenReturn(oidcCredentials);
    when(authenticationToken.getContext()).thenReturn(webContext);
  }

  @Test
  public void testSupports() {
    boolean supports = realm.supports(authenticationToken);
    assertTrue(supports);
  }

  @Test
  public void testSupportsFails() {

    // null token
    boolean supports = realm.supports(null);
    assertFalse(supports);

    // null credentials
    when(authenticationToken.getCredentials()).thenReturn(null);
    supports = realm.supports(authenticationToken);
    assertFalse(supports);

    // token not an OidcAuthenticationToken type
    SAMLAuthenticationToken samlAuthenticationToken = mock(SAMLAuthenticationToken.class);
    when(samlAuthenticationToken.getCredentials()).thenReturn("creds");
    supports = realm.supports(samlAuthenticationToken);
    assertFalse(supports);
  }

  @Test
  public void testDoGetAuthenticationInfo() throws ParseException {
    String idToken = getIdTokenBuilder().withClaim("nonce", "myNonce").sign(validAlgorithm);
    JWT jwt = SignedJWT.parse(idToken);

    when(oidcCredentials.getIdToken()).thenReturn(jwt);

    AuthenticationInfo authenticationInfo = realm.doGetAuthenticationInfo(authenticationToken);
    assertNotNull(authenticationInfo.getCredentials());
    assertNotNull(authenticationInfo.getPrincipals());
    assertEquals("admin", authenticationInfo.getPrincipals().getPrimaryPrincipal());
  }

  @Test
  public void testDoGetAuthenticationInfoWithMissingInfo() throws ParseException {
    JWT jwt = getIncompleteJwt();
    when(oidcCredentials.getIdToken()).thenReturn(jwt);

    AuthenticationInfo authenticationInfo = realm.doGetAuthenticationInfo(authenticationToken);
    assertNotNull(authenticationInfo.getCredentials());
    assertNotNull(authenticationInfo.getPrincipals());
    assertNotNull(authenticationInfo.getPrincipals().getPrimaryPrincipal());
    assertNotEquals("admin", authenticationInfo.getPrincipals().getPrimaryPrincipal());
  }

  @Test(expected = AuthenticationException.class)
  public void testDoGetAuthenticationInvalid() throws Exception {
    String idToken = getIdTokenBuilder().withClaim("nonce", "myNonce").sign(invalidAlgorithm);
    JWT jwt = SignedJWT.parse(idToken);
    when(oidcCredentials.getIdToken()).thenReturn(jwt);

    realm.doGetAuthenticationInfo(authenticationToken);
  }

  private JWT getIncompleteJwt() throws ParseException {
    // JWT is valid with a valid payload but doesn't an email and preferred_username
    String[] roles = {"create-realm", "offline_access", "admin", "uma_authorization"};
    String idToken =
        com.auth0
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
            .withClaim("nonce", "myNonce")
            .withArrayClaim("roles", roles)
            .sign(validAlgorithm);

    return SignedJWT.parse(idToken);
  }
}
