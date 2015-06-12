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
package org.codice.ddf.security.handler.api;

import java.security.Principal;

import org.opensaml.xml.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PKIAuthenticationToken extends BSTAuthenticationToken {
    public static final String BST_X509_LN = "X509";

    public static final String PKI_TOKEN_VALUE_TYPE =
            BSTAuthenticationToken.BST_NS + BSTAuthenticationToken.TOKEN_VALUE_SEPARATOR
                    + BST_X509_LN;

    private static final Logger LOGGER = LoggerFactory.getLogger(PKIAuthenticationToken.class);

    public PKIAuthenticationToken(Principal principal, byte[] certificates) {
        this(principal, certificates, BaseAuthenticationToken.DEFAULT_REALM);
    }

    public PKIAuthenticationToken(Object principal, String encodedCerts, String realm) {
        this(principal, encodedCerts.getBytes(), realm);
        credentials = Base64.decode(encodedCerts);
    }

    public PKIAuthenticationToken(Object principal, byte[] certificates, String realm) {
        super(principal, certificates, realm);
        setTokenValueType(BSTAuthenticationToken.BST_NS, BST_X509_LN);
        setTokenId(BST_X509_LN);
    }

    public String getDn() {
        String dn = null;
        if (principal instanceof Principal) {
            dn = ((Principal) principal).getName();
        } else if (principal instanceof String) {
            dn = (String) principal;
        }
        return dn;
    }

    public byte[] getCertificate() {
        byte[] certs = null;
        if (credentials instanceof byte[]) {
            certs = (byte[]) credentials;
        }
        return certs;
    }

    @Override
    public String getCredentials() {
        if (credentials instanceof byte[]) {
            return Base64.encodeBytes((byte[]) credentials, Base64.DONT_BREAK_LINES);
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
