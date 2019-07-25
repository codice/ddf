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
package org.codice.ddf.security.handler.oidc;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.pac4j.oidc.config.OidcConfiguration.IMPLICIT_FLOWS;

import com.google.common.annotations.VisibleForTesting;
import java.util.Map;
import org.codice.ddf.security.handler.api.OidcHandlerConfiguration;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.oidc.client.AzureAdClient;
import org.pac4j.oidc.client.GoogleOidcClient;
import org.pac4j.oidc.client.KeycloakOidcClient;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.AzureAdOidcConfiguration;
import org.pac4j.oidc.config.KeycloakOidcConfiguration;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.logout.OidcLogoutActionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OidcHandlerConfigurationImpl implements OidcHandlerConfiguration {
  private static final Logger LOGGER = LoggerFactory.getLogger(OidcHandlerConfigurationImpl.class);

  public static final String DEFAULT_CALLBACK_URL = "https://localhost:8993/search";

  public static final String IDP_TYPE_KEY = "idpType";
  public static final String CLIENT_ID_KEY = "clientId";
  public static final String REALM_KEY = "realm";
  public static final String SECRET_KEY = "secret";
  public static final String DISCOVERY_URI_KEY = "discoveryUri";
  public static final String BASE_URI_KEY = "baseUri";
  public static final String SCOPE_KEY = "scope";
  public static final String USE_NONCE_KEY = "useNonce";
  public static final String RESPONSE_TYPE_KEY = "responseType";
  public static final String RESPONSE_MODE_KEY = "responseMode";
  public static final String LOGOUT_URI_KEY = "logoutUri";

  private String idpType;
  private String clientId;
  private String realm;
  private String secret;
  private String discoveryUri;
  private String baseUri;
  private String scope;
  private boolean useNonce;
  private String responseType;
  private String responseMode;
  private String logoutUri;

  private OidcConfiguration oidcConfiguration;

  public void setProperties(Map<String, Object> properties) {
    if (properties == null || properties.isEmpty()) {
      LOGGER.warn("Received null or empty properties. Cannot update.");
      return;
    }

    idpType = (String) properties.getOrDefault(IDP_TYPE_KEY, idpType);
    clientId = (String) properties.getOrDefault(CLIENT_ID_KEY, idpType);
    realm = (String) properties.getOrDefault(REALM_KEY, realm);
    secret = (String) properties.getOrDefault(SECRET_KEY, secret);
    discoveryUri = (String) properties.getOrDefault(DISCOVERY_URI_KEY, discoveryUri);
    baseUri = (String) properties.getOrDefault(BASE_URI_KEY, baseUri);
    scope = (String) properties.getOrDefault(SCOPE_KEY, scope);
    useNonce = (boolean) properties.getOrDefault(USE_NONCE_KEY, useNonce);
    responseType = (String) properties.getOrDefault(RESPONSE_TYPE_KEY, responseType);
    responseMode = (String) properties.getOrDefault(RESPONSE_MODE_KEY, responseMode);
    logoutUri = (String) properties.getOrDefault(LOGOUT_URI_KEY, logoutUri);

    // TODO - Remove if fragment response_mode is supported
    if (IMPLICIT_FLOWS.contains(responseType)) {
      responseMode = "form_post";
    }

    oidcConfiguration = createOidcConfiguration(idpType, realm, baseUri);

    oidcConfiguration.setClientId(clientId);
    oidcConfiguration.setDiscoveryURI(discoveryUri);
    oidcConfiguration.setSecret(secret);
    oidcConfiguration.setScope(scope);
    oidcConfiguration.setResponseType(responseType);
    oidcConfiguration.setResponseMode(responseMode);
    oidcConfiguration.setUseNonce(useNonce);
    oidcConfiguration.setLogoutUrl(logoutUri);
    oidcConfiguration.setWithState(true);

    try {
      testConnection();
    } catch (TechnicalException e) {
      LOGGER.warn(
          "Failed to validate OIDC handler configuration. Please review configuration and ensure the auth server is reachable",
          e);
    }
  }

  @Override
  public OidcConfiguration getOidcConfiguration() {
    try {
      oidcConfiguration.init();
    } catch (TechnicalException e) {
      LOGGER.warn(
          "OIDC Configuration could not initialize; this may be due to a configuration issue. See the configuration under \"OIDC Handler Configuration\" in the Admin Console");
      throw e;
    }

    return oidcConfiguration;
  }

  @Override
  public OidcClient getOidcClient(String callBackUri) {
    OidcClient oidcClient = createOidcClient(idpType, oidcConfiguration, callBackUri);

    try {
      oidcClient.init();
    } catch (TechnicalException e) {
      LOGGER.warn(
          "OIDC Client could not initialize; this may be due to a configuration issue. See the configuration under \"OIDC Handler Configuration\" in the Admin Console");
      throw e;
    }

    return oidcClient;
  }

  @Override
  public OidcLogoutActionBuilder getOidcLogoutActionBuilder() {
    oidcConfiguration.init();
    return new OidcLogoutActionBuilder(oidcConfiguration);
  }

  @Override
  public void testConnection() {
    getOidcConfiguration();
    getOidcClient(DEFAULT_CALLBACK_URL);
  }

  @VisibleForTesting
  OidcConfiguration createOidcConfiguration(String idpType, String realm, String baseUri) {
    OidcConfiguration configuration;

    if ("Keycloak".equals(idpType)) {
      KeycloakOidcConfiguration keycloakOidcConfiguration = new KeycloakOidcConfiguration();
      keycloakOidcConfiguration.setRealm(realm);
      keycloakOidcConfiguration.setBaseUri(baseUri);
      configuration = keycloakOidcConfiguration;
    } else if ("Azure".equals(idpType)) {
      AzureAdOidcConfiguration azureAdOidcConfiguration = new AzureAdOidcConfiguration();
      azureAdOidcConfiguration.setTenant(realm);
      configuration = azureAdOidcConfiguration;
    } else {
      configuration = new OidcConfiguration();
    }

    return configuration;
  }

  @VisibleForTesting
  OidcClient createOidcClient(
      String idpType, OidcConfiguration oidcConfiguration, String callBackUri) {
    OidcClient oidcClient;

    if ("Keycloak".equals(idpType)) {
      oidcClient = new KeycloakOidcClient((KeycloakOidcConfiguration) oidcConfiguration);
    } else if ("Azure".equals(idpType)) {
      oidcClient = new AzureAdClient((AzureAdOidcConfiguration) oidcConfiguration);
    } else if ("Google".equals(idpType)) {
      oidcClient = new GoogleOidcClient(oidcConfiguration);
    } else {
      oidcClient = new OidcClient<>(oidcConfiguration);
    }

    oidcClient.setName(oidcConfiguration.getClientId());
    if (isBlank(callBackUri)) {
      oidcClient.setCallbackUrl(DEFAULT_CALLBACK_URL);
    } else {
      // Strip additional query parameters from the callBackUri
      String uri = callBackUri.split("&")[0];
      oidcClient.setCallbackUrl(uri);
    }

    return oidcClient;
  }
}
