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
package org.codice.ddf.security.handler.api;


import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

import java.security.Principal;

public class SAMLAuthenticationToken extends BaseAuthenticationToken {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(SAMLAuthenticationToken.class);
    boolean reference = true;
    /**
     * Constructor that only allows SecurityToken objects to be used as the credentials.
     *
     * @param principal represents the
     * @param token
     * @param realm
     */
    public SAMLAuthenticationToken(Principal principal, SecurityToken token, String realm) {
        super(principal, realm, token);
        reference = false;
    }

    public SAMLAuthenticationToken(Principal principal, String samlRef, String realm) {
        super(principal, realm, samlRef);
        reference = true;
    }

    public boolean isReference() {
        return reference;
    }

    public void replaceReferenece(SecurityToken token) {
        if (reference) {
            credentials = token;
            reference = false;
        } else {
            LOGGER.warn("Current token is not a reference - call to replace is ignored.");
        }
    }

    /**
     * Returns the SAML token as a DOM Element.
     *
     * @return the SAML token as a DOM element or null if it doesn't exist
     */
    public Element getSAMLTokenAsElement() {
        if (reference) {
            LOGGER.warn("Attempting to return a SAML token without converting from a reference.");
            return null;
        }

        SecurityToken token = (SecurityToken) getCredentials();
        if (token != null) {
            return token.getToken();
        }
        return null;
    }

    @Override
    public String getCredentialsAsXMLString() {
        String creds = "";
        Element element = getSAMLTokenAsElement();
        if (element != null) {
            DOMImplementationLS lsImpl = (DOMImplementationLS)element.getOwnerDocument().getImplementation().getFeature("LS", "3.0");
            LSSerializer serializer = lsImpl.createLSSerializer();
            serializer.getDomConfig().setParameter("xml-declaration", false); //by default its true, so set it to false to get String without xml-declaration
            creds = serializer.writeToString(element);
            LOGGER.trace("XML representation of SAML token: {}", creds);
        }
        return creds;
    }
}
