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

import static ddf.catalog.plugin.OAuthPluginException.ErrorType.AUTH_SOURCE;
import static ddf.catalog.plugin.OAuthPluginException.ErrorType.NO_AUTH;
import static ddf.security.SecurityConstants.SECURITY_SUBJECT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.codice.ddf.security.token.storage.api.TokenStorage.CLIENT_ID;
import static org.codice.ddf.security.token.storage.api.TokenStorage.DISCOVERY_URL;
import static org.codice.ddf.security.token.storage.api.TokenStorage.EXPIRES_AT;
import static org.codice.ddf.security.token.storage.api.TokenStorage.SECRET;
import static org.codice.ddf.security.token.storage.api.TokenStorage.SOURCE_ID;
import static org.codice.ddf.security.token.storage.api.TokenStorage.STATE;
import static org.codice.ddf.security.token.storage.api.TokenStorage.USER_ID;
import static org.codice.gsonsupport.GsonTypeAdapters.MAP_STRING_TO_OBJECT_TYPE;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jose.util.ResourceRetriever;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.TypelessAccessToken;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.plugin.OAuthPluginException;
import ddf.catalog.plugin.PreFederatedQueryPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.source.OAuthFederatedSource;
import ddf.catalog.source.Source;
import ddf.security.Subject;
import ddf.security.SubjectUtils;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.oauth2.client.Consumer;
import org.apache.cxf.rs.security.oauth2.client.OAuthClientUtils;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenGrant;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.grants.refresh.RefreshTokenGrant;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.http.client.utils.URIBuilder;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.security.oidc.validator.OidcTokenValidator;
import org.codice.ddf.security.oidc.validator.OidcValidationException;
import org.codice.ddf.security.token.storage.api.TokenInformation;
import org.codice.ddf.security.token.storage.api.TokenStorage;
import org.codice.gsonsupport.GsonTypeAdapters;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * If a source is configured to use OAuth 2.0's Authorization Code flow, this plugin will verify
 * that the user has the required tokens to use OAuth 2.0 before proceeding to federate. If an
 * access token is present but has expired, this plugin will attempt to refresh it and update the
 * token storage with the new one. If the user is not logged in or needs to authorize the use of
 * their tokens against this source, an {@link OAuthPluginException} will be thrown.
 */
public class OAuthPlugin implements PreFederatedQueryPlugin {

  private static final Logger LOGGER = LoggerFactory.getLogger(OAuthPlugin.class);

  static final Gson GSON =
      new GsonBuilder()
          .disableHtmlEscaping()
          .registerTypeAdapterFactory(GsonTypeAdapters.LongDoubleTypeAdapter.FACTORY)
          .create();

  private static final String AUTHORIZE_SOURCE_ENDPOINT =
      SystemBaseUrl.EXTERNAL.constructUrl("/search/catalog/internal/oauth/auth");
  private static final String OAUTH_REDIRECT_URL =
      SystemBaseUrl.EXTERNAL.constructUrl("/search/catalog/internal/oauth");
  private static final String RESPONSE_TYPE = "response_type";
  private static final String CLIENT_ID_PARAM = "client_id";
  private static final String REDIRECT_URI = "redirect_uri";
  private static final String OPENID_SCOPE = "openid";
  private static final String CODE_FLOW = "code";
  private static final String BEARER = "Bearer";
  private static final String OAUTH = "oauth";
  private static final String SCOPE = "scope";
  private static final String EXP = "exp";
  private static final String IAT = "iat";
  private static final int STATE_EXP = 5;

  private TokenStorage tokenStorage;
  private ResourceRetriever resourceRetriever;

  public OAuthPlugin(TokenStorage tokenStorage) {
    this.tokenStorage = tokenStorage;
    resourceRetriever = new DefaultResourceRetriever();
  }

