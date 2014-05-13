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
package org.codice.security.filter.authorization;

import ddf.security.Subject;
import ddf.security.permission.KeyValuePermission;
import ddf.security.permission.MatchOneCollectionPermission;
import org.apache.shiro.authz.Authorizer;
import org.apache.shiro.authz.Permission;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Created by tustisos on 5/8/14.
 */
public class AuthorizationFilter implements Filter {

    private static final transient Logger LOGGER = LoggerFactory
            .getLogger(AuthorizationFilter.class);

    private List<String> roles = new ArrayList<String>();

    private Authorizer authorizer;

    @Override public void init(FilterConfig filterConfig) throws ServletException {
        LOGGER.debug("Starting AuthZ filter.");
        //TODO get this from the policy
        roles.add("admin");
    }

    @Override public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        Subject subject = (Subject) httpRequest.getAttribute("ddf.security.subject");

        List<Permission> permissions = new ArrayList<Permission>();

        permissions.add(new KeyValuePermission("role", roles));

        MatchOneCollectionPermission matchOneCollectionPermission = new MatchOneCollectionPermission(permissions);

        boolean permitted = authorizer.isPermitted(subject.getPrincipals(), matchOneCollectionPermission);

        if(!permitted) {
            LOGGER.debug("Subject not authorized.");
            returnNotAuthorized(httpResponse);
        } else {
            LOGGER.debug("Subject is authorized!");
        }
    }

    private void returnNotAuthorized(HttpServletResponse response) {
        try {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentLength(0);
            response.flushBuffer();
        } catch (IOException ioe) {
            LOGGER.debug("Failed to send auth response: {}", ioe);
        }

    }

    @Override public void destroy() {
        LOGGER.debug("Destroying AuthZ filter.");
    }

    public Authorizer getAuthorizer() {
        return authorizer;
    }

    public void setAuthorizer(Authorizer authorizer) {
        this.authorizer = authorizer;
    }
}
