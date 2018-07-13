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
package org.codice.ddf.security.servlet.expiry;

import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.assertion.impl.SecurityAssertionImpl;
import ddf.security.common.SecurityTokenHolder;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.security.handler.api.SAMLAuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
public class SessionManagementService {
  private static final Logger LOGGER = LoggerFactory.getLogger(SessionManagementService.class);

  private SecurityManager securityManager;

  private Clock clock = Clock.systemUTC();

  @GET
  @Path("/expiry")
  public Response getExpiry(@Context HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    long timeLeft = 0;
    if (session != null) {
      Object securityToken = session.getAttribute(SecurityConstants.SAML_ASSERTION);
      if (securityToken instanceof SecurityTokenHolder) {
        timeLeft = getTimeLeft((SecurityTokenHolder) securityToken);
      }
    }
    return Response.ok(
            new ByteArrayInputStream(Long.toString(timeLeft).getBytes(StandardCharsets.UTF_8)))
        .build();
  }

  private long getTimeLeft(SecurityTokenHolder securityToken) {
    List<SecurityAssertionImpl> values =
        securityToken
            .getRealmTokenMap()
            .values()
            .stream()
            .map(SecurityAssertionImpl::new)
            .collect(Collectors.toList());
    values.sort(
        (o1, o2) -> {
          long l = o1.getNotOnOrAfter().getTime() - o2.getNotOnOrAfter().getTime();
          if (l > 0) {
            return 1;
          } else if (l < 0) {
            return -1;
          } else {
            return 0;
          }
        });
    return values.isEmpty()
        ? 0
        : Math.max(values.get(0).getNotOnOrAfter().getTime() - clock.millis(), 0);
  }

  @GET
  @Path("/renew")
  public Response getRenewal(@Context HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    Response[] response = new Response[1];
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
                    response[0] = Response.serverError().build();
                    LOGGER.error("Failed to renew", e);
                  }
                });
        if (response[0] == null) {
          response[0] =
              Response.ok(
                      new ByteArrayInputStream(
                          Long.toString(getTimeLeft(tokenHolder)).getBytes(StandardCharsets.UTF_8)))
                  .build();
        }
      }
    }
    return response[0];
  }

  @GET
  @Path("/invalidate")
  public Response getInvalidate(@Context HttpServletRequest request) {
    StringBuffer requestURL = request.getRequestURL();
    String requestQueryString = request.getQueryString();
    return Response.seeOther(
            URI.create(
                SystemBaseUrl.EXTERNAL
                    .constructUrl("logout")
                    .concat("?noPrompt=true")
                    .concat(requestQueryString != null ? "&" + requestQueryString : "")))
        .build();
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