  /**
   * Verifies that a source configured to use OAuth has a valid access token to process and that the
   * user has authorized the use of their data against this source.
   *
   * @param source source being queried
   * @param input query request
   * @throws OAuthPluginException if the user's access token is not available or if the source is
   *     not authorized
   */
  @Override
  public QueryRequest process(Source source, QueryRequest input) throws StopProcessingException {
    OAuthFederatedSource oauthSource = getSource(source);
    if (oauthSource == null) {
      return input;
    }

    Object securityAssertion = input.getProperties().get(SECURITY_SUBJECT);
    if (!(securityAssertion instanceof Subject)) {
      LOGGER.warn("A user Subject is not available.");
      throw new StopProcessingException("A user Subject is not available.");
    }

    Subject subject = (Subject) securityAssertion;
    String userId = SubjectUtils.getEmailAddress(subject);
    if (userId == null) {
      userId = SubjectUtils.getName(subject);
    }

    TokenInformation.TokenEntry tokenEntry = tokenStorage.read(userId, oauthSource.getId());

    OIDCProviderMetadata metadata;
    try {
      metadata =
          OIDCProviderMetadata.parse(
              resourceRetriever
                  .retrieveResource(new URL(oauthSource.getOauthDiscoveryUrl()))
                  .getContent());
    } catch (OAuthServiceException | IOException | ParseException e) {
      LOGGER.error(
          "Unable to retrieve OAuth provider's metadata for the {} source.", oauthSource.getId());
      throw new StopProcessingException("Unable to retrieve OAuth provider's metadata.");
    }

    if (tokenEntry == null) {
      // See if the user already logged in to the oauth provider for a different source
      findExistingTokens(oauthSource, userId, metadata);
      throw createNoAuthException(
          oauthSource, userId, metadata, "the user's tokens were not found.");
    }

    // Verifies that the stored oauth provider is the same one as the one being queried in other
    // words the same OAuth provider is being used by the source (it is not an outdated token)
    if (!oauthSource.getOauthDiscoveryUrl().equals(tokenEntry.getDiscoveryUrl())) {
      // the discoveryUrl is different from the one stored - the user must login
      tokenStorage.delete(userId, oauthSource.getId());
      findExistingTokens(oauthSource, userId, metadata);
      throw createNoAuthException(
          oauthSource,
          userId,
          metadata,
          "the oauth provider information has been changed and is different from the one stored.");
    }

    verifyAccessToken(oauthSource, userId, tokenEntry, metadata);
    return input;
  }

  /**
   * Used to get the {@link OAuthFederatedSource} since we get a proxy.
   *
   * @param source - the proxy
   * @return the matching {@link OAuthFederatedSource}
   */
  @VisibleForTesting
  OAuthFederatedSource getSource(Source source) {
    Bundle bundle = FrameworkUtil.getBundle(OAuthPlugin.class);
    if (bundle == null) {
      return null;
    }

    BundleContext bundleContext = bundle.getBundleContext();
    if (bundleContext == null) {
      return null;
    }

    OAuthFederatedSource oauthSource;
    try {
      Collection<ServiceReference<OAuthFederatedSource>> implementers =
          bundleContext.getServiceReferences(OAuthFederatedSource.class, null);
      oauthSource =
          implementers
              .stream()
              .map(bundleContext::getService)
              .filter(s -> s.getId().equals(source.getId()))
              .filter(s -> s.getAuthenticationType().equals(OAUTH))
              .filter(s -> s.getOauthFlow().equals(CODE_FLOW))
              .findFirst()
              .orElse(null);
    } catch (InvalidSyntaxException e) {
      LOGGER.warn("Error getting source.", e);
      return null;
    }

    return oauthSource;
  }

  /**
   * Verify that the access token has not expired. If it has, attempts to refresh it and store the
   * new access and refresh tokens in the token storage.
   */
  private void verifyAccessToken(
      OAuthFederatedSource oauthSource,
      String userId,
      TokenInformation.TokenEntry tokenEntry,
      OIDCProviderMetadata metadata)
      throws StopProcessingException {

    String accessToken = tokenEntry.getAccessToken();
    String refreshToken = tokenEntry.getRefreshToken();

    if (accessToken != null && !isExpired(accessToken)) {
      return;
    }

    if (refreshToken == null || isExpired(refreshToken)) {
      findExistingTokens(oauthSource, userId, metadata);
      throw createNoAuthException(oauthSource, userId, metadata, "refreshing token has expired.");
    }

    refreshTokens(refreshToken, oauthSource, userId, metadata);
  }

