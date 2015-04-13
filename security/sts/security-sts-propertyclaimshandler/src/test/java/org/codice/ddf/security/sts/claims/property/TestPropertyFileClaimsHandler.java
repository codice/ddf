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
package org.codice.ddf.security.sts.claims.property;

import org.apache.cxf.rt.security.claims.Claim;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.ProcessedClaimCollection;
import org.junit.Test;

import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.x500.X500Principal;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestPropertyFileClaimsHandler {

    @Test
    public void testRetrieveClaimValues() {
        PropertyFileClaimsHandler propertyFileClaimsHandler = new PropertyFileClaimsHandler();
        propertyFileClaimsHandler.setPropertyFileLocation("/users.properties");
        propertyFileClaimsHandler.setRoleClaimType("http://myroletype");

        ClaimCollection claimCollection = new ClaimCollection();
        Claim claim = new Claim();
        try {
            claim.setClaimType(new URI("http://myroletype"));
        } catch (URISyntaxException e) {
            fail("Could not create URI");
        }
        claimCollection.add(claim);
        ClaimsParameters claimsParameters = mock(ClaimsParameters.class);
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("admin");
        when(claimsParameters.getPrincipal()).thenReturn(principal);
        ProcessedClaimCollection processedClaimCollection = propertyFileClaimsHandler.retrieveClaimValues(claimCollection, claimsParameters);

        assertEquals(1, processedClaimCollection.size());
        assertEquals(5, processedClaimCollection.get(0).getValues().size());
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
