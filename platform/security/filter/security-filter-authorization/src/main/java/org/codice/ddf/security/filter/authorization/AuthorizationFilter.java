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
package org.codice.ddf.security.filter.authorization;

import ddf.security.common.audit.SecurityLogger;
import ddf.security.permission.CollectionPermission;
import java.io.IOException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.codice.ddf.platform.filter.AuthenticationException;
import org.codice.ddf.platform.filter.FilterChain;
import org.codice.ddf.platform.filter.SecurityFilter;
import org.codice.ddf.security.policy.context.ContextPolicy;
import org.codice.ddf.security.policy.context.ContextPolicyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Handler that implements authorization checking for contexts. */
public class AuthorizationFilter implements SecurityFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationFilter.class);

  private final ContextPolicyManager contextPolicyManager;

  /**
   * Default constructor
   *
   * @param contextPolicyManager
   */
  public AuthorizationFilter(ContextPolicyManager contextPolicyManager) {
    super();
    this.contextPolicyManager = contextPolicyManager;
  }

  @Override
  public void init() {
    LOGGER.debug("Starting AuthZ filter.");
  }

  @SuppressWarnings("PackageAccessibility")
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, AuthenticationException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    Subject subject = null;

    if (request.getAttribute(ContextPolicy.NO_AUTH_POLICY) != null) {
      LOGGER.debug("NO_AUTH_POLICY header was found, skipping authorization filter.");
      chain.doFilter(request, response);
    } else {
      try {
        subject = SecurityUtils.getSubject();
      } catch (Exception e) {
        LOGGER.debug("Unable to retrieve user from request.", e);
      }

      boolean permitted = true;

      final String path = httpRequest.getRequestURI();

      ContextPolicy policy = contextPolicyManager.getContextPolicy(path);

      CollectionPermission permissions = null;
      if (policy != null && subject != null) {
        permissions = policy.getAllowedAttributePermissions();
        if (!permissions.isEmpty()) {
          permitted = subject.isPermitted(permissions);
        }
      } else {
        LOGGER.warn(
            "Unable to determine policy for path {}. User is not permitted to continue. Check policy configuration!",
            path);
        permitted = false;
      }

      if (!permitted) {
        SecurityLogger.audit("Subject not authorized to view resource {}", path);
        LOGGER.debug("Subject not authorized.");
        returnNotAuthorized(httpResponse);
      } else {
        if (!permissions.isEmpty()) {
          SecurityLogger.audit("Subject is authorized to view resource {}", path);
        }
        LOGGER.debug("Subject is authorized!");
        chain.doFilter(request, response);
      }
    }
  }

  /**
   * Sets status and error codes to forbidden and returns response.
   *
   * @param response
   */
  private void returnNotAuthorized(HttpServletResponse response) {
    try {
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      response.flushBuffer();
    } catch (IOException ioe) {
      LOGGER.debug("Failed to send auth response: {}", ioe);
    }
  }

  @Override
  public void destroy() {
    LOGGER.debug("Destroying AuthZ filter.");
  }
}
