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
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import ddf.security.Subject;
import ddf.security.SubjectUtils;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import javax.ws.rs.core.Form;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.util.Strings;
import org.codice.ddf.security.oidc.validator.OidcTokenValidator;
import org.codice.ddf.security.oidc.validator.OidcValidationException;
import org.codice.ddf.security.token.storage.api.TokenInformation;
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

  private static final String CLIENT_CREDENTIALS = "client_credentials";
  private static final String ACCESS_TOKEN = "access_token";
  private static final String GRANT_TYPE = "grant_type";
  private static final String ID_TOKEN = "id_token";
  private static final String BEARER = "Bearer ";
  private static final String BASIC = "Basic ";
  private static final String EXP = "exp";

  private ResourceRetriever resourceRetriever;
  private TokenStorage tokenStorage;

  public OAuthSecurityImpl(TokenStorage tokenStorage) {
    this.tokenStorage = tokenStorage;
    resourceRetriever = new DefaultResourceRetriever();
  }

  /**
   * Gets the user's access token from the token storage to set it to the OAUTH header.
   *
   * @param client Non-null client to set the access token on.
   * @param subject subject used to get the user's id (email or username)
   * @param sourceId the id of the source using OAuth needed to get the correct tokens
   * @param clientId The client ID registered with the OAuth provider
   * @param clientSecret The client secret registered with the OAuth provider
   * @param discoveryUrl the metadata URL of the OAuth provider
   */
  public void setUserTokenOnClient(
      Client client,
      Subject subject,
      String sourceId,
      String clientId,
      String clientSecret,
      String discoveryUrl) {
    if (client == null || subject == null || Strings.isBlank(sourceId)) {
      return;
    }

    String userId = SubjectUtils.getEmailAddress(subject);
    if (userId == null) {
      userId = SubjectUtils.getName(subject);
    }

    TokenInformation.TokenEntry tokenEntry = tokenStorage.read(userId, sourceId);
    if (tokenEntry == null) {
      return;
    }

    LOGGER.debug("Adding access token to OAUTH header.");
    client.header(OAUTH, BEARER + tokenEntry.getAccessToken());
  }

  /**
   * Gets the system's access token from the token storage to set it to the OAUTH header. If one can
   * not be found, retrieves the system's access token from the configured OAuth provider. header
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

    TokenInformation.TokenEntry tokenEntry =
        tokenStorage.read(encodedClientIdSecret, CLIENT_CREDENTIALS);

    if (tokenEntry == null || !discoveryUrl.equalsIgnoreCase(tokenEntry.getDiscoveryUrl())) {
      String accessToken = getNewAccessToken(encodedClientIdSecret, discoveryUrl);
      if (accessToken == null) {
        return;
      }

      client.header(OAUTH, BEARER + accessToken);
      return;
    }

    if (isExpired(tokenEntry.getAccessToken())) {
      String accessToken = getNewAccessToken(encodedClientIdSecret, discoveryUrl);
      if (accessToken == null) {
        return;
      }

      client.header(OAUTH, BEARER + accessToken);
      return;
    }

    String accessToken = tokenEntry.getAccessToken();
    client.header(OAUTH, BEARER + accessToken);
  }

  /**
   * Gets an access token from the configured OAuth provider, saves it to the token storage and
   * returns it
   *
   * @param encodedClientIdSecret - the base 64 encoded clientId:secret
   * @param discoveryUrl - the URL where the Oauth provider's metadata is hosted
   * @return a client access token or null if one could not be returned
   */
  private String getNewAccessToken(String encodedClientIdSecret, String discoveryUrl) {
    OIDCProviderMetadata metadata;
    try {
      metadata =
          OIDCProviderMetadata.parse(
              resourceRetriever.retrieveResource(new URL(discoveryUrl)).getContent());
    } catch (IOException | ParseException e) {
      LOGGER.error("Unable to retrieve OAuth provider's metadata.", e);
      return null;
    }

    WebClient webClient = createWebClient(metadata.getTokenEndpointURI());
    webClient.header(AUTHORIZATION, BASIC + encodedClientIdSecret);
    webClient.accept(APPLICATION_JSON);

    Form formParam = new Form(GRANT_TYPE, CLIENT_CREDENTIALS);
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
      return null;
    }

    Map<String, String> map = GSON.fromJson(body, MAP_STRING_TO_OBJECT_TYPE);
    String idToken = map.get(ID_TOKEN);
    String accessToken = map.get(ACCESS_TOKEN);

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
    int status =
        tokenStorage.create(
            encodedClientIdSecret, CLIENT_CREDENTIALS, accessToken, null, discoveryUrl);
    if (status != SC_OK) {
      LOGGER.debug("Error storing user token.");
    }
    return accessToken;
  }

  /**
   * Checks if an access token is expired
   *
   * @param accessToken user's access token
   * @return true if the access token has expired, false otherwise
   */
  private boolean isExpired(String accessToken) {
    String accessTokenString = new String(Base64.getDecoder().decode(accessToken.split("\\.")[1]));
    Map<String, Object> map = GSON.fromJson(accessTokenString, MAP_STRING_TO_OBJECT_TYPE);
    long exp = (Long) map.get(EXP);
    return exp - Instant.now().getEpochSecond() <= 60;
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
