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
package ddf.security.service.impl.cas;

import javax.servlet.http.HttpServletRequest;

import org.jasig.cas.client.authentication.AttributePrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.service.SecurityServiceException;
import ddf.security.service.TokenRequestHandler;
import ddf.security.sts.client.configuration.STSClientConfiguration;

/**
 * Implementation of {@link ddf.security.service.TokenRequestHandler} that is specific to CAS.
 * Allows clients to send in a {@link javax.servlet.http.HttpServletRequest} and retrieve a token
 * that can be used to create a subject.
 * 
 */
public class CASTokenRequestHandler implements TokenRequestHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CASTokenRequestHandler.class);

    private STSClientConfiguration stsClientConfig;

    @Override
    public Object createToken(HttpServletRequest request) throws SecurityServiceException {
        AttributePrincipal attributePrincipal = (AttributePrincipal) request.getUserPrincipal();
        String proxyTicket = null;
        String stsAddress = stsClientConfig.getAddress();

        if (attributePrincipal != null) {
            LOGGER.debug("Getting proxy ticket for {}", stsAddress);
            proxyTicket = attributePrincipal.getProxyTicketFor(stsAddress);
            if (proxyTicket != null) {
                LOGGER.debug("Retrieved proxy ticket: {}", proxyTicket);
                return new CasAuthenticationToken(proxyTicket, stsAddress);
            } else {
                throw new SecurityServiceException(
                        "Could not get Proxy Ticket from CAS server. Check CAS log for error.");
            }
        } else {
            throw new SecurityServiceException(
                    "Could not get the principal from the incoming request.");
        }

    }

    public void setStsClientConfiguration(STSClientConfiguration stsClientConfig) {
        this.stsClientConfig = stsClientConfig;
    }

}
