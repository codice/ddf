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
package org.codice.ddf.security.handler.cas;

import ddf.security.sts.client.configuration.STSClientConfiguration;
import org.codice.ddf.security.handler.api.AuthenticationHandler;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.codice.ddf.security.handler.cas.filter.ProxyFilter;
import org.codice.ddf.security.handler.cas.filter.ProxyFilterChain;
import org.jasig.cas.client.authentication.AttributePrincipal;
import org.jasig.cas.client.util.AbstractCasFilter;
import org.jasig.cas.client.validation.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Authentication Handler for CAS. Runs through CAS filter chain if no CAS ticket is present.
 */
public class CasHandler implements AuthenticationHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CasHandler.class);

    /**
     * CAS type to use when configuring context policy.
     */
    public static final String AUTH_TYPE = "CAS";

    public static final String SOURCE = "CASHandler";

    protected String realm = BaseAuthenticationToken.DEFAULT_REALM;

    private STSClientConfiguration clientConfiguration;

    private ProxyFilter proxyFilter;

    @Override
    public String getAuthenticationType() {
        return AUTH_TYPE;
    }

    @Override
    public HandlerResult getNormalizedToken(ServletRequest request, ServletResponse response,
            FilterChain chain, boolean resolve) throws ServletException {

        // Default to NO_ACTION and set the source as this handler
        HandlerResult handlerResult = new HandlerResult(HandlerResult.Status.NO_ACTION, null);
        handlerResult.setSource(realm + "-" + SOURCE);

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String path = httpRequest.getServletPath();
        LOGGER.debug("Doing CAS authentication and authorization for path {}", path);

        // if the request contains the principal, return it
        if (containsPrincipal(httpRequest)) {
            LOGGER.debug("Found previous CAS attribute, using that same session.");
            CASAuthenticationToken token = getAuthenticationToken(httpRequest);
            if (token != null) {
                handlerResult.setToken(token);
                handlerResult.setStatus(HandlerResult.Status.COMPLETED);
                LOGGER.debug("Successfully set authentication token, returning result with token.");
            } else {
                LOGGER.debug("Could not create authentication token, returning NO_ACTION result.");
            }
        } else {
            if (resolve) {
                try {
                    LOGGER.debug(
                            "Calling cas authentication and validation filters to perform redirects.");
                    proxyFilter.doFilter(request, response, new ProxyFilterChain(null));
                    handlerResult.setStatus(HandlerResult.Status.REDIRECTED);
                } catch (IOException e) {
                    throw new ServletException(e);
                }
            } else {
                LOGGER.warn(
                        "No cas authentication information found and resolve is not enabled, returning NO_ACTION.");
            }
        }

        return handlerResult;
    }

    @Override
    public HandlerResult handleError(ServletRequest servletRequest, ServletResponse servletResponse,
            FilterChain chain) throws ServletException {
        HandlerResult handlerResult;
        LOGGER.warn("handleError was called on the CasHandler, cannot do anything.");
        handlerResult = new HandlerResult(HandlerResult.Status.NO_ACTION, null);
        return handlerResult;
    }

    /**
     * Gets the CAS proxy ticket that will be used by the STS to get a SAML assertion.
     *
     * @param request The Http servlet request.
     * @return Returns the CAS proxy ticket that will be used by the STS to get a SAML assertion.
     */
    private CASAuthenticationToken getAuthenticationToken(HttpServletRequest request) {

        CASAuthenticationToken token = null;
        Assertion assertion;
        AttributePrincipal attributePrincipal = null;

        assertion = (Assertion) request.getSession()
                .getAttribute(AbstractCasFilter.CONST_CAS_ASSERTION);

        if (assertion != null) {
            attributePrincipal = assertion.getPrincipal();
        }

        LOGGER.debug("Got the following attributePrincipal: {}", attributePrincipal);

        if (attributePrincipal != null) {
            LOGGER.debug("Getting proxy ticket for {}", clientConfiguration.getAddress());
            String proxyTicket = attributePrincipal
                    .getProxyTicketFor(clientConfiguration.getAddress());
            LOGGER.debug("proxy ticket: {}", proxyTicket);
            LOGGER.debug("Creating AuthenticationToken with {}|{} as the credentials.", proxyTicket,
                    clientConfiguration.getAddress());
            token = new CASAuthenticationToken(attributePrincipal, proxyTicket,
                    clientConfiguration.getAddress(), realm);
        } else {
            LOGGER.warn("Couldn't get user information for CAS authentication.");
        }

        return token;
    }

    /**
     * Checks if the incoming request contains a cas principal.
     *
     * @param request Incoming request that should be checked.
     * @return true if a cas principal was found, false if not.
     */
    private boolean containsPrincipal(HttpServletRequest request) {
        return (request.getSession() != null
                && request.getSession().getAttribute(AbstractCasFilter.CONST_CAS_ASSERTION)
                != null);
    }

    public STSClientConfiguration getClientConfiguration() {
        return clientConfiguration;
    }

    public void setClientConfiguration(STSClientConfiguration clientConfiguration) {
        this.clientConfiguration = clientConfiguration;
    }

    public ProxyFilter getProxyFilter() {
        return proxyFilter;
    }

    public void setProxyFilter(ProxyFilter proxyFilter) {
        this.proxyFilter = proxyFilter;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getRealm() {
        return realm;
    }
}
