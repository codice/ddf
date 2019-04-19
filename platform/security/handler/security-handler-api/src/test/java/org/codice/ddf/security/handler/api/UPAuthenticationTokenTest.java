/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.security.handler.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Base64;
import org.junit.Test;

public class UPAuthenticationTokenTest {
  public static final String TEST_NAME = "name";

  public static final String TEST_PW = "pw";

  public static final String TEST_CREDS =
      BSTAuthenticationToken.BST_PRINCIPAL
          + TEST_NAME
          + BSTAuthenticationToken.NEWLINE
          + BSTAuthenticationToken.BST_CREDENTIALS
          + TEST_PW;

  @Test
  public void testGetters() throws Exception {
    UPAuthenticationToken token = new UPAuthenticationToken(TEST_NAME, TEST_PW);
    assertEquals(TEST_NAME, token.getUsername());
    assertEquals(TEST_PW, token.getPassword());
    assertEquals(UPAuthenticationToken.UP_TOKEN_VALUE_TYPE, token.tokenValueType);
    assertEquals(UPAuthenticationToken.BST_USERNAME_LN, token.tokenId);

    token = new UPAuthenticationToken(TEST_NAME, TEST_PW);
    assertEquals(TEST_NAME, token.getUsername());
    assertEquals(TEST_PW, token.getPassword());
    assertEquals(UPAuthenticationToken.UP_TOKEN_VALUE_TYPE, token.tokenValueType);
    assertEquals(UPAuthenticationToken.BST_USERNAME_LN, token.tokenId);

    token = new UPAuthenticationToken(null, null);
    assertNull(token.getUsername());
    assertNull(token.getPassword());
    assertEquals(UPAuthenticationToken.UP_TOKEN_VALUE_TYPE, token.tokenValueType);
    assertEquals(UPAuthenticationToken.BST_USERNAME_LN, token.tokenId);

    token = new UPAuthenticationToken("", "");
    assertEquals("", token.getUsername());
    assertEquals("", token.getPassword());
    assertEquals(UPAuthenticationToken.UP_TOKEN_VALUE_TYPE, token.tokenValueType);
    assertEquals(UPAuthenticationToken.BST_USERNAME_LN, token.tokenId);
  }

  @Test
  public void testGetBinarySecurityToken() throws Exception {
    UPAuthenticationToken token = new UPAuthenticationToken(TEST_NAME, TEST_PW);
    String expectedBST = Base64.getEncoder().encodeToString(TEST_CREDS.getBytes());
    assertEquals(expectedBST, token.getEncodedCredentials());
  }

  @Test
  public void testParse() throws Exception {
    // test normal case
    BaseAuthenticationToken bat = UPAuthenticationToken.parse(TEST_CREDS, false);
    UPAuthenticationToken upt =
        new UPAuthenticationToken(bat.getPrincipal().toString(), bat.getCredentials().toString());
    assertNotNull(upt);
    assertEquals(TEST_NAME, upt.getUsername());
    assertEquals(TEST_PW, upt.getPassword());
    assertEquals(UPAuthenticationToken.UP_TOKEN_VALUE_TYPE, upt.tokenValueType);
    assertEquals(UPAuthenticationToken.BST_USERNAME_LN, upt.tokenId);
  }
}
