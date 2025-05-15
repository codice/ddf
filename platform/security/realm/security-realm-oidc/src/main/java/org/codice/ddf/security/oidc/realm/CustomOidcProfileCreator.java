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

import static org.pac4j.core.profile.AttributeLocation.PROFILE_ATTRIBUTE;
import static org.pac4j.core.util.CommonHelper.assertNotNull;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import java.util.Map;
import java.util.Optional;
import org.apache.shiro.authc.AuthenticationException;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.profile.ProfileHelper;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.core.profile.jwt.JwtClaims;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.credentials.OidcCredentials;
import org.pac4j.oidc.profile.OidcProfile;
import org.pac4j.oidc.profile.creator.OidcProfileCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomOidcProfileCreator extends OidcProfileCreator {

  private static final Logger LOGGER = LoggerFactory.getLogger(CustomOidcProfileCreator.class);

  public CustomOidcProfileCreator(OidcConfiguration configuration, OidcClient client) {
    super(configuration, client);
  }

  @Override
  public Optional<UserProfile> create(CallContext callContext, Credentials cred) {
    init();
    OidcCredentials credentials = (OidcCredentials) cred;
    final OidcProfile profile = (OidcProfile) getProfileDefinition().newProfile();

    final AccessToken accessToken = credentials.toAccessToken();
    if (accessToken != null && !accessToken.getValue().isEmpty()) {
      profile.setAccessToken(accessToken);
    }

    final RefreshToken refreshToken = credentials.toRefreshToken();
    if (refreshToken != null && !refreshToken.getValue().isEmpty()) {
      profile.setRefreshToken(refreshToken);
      LOGGER.debug("Found refresh token");
    }

    final JWT idToken = credentials.toIdToken();
    profile.setIdTokenString(idToken.getParsedString());

    try {
      JWTClaimsSet claimsSet = idToken.getJWTClaimsSet();
      assertNotNull("claimsSet", claimsSet);
      profile.setId(ProfileHelper.sanitizeIdentifier(claimsSet.getSubject()));

      for (final Map.Entry<String, Object> entry : claimsSet.getClaims().entrySet()) {
        if (!JwtClaims.SUBJECT.equals(entry.getKey())
            && profile.getAttribute(entry.getKey()) == null) {
          getProfileDefinition()
              .convertAndAdd(profile, PROFILE_ATTRIBUTE, entry.getKey(), entry.getValue());
        }
      }

      profile.setTokenExpirationAdvance(configuration.getTokenExpirationAdvance());

      return Optional.of(profile);

    } catch (final java.text.ParseException e) {
      throw new AuthenticationException(e);
    }
  }
}
