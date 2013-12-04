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

package org.codice.ddf.ui.filter;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;

/**
 * Servlet for forcing a redirect from the default /search location to the actual web app location.
 *
 * This exists because in some instances we may want to redirect to the simple search ui, rather than
 * the Cesium based search ui or even to a load balancer.
 */
public class RedirectServlet extends HttpServlet {

    private Logger logger = LoggerFactory.getLogger(RedirectServlet.class);

    private RedirectConfiguration redirectConfiguration;

    @Override
    public void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws ServletException, IOException {
        if(StringUtils.isNotBlank(redirectConfiguration.getDefaultUri())) {
            URI uri = URI.create(redirectConfiguration.getDefaultUri());
            if(uri.isAbsolute()) {
                logger.warn("Redirecting /search to an absolute URI: "+redirectConfiguration.getDefaultUri());
            }
            else {
                logger.info("Redirecting /search to a relative URI: "+redirectConfiguration.getDefaultUri());
            }
            servletResponse.sendRedirect(redirectConfiguration.getDefaultUri());
        }
        else {
            logger.warn("Search page redirection has not been configured.");
            servletResponse.sendError(404);
        }
    }

    public RedirectConfiguration getRedirectConfiguration() {
        return redirectConfiguration;
    }

    public void setRedirectConfiguration(RedirectConfiguration redirectConfiguration) {
        this.redirectConfiguration = redirectConfiguration;
    }
}