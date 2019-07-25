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

import ddf.action.Action;
import ddf.action.ActionProvider;
import ddf.action.impl.ActionImpl;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.SubjectUtils;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.assertion.jwt.impl.SecurityAssertionJwt;
import ddf.security.common.SecurityTokenHolder;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.security.handler.api.OidcHandlerConfiguration;
import org.pac4j.core.context.J2EContext;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.J2ESessionStore;
import org.pac4j.core.http.ajax.DefaultAjaxRequestResolver;
import org.pac4j.core.redirect.RedirectAction;
import org.pac4j.oidc.logout.OidcLogoutActionBuilder;
import org.pac4j.oidc.profile.OidcProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OidcLogoutActionProvider implements ActionProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(OidcLogoutActionProvider.class);

  private static final String ID = "security.logout.oidc";

  private static final String TITLE = "OIDC Logout";

  private static final String DESCRIPTION =
      "Logging out of the Identity Provider(IdP) will logout all external clients signed in via that Identity Provider.";

  private final OidcHandlerConfiguration handlerConfiguration;

  public OidcLogoutActionProvider(OidcHandlerConfiguration handlerConfiguration) {
    this.handlerConfiguration = handlerConfiguration;
  }

  /**
   * *
   *
   * @param <T> is a Map<String, Subject>
   * @param subjectMap containing the corresponding subject
   * @return OidcLogoutActionProvider containing the logout url
   */
  public <T> Action getAction(T subjectMap) {
    if (!canHandle(subjectMap)) {
      return null;
    }

    String logoutUrlString = "";
    URL logoutUrl = null;

    try {
      HttpServletRequest request = (HttpServletRequest) ((Map) subjectMap).get("http_request");
      HttpServletResponse response = (HttpServletResponse) ((Map) subjectMap).get("http_response");

      J2ESessionStore sessionStore = new J2ESessionStore();
      J2EContext j2EContext = new J2EContext(request, response, sessionStore);

      HttpSession session = request.getSession(false);
      SecurityTokenHolder tokenHolder = null;
      if (session != null) {
        tokenHolder =
            (SecurityTokenHolder) session.getAttribute(SecurityConstants.SECURITY_TOKEN_KEY);
      }

      OidcProfile oidcProfile = null;
      if (tokenHolder != null && tokenHolder.getPrincipals() != null) {
        Collection<SecurityAssertion> securityAssertions =
            tokenHolder.getPrincipals().byType(SecurityAssertion.class);
        for (SecurityAssertion securityAssertion : securityAssertions) {
          if (SecurityAssertionJwt.JWT_TOKEN_TYPE.equals(securityAssertion.getTokenType())) {
            oidcProfile = (OidcProfile) securityAssertion.getToken();
            break;
          }
        }
      }

      if (oidcProfile == null) {
        throw new IllegalStateException("Unable to determine OIDC profile for logout");
      }

      OidcLogoutActionBuilder logoutActionBuilder =
          handlerConfiguration.getOidcLogoutActionBuilder();
      logoutActionBuilder.setAjaxRequestResolver(
          new DefaultAjaxRequestResolver() {
            @Override
            public boolean isAjax(final WebContext context) {
              return false;
            }
          });

      RedirectAction logoutAction =
          logoutActionBuilder.getLogoutAction(
              j2EContext, oidcProfile, SystemBaseUrl.EXTERNAL.constructUrl("/oidc/logout", true));

      logoutUrlString = logoutAction.getLocation();

      logoutUrl = new URL(logoutUrlString);
    } catch (MalformedURLException e) {
      LOGGER.info("Unable to resolve URL: {}", logoutUrlString);
    } catch (ClassCastException e) {
      LOGGER.debug("Unable to cast parameter to Map<String, Object>, {}", subjectMap, e);
    }
    return new ActionImpl(ID, TITLE, DESCRIPTION, logoutUrl);
  }

  @Override
  public String getId() {
    return ID;
  }

  private <T> boolean canHandle(T subjectMap) {
    if (!(subjectMap instanceof Map)) {
      return false;
    }

    Object subject = ((Map) subjectMap).get(SecurityConstants.SECURITY_SUBJECT);
    if (!(subject instanceof Subject)) {
      return false;
    }

    String type = SubjectUtils.getType((org.apache.shiro.subject.Subject) subject);
    return type != null && type.equals(SecurityAssertionJwt.JWT_TOKEN_TYPE);
  }
}
