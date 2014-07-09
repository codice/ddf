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
package org.codice.ddf.security.handler.anonymous;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.ws.security.sts.provider.model.secext.AttributedString;
import org.apache.cxf.ws.security.sts.provider.model.secext.PasswordString;
import org.apache.cxf.ws.security.sts.provider.model.secext.UsernameTokenType;
import org.apache.ws.security.WSConstants;
import org.codice.ddf.security.handler.api.AuthenticationHandler;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.jasypt.contrib.org.apache.commons.codec_1_3.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.security.Principal;

/**
 * Handler that allows anonymous user access via a guest user account. The guest/guest account
 * must be present in the user store for this handler to work correctly.
 */
public class AnonymousHandler implements AuthenticationHandler {
    public static final Logger LOGGER = LoggerFactory.getLogger(AnonymousHandler.class.getName());

    /**
     * Anonymous type to use when configuring context policy.
     */
    public static final String AUTH_TYPE = "ANON";

    private static final JAXBContext utContext = initContext();

    private static JAXBContext initContext() {
        try {
            return JAXBContext.newInstance(UsernameTokenType.class);
        } catch (JAXBException e) {
            LOGGER.error("Unable to create UsernameToken JAXB context.", e);
        }
        return null;
    }

    @Override
    public String getAuthenticationType() {
        return AUTH_TYPE;
    }

    /**
     * Extracts a Principal from a UsernameToken
     *
     * @param result
     * @return Principal
     */
    private Principal getPrincipal(final UsernameTokenType result) {
        return new Principal() {
            private String username = result.getUsername().getValue();

            @Override public String getName() {
                return username;
            }
        };
    }

    /**
     * This method takes an anonymous request and attaches a UsernameTokenType
     * to the HTTP request to allow access. The method also allows the user to
     * sign-in and authenticate.
     *
     * @param request  http request to obtain attributes from and to pass into any local filter chains required
     * @param response http response to return http responses or redirects
     * @param chain    original filter chain (should not be called from your handler)
     * @param resolve  flag with true implying that credentials should be obtained, false implying return if no credentials are found.
     * @return HandlerResult
     */
    @Override
    public HandlerResult getNormalizedToken(ServletRequest request, ServletResponse response,
            FilterChain chain, boolean resolve) {

        HandlerResult handlerResult;

        UsernameTokenType result = setAuthenticationInfo((HttpServletRequest) request);
        String usernameToken = getUsernameTokenElement(result);
        Principal principal = getPrincipal(result);
        handlerResult = new HandlerResult(HandlerResult.Status.COMPLETED, principal,
                usernameToken);

        return handlerResult;
    }

    /**
     * This method handles errors related to failures related to credentials
     * verification. It returns a HTTP status code, 500 Internal Server Error,
     * which is then handled in Menu.view.js of the ddf-ui module.
     *
     * @param servletRequest  http request to obtain attributes from and to pass into any local filter chains required
     * @param servletResponse http response to return http responses or redirects
     * @param chain           original filter chain (should not be called from your handler)
     * @return HandlerResult
     * @throws ServletException
     */
    @Override
    public HandlerResult handleError(ServletRequest servletRequest, ServletResponse servletResponse,
            FilterChain chain) throws ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
        httpResponse.setStatus(500);
        try {
            httpResponse.getWriter().write("Username/Password is invalid.");
            httpResponse.flushBuffer();
        } catch (IOException e) {
            LOGGER.debug("Failed to send auth response: {}", e);
        }

        HandlerResult result = new HandlerResult();
        LOGGER.debug("In error handler for anonymous - returning no action taken.");
        result.setStatus(HandlerResult.Status.NO_ACTION);
        return result;
    }

    /**
     * Returns the UsernameToken marshalled as a String so that it can be attached to the
     * {@link org.codice.ddf.security.handler.api.HandlerResult} object.
     *
     * @param usernameTokenType
     * @return String
     */
    private synchronized String getUsernameTokenElement(UsernameTokenType usernameTokenType) {
        Writer writer = new StringWriter();
        Marshaller marshaller = null;
        if (utContext != null) {
            try {
                marshaller = utContext.createMarshaller();
                marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            } catch (JAXBException e) {
                LOGGER.error("Exception while creating UsernameToken marshaller.", e);
            }

            JAXBElement<UsernameTokenType> usernameTokenElement = new JAXBElement<UsernameTokenType>(
                    new QName(
                            "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
                            "UsernameToken"), UsernameTokenType.class,
                    usernameTokenType
            );

            if (marshaller != null) {
                try {
                    marshaller.marshal(usernameTokenElement, writer);
                } catch (JAXBException e) {
                    LOGGER.error("Exception while writing username token.", e);
                }
            }
        }

        return writer.toString();
    }

    /**
     * This method uses the data passed in the HttpServletRequest to generate
     * and return UsernameTokenType.
     *
     * @param request http request to obtain attributes from and to pass into any local filter chains required
     * @return UsernameTokenType
     */
    private UsernameTokenType setAuthenticationInfo(HttpServletRequest request) {
        HttpServletRequest httpRequest = request;

        String username = "guest";
        String password = "guest";

        /**
         * Parse the header data and extract the username and password.
         *
         * Change the username and password if request contains values.
         */
        String header = httpRequest.getHeader("Authorization");
        if (!StringUtils.isEmpty(header)) {
            String headerData[] = header.split(" ");
            if (headerData.length == 2) {
                String decodedHeader = new String(Base64.decodeBase64(headerData[1].getBytes()));
                String decodedHeaderData[] = decodedHeader.split(":");
                if (decodedHeaderData.length == 2) {
                        username = decodedHeaderData[0];
                        password = decodedHeaderData[1];
                }
            }
        }

        /**
         * Use the collected information to set the username and password and
         * generate UsernameTokenType to return.
         */
        UsernameTokenType usernameTokenType = new UsernameTokenType();
        AttributedString user = new AttributedString();
        user.setValue(username);
        usernameTokenType.setUsername(user);
        PasswordString passwordString = new PasswordString();
        passwordString.setValue(password);
        passwordString.setType(WSConstants.PASSWORD_TEXT);
        JAXBElement<PasswordString> passwordType = new JAXBElement<PasswordString>(
                QNameConstants.PASSWORD, PasswordString.class, passwordString);
        usernameTokenType.getAny().add(passwordType);

        return usernameTokenType;
    }
}