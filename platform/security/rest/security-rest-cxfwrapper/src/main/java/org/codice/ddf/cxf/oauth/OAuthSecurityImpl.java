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

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static org.codice.gsonsupport.GsonTypeAdapters.MAP_STRING_TO_OBJECT_TYPE;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jose.util.ResourceRetriever;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.TypelessAccessToken;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import ddf.security.Subject;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MultivaluedMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.oauth2.client.Consumer;
import org.apache.cxf.rs.security.oauth2.client.OAuthClientUtils;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenGrant;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.grants.refresh.RefreshTokenGrant;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.util.Strings;
import org.apache.shiro.session.Session;
import org.codice.ddf.security.oidc.validator.OidcTokenValidator;
import org.codice.ddf.security.oidc.validator.OidcValidationException;
import org.codice.ddf.security.token.storage.api.TokenInformation;
import org.codice.ddf.security.token.storage.api.TokenInformation.TokenEntry;
import org.codice.ddf.security.token.storage.api.TokenStorage;
import org.codice.gsonsupport.GsonTypeAdapters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OAuthSecurityImpl implements OAuthSecurity {

  private static final Logger LOGGER = LoggerFactory.getLogger(OAuthSecurityImpl.class);

  static final Gson GSON =
      new GsonBuilder()
          .disableHtmlEscaping()
          .registerTypeAdapterFactory(GsonTypeAdapters.LongDoubleTypeAdapter.FACTORY)
          .create();

  private static final String ADDING_TOKEN = "Adding access token to OAUTH header.";
  private static final String CLIENT_CREDENTIALS = "client_credentials";
  private static final String REFRESH_TOKEN = "refresh_token";
  private static final String ACCESS_TOKEN = "access_token";
  private static final String GRANT_TYPE = "grant_type";
  private static final String OPENID_SCOPE = "openid";
  private static final String ID_TOKEN = "id_token";
  private static final String USERNAME = "username";

  @SuppressWarnings("squid:S2068" /* Not a hardcoded password */)
  private static final String PASSWORD = "password";

  private static final String BEARER = "Bearer ";
  private static final String BASIC = "Basic ";
  private static final String SCOPE = "scope";
  private static final String EXP = "exp";
  private static final String IAT = "iat";

  private ResourceRetriever resourceRetriever;
  private TokenStorage tokenStorage;

  public OAuthSecurityImpl(TokenStorage tokenStorage) {
    this.tokenStorage = tokenStorage;
    resourceRetriever = new DefaultResourceRetriever();
  }

  /**
   * Gets the user's access token from the token storage to set it to the OAUTH header. Used when a
   * source is configured to use Authentication Code flow/grant.
   *
   * @param client Non-null client to set the access token on.
   * @param subject subject used to get the session ID
   * @param sourceId the id of the source using OAuth needed to get the correct tokens
   */
  public void setUserTokenOnClient(Client client, Subject subject, String sourceId) {
    if (client == null || subject == null || Strings.isBlank(sourceId)) {
      return;
    }

    Session session = subject.getSession(false);
    if (session == null) {
      LOGGER.warn("The user's session is not available.");
      return;
    }

    String sessionId = (String) session.getId();
    if (sessionId == null) {
      LOGGER.warn("The user's session ID is not available.");
      return;
    }

    TokenInformation.TokenEntry tokenEntry = tokenStorage.read(sessionId, sourceId);
    if (tokenEntry == null) {
      return;
    }

    LOGGER.debug(ADDING_TOKEN);
    client.header(OAUTH, BEARER + tokenEntry.getAccessToken());
  }

  /**
   * Gets the user's access token from the token storage to set it to the OAUTH header. If one is
   * not available, make a call to the OAuth provider to get tokens. Used when a source is
   * configured to use Resource Owner Password Credentials flow/grant.
   *
   * @param client Non-null client to set the access token on.
   * @param sourceId The ID of the source using OAuth needed to get the correct tokens
   * @param clientId The client ID registered with the OAuth provider
   * @param clientSecret The client secret registered with the OAuth provider
   * @param username The user's username
   * @param password The user's password
   * @param discoveryUrl The metadata URL of the OAuth provider
   * @param additionalParameters Additional queryParameters to send to the OAuth provider
   */
  public void setUserTokenOnClient(
      Client client,
      String sourceId,
      String clientId,
      String clientSecret,
      String username,
      String password,
      String discoveryUrl,
      Map<String, String> additionalParameters) {

    if (client == null
        || Strings.isBlank(sourceId)
        || Strings.isBlank(clientId)
        || Strings.isBlank(clientSecret)
        || Strings.isBlank(username)
        || Strings.isBlank(password)
        || Strings.isBlank(discoveryUrl)) {
      return;
    }

    Map<String, String> queryParameters =
        additionalParameters == null ? new HashMap<>() : new HashMap<>(additionalParameters);
    queryParameters.put(USERNAME, username);
    queryParameters.put(PASSWORD, password);

    String accessToken =
        getValidToken(
            username, sourceId, clientId, clientSecret, discoveryUrl, PASSWORD, queryParameters);
    if (accessToken != null) {
      LOGGER.debug(ADDING_TOKEN);
      client.header(OAUTH, BEARER + accessToken);
    }
  }

  /**
   * Gets the system's access token from the token storage to set it to the OAUTH header. If one can
   * not be found, retrieves the system's access token from the configured OAuth provider. Used when
   * a source is configured to use Client Credentials flow/grant.
   *
   * @param client Non-null client to set the access token on.
   * @param clientId The client ID registered with the OAuth provider
   * @param clientSecret The client secret registered with the OAuth provider
   * @param discoveryUrl the metadata URL of the OAuth provider
   */
  public void setSystemTokenOnClient(
      Client client, String clientId, String clientSecret, String discoveryUrl) {
    if (client == null
        || Strings.isBlank(clientId)
        || Strings.isBlank(clientSecret)
        || Strings.isBlank(discoveryUrl)) {
      return;
    }

    String encodedClientIdSecret =
        Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(UTF_8));

    Map<String, String> queryParameters = Collections.singletonMap(GRANT_TYPE, CLIENT_CREDENTIALS);

    String accessToken =
        getValidToken(
            encodedClientIdSecret,
            CLIENT_CREDENTIALS,
            clientId,
            clientSecret,
            discoveryUrl,
            CLIENT_CREDENTIALS,
            queryParameters);
    if (accessToken != null) {
      LOGGER.debug(ADDING_TOKEN);
      client.header(OAUTH, BEARER + accessToken);
    }
  }

  /**
   * Attempts to get an unexpired access token from the token storage or by making a request to the
   * OAuth provider.
   *
   * @param id The ID used when retrieving tokens from the token storage
   * @param sourceId The ID of the source using OAuth needed to get the correct tokens
   * @param clientId The client ID registered with the OAuth provider
   * @param clientSecret The client secret registered with the OAuth provider
   * @param discoveryUrl The discovery URL of the OAuth provider
   * @param grantType The grant type used if a request is sent to get a new token
   * @param queryParameters Parameters used if a request is sent to get a new token
   * @return an access token or null if all means of getting one fail
   */
  private String getValidToken(
      String id,
      String sourceId,
      String clientId,
      String clientSecret,
      String discoveryUrl,
      String grantType,
      Map<String, String> queryParameters) {

    TokenEntry tokenEntry = tokenStorage.read(id, sourceId);

    if (tokenEntry != null
        && discoveryUrl.equalsIgnoreCase(tokenEntry.getDiscoveryUrl())
        && !isExpired(tokenEntry.getAccessToken())) {

      return tokenEntry.getAccessToken();
    }

    OIDCProviderMetadata metadata;
    try {
      metadata =
          OIDCProviderMetadata.parse(
              resourceRetriever.retrieveResource(new URL(discoveryUrl)).getContent());
    } catch (IOException | ParseException e) {
      LOGGER.error("Unable to retrieve OAuth provider's metadata.", e);
      return null;
    }

    if (tokenEntry != null
        && discoveryUrl.equalsIgnoreCase(tokenEntry.getDiscoveryUrl())
        && isExpired(tokenEntry.getAccessToken())
        && !isExpired(tokenEntry.getRefreshToken())) {

      // refresh token
      return refreshToken(
          id,
          sourceId,
          clientId,
          clientSecret,
          discoveryUrl,
          tokenEntry.getRefreshToken(),
          metadata);
    }

    // Make a call to get a token
    String encodedClientIdSecret =
        Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(UTF_8));

    return getNewAccessToken(
        id, sourceId, encodedClientIdSecret, discoveryUrl, grantType, queryParameters, metadata);
  }

  /**
   * Gets an access token from the configured OAuth provider, saves it to the token storage and
   * returns it
   *
   * @param id The ID to use when storing tokens
   * @param sourceId The ID of the source using OAuth to use when storing tokens
   * @param encodedClientIdSecret The base 64 encoded clientId:secret
   * @param discoveryUrl The URL where the Oauth provider's metadata is hosted
   * @param grantType The OAuth grand type to use
   * @param queryParameters Query parameters to send
   * @return a client access token or null if one could not be returned
   */
  private String getNewAccessToken(
      String id,
      String sourceId,
      String encodedClientIdSecret,
      String discoveryUrl,
      String grantType,
      Map<String, String> queryParameters,
      OIDCProviderMetadata metadata) {

    WebClient webClient = createWebClient(metadata.getTokenEndpointURI());
    webClient.header(AUTHORIZATION, BASIC + encodedClientIdSecret);
    webClient.accept(APPLICATION_JSON);

    Form formParam = new Form(GRANT_TYPE, grantType);
    formParam.param(SCOPE, OPENID_SCOPE);
    queryParameters.forEach(formParam::param);
    javax.ws.rs.core.Response response = webClient.form(formParam);

    String body;
    try {
      body = IOUtils.toString((InputStream) response.getEntity(), UTF_8);
    } catch (IOException e) {
      LOGGER.debug("Unable to retrieve system access token.", e);
      return null;
    }

    if (response.getStatus() != HttpStatus.SC_OK) {
      LOGGER.debug("Unable to retrieve system access token. {}", body);
      if (LOGGER.isTraceEnabled()) {
        sanitizeFormParameters(formParam);
        LOGGER.trace(
            "Unable to retrieve system access token. Headers: {}, Request: {}, Status: {}, Response: {}",
            webClient.getHeaders(),
            formParam.asMap(),
            response.getStatus(),
            body);
      }
      return null;
    }

    Map<String, String> map = GSON.fromJson(body, MAP_STRING_TO_OBJECT_TYPE);
    String idToken = map.get(ID_TOKEN);
    String accessToken = map.get(ACCESS_TOKEN);
    String refreshToken = map.get(REFRESH_TOKEN);

    JWT jwt = null;
    try {
      if (idToken != null) {
        jwt = SignedJWT.parse(idToken);
      }
    } catch (java.text.ParseException e) {
      LOGGER.debug("Error parsing ID token.", e);
    }

    try {
      OidcTokenValidator.validateAccessToken(
          new BearerAccessToken(accessToken), jwt, resourceRetriever, metadata, null);
    } catch (OidcValidationException e) {
      LOGGER.warn("Error validating system access token.", e);
      return null;
    }

    LOGGER.debug("Successfully retrieved system access token.");
    int status = tokenStorage.create(id, sourceId, accessToken, refreshToken, discoveryUrl);
    if (status != SC_OK) {
      LOGGER.debug("Error storing user token.");
    }
    return accessToken;
  }

  private void sanitizeFormParameters(Form formParam) {
    MultivaluedMap<String, String> formParamMap = formParam.asMap();
    List<String> passwords = formParamMap.get(PASSWORD);
    if (passwords != null && passwords.size() == 1) {
      String password = passwords.get(0);
      if (isEncrypted(password)) {
        LOGGER.warn("Trying to use an encrypted password to retrieve system access token.");
      }
      formParamMap.replace(
          PASSWORD, Collections.singletonList(StringUtils.repeat("*", password.length())));
    }
  }

  private boolean isEncrypted(String s) {
    return s.startsWith("ENC(") && s.endsWith(")");
  }

  /**
   * Attempts to refresh an expired access token
   *
   * @param id The ID to use when storing tokens
   * @param sourceId The ID of the source using OAuth to use when storing tokens
   * @param clientId The client ID registered with the OAuth provider
   * @param clientSecret The client secret registered with the OAuth provider
   * @param discoveryUrl The URL where the OAuth provider's metadata is hosted
   * @param refreshToken The unexpired refresh token to use
   * @param metadata The OAuh provider's metadata
   * @return refreshed access token
   */
  private String refreshToken(
      String id,
      String sourceId,
      String clientId,
      String clientSecret,
      String discoveryUrl,
      String refreshToken,
      OIDCProviderMetadata metadata) {

    if (refreshToken == null || isExpired(refreshToken)) {
      LOGGER.debug("Error refreshing access token: unable to find an unexpired refresh token.");
      return null;
    }

    ClientAccessToken clientAccessToken;
    try {
      LOGGER.debug("Attempting to refresh the user's access token.");

      WebClient webClient = createWebClient(metadata.getTokenEndpointURI());
      Consumer consumer = new Consumer(clientId, clientSecret);
      AccessTokenGrant accessTokenGrant = new RefreshTokenGrant(refreshToken);
      clientAccessToken = OAuthClientUtils.getAccessToken(webClient, consumer, accessTokenGrant);
    } catch (OAuthServiceException e) {
      LOGGER.debug("Error refreshing access token.", e);
      return null;
    }

    // Validate new access token
    try {
      AccessToken accessToken = convertCxfAccessTokenToNimbusdsToken(clientAccessToken);
      OidcTokenValidator.validateAccessToken(accessToken, null, resourceRetriever, metadata, null);
    } catch (OidcValidationException e) {
      LOGGER.debug("Error validating access token.");
      return null;
    }

    // Store new tokens
    String newAccessToken = clientAccessToken.getTokenKey();
    String newRefreshToken = clientAccessToken.getRefreshToken();

    int status = tokenStorage.create(id, sourceId, newAccessToken, newRefreshToken, discoveryUrl);
    if (status != SC_OK) {
      LOGGER.warn("Error updating the token information.");
    }

    return newAccessToken;
  }

  /**
   * Checks if a token is expired
   *
   * @param token The token to test
   * @return true if the token has expired, false otherwise
   */
  private boolean isExpired(String token) {
    if (token == null) {
      return true;
    }

    String accessTokenString = new String(Base64.getDecoder().decode(token.split("\\.")[1]));
    Map<String, Object> map = GSON.fromJson(accessTokenString, MAP_STRING_TO_OBJECT_TYPE);
    long exp = (Long) map.get(EXP);
    long iat = (Long) map.get(IAT);

    long lifetime = exp - iat;
    long left = exp - Instant.now().getEpochSecond();
    long min = (long) Math.min(60, lifetime * 0.5);
    return left <= min;
  }

  /** Converts a {@link ClientAccessToken} to an {@link AccessToken} */
  private AccessToken convertCxfAccessTokenToNimbusdsToken(ClientAccessToken clientAccessToken) {
    Scope scope = new Scope();
    if (clientAccessToken.getApprovedScope() != null) {
      Arrays.stream(clientAccessToken.getApprovedScope().split("\\s+")).forEach(scope::add);
    }

    if (BEARER.equalsIgnoreCase(clientAccessToken.getTokenType())) {
      return new BearerAccessToken(
          clientAccessToken.getTokenKey(), clientAccessToken.getExpiresIn(), scope);
    } else {
      return new TypelessAccessToken(clientAccessToken.getTokenKey());
    }
  }

  @VisibleForTesting
  WebClient createWebClient(URI uri) {
    return WebClient.create(uri);
  }

  @VisibleForTesting
  void setResourceRetriever(ResourceRetriever resourceRetriever) {
    this.resourceRetriever = resourceRetriever;
  }
}