  /**
   * Looks through the user's tokens to see if there are tokens from a different source connected to
   * the same OAuth provider. The discovery URLs need to match. If a match is found an authorize
   * source exception will be thrown so the user can authorize to query the new source instead of
   * logging in.
   */
  private void findExistingTokens(
      OAuthFederatedSource oauthSource, String userId, OIDCProviderMetadata metadata)
      throws StopProcessingException {
    TokenInformation tokenInformation = tokenStorage.read(userId);
    if (tokenInformation == null
        || !tokenInformation.getDiscoveryUrls().contains(oauthSource.getOauthDiscoveryUrl())) {
      return;
    }

    // Verify that an unexpired token exists
    List<TokenInformation.TokenEntry> matchingTokenEntries =
        tokenInformation
            .getTokenEntries()
            .values()
            .stream()
            .filter(entry -> entry.getDiscoveryUrl().equals(oauthSource.getOauthDiscoveryUrl()))
            .collect(Collectors.toList());

    TokenInformation.TokenEntry tokenEntry =
        matchingTokenEntries
            .stream()
            .filter(entry -> entry.getAccessToken() != null)
            .filter(entry -> !isExpired(entry.getAccessToken()))
            .findAny()
            .orElse(null);

    if (tokenEntry == null) {
      // does one with a valid refresh token exist
      tokenEntry =
          matchingTokenEntries
              .stream()
              .filter(entry -> entry.getRefreshToken() != null)
              .filter(entry -> !isExpired(entry.getRefreshToken()))
              .findAny()
              .orElse(null);

      if (tokenEntry == null) {
        return;
      }

      refreshTokens(tokenEntry.getRefreshToken(), oauthSource, userId, metadata);
    }

    LOGGER.debug(
        "Unable to process query. The user needs to authorize to query the {} source.",
        oauthSource.getId());

    Map<String, String> parameters = new HashMap<>();
    parameters.put(USER_ID, userId);
    parameters.put(SOURCE_ID, oauthSource.getId());
    parameters.put(DISCOVERY_URL, oauthSource.getOauthDiscoveryUrl());
    throw new OAuthPluginException(
        oauthSource.getId(),
        buildUrl(AUTHORIZE_SOURCE_ENDPOINT, parameters),
        AUTHORIZE_SOURCE_ENDPOINT,
        parameters,
        AUTH_SOURCE);
  }

  /**
   * Checks if a token is expired
   *
   * @param token user's token
   * @return true if the token has expired, false otherwise
   */
  private boolean isExpired(String token) {
    String accessTokenString = new String(Base64.getDecoder().decode(token.split("\\.")[1]));
    Map<String, Object> map = GSON.fromJson(accessTokenString, MAP_STRING_TO_OBJECT_TYPE);
    long exp = (Long) map.get(EXP);
    long iat = (Long) map.get(IAT);

    long lifetime = exp - iat;
    long left = exp - Instant.now().getEpochSecond();
    long min = (long) Math.min(60, lifetime * 0.5);
    return left <= min;
  }

