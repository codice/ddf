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

import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.assertion.jwt.impl.SecurityAssertionJwt;
import java.security.Principal;
import java.util.List;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.codice.ddf.security.handler.api.OidcAuthenticationToken;
import org.codice.ddf.security.handler.api.OidcHandlerConfiguration;
import org.pac4j.core.context.WebContext;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.credentials.OidcCredentials;
import org.pac4j.oidc.profile.OidcProfile;
import org.pac4j.oidc.profile.creator.OidcProfileCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OidcRealm extends AuthenticatingRealm {

  private static final Logger LOGGER = LoggerFactory.getLogger(OidcRealm.class);

  private List<String> usernameAttributeList;
  private OidcHandlerConfiguration oidcHandlerConfiguration;

  /** Determine if the supplied token is supported by this realm. */
  @Override
  public boolean supports(AuthenticationToken token) {
    if (!(token instanceof OidcAuthenticationToken)) {
      LOGGER.debug(
          "The supplied authentication token is not an instance of SessionToken or OidcAuthenticationToken. Sending back not supported.");
      return false;
    }

    OidcAuthenticationToken oidcToken = (OidcAuthenticationToken) token;

    OidcCredentials credentials = (OidcCredentials) oidcToken.getCredentials();

    if (credentials == null
        || (credentials.getCode() == null
            && credentials.getAccessToken() == null
            && credentials.getIdToken() == null)) {
      LOGGER.debug(
          "The supplied authentication token has null/empty credentials. Sending back no supported.");
      return false;
    }

    WebContext webContext = (WebContext) oidcToken.getContext();
    if (webContext == null) {
      LOGGER.debug(
          "The supplied authentication token has null web context. Sending back not supported.");
      return false;
    }

    LOGGER.debug("Token {} is supported by {}.", token.getClass(), OidcRealm.class.getName());
    return true;
  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken)
      throws AuthenticationException {
    // token is guaranteed to be of type OidcAuthenticationToken by the supports() method
    OidcAuthenticationToken oidcAuthenticationToken = (OidcAuthenticationToken) authenticationToken;
    OidcCredentials credentials = (OidcCredentials) oidcAuthenticationToken.getCredentials();
    OidcConfiguration oidcConfiguration = oidcHandlerConfiguration.getOidcConfiguration();
    OIDCProviderMetadata oidcProviderMetadata = oidcConfiguration.findProviderMetadata();
    WebContext webContext = (WebContext) oidcAuthenticationToken.getContext();
    OidcClient oidcClient = oidcHandlerConfiguration.getOidcClient(webContext.getFullRequestURL());

    OidcCredentialsResolver oidcCredentialsResolver =
        new OidcCredentialsResolver(oidcConfiguration, oidcClient, oidcProviderMetadata);

    oidcCredentialsResolver.resolveIdToken(credentials, webContext);

    // problem getting id token, invalidate credentials
    if (credentials.getIdToken() == null) {
      webContext.getSessionStore().destroySession(webContext);

      String msg =
          String.format(
              "Could not fetch id token with Oidc credentials (%s). "
                  + "This may be due to the credentials expiring. "
                  + "Invalidating session in order to acquire valid credentials.",
              credentials);

      LOGGER.warn(msg);
      throw new AuthenticationException(msg);
    }

    OidcProfileCreator oidcProfileCreator = new CustomOidcProfileCreator(oidcConfiguration);
    OidcProfile profile = oidcProfileCreator.create(credentials, webContext);

    SimpleAuthenticationInfo simpleAuthenticationInfo = new SimpleAuthenticationInfo();
    SimplePrincipalCollection principalCollection =
        createPrincipalCollectionFromCredentials(profile);
    simpleAuthenticationInfo.setPrincipals(principalCollection);
    simpleAuthenticationInfo.setCredentials(credentials);

    return simpleAuthenticationInfo;
  }

  private SimplePrincipalCollection createPrincipalCollectionFromCredentials(OidcProfile profile) {
    SimplePrincipalCollection principals = new SimplePrincipalCollection();
    SecurityAssertion securityAssertion = null;
    try {
      securityAssertion = new SecurityAssertionJwt(profile, usernameAttributeList);
      Principal principal = securityAssertion.getPrincipal();
      if (principal != null) {
        principals.add(principal.getName(), getName());
      }
    } catch (Exception e) {
      LOGGER.warn(
          "Encountered error while trying to get the Principal for the SecurityToken. Security functions may not work properly.",
          e);
    }
    if (securityAssertion != null) {
      principals.add(securityAssertion, getName());
    }
    return principals;
  }

  public List<String> getUsernameAttributeList() {
    return usernameAttributeList;
  }

  public void setUsernameAttributeList(List<String> usernameAttributeList) {
    this.usernameAttributeList = usernameAttributeList;
  }

  public void setOidcHandlerConfiguration(OidcHandlerConfiguration oidcHandlerConfiguration) {
    this.oidcHandlerConfiguration = oidcHandlerConfiguration;
  }
}
