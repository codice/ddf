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
package org.codice.ddf.security.handler.pki;

import ddf.security.common.util.PropertiesLoader;
import org.apache.cxf.ws.security.sts.provider.model.secext.BinarySecurityTokenType;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.CredentialException;
import org.apache.ws.security.components.crypto.Merlin;
import org.apache.ws.security.util.Base64;
import org.codice.ddf.security.handler.api.AuthenticationHandler;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.codice.ddf.security.handler.api.PKIAuthenticationToken;
import org.codice.ddf.security.handler.api.PKIAuthenticationTokenFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.security.cert.X509Certificate;

/**
 * Handler for PKI based authentication. X509 chain will be extracted from the HTTP request and
 * converted to a BinarySecurityToken.
 */
public class PKIHandler implements AuthenticationHandler {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(PKIHandler.class);

    /**
     * PKI type to use when configuring context policy.
     */
    public static final String AUTH_TYPE = "PKI";

    public static final String SOURCE = "PKIHandler";

    protected String realm = BaseAuthenticationToken.DEFAULT_REALM;

    protected PKIAuthenticationTokenFactory tokenFactory;

    @Override
    public String getAuthenticationType() {
        return AUTH_TYPE;
    }

    /**
     * Handler implementing PKI authentication. Returns the {@link org.codice.ddf.security.handler.api.HandlerResult} containing
     * a BinarySecurityToken if the operation was successful.
     *
     * @param request  http request to obtain attributes from and to pass into any local filter chains required
     * @param response http response to return http responses or redirects
     * @param chain    original filter chain (should not be called from your handler)
     * @param resolve  flag with true implying that credentials should be obtained, false implying return if no credentials are found.
     * @return result of handling this request - status and optional tokens
     * @throws ServletException
     */
    @Override
    public HandlerResult getNormalizedToken(ServletRequest request, ServletResponse response,
      FilterChain chain, boolean resolve) throws ServletException {

        HandlerResult handlerResult = new HandlerResult(HandlerResult.Status.NO_ACTION, null);
        handlerResult.setSource(realm + "-" + SOURCE);

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getServletPath();
        LOGGER.debug("Doing PKI authentication and authorization for path {}", path);

        //doesn't matter what the resolve flag is set to, we do the same action
        PKIAuthenticationToken token = extractAuthenticationInfo(httpRequest);

        if (token != null) {
            handlerResult.setToken(token);
            handlerResult.setStatus(HandlerResult.Status.COMPLETED);
        }
        return handlerResult;
    }

    @Override
    public HandlerResult handleError(ServletRequest servletRequest, ServletResponse servletResponse,
      FilterChain chain) throws ServletException {
        HandlerResult result = new HandlerResult(HandlerResult.Status.NO_ACTION, null);
        result.setSource(realm + "-" + SOURCE);
        LOGGER.debug("In error handler for pki - no action taken.");
        return result;
    }

    protected PKIAuthenticationToken extractAuthenticationInfo(HttpServletRequest request) {
        PKIAuthenticationToken token;
        X509Certificate[] certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");

        token = tokenFactory.getTokenFromCerts(certs);
        return token;
    }

    public void setTokenFactory(PKIAuthenticationTokenFactory factory) {
        tokenFactory = factory;
    }
}