  /**
   * Attempts to refresh the user's access token and saves the new tokens in the token storage
   *
   * @param refreshToken refresh token used to refresh access token
   * @param oauthSource source being queried
   * @throws OAuthPluginException if the access token could not be renewed
   */
  private void refreshTokens(
      String refreshToken,
      OAuthFederatedSource oauthSource,
      String userId,
      OIDCProviderMetadata metadata)
      throws StopProcessingException {
    if (refreshToken == null) {
      throw createNoAuthException(
          oauthSource, userId, metadata, "unable to find the user's refresh token.");
    }

    ClientAccessToken clientAccessToken;
    try {
      LOGGER.debug("Attempting to refresh the user's access token.");

      WebClient webClient = createWebclient(metadata.getTokenEndpointURI().toURL().toString());
      Consumer consumer =
          new Consumer(oauthSource.getOauthClientId(), oauthSource.getOauthClientSecret());
      AccessTokenGrant accessTokenGrant = new RefreshTokenGrant(refreshToken);
      clientAccessToken = OAuthClientUtils.getAccessToken(webClient, consumer, accessTokenGrant);
    } catch (OAuthServiceException e) {
      findExistingTokens(oauthSource, userId, metadata);

      String error = e.getError() != null ? e.getError().getError() : "";
      throw createNoAuthException(
          oauthSource, userId, metadata, "failed to refresh access token " + error);
    } catch (MalformedURLException e) {
      throw createNoAuthException(
          oauthSource, userId, metadata, "malformed token endpoint URL. " + e.getMessage());
    }

    // Validate new access token
    try {
      AccessToken accessToken = convertCxfAccessTokenToNimbusdsToken(clientAccessToken);
      OidcTokenValidator.validateAccessToken(accessToken, null, resourceRetriever, metadata, null);
    } catch (OidcValidationException e) {
      throw createNoAuthException(
          oauthSource, userId, metadata, "failed to validate refreshed access token.");
    }

    // Store new tokens
    String newAccessToken = clientAccessToken.getTokenKey();
    String newRefreshToken = clientAccessToken.getRefreshToken();

    int status =
        tokenStorage.create(
            userId,
            oauthSource.getId(),
            newAccessToken,
            newRefreshToken,
            oauthSource.getOauthDiscoveryUrl());
    if (status != SC_OK) {
      LOGGER.warn("Error updating the token information.");
    }
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

  /**
   * Creates an {@link OAuthPluginException} to throw when the user needs to login. This method will
   * construct a url to hit the OAuth provider with a redirect URL to DDF's OAuth Application. The
   * URL will consist of a state parameter that will correspond to an entry in the {@link
   * TokenStorage}'s state map which will hold the user's and OAuth provider's information.
   */
  private OAuthPluginException createNoAuthException(
      OAuthFederatedSource oauthSource, String userId, OIDCProviderMetadata metadata, String reason)
      throws StopProcessingException {

    LOGGER.debug(
        "Unable to process query. The {} source requires OAuth 2.0 but {}.",
        oauthSource.getId(),
        reason);

    String state = UUID.randomUUID().toString();
    Map<String, Object> stateMap = new HashMap<>();
    stateMap.put(USER_ID, userId);
    stateMap.put(SOURCE_ID, oauthSource.getId());
    stateMap.put(CLIENT_ID, oauthSource.getOauthClientId());
    stateMap.put(SECRET, oauthSource.getOauthClientSecret());
    stateMap.put(DISCOVERY_URL, oauthSource.getOauthDiscoveryUrl());
    stateMap.put(EXPIRES_AT, Instant.now().plus(STATE_EXP, ChronoUnit.MINUTES).getEpochSecond());
    tokenStorage.getStateMap().put(state, stateMap);

    Map<String, String> parameters = new HashMap<>();
    parameters.put(RESPONSE_TYPE, CODE_FLOW);
    parameters.put(CLIENT_ID_PARAM, oauthSource.getOauthClientId());
    parameters.put(SCOPE, OPENID_SCOPE);
    parameters.put(REDIRECT_URI, OAUTH_REDIRECT_URL);
    parameters.put(STATE, state);

    String url = metadata.getAuthorizationEndpointURI().toString();
    return new OAuthPluginException(
        oauthSource.getId(), buildUrl(url, parameters), url, parameters, NO_AUTH);
  }

  private String buildUrl(String baseUrl, Map<String, String> parameters) {
    try {
      URIBuilder uriBuilder = new URIBuilder(baseUrl);
      parameters.forEach(uriBuilder::addParameter);
      return uriBuilder.build().toURL().toString();
    } catch (URISyntaxException | MalformedURLException e) {
      return null;
    }
  }

  @VisibleForTesting
  WebClient createWebclient(String url) {
    return WebClient.create(url);
  }

  @VisibleForTesting
  void setResourceRetriever(ResourceRetriever resourceRetriever) {
    this.resourceRetriever = resourceRetriever;
  }
}
