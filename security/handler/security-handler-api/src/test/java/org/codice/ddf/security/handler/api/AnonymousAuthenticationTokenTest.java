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
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AnonymousAuthenticationTokenTest {

    @Test
    public void testConstructor() {
        final String realm = "someRealm";
        AnonymousAuthenticationToken token = new AnonymousAuthenticationToken(realm);
        assertTrue(token.getPrincipal() instanceof AnonymousPrincipal);
        assertEquals(AnonymousAuthenticationToken.ANONYMOUS_CREDENTIALS, token.getCredentials());
        assertEquals(realm, token.getRealm());
        assertEquals(AnonymousAuthenticationToken.ANONYMOUS_TOKEN_VALUE_TYPE, token.tokenValueType);
        assertEquals(AnonymousAuthenticationToken.BST_ANONYMOUS_LN, token.tokenId);
    }
}
