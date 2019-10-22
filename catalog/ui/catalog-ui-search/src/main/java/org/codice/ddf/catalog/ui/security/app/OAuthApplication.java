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

import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.apache.http.HttpStatus.SC_OK;
import static org.codice.ddf.security.token.storage.api.TokenStorage.CLIENT_ID;
import static org.codice.ddf.security.token.storage.api.TokenStorage.DISCOVERY_URL;
import static org.codice.ddf.security.token.storage.api.TokenStorage.SECRET;
import static org.codice.ddf.security.token.storage.api.TokenStorage.SOURCE_ID;
import static org.codice.ddf.security.token.storage.api.TokenStorage.STATE;
import static org.codice.ddf.security.token.storage.api.TokenStorage.USER_ID;
import static spark.Spark.after;
import static spark.Spark.get;

import com.google.common.annotations.VisibleForTesting;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jose.util.ResourceRetriever;
import com.nimbusds.jwt.JWT;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import org.apache.http.HttpStatus;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.security.oidc.resolver.OidcCredentialsResolver;
import org.codice.ddf.security.oidc.validator.OidcTokenValidator;
import org.codice.ddf.security.oidc.validator.OidcValidationException;
import org.codice.ddf.security.token.storage.api.TokenInformation;
import org.codice.ddf.security.token.storage.api.TokenStorage;
import org.pac4j.core.exception.TechnicalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.QueryParamsMap;
import spark.Response;
import spark.servlet.SparkApplication;

public class OAuthApplication implements SparkApplication {

  private static final Logger LOGGER = LoggerFactory.getLogger(OAuthApplication.class);

  private static final String CODE = "code";
  private static final String REDIRECT_URL =
      SystemBaseUrl.EXTERNAL.constructUrl("/search/catalog/internal/oauth");

  private final TokenStorage tokenStorage;
  private ResourceRetriever resourceRetriever;

  public OAuthApplication(TokenStorage tokenStorage) {
    this.tokenStorage = tokenStorage;
    this.resourceRetriever = new DefaultResourceRetriever();
  }

  @Override
  public void init() {
    /*
     Endpoint called with a temporary authorization code when a user signs in to an oauth
     provider. This endpoint will make a call to the oauth provider's token endpoint to exchange
     the code for tokens and stores the access and refresh tokens in the token storage. Since the
     login to the oauth provider is done in a new tab, a call to this endpoint will close that
     tab.
    */
    get(
        "/oauth",
        (req, res) -> {
          QueryParamsMap paramsMap = req.queryMap();
          String state = paramsMap.get(STATE).value();
          String code = paramsMap.get(CODE).value();

          if (state == null || code == null) {
            LOGGER.warn("Unable to process unknown state and/or code.");
            return closeBrowser(res);
          }

          Map<String, Object> stateMap = tokenStorage.getStateMap().remove(state);
          if (stateMap == null) {
            LOGGER.warn("Unable to process unknown state.");
            return closeBrowser(res);
          }

          return processCodeFlow(res, code, stateMap);
        });

    /*
     Endpoint called when a user's tokens are available but the user needs to authorize the use of
     their tokens against a given source. This endpoint will store the existing tokens in the
     token storage under the authorized source.
    */
    get(
        "/oauth/auth",
        (req, res) -> {
          QueryParamsMap paramsMap = req.queryMap();
          String userId = paramsMap.get(USER_ID).value();
          String sourceId = paramsMap.get(SOURCE_ID).value();
          String discoveryUrl = paramsMap.get(DISCOVERY_URL).value();

          if (userId == null || sourceId == null || discoveryUrl == null) {
            LOGGER.warn("Unable to process unknown user state.");
            return closeBrowser(res);
          }

          updateAuthorizedSource(userId, sourceId, discoveryUrl);
          return "";
        });

    after((req, res) -> res.type(TEXT_HTML));
  }

