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

import ddf.security.principal.AnonymousPrincipal;
import org.apache.cxf.common.util.StringUtils;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Authentication token representing an anonymous user's credentials
 */
public class AnonymousAuthenticationToken extends BSTAuthenticationToken {

    public static final String ANONYMOUS_CREDENTIALS = "Anonymous";

    private static final Logger LOGGER = LoggerFactory.getLogger(AnonymousAuthenticationToken.class);

    private static final String BST_CREDENTIALS = "Credentials:";

    private static final String BST_REALM = "Realm:";

    private static final String NEWLINE = "\n";

    public AnonymousAuthenticationToken(String realm) {
        super(new AnonymousPrincipal(), ANONYMOUS_CREDENTIALS, realm);
        setTokenId(BSTAuthenticationToken.DDF_BST_ANONYMOUS_LN);
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
    public static AnonymousAuthenticationToken parse(String creds, boolean isEncoded) {
        AnonymousAuthenticationToken anont = null;

        try {
            String unencodedCreds = isEncoded ? new String(Base64.decode(creds)) : creds;
            if (!StringUtils.isEmpty(unencodedCreds) && unencodedCreds.startsWith(BST_CREDENTIALS)) {
                String[] components = unencodedCreds.split(NEWLINE);
                if (components.length == 2) {
                    String c = AnonymousAuthenticationToken.parseComponent(components[0], BST_CREDENTIALS);
                    String r = AnonymousAuthenticationToken.parseComponent(components[1], BST_REALM);

                    // require a username, everything else can be empty
                    if (!StringUtils.isEmpty(r)) {
                        anont = new AnonymousAuthenticationToken(r);
                        anont.setCredentials(c);
                    }
                }
            }
        } catch (WSSecurityException e) {
            LOGGER.warn("Exception decoding specified credentials: {}", e.getMessage(), e);
        }
        return anont;
    }

    private static String parseComponent(String s, String expectedStartsWith) {
        String value = "";
        int minLength = expectedStartsWith == null ? 1 : expectedStartsWith.length() + 1;
        if ((s != null) && (s.length() > minLength)) {
            value = s.substring(minLength - 1);
        }
        return value;
    }

    @Override
    public String getEncodedCredentials() {
        String creds = buildCredentialString();
        String encodedCreds = Base64.encode(creds.getBytes());
        LOGGER.trace("BST: {}", encodedCreds);
        return encodedCreds;
    }

    private String buildCredentialString() {
        StringBuilder builder = new StringBuilder();
        builder.append(BST_CREDENTIALS);
        builder.append(credentials);
        builder.append(NEWLINE);
        builder.append(BST_REALM);
        builder.append(realm);
        String retVal = builder.toString();
        LOGGER.trace("Credential String: {}", retVal);
        return retVal;
    }
}
