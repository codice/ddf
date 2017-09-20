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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.security.Principal;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.x500.X500Principal;
import org.apache.cxf.rt.security.claims.Claim;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.ProcessedClaimCollection;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

public class TestPropertyFileClaimsHandler {

  @Test
  public void testRetrieveClaimValues() {
    PropertyFileClaimsHandler propertyFileClaimsHandler = new PropertyFileClaimsHandler();
    propertyFileClaimsHandler.setPropertyFileLocation("/users.properties");
    propertyFileClaimsHandler.setRoleClaimType("http://myroletype");
    propertyFileClaimsHandler.setIdClaimType("http://myidtype");

    ClaimCollection claimCollection = new ClaimCollection();
    Claim claim1 = new Claim();
    claim1.setClaimType(URI.create("http://myroletype"));
    Claim claim2 = new Claim();
    claim2.setClaimType(URI.create("http://myidtype"));
    claimCollection.add(claim1);
    claimCollection.add(claim2);
    ClaimsParameters claimsParameters = mock(ClaimsParameters.class);
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("admin");
    when(claimsParameters.getPrincipal()).thenReturn(principal);
    ProcessedClaimCollection processedClaimCollection =
        propertyFileClaimsHandler.retrieveClaimValues(claimCollection, claimsParameters);

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

  @Test
  public void testRetrieveClaimsValuesNullPrincipal() {
    PropertyFileClaimsHandler claimsHandler = new PropertyFileClaimsHandler();
    ClaimsParameters claimsParameters = new ClaimsParameters();
    ClaimCollection claimCollection = new ClaimCollection();
    ProcessedClaimCollection processedClaims =
        claimsHandler.retrieveClaimValues(claimCollection, claimsParameters);

    Assert.assertThat(processedClaims.size(), CoreMatchers.is(equalTo(0)));
  }
}
