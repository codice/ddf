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
package org.codice.ddf.security.handler.oauth;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.pac4j.core.context.WebContext;
import org.pac4j.oauth.config.OAuth20Configuration;
import org.pac4j.oidc.credentials.OidcCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomOAuthCredentialsExtractor {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(CustomOAuthCredentialsExtractor.class);

  public OidcCredentials getOauthCredentialsAsOidcCredentials(final WebContext context) {
    OidcCredentials credentials = new OidcCredentials();

    try {
      final String codeParam = context.getRequestParameter(OAuth20Configuration.OAUTH_CODE);
      if (codeParam != null) {
        credentials.setCode(
            new AuthorizationCode(URLDecoder.decode(codeParam, StandardCharsets.UTF_8.name())));
      } else {
        LOGGER.debug("No OAuth2 code found on request.");
      }

      final String accessTokenParam = context.getRequestParameter("access_token");
      final String accessTokenHeader = getAccessTokenFromHeader(context);
      final String accessToken = accessTokenParam != null ? accessTokenParam : accessTokenHeader;
      if (isNotBlank(accessToken)) {
        credentials.setAccessToken(
            new BearerAccessToken(URLDecoder.decode(accessToken, StandardCharsets.UTF_8.name())));
      } else {
        LOGGER.debug("No OAuth2 access token found on request.");
      }
    } catch (UnsupportedEncodingException e) {
      LOGGER.debug("Error decoding the authorization code/access token from url parameters.", e);
    }

    return credentials;
  }

  private String getAccessTokenFromHeader(WebContext context) {
    String authorizationHeader = context.getRequestHeader("Authorization");
    String[] authorizationArray = null;
    if (authorizationHeader != null) {
      authorizationArray = authorizationHeader.split(" ");
    }

    if (authorizationArray != null
        && "bearer".equalsIgnoreCase(authorizationArray[0])
        && authorizationArray.length == 2) {
      return authorizationArray[1];
    }
    return null;
  }
}
