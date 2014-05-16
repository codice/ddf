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
package org.codice.security.filter.cas;

import ddf.security.cas.client.ProxyFilter;
import ddf.security.sts.client.configuration.STSClientConfiguration;
import org.apache.cxf.ws.security.sts.provider.model.secext.BinarySecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.model.secext.UsernameTokenType;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.util.Base64;
import org.codice.security.handler.api.AuthenticationHandler;
import org.codice.security.handler.api.HandlerResult;
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
public class CasFilter implements AuthenticationHandler {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(CasFilter.class);

    private static Marshaller marshaller = null;

    private STSClientConfiguration clientConfiguration;

    private ProxyFilter proxyFilter;

    @Override
    public HandlerResult getNormalizedToken(ServletRequest request, ServletResponse response,
            FilterChain chain, boolean resolve) throws ServletException {
        HandlerResult filterResult;
        String proxyTicket = getProxyTicket((HttpServletRequest) request);
        if(resolve) {
            if(proxyTicket == null) {
                try {
                    proxyFilter.doFilter(request, response, chain);
                } catch (IOException e) {
                    throw new ServletException(e);
                }
                filterResult = new HandlerResult(HandlerResult.Status.REDIRECTED, null, "");
                return filterResult;
            } else {
                String bst = getBinarySecurityToken(proxyTicket);
                filterResult = new HandlerResult(HandlerResult.Status.COMPLETED, ((HttpServletRequest)request).getUserPrincipal(), bst);
                return filterResult;
            }
        } else {
            if(proxyTicket == null) {
                filterResult = new HandlerResult(HandlerResult.Status.NO_ACTION, null, "");
                return filterResult;
            } else {
                String bst = getBinarySecurityToken(proxyTicket);
                filterResult = new HandlerResult(HandlerResult.Status.COMPLETED, ((HttpServletRequest)request).getUserPrincipal(), bst);
                return filterResult;
            }
        }
    }

    /**
     * Gets the CAS proxy ticket that will be used by the STS to get a SAML assertion.
     *
     * @param request
     *            The Http servlet request.
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

    /**
     * Creates a binary security token based on the provided credential.
     */
    private synchronized String getBinarySecurityToken(String credential) {
        JAXBContext context;
        Writer writer = new StringWriter();

        BinarySecurityTokenType binarySecurityTokenType = new BinarySecurityTokenType();
        binarySecurityTokenType.setValueType("#CAS");
        binarySecurityTokenType.setEncodingType(WSConstants.SOAPMESSAGE_NS + "#Base64Binary");
        binarySecurityTokenType.setId("CAS");
        binarySecurityTokenType.setValue(Base64.encode(credential.getBytes()));
        JAXBElement<BinarySecurityTokenType> binarySecurityTokenElement = new JAXBElement<BinarySecurityTokenType>(
                new QName(
                        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
                        "BinarySecurityToken"), BinarySecurityTokenType.class,
                binarySecurityTokenType
        );

        if(marshaller == null) {
            try {
                context = JAXBContext.newInstance(BinarySecurityTokenType.class);
                marshaller = context.createMarshaller();
                marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            } catch (JAXBException e) {
                LOGGER.error("Exception while creating UsernameToken marshaller.", e);
            }
        }

        try {
            marshaller.marshal(binarySecurityTokenElement, writer);
        } catch (JAXBException e) {
            LOGGER.error("Exception while writing username token.", e);
        }

        String binarySecurityToken = writer.toString();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Binary Security Token: " + binarySecurityToken);
        }

        return binarySecurityToken;
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
}
