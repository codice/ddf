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

import ddf.security.cas.client.ProxyFilter;
import ddf.security.sts.client.configuration.STSClientConfiguration;
import org.apache.cxf.ws.security.sts.provider.model.secext.BinarySecurityTokenType;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.util.Base64;
import org.codice.ddf.security.handler.api.AuthenticationHandler;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.jasig.cas.client.authentication.AttributePrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Authentication Handler for CAS. Runs through CAS filter chain if no CAS ticket is present.
 */
public class CasHandler implements AuthenticationHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CasHandler.class);

    private static Marshaller marshaller = null;

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

        String proxyTicket = getProxyTicket((HttpServletRequest) request);
        // if I have a proxy ticket, create the token and return a completed status
        if (proxyTicket != null) {
            CASAuthenticationToken token = new CASAuthenticationToken(httpRequest.getUserPrincipal(), proxyTicket, realm);
            handlerResult.setToken(token);
            handlerResult.setStatus(HandlerResult.Status.COMPLETED);
        } else {
            // we didn't find a ticket, see if we are to go get one
            if (resolve) {
                try {
                    proxyFilter.doFilter(request, response, chain);
                } catch (IOException e) {
                    throw new ServletException(e);
                }
                handlerResult.setStatus(HandlerResult.Status.REDIRECTED);
            }
        }
        return handlerResult;
    }

    @Override
    public HandlerResult handleError(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws ServletException {
        HandlerResult handlerResult;
        try {
            proxyFilter.doFilter(servletRequest, servletResponse, chain);
        } catch (IOException e) {
            LOGGER.warn("Exception invoking the proxy filter: {}", e.getMessage(), e);
            throw new ServletException(e);
        }
        handlerResult = new HandlerResult(HandlerResult.Status.REDIRECTED, null);
        return handlerResult;
    }

    /**
     * Gets the CAS proxy ticket that will be used by the STS to get a SAML assertion.
     *
     * @param request The Http servlet request.
     * @return Returns the CAS proxy ticket that will be used by the STS to get a SAML assertion.
     */
    private String getProxyTicket(HttpServletRequest request) {
        AttributePrincipal attributePrincipal = (AttributePrincipal) request.getUserPrincipal();
        String proxyTicket = null;

        if (attributePrincipal != null) {
            LOGGER.debug("Getting proxy ticket for {}", clientConfiguration.getAddress());
            proxyTicket = attributePrincipal.getProxyTicketFor(clientConfiguration.getAddress());
            LOGGER.info("proxy ticket: {}", proxyTicket);
        } else {
            LOGGER.error("attribute principal is null!");
        }

        return proxyTicket;
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
