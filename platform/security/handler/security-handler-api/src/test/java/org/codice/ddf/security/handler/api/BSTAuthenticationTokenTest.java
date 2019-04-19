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

import java.util.Base64;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class BSTAuthenticationTokenTest {

  public static final String PRINCIPAL = "somePrincipal";

  public static final String CREDENTIALS = "someCredentials";

  public static final String TEST_STRING =
      BSTAuthenticationToken.BST_PRINCIPAL
          + PRINCIPAL
          + BSTAuthenticationToken.NEWLINE
          + BSTAuthenticationToken.BST_CREDENTIALS
          + CREDENTIALS;

  @Rule public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void testParse() throws WSSecurityException {
    // test normal case
    BaseAuthenticationToken bat = BSTAuthenticationToken.parse(TEST_STRING, false);
    MockBSTAuthenticationToken mockToken =
        new MockBSTAuthenticationToken(bat.getPrincipal(), bat.getCredentials());
    assertNotNull(mockToken);
    assertEquals(PRINCIPAL, mockToken.getPrincipal());
    assertEquals(CREDENTIALS, mockToken.getCredentials());
  }

  @Test
  public void testParsePrincipal() throws WSSecurityException {
    // Empty Prinicpal
    BaseAuthenticationToken bat =
        BSTAuthenticationToken.parse(
            BSTAuthenticationToken.BST_PRINCIPAL
                + ""
                + BSTAuthenticationToken.NEWLINE
                + BSTAuthenticationToken.BST_CREDENTIALS
                + CREDENTIALS,
            false);
    MockBSTAuthenticationToken mockToken =
        new MockBSTAuthenticationToken(bat.getPrincipal(), bat.getCredentials());
    assertNotNull(mockToken);
    assertEquals("", mockToken.getPrincipal());
    assertEquals(CREDENTIALS, mockToken.getCredentials());
    // Missing Principal
    expectedException.expect(WSSecurityException.class);
    BSTAuthenticationToken.parse(BSTAuthenticationToken.BST_CREDENTIALS + "name", false);
  }

  @Test
  public void testParseCredentials() throws WSSecurityException {
    // Empty Credentials
    BaseAuthenticationToken bat =
        BSTAuthenticationToken.parse(
            BSTAuthenticationToken.BST_PRINCIPAL
                + PRINCIPAL
                + BSTAuthenticationToken.NEWLINE
                + BSTAuthenticationToken.BST_CREDENTIALS
                + "",
            false);
    MockBSTAuthenticationToken mockToken =
        new MockBSTAuthenticationToken(bat.getPrincipal(), bat.getCredentials());
    assertNotNull(mockToken);
    assertEquals(PRINCIPAL, mockToken.getPrincipal());
    assertEquals("", mockToken.getCredentials());
    // Missing Credentials
    expectedException.expect(WSSecurityException.class);
    BSTAuthenticationToken.parse(BSTAuthenticationToken.BST_CREDENTIALS + "name", false);
  }

  @Test
  public void testParseDecode() throws WSSecurityException {
    // make sure we unencode if necessary
    BaseAuthenticationToken bat =
        BSTAuthenticationToken.parse(
            Base64.getEncoder().encodeToString(TEST_STRING.getBytes()), true);
    MockBSTAuthenticationToken mockToken =
        new MockBSTAuthenticationToken(bat.getPrincipal(), bat.getCredentials());
    assertNotNull(mockToken);
    assertEquals(PRINCIPAL, mockToken.getPrincipal());
    assertEquals(CREDENTIALS, mockToken.getCredentials());
  }
}
