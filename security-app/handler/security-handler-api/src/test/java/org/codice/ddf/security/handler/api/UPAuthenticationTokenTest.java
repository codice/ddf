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

import junit.framework.TestCase;
import org.apache.ws.security.util.Base64;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UPAuthenticationTokenTest extends TestCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(UPAuthenticationToken.class);

    public static final String TEST_NAME = "name";

    public static final String TEST_PW = "pw";

    public static final String TEST_REALM = "realm";

    public static final String TEST_CREDS = "Username:name\nPassword:pw\nRealm:realm";

    public void testGetters() throws Exception {
        UPAuthenticationToken token = new UPAuthenticationToken(TEST_NAME, TEST_PW, TEST_REALM);
        assertEquals(TEST_NAME, token.getUsername());
        assertEquals(TEST_PW, token.getPassword());
        assertEquals(TEST_REALM, token.getRealm());

        token = new UPAuthenticationToken(TEST_NAME, TEST_PW);
        assertEquals(TEST_NAME, token.getUsername());
        assertEquals(TEST_PW, token.getPassword());
        assertEquals(BaseAuthenticationToken.DEFAULT_REALM, token.getRealm());

        token = new UPAuthenticationToken(null, null, null);
        assertNull(token.getUsername());
        assertNull(token.getPassword());
        assertNull(token.getRealm());

        token = new UPAuthenticationToken("", "", "");
        assertEquals("", token.getUsername());
        assertEquals("", token.getPassword());
        assertEquals("", token.getRealm());
    }

    public void testGetBinarySecurityToken() throws Exception {
        UPAuthenticationToken token = new UPAuthenticationToken(TEST_NAME, TEST_PW, TEST_REALM);
        String expectedBST = Base64.encode(TEST_CREDS.getBytes());
        assertEquals(expectedBST, token.getEncodedCredentials());
    }

    public void testParse() throws Exception {
        // test normal case
        UPAuthenticationToken upt = UPAuthenticationToken.parse(TEST_CREDS, false);
        assertNotNull(upt);
        assertEquals(TEST_NAME, upt.getUsername());
        assertEquals(TEST_PW, upt.getPassword());
        assertEquals(TEST_REALM, upt.getRealm());

        // test missing/empty fields
        upt = UPAuthenticationToken.parse("Username:name\n\nRealm:realm", false);
        assertNotNull(upt);
        assertEquals(TEST_NAME, upt.getUsername());
        assertEquals("", upt.getPassword());
        assertEquals(TEST_REALM, upt.getRealm());

        upt = UPAuthenticationToken.parse("Username:name\nPassword:\nRealm:", false);
        assertNotNull(upt);
        assertEquals(TEST_NAME, upt.getUsername());
        assertEquals("", upt.getPassword());
        assertEquals("", upt.getRealm());

        upt = UPAuthenticationToken.parse("Username:\nPassword:\nRealm:", false);
        assertNull(upt);

        assertNull(UPAuthenticationToken.parse("garbage string", false));
        assertNull(UPAuthenticationToken.parse(null, false));
        assertNull(UPAuthenticationToken.parse("", false));

        upt = UPAuthenticationToken.parse("Username:admin\nPassword:admin\nRealm:DDF", false);
        String expectedToken = "<BinarySecurityToken ValueType=\"urn:ddf:security:sso#DDFToken\" " +
          "EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\" " +
          "ns1:Id=\"DDFUsername\" " +
          "xmlns=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" " +
          "xmlns:ns1=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">" +
          "VXNlcm5hbWU6YWRtaW4KUGFzc3dvcmQ6YWRtaW4KUmVhbG06RERG</BinarySecurityToken>";
        String bst = upt.getBinarySecurityToken();
        assertXMLEqual(expectedToken, bst);

        // make sure we unencode if necessary
        upt = UPAuthenticationToken.parse(Base64.encode(TEST_CREDS.getBytes()), true);
        assertNotNull(upt);
        assertEquals(TEST_NAME, upt.getUsername());
        assertEquals(TEST_PW, upt.getPassword());
        assertEquals(TEST_REALM, upt.getRealm());
    }
}
