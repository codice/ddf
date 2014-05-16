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
package org.codice.security.handler.anonymous;


import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.ws.security.sts.provider.model.secext.AttributedString;
import org.apache.cxf.ws.security.sts.provider.model.secext.PasswordString;
import org.apache.cxf.ws.security.sts.provider.model.secext.UsernameTokenType;
import org.apache.ws.security.WSConstants;
import org.codice.security.handler.api.AuthenticationHandler;
import org.codice.security.handler.api.HandlerResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBElement;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Handler that allows anonymous user access via a guest user account. The guest/guest account
 * must be present in the user store for this handler to work correctly.
 */
public class AnonymousHandler implements AuthenticationHandler {
    public static final Logger logger = LoggerFactory.getLogger(AnonymousHandler.class.getName());

    @Override
    public HandlerResult getNormalizedToken(ServletRequest request, ServletResponse response, FilterChain chain, boolean resolve) {
        HandlerResult result = new HandlerResult();
        result.setStatus(HandlerResult.FilterStatus.NO_ACTION);

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
        JAXB.marshal(usernameTokenType, writer);

        String usernameSecurityToken = writer.toString();
        logger.debug("Security token returned: {}", usernameSecurityToken);

        result.setAuthCredentials(usernameSecurityToken);
        result.setStatus(HandlerResult.FilterStatus.COMPLETED);
        return result;
    }
}
