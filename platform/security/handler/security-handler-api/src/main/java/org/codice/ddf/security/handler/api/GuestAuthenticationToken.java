/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.security.handler.api;

import org.apache.commons.lang.StringUtils;

import ddf.security.common.audit.SecurityLogger;
import ddf.security.principal.GuestPrincipal;

/**
 * Authentication token representing a guest user's credentials
 */
public class GuestAuthenticationToken extends BSTAuthenticationToken {

    public static final String GUEST_CREDENTIALS = "Guest";

    public static final String BST_GUEST_LN = "Guest";

    public static final String GUEST_TOKEN_VALUE_TYPE =
            BSTAuthenticationToken.BST_NS + BSTAuthenticationToken.TOKEN_VALUE_SEPARATOR
                    + BST_GUEST_LN;

    public GuestAuthenticationToken(String realm, String ip) {
        super(new GuestPrincipal(ip), GUEST_CREDENTIALS, realm);
        setTokenValueType(BSTAuthenticationToken.BST_NS, BST_GUEST_LN);
        setTokenId(BST_GUEST_LN);

        if (!StringUtils.isEmpty(ip)) {
            SecurityLogger.audit("Guest token generated for IP address: " + ip);
        }
    }

    public String getIpAddress() {
        String ip = null;
        if (principal instanceof GuestPrincipal) {
            ip = ((GuestPrincipal) principal).getAddress();
        } else if (principal instanceof String) {
            ip = GuestPrincipal.parseAddressFromName((String) principal);
        }
        return ip;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Guest IP: ");
        sb.append(getIpAddress());
        sb.append("; realm: ");
        sb.append(realm);
        return sb.toString();
    }
}
