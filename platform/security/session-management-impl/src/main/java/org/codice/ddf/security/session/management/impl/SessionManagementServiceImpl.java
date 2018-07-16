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
package org.codice.ddf.security.session.management.impl;

import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.assertion.impl.SecurityAssertionImpl;
import ddf.security.common.SecurityTokenHolder;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;
import java.net.URI;
import java.time.Clock;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.security.handler.api.SAMLAuthenticationToken;
import org.codice.ddf.security.session.management.service.SessionManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionManagementServiceImpl implements SessionManagementService {

  private static final Logger LOGGER = LoggerFactory.getLogger(SessionManagementServiceImpl.class);

  private SecurityManager securityManager;

  private Clock clock = Clock.systemUTC();

  @Override
  public String getExpiry(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    long timeLeft = 0;
    if (session != null) {
      Object securityToken = session.getAttribute(SecurityConstants.SAML_ASSERTION);
      if (securityToken instanceof SecurityTokenHolder) {
        timeLeft = getTimeLeft((SecurityTokenHolder) securityToken);
      }
    }
    return Long.toString(timeLeft);
  }

  @Override
  public String getRenewal(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    boolean[] securityServiceExceptionThrown = new boolean[1];
    securityServiceExceptionThrown[0] = false;

    String timeLeft = null;
    if (session != null) {
      Object securityToken = session.getAttribute(SecurityConstants.SAML_ASSERTION);
      if (securityToken instanceof SecurityTokenHolder) {
        SecurityTokenHolder tokenHolder = (SecurityTokenHolder) securityToken;
        Map<String, SecurityToken> realmTokenMap = tokenHolder.getRealmTokenMap();
        realmTokenMap
            .keySet()
            .forEach(
                s -> {
                  try {
                    doRenew(s, realmTokenMap.get(s), tokenHolder);
                  } catch (SecurityServiceException e) {
                    securityServiceExceptionThrown[0] = true;
                    LOGGER.error("Failed to renew", e);
                  }
                });

        if (securityServiceExceptionThrown[0]) {
          return null;
        }

        timeLeft = Long.toString(getTimeLeft(tokenHolder));
      }
    }
    return timeLeft;
  }

  @Override
  public URI getInvalidate(HttpServletRequest request) {
    String requestQueryString = request.getQueryString();
    return URI.create(
        SystemBaseUrl.EXTERNAL
            .constructUrl("/logout?noPrompt=true")
            .concat(requestQueryString != null ? "&" + requestQueryString : ""));
  }

  private long getTimeLeft(SecurityTokenHolder securityToken) {
    return securityToken
        .getRealmTokenMap()
        .values()
        .stream()
        .map(SecurityAssertionImpl::new)
        .map(SecurityAssertionImpl::getNotOnOrAfter)
        .map(Date::getTime)
        .min(Comparator.comparing(Long::valueOf))
        .map(m -> Math.max(m - clock.millis(), 0))
        .orElse(0L);
  }

  private void doRenew(String realm, SecurityToken securityToken, SecurityTokenHolder tokenHolder)
      throws SecurityServiceException {
    SAMLAuthenticationToken samlToken =
        new SAMLAuthenticationToken(securityToken.getPrincipal(), securityToken, realm);
    Subject subject = securityManager.getSubject(samlToken);
    for (Object principal : subject.getPrincipals().asList()) {
      if (principal instanceof SecurityAssertion) {
        tokenHolder.addSecurityToken(realm, ((SecurityAssertion) principal).getSecurityToken());
      }
    }
  }

  public void setSecurityManager(SecurityManager securityManager) {
    this.securityManager = securityManager;
  }

  public void setClock(Clock clock) {
    this.clock = clock;
  }
}
