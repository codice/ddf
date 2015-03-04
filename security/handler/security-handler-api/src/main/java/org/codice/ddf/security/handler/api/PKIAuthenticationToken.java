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

import org.apache.ws.security.WSConstants;
import org.apache.ws.security.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;

public class PKIAuthenticationToken extends BSTAuthenticationToken {
    private static final Logger LOGGER = LoggerFactory.getLogger(PKIAuthenticationToken.class);

    public static final String X509_PKI_PATH = "X509PKIPathv1";
    //public static final String GX_X509_PKI_PATH="#GXX509PKIPathv1";
    //public static final String X509_V3 = "#X509v3";

    public PKIAuthenticationToken(Principal principal, byte[] certificates) {
        this(principal, certificates, BaseAuthenticationToken.DEFAULT_REALM);
    }

    public PKIAuthenticationToken(Principal principal, byte[] certificates, String realm) {
        super(principal, certificates, realm);
        setTokenValueType(WSConstants.X509TOKEN_NS, X509_PKI_PATH);
        setTokenId(BSTAuthenticationToken.DDF_BST_X509_LN);
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
    public String getEncodedCredentials() {
        String certificate = Base64.encode(getCertificate());
        LOGGER.trace("BST: {}", certificate);
        return certificate;
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
