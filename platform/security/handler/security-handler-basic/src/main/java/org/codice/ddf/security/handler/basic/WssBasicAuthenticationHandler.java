/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.security.handler.basic;

import java.io.StringWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.cxf.common.jaxb.JAXBContextCache;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.ws.security.sts.provider.model.ObjectFactory;
import org.apache.cxf.ws.security.sts.provider.model.secext.AttributedString;
import org.apache.cxf.ws.security.sts.provider.model.secext.PasswordString;
import org.apache.cxf.ws.security.sts.provider.model.secext.UsernameTokenType;
import org.apache.wss4j.dom.WSConstants;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.w3c.dom.Document;

public class WssBasicAuthenticationHandler extends AbstractBasicAuthenticationHandler {
    /**
     * WS-Security compliant basic type to use when configuring context policy.
     */
    private static final String AUTH_TYPE = "WSSBASIC";

    protected BaseAuthenticationToken getBaseAuthenticationToken(String realm, String username,
            String password) {
        UsernameTokenType usernameTokenType = new UsernameTokenType();
        AttributedString user = new AttributedString();
        user.setValue(username);
        usernameTokenType.setUsername(user);

        // Add a password
        PasswordString pass = new PasswordString();
        pass.setValue(password);
        pass.setType(WSConstants.PASSWORD_TEXT);
        JAXBElement<PasswordString> passwordType = new JAXBElement<PasswordString>(
                QNameConstants.PASSWORD, PasswordString.class, pass);
        usernameTokenType.getAny().add(passwordType);
        // Marshall the received JAXB object into a DOM Element
        String usernameToken = null;
        Writer writer = new StringWriter();
        try {
            Set<Class<?>> classes = new HashSet<Class<?>>();
            classes.add(ObjectFactory.class);
            classes.add(
                    org.apache.cxf.ws.security.sts.provider.model.wstrust14.ObjectFactory.class);

            JAXBContextCache.CachedContextAndSchemas cache = JAXBContextCache
                    .getCachedContextAndSchemas(classes, null, null, null, false);
            JAXBContext jaxbContext = cache.getContext();

            Marshaller marshaller = jaxbContext.createMarshaller();
            Document doc = DOMUtils.createDocument();
            JAXBElement<UsernameTokenType> tokenType = new JAXBElement<UsernameTokenType>(
                    QNameConstants.USERNAME_TOKEN, UsernameTokenType.class, usernameTokenType);
            marshaller.marshal(tokenType, writer);
            usernameToken = writer.toString();
        } catch (JAXBException ex) {
            LOGGER.warn("", ex);
        }
        BaseAuthenticationToken baseAuthenticationToken = new BaseAuthenticationToken(null, null,
                usernameToken);
        baseAuthenticationToken.setUseWssSts(true);
        return baseAuthenticationToken;
    }

    @Override
    public String getAuthenticationType() {
        return AUTH_TYPE;
    }
}
