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

import org.apache.ws.security.WSConstants;
import org.apache.ws.security.util.Base64;
import org.codice.ddf.security.handler.api.BSTAuthenticationToken;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;

public class CASAuthenticationToken extends BSTAuthenticationToken {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(CASAuthenticationToken.class);

    public static final String CAS_ID = "CAS";

    public static final String CAS_VALUE_TYPE = "#" + CAS_ID;

    public CASAuthenticationToken(Principal principal, String proxyTicket) {
        this(principal, proxyTicket, BaseAuthenticationToken.DEFAULT_REALM);
    }

    public CASAuthenticationToken(Principal principal, String proxyTicket, String realm) {
        super(principal, proxyTicket, realm);
        setTokenValueType(WSConstants.X509TOKEN_NS, CAS_VALUE_TYPE);
        setTokenId(CAS_ID);
    }

    public String getProxyTicket() {
        String ticket = (String) getCredentials();
        return ticket;
    }

    public String getUser() {
        String user = null;
        if (principal instanceof Principal)
            user = ((Principal) principal).getName();
        else if (principal instanceof String)
            user = (String) principal;
        return user;
    }

    public byte[] getCertificate() {
        byte[] certs = null;
        if (credentials instanceof byte[])
            certs = (byte[]) credentials;
        return certs;
    }

    @Override
    public String getEncodedCredentials() {
        String encodedTicket = Base64.encode(getProxyTicket().getBytes());
        LOGGER.trace("BST: {}", encodedTicket);
        return encodedTicket;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("User: ");
        sb.append(getUser());
        sb.append("; ticket: ");
        String ticket = getProxyTicket();
        if ((ticket != null) && (ticket.length() > 5))
            sb.append(getProxyTicket().substring(0, 5));
        else
            sb.append(ticket);
        sb.append("...; realm: ");
        sb.append(realm);
        return sb.toString();
    }
}