  /**
   * Gets tokens from an existing source with the same discovery url and saves it to the given
   * source id
   *
   * @param userId the user's unique identifier (email or username)
   * @param sourceId the source to save the tokens to
   * @param discoveryUrl the metadata url of the OAuth provider
   */
  @VisibleForTesting
  void updateAuthorizedSource(String userId, String sourceId, String discoveryUrl) {
    TokenInformation tokenInformation = tokenStorage.read(userId);

    TokenInformation.TokenEntry tokenEntry =
        tokenInformation
            .getTokenEntries()
            .values()
            .stream()
            .filter(entry -> discoveryUrl.equals(entry.getDiscoveryUrl()))
            .findFirst()
            .orElse(null);

    if (tokenEntry != null) {
      int status =
          tokenStorage.create(
              userId,
              sourceId,
              tokenEntry.getAccessToken(),
              tokenEntry.getRefreshToken(),
              discoveryUrl);
      if (status != SC_OK) {
        LOGGER.warn("Error updating user's authorized sources.");
      }
    }
  }

  /**
   * Processes tokens received through the authorization code flow.
   *
   * @param res - response
   * @param code - authorization code
   * @param state - map containing user and OAuth provider information
   */
  @VisibleForTesting
  String processCodeFlow(Response res, String code, Map<String, Object> state) {

    String userId = (String) state.get(USER_ID);
    String sourceId = (String) state.get(SOURCE_ID);
    String clientId = (String) state.get(CLIENT_ID);
    String clientSecret = (String) state.get(SECRET);
    String discoveryUrl = (String) state.get(DISCOVERY_URL);

    if (clientId == null || clientSecret == null) {
      LOGGER.error("Unable to find client id and secret for Oauth provider.");
      return closeBrowser(res);
    }

    OIDCProviderMetadata metadata;
    OIDCTokens oidcTokens;
    try {
      metadata =
          OIDCProviderMetadata.parse(
              resourceRetriever.retrieveResource(new URL(discoveryUrl)).getContent());
      oidcTokens = getTokens(code, clientId, clientSecret, metadata);

    } catch (TechnicalException | IOException | com.nimbusds.oauth2.sdk.ParseException e) {
      LOGGER.warn("Error getting tokens.", e);
      return closeBrowser(res);
    }

    if (oidcTokens == null) {
      LOGGER.warn("Error getting tokens.");
      return closeBrowser(res);
    }

    JWT idToken = oidcTokens.getIDToken();
    AccessToken accessToken = oidcTokens.getAccessToken();
    RefreshToken refreshToken = oidcTokens.getRefreshToken();

    try {
      OidcTokenValidator.validateAccessToken(
          accessToken, idToken, resourceRetriever, metadata, null);
    } catch (OidcValidationException e) {
      LOGGER.warn("Error validating access token.", e);
      return closeBrowser(res);
    }

    int status =
        tokenStorage.create(
            userId, sourceId, accessToken.getValue(), refreshToken.getValue(), discoveryUrl);
    if (status != SC_OK) {
      LOGGER.warn("Error storing user token.");
    }
    return closeBrowser(res);
  }

  private String closeBrowser(Response res) {
    res.status(HttpStatus.SC_OK);
    res.type(TEXT_HTML);
    return "<html><body><p>Thank you for signing in. Please close this window if it doesn't close automatically.</p>"
        + "<script>window.close()</script></body></html>";
  }

  @VisibleForTesting
  OIDCTokens getTokens(
      String code, String clientId, String clientSecret, OIDCProviderMetadata metadata)
      throws IOException, ParseException {
    ClientAuthentication clientAuthentication =
        new ClientSecretBasic(new ClientID(clientId), new Secret(clientSecret));

    AuthorizationGrant grant =
        new AuthorizationCodeGrant(new AuthorizationCode(code), URI.create(REDIRECT_URL));
    return OidcCredentialsResolver.getOidcTokens(grant, metadata, clientAuthentication);
  }

  @VisibleForTesting
  void setResourceRetriever(ResourceRetriever resourceRetriever) {
    this.resourceRetriever = resourceRetriever;
  }
}
