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

import ddf.security.principal.AnonymousPrincipal;

/**
 * Authentication token representing an anonymous user's credentials
 */
public class AnonymousAuthenticationToken extends BSTAuthenticationToken {

    public static final String ANONYMOUS_CREDENTIALS = "Anonymous";

    public static final String BST_ANONYMOUS_LN = "Anonymous";

    public static final String ANONYMOUS_TOKEN_VALUE_TYPE =
            BSTAuthenticationToken.BST_NS + BSTAuthenticationToken.TOKEN_VALUE_SEPARATOR
                    + BST_ANONYMOUS_LN;

    public AnonymousAuthenticationToken(String realm) {
        super(new AnonymousPrincipal(), ANONYMOUS_CREDENTIALS, realm);
        setTokenValueType(BSTAuthenticationToken.BST_NS, BST_ANONYMOUS_LN);
        setTokenId(BST_ANONYMOUS_LN);
    }
}
