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

import org.apache.cxf.common.util.StringUtils;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UPAuthenticationToken extends BSTAuthenticationToken {
    private static final String BST_USERNAME = "Username:";

    private static final String BST_PASSWORD = "Password:";

    private static final String BST_REALM = "Realm:";

    private static final String NEWLINE = "\n";

    private static final transient Logger LOGGER = LoggerFactory.getLogger(UPAuthenticationToken.class);

    public UPAuthenticationToken(String username, String password) {
        super(username, password);
    }

    public UPAuthenticationToken(String username, String password, String realm) {
        super(username, password, realm);
        setTokenId(BSTAuthenticationToken.DDF_BST_USERNAME_LN);
    }

    public String getUsername() {
        String username = null;
        if (principal instanceof String)
            username = (String) principal;
        return username;
    }

    public String getPassword() {
        String pw = null;
        if (credentials instanceof String)
            pw = (String) credentials;
        return pw;
    }

    @Override
    public String getEncodedCredentials() {
        String creds = buildCredentialString();
        String encodedCreds = Base64.encode(creds.getBytes());
        LOGGER.trace("BST: {}", encodedCreds);
        return encodedCreds;
    }

    /**
     * Creates an instance of UPAuthenticationToken by parsing the given credential string. The
     * passed boolean indicates if the provided credentials are encoded or not.
     * If the string contains the necessary components (username, password, realm), a new instance of
     * UPAuthenticaitonToken is created and initialized with the credentials. If not, a null value
     * is returned.
     *
     * @param creds unencoded credentials string
     * @return initialized username/password token if parsed successfully, null otherwise
     */
    public static UPAuthenticationToken parse(String creds, boolean isEncoded) {
        UPAuthenticationToken upt = null;

        try {
            String unencodedCreds = isEncoded ? new String(Base64.decode(creds)) : creds;
            if (!StringUtils.isEmpty(unencodedCreds) && unencodedCreds.startsWith(BST_USERNAME)) {
                String[] components = unencodedCreds.split(NEWLINE);
                if (components.length == 3) {
                    String u = UPAuthenticationToken.parseComponent(components[0], BST_USERNAME);
                    String p = UPAuthenticationToken.parseComponent(components[1], BST_PASSWORD);
                    String r = UPAuthenticationToken.parseComponent(components[2], BST_REALM);

                    // require a username, everything else can be empty
                    if (!StringUtils.isEmpty(u))
                        upt = new UPAuthenticationToken(u, p, r);
                }
            }
        } catch (WSSecurityException e) {
            LOGGER.warn("Exception decoding specified credentials: {}", e.getMessage(), e);
        }
        return upt;
    }

    private static String parseComponent(String s, String expectedStartsWith) {
        String value = "";
        int minLength = expectedStartsWith == null ? 1 : expectedStartsWith.length() + 1;
        if ((s != null) && (s.length() > minLength)) {
            value = s.substring(minLength - 1);
        }
        return value;
    }

    private String buildCredentialString() {
        StringBuilder builder = new StringBuilder();
        builder.append(BST_USERNAME);
        builder.append(getUsername());
        builder.append(NEWLINE);
        builder.append(BST_PASSWORD);
        builder.append(getPassword());
        builder.append(NEWLINE);
        builder.append(BST_REALM);
        builder.append(realm);
        String retVal = builder.toString();
        LOGGER.trace("Credential String: {}", retVal);
        return retVal;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("username: ");
        sb.append(getUsername());
        sb.append("; password: *****; realm: ");
        sb.append(realm);
        return sb.toString();
    }
}
