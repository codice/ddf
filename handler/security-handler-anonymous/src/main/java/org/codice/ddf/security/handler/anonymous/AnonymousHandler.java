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


import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.ws.security.sts.provider.model.secext.AttributedString;
import org.apache.cxf.ws.security.sts.provider.model.secext.PasswordString;
import org.apache.cxf.ws.security.sts.provider.model.secext.UsernameTokenType;
import org.apache.ws.security.WSConstants;
import org.codice.ddf.security.handler.api.AuthenticationHandler;
import org.codice.ddf.security.handler.api.HandlerResult;
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
import java.io.StringWriter;
import java.io.Writer;
import java.security.Principal;

/**
 * Handler that allows anonymous user access via a guest user account. The guest/guest account
 * must be present in the user store for this handler to work correctly.
 */
public class AnonymousHandler implements AuthenticationHandler {
    public static final Logger logger = LoggerFactory.getLogger(AnonymousHandler.class.getName());

    /**
     * Anonymous type to use when configuring context policy.
     */
    public static final String AUTH_TYPE = "ANON";

    private Marshaller marshaller = null;

    @Override
    public String getAuthenticationType() {
        return AUTH_TYPE;
    }

    /**
     * Extracts a Principal from a UsernameToken
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

    @Override
    public HandlerResult getNormalizedToken(ServletRequest request, ServletResponse response, FilterChain chain, boolean resolve) {
        HandlerResult result = new HandlerResult();
        result.setStatus(HandlerResult.Status.NO_ACTION);

        // For anonymous - always generate authentication credentials as 'guest'
        UsernameTokenType usernameTokenType = new UsernameTokenType();
        AttributedString user = new AttributedString();
        user.setValue("guest");
        usernameTokenType.setUsername(user);

        // Add a password
        PasswordString password = new PasswordString();
        password.setValue("guest");
        password.setType(WSConstants.PASSWORD_TEXT);
        JAXBElement<PasswordString> passwordType = new JAXBElement<PasswordString>(QNameConstants.PASSWORD, PasswordString.class, password);
        usernameTokenType.getAny().add(passwordType);

        Writer writer = new StringWriter();

        JAXBContext context;
        if (marshaller == null) {
            try {
                context = JAXBContext.newInstance(UsernameTokenType.class);
                marshaller = context.createMarshaller();
                marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            } catch (JAXBException e) {
                logger.error("Exception while creating UsernameToken marshaller.", e);
            }
        }

        JAXBElement<UsernameTokenType> usernameTokenElement = new JAXBElement<UsernameTokenType>(
                new QName(
                        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
                        "UsernameToken"), UsernameTokenType.class,
                usernameTokenType);

        try {
            marshaller.marshal(usernameTokenElement, writer);
        } catch (JAXBException e) {
            logger.error("Unable to create username token for anonymous user.", e);
        }

        String usernameSecurityToken = writer.toString();
        logger.debug("Security token returned: {}", usernameSecurityToken);

        result.setAuthCredentials(usernameSecurityToken);
        result.setStatus(HandlerResult.Status.COMPLETED);
        result.setPrincipal(getPrincipal(usernameTokenType));
        return result;
    }

    @Override
    public HandlerResult handleError(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws ServletException {
        HandlerResult result = new HandlerResult();
        logger.debug("In error handler for anonymous - returning no action taken.");
        result.setStatus(HandlerResult.Status.NO_ACTION);
        return result;
    }
}
