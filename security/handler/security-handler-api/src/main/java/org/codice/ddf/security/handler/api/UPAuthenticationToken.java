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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UPAuthenticationToken extends BSTAuthenticationToken {

    private static final Logger LOGGER = LoggerFactory.getLogger(UPAuthenticationToken.class);

    public static final String BST_USERNAME_LN = "Username";

    public static final String UP_TOKEN_VALUE_TYPE =
            BSTAuthenticationToken.BST_NS + BSTAuthenticationToken.TOKEN_VALUE_SEPARATOR
                    + BST_USERNAME_LN;

    public UPAuthenticationToken(String username, String password) {
        this(username, password, BaseAuthenticationToken.DEFAULT_REALM);
    }

    public UPAuthenticationToken(String username, String password, String realm) {
        super(username, password, realm);
        setTokenValueType(BSTAuthenticationToken.BST_NS, BST_USERNAME_LN);
        setTokenId(BST_USERNAME_LN);
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
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("username: ");
        sb.append(getUsername());
        sb.append("; password: *****; realm: ");
        sb.append(realm);
        return sb.toString();
    }
}
