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
package org.codice.ddf.security.claims.anonymous;

import org.apache.cxf.sts.claims.Claim;
import org.apache.cxf.sts.claims.ClaimCollection;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.RequestClaim;
import org.apache.cxf.sts.claims.RequestClaimCollection;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class AnonymousClaimsHandlerTest {

    @Test
    public void testSettingClaimsMapList() throws URISyntaxException {
        AnonymousClaimsHandler claimsHandler = new AnonymousClaimsHandler();
        claimsHandler.setAttributes(Arrays.asList("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier=Anonymous", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress=Anonymous@anon.com", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname=Anon"));

        Map<URI, List<String>> claimsMap = claimsHandler.getClaimsMap();

        List<String> value = claimsMap.get(new URI("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier"));
        assertEquals("Anonymous", value.get(0));

        value = claimsMap.get(new URI("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress"));
        assertEquals("Anonymous@anon.com", value.get(0));

        value = claimsMap.get(new URI("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname"));
        assertEquals("Anon", value.get(0));

        claimsHandler = new AnonymousClaimsHandler();
        claimsHandler.setAttributes(Arrays.asList("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier=Anonymous,http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress=Anonymous@anon.com,http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname=Anon"));

        value = claimsMap.get(new URI("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier"));
        assertEquals("Anonymous", value.get(0));

        value = claimsMap.get(new URI("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress"));
        assertEquals("Anonymous@anon.com", value.get(0));

        value = claimsMap.get(new URI("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname"));
        assertEquals("Anon", value.get(0));
    }

    @Test
    public void testSettingClaimsMapString() throws URISyntaxException {
        AnonymousClaimsHandler claimsHandler = new AnonymousClaimsHandler();
        claimsHandler.setAttributes("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier=Anonymous,http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress=Anonymous@anon.com,http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname=Anon");

        Map<URI, List<String>> claimsMap = claimsHandler.getClaimsMap();

        List<String> value = claimsMap.get(new URI("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier"));
        assertEquals("Anonymous", value.get(0));

        value = claimsMap.get(new URI("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress"));
        assertEquals("Anonymous@anon.com", value.get(0));

        value = claimsMap.get(new URI("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname"));
        assertEquals("Anon", value.get(0));
    }

    @Test
    public void testRetrieveClaims() throws URISyntaxException {
        AnonymousClaimsHandler claimsHandler = new AnonymousClaimsHandler();
        claimsHandler.setAttributes(Arrays.asList("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier=Anonymous", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress=Anonymous@anon.com|someguy@somesite.com|somedude@cool.com", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname=Anon"));

        RequestClaimCollection requestClaims = new RequestClaimCollection();
        RequestClaim requestClaim = new RequestClaim();
        URI nameURI = new URI("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier");
        requestClaim.setClaimType(nameURI);
        requestClaims.add(requestClaim);
        requestClaim = new RequestClaim();
        URI emailURI = new URI("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress");
        requestClaim.setClaimType(emailURI);
        requestClaims.add(requestClaim);
        requestClaim = new RequestClaim();
        URI fooURI = new URI("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/foobar");
        requestClaim.setClaimType(fooURI);
        requestClaim.setOptional(true);
        requestClaims.add(requestClaim);

        ClaimsParameters claimsParameters = new ClaimsParameters();

        List<URI> supportedClaims = claimsHandler.getSupportedClaimTypes();

        assertEquals(3, supportedClaims.size());

        ClaimCollection claimsCollection = claimsHandler.retrieveClaimValues(requestClaims, claimsParameters);

        assertEquals(2, claimsCollection.size());

        for(Claim claim : claimsCollection) {
            if(claim.getClaimType().equals(nameURI)) {
                assertEquals(1, claim.getValues().size());
                assertEquals("Anonymous", claim.getValues().get(0));
            } else if(claim.getClaimType().equals(emailURI)) {
                assertEquals(3, claim.getValues().size());
                List<String> values = claim.getValues();
                assertEquals("Anonymous@anon.com", values.get(0));
                assertEquals("someguy@somesite.com", values.get(1));
                assertEquals("somedude@cool.com", values.get(2));
            }
            assertFalse(claim.getClaimType().equals(fooURI));
        }
    }
}
