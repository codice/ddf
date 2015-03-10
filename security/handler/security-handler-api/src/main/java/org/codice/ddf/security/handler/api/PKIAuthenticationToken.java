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

import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;

public class PKIAuthenticationToken extends BSTAuthenticationToken {
    private static final Logger LOGGER = LoggerFactory.getLogger(PKIAuthenticationToken.class);

    public static final String BST_X509_LN = "X509";

    public static final String PKI_TOKEN_VALUE_TYPE =
            BSTAuthenticationToken.BST_NS + BSTAuthenticationToken.TOKEN_VALUE_SEPARATOR
                    + BST_X509_LN;

    public PKIAuthenticationToken(Principal principal, byte[] certificates) {
        this(principal, certificates, BaseAuthenticationToken.DEFAULT_REALM);
    }

    public PKIAuthenticationToken(Object principal, String encodedCerts, String realm) {
        this(principal, encodedCerts.getBytes(), realm);
        try {
            credentials = Base64.decode(encodedCerts);
        } catch (WSSecurityException e) {
            LOGGER.warn("Unable to decode certs", e);
        }

    }

    public PKIAuthenticationToken(Object principal, byte[] certificates, String realm) {
        super(principal, certificates, realm);
        setTokenValueType(BSTAuthenticationToken.BST_NS, BST_X509_LN);
        setTokenId(BST_X509_LN);
    }

    public String getDn() {
        String dn = null;
        if (principal instanceof Principal)
            dn = ((Principal) principal).getName();
        else if (principal instanceof String)
            dn = (String) principal;
        return dn;
    }

    public byte[] getCertificate() {
        byte[] certs = null;
        if (credentials instanceof byte[])
            certs = (byte[]) credentials;
        return certs;
    }

    @Override
    public String getCredentials() {
        if (credentials instanceof byte[]) {
            return Base64.encode((byte[]) credentials);
        }
        return "";
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("dn: ");
        sb.append(getDn());
        sb.append("; realm: ");
        sb.append(realm);
        return sb.toString();
    }
}
