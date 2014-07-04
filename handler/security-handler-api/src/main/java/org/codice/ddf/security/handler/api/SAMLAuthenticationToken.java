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
import org.w3c.dom.Element;

import java.security.Principal;

public class SAMLAuthenticationToken extends BaseAuthenticationToken {

    /**
     * Constructor that only allows SecurityToken objects to be used as the credentials.
     *
     * @param principal represents the
     * @param token
     * @param realm
     */
    public SAMLAuthenticationToken(Principal principal, SecurityToken token, String realm) {
        super(principal, realm, token);
    }

    /**
     * Returns the SAML token as a DOM Element.
     *
     * @return the SAML token as a DOM element or null if it doesn't exist
     */
    public Element getSAMLTokenAsElement() {
        SecurityToken token = (SecurityToken) getCredentials();
        if (token != null) {
            return token.getToken();
        }
        return null;
    }
}
