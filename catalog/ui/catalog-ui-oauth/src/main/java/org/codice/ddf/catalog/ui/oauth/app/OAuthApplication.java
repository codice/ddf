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
package org.codice.ddf.catalog.ui.oauth.app;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.HttpHeaders.LOCATION;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_SEE_OTHER;
import static org.codice.ddf.security.token.storage.api.TokenStorage.CLIENT_ID;
import static org.codice.ddf.security.token.storage.api.TokenStorage.DISCOVERY_URL;
import static org.codice.ddf.security.token.storage.api.TokenStorage.SECRET;
import static org.codice.ddf.security.token.storage.api.TokenStorage.SOURCE_ID;
import static org.codice.ddf.security.token.storage.api.TokenStorage.STATE;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.servlet.http.HttpSession;
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
  private static final String REDIRECT_URI = "redirect_uri";
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
     Endpoint called with a temporary authorization code when a user signs in to an OAuth
     provider. This endpoint will make a call to the OAuth provider's token endpoint to exchange
     the code for tokens and stores the access and refresh tokens in the token storage. Since the
     login to the OAuth provider is done in a new tab, a call to this endpoint will close that
     tab.
    */
    get(
        "/",
        (req, res) -> {
          QueryParamsMap paramsMap = req.queryMap();
          String state = paramsMap.get(STATE).value();
          String code = paramsMap.get(CODE).value();
          String redirectUri = paramsMap.get(REDIRECT_URI).value();

          if (state == null || code == null) {
            LOGGER.warn("Unable to process unknown state and/or code.");
            return closeBrowser(res);
          }

          Map<String, Object> stateMap = tokenStorage.getStateMap().remove(state);
          if (stateMap == null) {
            LOGGER.warn("Unable to process unknown state.");
            return closeBrowser(res);
          }

          HttpSession session = req.raw().getSession(false);
          if (session == null) {
            LOGGER.error("The user's session is not available.");
            return closeBrowser(res);
          }

          String sessionId = session.getId();
          if (sessionId == null) {
            LOGGER.error("The user's session ID is not available.");
            return closeBrowser(res);
          }

          return processCodeFlow(res, sessionId, code, stateMap, redirectUri);
        });

    /*
     Endpoint called when a user's tokens are available but the user needs to authorize the use of
     their tokens against a given source. This endpoint will store the existing tokens in the
     token storage under the authorized source.
    */
    get(
        "/auth",
        (req, res) -> {
          QueryParamsMap paramsMap = req.queryMap();
          String sourceId = paramsMap.get(SOURCE_ID).value();
          String redirectUri = paramsMap.get(REDIRECT_URI).value();
          String discoveryUrl = paramsMap.get(DISCOVERY_URL).value();

          if (sourceId == null || discoveryUrl == null) {
            LOGGER.warn("Unable to process unknown user state.");
            return "";
          }

          HttpSession session = req.raw().getSession(false);
          if (session == null) {
            LOGGER.warn("The user's session is not available.");
            return "";
          }

          String sessionId = session.getId();
          if (sessionId == null) {
            LOGGER.warn("The user's session ID is not available.");
            return "";
          }

          String accessToken = updateAuthorizedSource(sessionId, sourceId, discoveryUrl);

          if (redirectUri != null && accessToken != null) {
            return closeBrowser(res, redirectUri, accessToken);
          }
          return "";
        });
  }

  /**
   * Gets tokens from an existing source with the same discovery url and saves it to the given
   * source id
   *
   * @param sessionId the session ID used to store tokens
   * @param sourceId the source to save the tokens to
   * @param discoveryUrl the metadata url of the OAuth provider
   * @return the corresponding access token
   */
  @VisibleForTesting
  String updateAuthorizedSource(String sessionId, String sourceId, String discoveryUrl) {
    TokenInformation tokenInformation = tokenStorage.read(sessionId);

    TokenInformation.TokenEntry tokenEntry =
        tokenInformation
            .getTokenEntries()
            .values()
            .stream()
            .filter(entry -> discoveryUrl.equals(entry.getDiscoveryUrl()))
            .findFirst()
            .orElse(null);

    if (tokenEntry == null) {
      return null;
    }

    int status =
        tokenStorage.create(
            sessionId,
            sourceId,
            tokenEntry.getAccessToken(),
            tokenEntry.getRefreshToken(),
            discoveryUrl);
    if (status != SC_OK) {
      LOGGER.warn("Error updating user's authorized sources.");
    }

    return tokenEntry.getAccessToken();
  }

  /**
   * Processes tokens received through the authorization code flow.
   *
   * @param res - response
   * @param sessionId - the session ID used to store tokens
   * @param code - authorization code
   * @param state - map containing user and OAuth provider information
   * @param redirectUri - the uri to redirect to
   */
  @VisibleForTesting
  String processCodeFlow(
      Response res, String sessionId, String code, Map<String, Object> state, String redirectUri) {

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
      oidcTokens = getTokens(code, clientId, clientSecret, redirectUri, metadata);

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

    String refreshTokenValue = null;
    if (refreshToken != null) {
      refreshTokenValue = refreshToken.getValue();
    }

    String accessTokenValue = accessToken.getValue();
    int status =
        tokenStorage.create(sessionId, sourceId, accessTokenValue, refreshTokenValue, discoveryUrl);
    if (status != SC_OK) {
      LOGGER.warn("Error storing user token.");
    }

    if (redirectUri != null) {
      return closeBrowser(res, redirectUri, accessTokenValue);
    }

    return closeBrowser(res);
  }

  private String closeBrowser(Response res) {
    res.status(SC_OK);
    res.type(TEXT_HTML);
    return "<html><body><p>Thank you for signing in. Please close this window if it doesn't close automatically.</p>"
        + "<script>window.close()</script></body></html>";
  }

  private String closeBrowser(Response res, String redirectUri, String accessToken) {
    res.status(SC_SEE_OTHER);
    res.header(LOCATION, redirectUri);
    res.header(AUTHORIZATION, "Bearer " + accessToken);
    return "";
  }

  @VisibleForTesting
  OIDCTokens getTokens(
      String code,
      String clientId,
      String clientSecret,
      String redirectUri,
      OIDCProviderMetadata metadata)
      throws IOException, ParseException {
    ClientAuthentication clientAuthentication =
        new ClientSecretBasic(new ClientID(clientId), new Secret(clientSecret));

    String redirect = REDIRECT_URL;
    if (redirectUri != null) {
      redirect =
          redirect.concat(
              "?"
                  + REDIRECT_URI
                  + "="
                  + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8.name()));
    }

    AuthorizationGrant grant =
        new AuthorizationCodeGrant(new AuthorizationCode(code), URI.create(redirect));
    return OidcCredentialsResolver.getOidcTokens(grant, metadata, clientAuthentication);
  }

  @VisibleForTesting
  void setResourceRetriever(ResourceRetriever resourceRetriever) {
    this.resourceRetriever = resourceRetriever;
  }
}
