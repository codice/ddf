/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package org.codice.ddf.security.filter.authorization;

import ddf.security.permission.ActionPermission;
import ddf.security.permission.CollectionPermission;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.codice.ddf.security.policy.context.ContextPolicy;
import org.codice.ddf.security.policy.context.ContextPolicyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;

/**
 * Handler that implements authorization checking for contexts.
 */
public class AuthorizationFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationFilter.class);

    private final ContextPolicyManager contextPolicyManager;

    private boolean shouldActionAuthorize;

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
    public void init(FilterConfig filterConfig) throws ServletException {
        LOGGER.debug("Starting AuthZ filter.");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        org.apache.shiro.subject.Subject subject = null;

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
            if (shouldActionAuthorize) {
                ActionPermission actionPermission = new ActionPermission(httpRequest.getRequestURI());
                permitted = (subject != null) && subject.isPermitted(actionPermission);
            } else {
                String path = StringUtils.isNotBlank(httpRequest.getContextPath()) ? httpRequest
                        .getContextPath() : httpRequest.getServletPath()
                        + StringUtils.defaultString(httpRequest.getPathInfo());
                if (StringUtils.isEmpty(path)) {
                    path = httpRequest.getRequestURI();
                }
                ContextPolicy policy = contextPolicyManager.getContextPolicy(path);

                if (policy != null) {
                    Collection<CollectionPermission> permissions = policy.getAllowedAttributePermissions();

                    for (CollectionPermission permission : permissions) {
                        if (subject == null || !subject.isPermittedAll(
                                permission.getPermissionList())) {
                            permitted = false;
                        }
                    }
                } else {
                    LOGGER.warn("Unable to determine policy for path {}. User is not permitted to continue. Check policy configuration!", path);
                    permitted = false;
                }
            }

            if (!permitted) {
                LOGGER.debug("Subject not authorized.");
                returnNotAuthorized(httpResponse);
            } else {
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

    public void setShouldActionAuthorize(boolean actionAuthorize) {
        this.shouldActionAuthorize = actionAuthorize;
    }

}
