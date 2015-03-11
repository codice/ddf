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

import org.opensaml.xml.util.Base64;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class UPAuthenticationTokenTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(UPAuthenticationToken.class);

    public static final String TEST_NAME = "name";

    public static final String TEST_PW = "pw";

    public static final String TEST_REALM = "realm";

    public static final String TEST_CREDS = BSTAuthenticationToken.BST_PRINCIPAL + TEST_NAME +
            BSTAuthenticationToken.NEWLINE + BSTAuthenticationToken.BST_CREDENTIALS + TEST_PW +
            BSTAuthenticationToken.NEWLINE + BSTAuthenticationToken.BST_REALM + TEST_REALM;

    @Test
    public void testGetters() throws Exception {
        UPAuthenticationToken token = new UPAuthenticationToken(TEST_NAME, TEST_PW, TEST_REALM);
        assertEquals(TEST_NAME, token.getUsername());
        assertEquals(TEST_PW, token.getPassword());
        assertEquals(TEST_REALM, token.getRealm());
        assertEquals(UPAuthenticationToken.UP_TOKEN_VALUE_TYPE, token.tokenValueType);
        assertEquals(UPAuthenticationToken.BST_USERNAME_LN, token.tokenId);

        token = new UPAuthenticationToken(TEST_NAME, TEST_PW);
        assertEquals(TEST_NAME, token.getUsername());
        assertEquals(TEST_PW, token.getPassword());
        assertEquals(BaseAuthenticationToken.DEFAULT_REALM, token.getRealm());
        assertEquals(UPAuthenticationToken.UP_TOKEN_VALUE_TYPE, token.tokenValueType);
        assertEquals(UPAuthenticationToken.BST_USERNAME_LN, token.tokenId);

        token = new UPAuthenticationToken(null, null, null);
        assertNull(token.getUsername());
        assertNull(token.getPassword());
        assertNull(token.getRealm());
        assertEquals(UPAuthenticationToken.UP_TOKEN_VALUE_TYPE, token.tokenValueType);
        assertEquals(UPAuthenticationToken.BST_USERNAME_LN, token.tokenId);

        token = new UPAuthenticationToken("", "", "");
        assertEquals("", token.getUsername());
        assertEquals("", token.getPassword());
        assertEquals("", token.getRealm());
        assertEquals(UPAuthenticationToken.UP_TOKEN_VALUE_TYPE, token.tokenValueType);
        assertEquals(UPAuthenticationToken.BST_USERNAME_LN, token.tokenId);
    }

    @Test
    public void testGetBinarySecurityToken() throws Exception {
        UPAuthenticationToken token = new UPAuthenticationToken(TEST_NAME, TEST_PW, TEST_REALM);
        String expectedBST = Base64.encodeBytes(TEST_CREDS.getBytes(), Base64.DONT_BREAK_LINES);
        assertEquals(expectedBST, token.getEncodedCredentials());
    }

    @Test
    public void testParse() throws Exception {
        // test normal case
        BaseAuthenticationToken bat = UPAuthenticationToken.parse(TEST_CREDS, false);
        UPAuthenticationToken upt = new UPAuthenticationToken(bat.getPrincipal().toString(),
                bat.getCredentials().toString(), bat.getRealm());
        assertNotNull(upt);
        assertEquals(TEST_NAME, upt.getUsername());
        assertEquals(TEST_PW, upt.getPassword());
        assertEquals(TEST_REALM, upt.getRealm());
        assertEquals(UPAuthenticationToken.UP_TOKEN_VALUE_TYPE, upt.tokenValueType);
        assertEquals(UPAuthenticationToken.BST_USERNAME_LN, upt.tokenId);
    }
}
