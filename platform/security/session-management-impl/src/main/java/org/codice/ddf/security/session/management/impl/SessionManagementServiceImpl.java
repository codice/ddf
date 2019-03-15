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
import ddf.security.assertion.SecurityAssertion;
import ddf.security.assertion.saml.impl.SecurityAssertionSaml;
import ddf.security.common.SecurityTokenHolder;
import ddf.security.http.SessionFactory;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;
import java.net.URI;
import java.time.Clock;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.PrincipalCollection;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.security.handler.api.GuestAuthenticationToken;
import org.codice.ddf.security.handler.api.SAMLAuthenticationToken;
import org.codice.ddf.security.session.management.service.SessionManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionManagementServiceImpl implements SessionManagementService {

  private static final Logger LOGGER = LoggerFactory.getLogger(SessionManagementServiceImpl.class);

  private SecurityManager securityManager;

  private Clock clock = Clock.systemUTC();

  private SessionFactory sessionFactory;

  @Override
  public String getExpiry(HttpServletRequest request) {
    long timeLeft = 0;
    HttpSession session = sessionFactory.getOrCreateSession(request);
    if (session == null) {
      return Long.toString(timeLeft);
    }

    Object securityToken = session.getAttribute(SecurityConstants.SECURITY_TOKEN_KEY);
    if (!(securityToken instanceof SecurityTokenHolder)) {
      return Long.toString(timeLeft);
    }

    timeLeft = getTimeLeft(((SecurityTokenHolder) securityToken).getPrincipals(), session);
    return Long.toString(timeLeft);
  }

  @Override
  public String getRenewal(HttpServletRequest request) {
    long timeLeft = 0;
    HttpSession session = sessionFactory.getOrCreateSession(request);
    if (session == null) {
      return Long.toString(timeLeft);
    }

    Object securityToken = session.getAttribute(SecurityConstants.SECURITY_TOKEN_KEY);
    if (!(securityToken instanceof SecurityTokenHolder)) {
      return Long.toString(timeLeft);
    }

    SecurityTokenHolder tokenHolder = (SecurityTokenHolder) securityToken;
    PrincipalCollection renewedPrincipals;
    try {
      renewedPrincipals = renewSecurityAssertions(tokenHolder, request);
    } catch (SecurityServiceException e) {
      LOGGER.error("Failed to renew", e);
      return Long.toString(timeLeft);
    }

    if (renewedPrincipals != null) {
      tokenHolder.setPrincipals(renewedPrincipals);
    }
    timeLeft = getTimeLeft(renewedPrincipals, session);
    return Long.toString(timeLeft);
  }

  @Override
  public URI getInvalidate(HttpServletRequest request) {
    String requestQueryString = request.getQueryString();
    return URI.create(
        SystemBaseUrl.EXTERNAL
            .constructUrl("/logout")
            .concat(requestQueryString != null ? "?" + requestQueryString : ""));
  }

  private long getTimeLeft(PrincipalCollection principals, HttpSession session) {
    long timeLeft = session.getMaxInactiveInterval() * 1000L;

    if (principals != null) {
      long earliestAssertionExpiration =
          principals
              .byType(SecurityAssertion.class)
              .stream()
              .map(SecurityAssertion::getNotOnOrAfter)
              .map(Date::getTime)
              .min(Comparator.naturalOrder())
              .orElse(Long.MAX_VALUE);
      timeLeft = Math.min(earliestAssertionExpiration - clock.millis(), timeLeft);
    }

    return Math.max(timeLeft, 0);
  }

  private PrincipalCollection renewSecurityAssertions(
      SecurityTokenHolder tokenHolder, HttpServletRequest request) throws SecurityServiceException {
    AuthenticationToken token;

    Collection<SecurityAssertion> assertions =
        tokenHolder.getPrincipals().byType(SecurityAssertion.class);
    if (assertions.isEmpty()) {
      return null;
    } else if (assertions.size() == 1) {
      /*
       * Only guest and SAML assertions can be renewed. If there's exactly one assertion and it's
       * one of these types, renew it. Otherwise, ignore it.
       */
      SecurityAssertion assertion = assertions.iterator().next();
      if (assertion.getWeight() == SecurityAssertion.NO_AUTH_WEIGHT) {
        token = new GuestAuthenticationToken(assertion.getPrincipal().getName());
      } else if (assertion instanceof SecurityAssertionSaml) {
        token =
            new SAMLAuthenticationToken(null, tokenHolder.getPrincipals(), request.getRemoteAddr());
      } else {
        return null;
      }
    } else {
      /*
       * If there are multiple assertions, user must have signed in with guest auth enabled, and
       * the assertions comprise a guest assertion and an assertion for the auth type they used.
       * If the auth type was SAML, renew them both. Otherwise, do nothing.
       *
       * Assumption: users can't sign in with multiple different auth types simultaneously.
       */
      if (!tokenHolder.getPrincipals().byType(SecurityAssertionSaml.class).isEmpty()) {
        token =
            new SAMLAuthenticationToken(null, tokenHolder.getPrincipals(), request.getRemoteAddr());
      } else {
        return null;
      }
    }

    return securityManager.getSubject(token).getPrincipals();
  }

  public void setSecurityManager(SecurityManager securityManager) {
    this.securityManager = securityManager;
  }

  public void setClock(Clock clock) {
    this.clock = clock;
  }

  public void setSessionFactory(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }
}
