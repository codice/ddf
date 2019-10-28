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
package org.codice.ddf.security.sts.claims.property;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.security.claims.ClaimsCollection;
import ddf.security.claims.ClaimsParameters;
import java.security.Principal;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.x500.X500Principal;
import org.junit.Test;

public class PropertyFileClaimsHandlerTest {

  @Test
  public void testRetrieveClaimValues() {
    PropertyFileClaimsHandler propertyFileClaimsHandler = new PropertyFileClaimsHandler();
    propertyFileClaimsHandler.setPropertyFileLocation("/users.properties");
    propertyFileClaimsHandler.setRoleClaimType("http://myroletype");
    propertyFileClaimsHandler.setIdClaimType("http://myidtype");

    ClaimsParameters claimsParameters = mock(ClaimsParameters.class);
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("admin");
    when(claimsParameters.getPrincipal()).thenReturn(principal);
    ClaimsCollection processedClaimCollection =
        propertyFileClaimsHandler.retrieveClaims(claimsParameters);

    assertEquals(2, processedClaimCollection.size());
    assertEquals(4, processedClaimCollection.get(0).getValues().size());
    assertEquals("admin", processedClaimCollection.get(1).getValues().get(0));
  }

  @Test
  public void testGetUser() {
    PropertyFileClaimsHandler propertyFileClaimsHandler = new PropertyFileClaimsHandler();

    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("mydude");
    String user = propertyFileClaimsHandler.getUser(principal);
    assertEquals("mydude", user);

    principal = new X500Principal("cn=myxman,ou=someunit,o=someorg");
    user = propertyFileClaimsHandler.getUser(principal);
    assertEquals("myxman", user);

    principal = new KerberosPrincipal("mykman@SOMEDOMAIN.COM");
    user = propertyFileClaimsHandler.getUser(principal);
    assertEquals("mykman", user);
  }
}
