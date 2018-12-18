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
package ddf.security.sts.claimsHandler;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.Map;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.x500.X500Principal;
import org.junit.Test;

public class AttributeMapLoaderTest {

  private static final String BAD_KEY = "BAD_KEY";

  private static final String MAP_FILE = "testMap.properties";

  private static final String NO_MAP_FILE = "noMap.properties";

  private static final String TEST_USER = "testuser";

  private static final String KERBEROS_USER = TEST_USER + "/ddf.org";

  private static final String KERBEROS_PRINCIPAL = KERBEROS_USER + "@REALM";

  private static final String X500_DN = "CN=" + TEST_USER + ", OU=LDAP, O=DDF, C=US";

  private static final String DEFAULT_BASE_DN = "OU=LDAP, OU=DEFAULT, O=DDF, C=US";

  private static final String[] X500_BASE_DN_ARR = {"OU=LDAP", "O=DDF", "C=US"};

  private static final String[] X500_DEFAULT_BASE_DN_ARR = {
    "OU=LDAP", "OU=DEFAULT", "O=DDF", "C=US"
  };

  AttributeMapLoader attributeMapLoader = new AttributeMapLoader();

  /**
   * Tests loading the attributes from a file.
   *
   * @throws java.io.FileNotFoundException
   */
  @Test
  public void testAttributeFile() {
    Map<String, String> returnedMap = attributeMapLoader.buildClaimsMapFile(MAP_FILE);
    assertEquals(
        "uid",
        returnedMap.get("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier"));
    assertTrue(
        returnedMap.containsKey("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role"));
    assertFalse(returnedMap.containsKey(BAD_KEY));
  }

  /** Tests Loading the attributes from a non-existing file. Should return an empty map. */
  @Test
  public void testNoAttributeFile() {
    Map<String, String> returnedMap = attributeMapLoader.buildClaimsMapFile(NO_MAP_FILE);
    assertNotNull(returnedMap);
    assertTrue(returnedMap.isEmpty());
  }

  @Test
  public void testPlainGetUser() {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn(TEST_USER);

    assertEquals(TEST_USER, attributeMapLoader.getUser(principal));
  }

  @Test
  public void testKerberosGetUser() {
    Principal principal = new KerberosPrincipal(KERBEROS_PRINCIPAL);

    assertEquals(TEST_USER, attributeMapLoader.getUser(principal));
  }

  @Test
  public void testX500GetUser() {
    Principal principal = new X500Principal(X500_DN);

    assertEquals(TEST_USER, attributeMapLoader.getUser(principal));
  }

  @Test
  public void testGetBaseDnX500() {
    Principal principal = new X500Principal(X500_DN);

    String baseDN = attributeMapLoader.getBaseDN(principal, DEFAULT_BASE_DN, false);

    String[] split = baseDN.replaceAll("\\s", "").split(",");
    assertArrayEquals(X500_BASE_DN_ARR, split);
  }

  @Test
  public void testGetBaseDnX500Override() {
    Principal principal = new X500Principal(X500_DN);

    String baseDN = attributeMapLoader.getBaseDN(principal, DEFAULT_BASE_DN, true);

    String[] split = baseDN.replaceAll("\\s", "").split(",");
    assertArrayEquals(X500_DEFAULT_BASE_DN_ARR, split);
  }

  @Test
  public void testGetBaseDnX500EmptyDN() {
    Principal principal = new X500Principal("CN=FOOBAR");

    String baseDN = attributeMapLoader.getBaseDN(principal, DEFAULT_BASE_DN, false);

    String[] split = baseDN.replaceAll("\\s", "").split(",");
    assertArrayEquals(X500_DEFAULT_BASE_DN_ARR, split);
  }

  @Test
  public void testGetBaseDnNonX500() {
    Principal principal = new KerberosPrincipal(KERBEROS_PRINCIPAL);

    String baseDN = attributeMapLoader.getBaseDN(principal, DEFAULT_BASE_DN, false);

    String[] split = baseDN.replaceAll("\\s", "").split(",");
    assertArrayEquals(X500_DEFAULT_BASE_DN_ARR, split);
  }
}
