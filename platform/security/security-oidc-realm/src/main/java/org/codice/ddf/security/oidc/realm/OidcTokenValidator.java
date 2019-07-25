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

import static org.pac4j.oidc.config.OidcConfiguration.IMPLICIT_FLOWS;

import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.Header;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.JWSVerifierFactory;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jose.util.ResourceRetriever;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.openid.connect.sdk.claims.AccessTokenHash;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import com.nimbusds.openid.connect.sdk.validators.AccessTokenValidator;
import java.security.Key;
import java.util.List;
import java.util.ListIterator;
import net.minidev.json.JSONObject;
import org.apache.shiro.authc.AuthenticationException;
import org.pac4j.core.context.WebContext;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.profile.creator.TokenValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OidcTokenValidator {
  private static final Logger LOGGER = LoggerFactory.getLogger(OidcTokenValidator.class);

  private List<JWSAlgorithm> userInfoSigAlgList;

  private OidcConfiguration configuration;
  private OIDCProviderMetadata metadata;
  private ResourceRetriever resourceRetriever;

  public OidcTokenValidator(OidcConfiguration configuration, OIDCProviderMetadata metadata) {
    this.configuration = configuration;
    this.metadata = metadata;

    resourceRetriever = configuration.findResourceRetriever();
    userInfoSigAlgList = metadata.getUserInfoJWSAlgs();
  }

  /**
   * Validates id tokens.
   *
   * <ul>
   *   <li>If the ID token is not signed, an exception is thrown
   *   <li>If the ID token is signed, the required signing algorithm list from the metadata is used
   *       along with the header to validate it
   *
   * @param idToken - id token to validate
   * @param webContext - the web context used to get the session information
   */
  public IDTokenClaimsSet validateIdTokens(JWT idToken, WebContext webContext) {
    if (!(idToken instanceof SignedJWT)) {
      LOGGER.error("Error validating id token. ID token was not signed.");
      throw new AuthenticationException("Error validating id token. ID token was not signed.");
    }

    try {
      // get nonce
      Nonce nonce = null;
      if (configuration.isUseNonce()) {
        Object nonceString =
            webContext.getSessionStore().get(webContext, OidcConfiguration.NONCE_SESSION_ATTRIBUTE);
        if (nonceString != null) {
          nonce = new Nonce((String) nonceString);
        }
      }

      TokenValidator tokenValidator = new TokenValidator(configuration);
      return tokenValidator.validate(idToken, nonce);
    } catch (Exception e) {
      LOGGER.error("Error validating id token.", e);
      throw new AuthenticationException("Error validating id token.", e);
    }
  }

  /**
   * Validates id tokens received from the userinfo endpoint.
   *
   * <ul>
   *   <li>If the ID token is not signed, validation is ignored
   *   <li>If the ID token is signed
   *       <ul>
   *         <li>If the userinfo signing algorithms are listed in the metadata, we use that
   *             information along with the header attributes to validate the token
   *         <li>If the userinfo signing algorithms are NOT listed in the metadata, we just use the
   *             header attributes to validate the token
   *       </ul>
   *
   * @param idToken - id token to validate
   */
  public void validateUserInfoIdToken(JWT idToken) {
    try {

      if (!(idToken instanceof SignedJWT)) {
        LOGGER.info("ID token received from the userinfo endpoint was not signed.");
        return;
      }

      JWKSource jwkSource = new RemoteJWKSet(metadata.getJWKSetURI().toURL(), resourceRetriever);

      SignedJWT signedJWT = ((SignedJWT) idToken);
      JWSAlgorithm jwsAlgorithm = signedJWT.getHeader().getAlgorithm();

      if (userInfoSigAlgList.isEmpty()) {
        LOGGER.warn(
            "A JWS algorithm was not listed in the OpenID Connect provider metadata. "
                + "Using JWS algorithm specified in the header.");
      } else {
        if (!userInfoSigAlgList.contains(jwsAlgorithm)) {
          LOGGER.error("The signature algorithm of the id token do not match the expected ones.");
          throw new AuthenticationException(
              "The signature algorithm of the id token do not match the expected ones.");
        }
      }

      JWSKeySelector jwsKeySelector = new JWSVerificationKeySelector(jwsAlgorithm, jwkSource);
      JWSVerifierFactory jwsVerifierFactory = new DefaultJWSVerifierFactory();

      List<? extends Key> keyCandidates = jwsKeySelector.selectJWSKeys(signedJWT.getHeader(), null);

      if (keyCandidates == null || keyCandidates.isEmpty()) {
        throw new AuthenticationException(
            "Error Validating userinfo ID token. No matching key(s) found");
      }

      ListIterator<? extends Key> it = keyCandidates.listIterator();

      while (it.hasNext()) {

        JWSVerifier verifier =
            jwsVerifierFactory.createJWSVerifier(signedJWT.getHeader(), it.next());

        if (verifier == null) {
          continue;
        }

        final boolean validSignature = signedJWT.verify(verifier);

        if (validSignature) {
          return;
        }

        if (!it.hasNext()) {
          throw new AuthenticationException(
              "Error Validating userinfo ID token. Invalid signature");
        }
      }

      throw new AuthenticationException(
          "Error Validating userinfo ID token. No matching verifier(s) found");
    } catch (Exception e) {
      LOGGER.error("Error validating id token.", e);
      throw new AuthenticationException("Error validating id token.", e);
    }
  }

  /**
   * Validates an access token.
   *
   * @param accessToken - token to validate
   * @param idToken - the corresponding id token or null if one is not available
   */
  public void validateAccessToken(AccessToken accessToken, JWT idToken) {
    if (accessToken == null) {
      return;
    }

    validateAccessTokenSignature(accessToken, idToken);

    if (idToken != null) {
      validateAccessTokenAtHash(accessToken, idToken);
    }
  }

  /**
   * Validates an access token's signature
   *
   * @param accessToken - the token to validate
   * @param idToken - the corresponding ID token or null if one is not available. If an ID token is
   *     provided, the signature algorithm in the ID token is used. Otherwise the Algorithm provided
   *     in the header of the access token is used.
   */
  private void validateAccessTokenSignature(AccessToken accessToken, JWT idToken) {
    try {
      ConfigurableJWTProcessor jwtProcessor = new DefaultJWTProcessor();

      JWKSource keySource = new RemoteJWKSet(metadata.getJWKSetURI().toURL(), resourceRetriever);

      // Get signature algorithm, if ID token is given get algorithm from ID Token otherwise
      // get algorithm from access token header
      Algorithm expectedAlgorithm;
      if (idToken == null || idToken.getHeader().getAlgorithm() == Algorithm.NONE) {
        String accessTokenString = accessToken.getValue();
        Base64URL header =
            new Base64URL(accessTokenString.substring(0, accessTokenString.indexOf('.')));
        JSONObject jsonObject = JSONObjectUtils.parse(header.decodeToString());
        expectedAlgorithm = Header.parseAlgorithm(jsonObject);
      } else {
        expectedAlgorithm = idToken.getHeader().getAlgorithm();
      }

      if (expectedAlgorithm == Algorithm.NONE) {
        LOGGER.error("Error validating access token. Access token was not signed.");
        throw new AuthenticationException(
            "Error validating access token. Access token was not signed.");
      }

      JWSAlgorithm expectedJWSAlgorithm =
          new JWSAlgorithm(expectedAlgorithm.getName(), expectedAlgorithm.getRequirement());

      JWSKeySelector keySelector = new JWSVerificationKeySelector(expectedJWSAlgorithm, keySource);
      jwtProcessor.setJWSKeySelector(keySelector);
      jwtProcessor.process(accessToken.getValue(), null);

    } catch (Exception e) {
      LOGGER.error("Error validating access token.", e);
      throw new AuthenticationException("Error validating access token.", e);
    }
  }

  /**
   * Validates the at_hash parameter in the ID token against the access token. If implicit flow is
   * used with a id_token token response type is used. The at_hash value is required.
   *
   * @param accessToken - the token to validate
   * @param idToken - the corresponding ID token
   */
  private void validateAccessTokenAtHash(AccessToken accessToken, JWT idToken) {
    try {
      Object atHash = idToken.getJWTClaimsSet().getClaim("at_hash");
      if (atHash == null && !IMPLICIT_FLOWS.contains(configuration.getResponseType())) {
        return;
      }

      if (atHash == null) {
        String errorMessage =
            "at_hash value not found in response. If the ID Token is issued from the Authorization Endpoint with "
                + "an access_token value, which is the case for the response_type value id_token token, this is REQUIRED";
        LOGGER.error(errorMessage);
        throw new AuthenticationException(errorMessage);
      }

      JWSAlgorithm jwsAlgorithm = new JWSAlgorithm(idToken.getHeader().getAlgorithm().getName());
      AccessTokenHash accessTokenHash = new AccessTokenHash((String) atHash);
      AccessTokenValidator.validate(accessToken, jwsAlgorithm, accessTokenHash);
    } catch (Exception e) {
      LOGGER.error("Error validating access token.", e);
      throw new AuthenticationException("Error validating access token.", e);
    }
  }
}
