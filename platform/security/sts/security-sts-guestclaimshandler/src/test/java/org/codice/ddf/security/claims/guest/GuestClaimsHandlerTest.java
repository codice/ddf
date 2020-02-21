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
package org.codice.ddf.security.claims.guest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.google.common.collect.ImmutableMap;
import ddf.security.principal.GuestPrincipal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.cxf.rt.security.claims.Claim;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.ProcessedClaim;
import org.apache.cxf.sts.claims.ProcessedClaimCollection;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;
import org.junit.Test;

public class GuestClaimsHandlerTest {

  @Test
  public void testSettingClaimsMapList() throws URISyntaxException {
    GuestClaimsConfig claimsConfig = new GuestClaimsConfig();
    claimsConfig.setAttributes(
        Arrays.asList(
            "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier=Guest",
            "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress=Guest@guest.com",
            "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname=Guest"));
    GuestClaimsHandler claimsHandler = new GuestClaimsHandler();
    claimsHandler.setGuestClaimsConfig(claimsConfig);

    Map<URI, List<String>> claimsMap = claimsConfig.getClaimsMap();

    List<String> value =
        claimsMap.get(
            new URI("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier"));
    assertEquals("Guest", value.get(0));

    value =
        claimsMap.get(
            new URI("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress"));
    assertEquals("Guest@guest.com", value.get(0));

    value = claimsMap.get(new URI("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname"));
    assertEquals("Guest", value.get(0));

    claimsConfig = new GuestClaimsConfig();
    claimsConfig.setAttributes(
        Arrays.asList(
            "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier=Guest,http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress=Guest@guest.com,http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname=Guest"));
    claimsHandler = new GuestClaimsHandler();
    claimsHandler.setGuestClaimsConfig(claimsConfig);

    value =
        claimsMap.get(
            new URI("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier"));
    assertEquals("Guest", value.get(0));

    value =
        claimsMap.get(
            new URI("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress"));
    assertEquals("Guest@guest.com", value.get(0));

    value = claimsMap.get(new URI("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname"));
    assertEquals("Guest", value.get(0));
  }

  @Test
  public void testRetrieveClaims() throws URISyntaxException {
    GuestClaimsConfig claimsConfig = new GuestClaimsConfig();
    claimsConfig.setAttributes(
        Arrays.asList(
            "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier=Guest",
            "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress=Guest@guest.com|someguy@somesite.com|somedude@cool.com",
            "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname=Guest"));
    GuestClaimsHandler claimsHandler = new GuestClaimsHandler();
    claimsHandler.setGuestClaimsConfig(claimsConfig);

    ClaimCollection requestClaims = new ClaimCollection();
    Claim requestClaim = new Claim();
    URI nameURI = new URI("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier");
    requestClaim.setClaimType(nameURI);
    requestClaims.add(requestClaim);
    requestClaim = new Claim();
    URI emailURI = new URI("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress");
    requestClaim.setClaimType(emailURI);
    requestClaims.add(requestClaim);
    requestClaim = new Claim();
    URI fooURI = new URI("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/foobar");
    requestClaim.setClaimType(fooURI);
    requestClaim.setOptional(true);
    requestClaims.add(requestClaim);

    ClaimsParameters claimsParameters = new ClaimsParameters();
    claimsParameters.setPrincipal(new GuestPrincipal("127.0.0.1"));

    List<URI> supportedClaims = claimsHandler.getSupportedClaimTypes();

    assertEquals(3, supportedClaims.size());

    ProcessedClaimCollection claimsCollection =
        claimsHandler.retrieveClaimValues(requestClaims, claimsParameters);

    assertEquals(2, claimsCollection.size());

    for (ProcessedClaim claim : claimsCollection) {
      if (claim.getClaimType().equals(nameURI)) {
        assertEquals(1, claim.getValues().size());
        assertEquals("Guest", claim.getValues().get(0));
      } else if (claim.getClaimType().equals(emailURI)) {
        assertEquals(3, claim.getValues().size());
        List<Object> values = claim.getValues();
        assertEquals("Guest@guest.com", values.get(0));
        assertEquals("someguy@somesite.com", values.get(1));
        assertEquals("somedude@cool.com", values.get(2));
      }
      assertFalse(claim.getClaimType().equals(fooURI));
    }

    claimsParameters = new ClaimsParameters();
    claimsCollection = claimsHandler.retrieveClaimValues(requestClaims, claimsParameters);

    assertEquals(2, claimsCollection.size());

    claimsParameters = new ClaimsParameters();
    claimsParameters.setPrincipal(new CustomTokenPrincipal("SomeValue"));
    claimsCollection = claimsHandler.retrieveClaimValues(requestClaims, claimsParameters);

    assertEquals(2, claimsCollection.size());
  }

  @Test
  public void testRetrieveClaimsWithAdditionalPropertyClaim() throws URISyntaxException {
    GuestClaimsConfig claimsConfig = new GuestClaimsConfig();
    claimsConfig.setAttributes(
        Arrays.asList(
            "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier=Guest",
            "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress=Guest@guest.com|someguy@somesite.com|somedude@cool.com",
            "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname=Guest"));
    GuestClaimsHandler claimsHandler = new GuestClaimsHandler();
    claimsHandler.setGuestClaimsConfig(claimsConfig);

    ClaimCollection requestClaims = new ClaimCollection();
    Claim requestClaim = new Claim();
    URI nameURI = new URI("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier");
    requestClaim.setClaimType(nameURI);
    requestClaims.add(requestClaim);
    requestClaim = new Claim();
    URI emailURI = new URI("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress");
    requestClaim.setClaimType(emailURI);
    requestClaims.add(requestClaim);
    requestClaim = new Claim();
    URI fooURI = new URI("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/foobar");
    requestClaim.setClaimType(fooURI);
    requestClaim.setOptional(true);
    requestClaims.add(requestClaim);

    ClaimsParameters claimsParameters = new ClaimsParameters();
    claimsParameters.setPrincipal(new GuestPrincipal("127.0.0.1"));
    claimsParameters.setAdditionalProperties(ImmutableMap.of("IpAddress", "127.0.0.1"));

    List<URI> supportedClaims = claimsHandler.getSupportedClaimTypes();

    assertEquals(3, supportedClaims.size());

    ProcessedClaimCollection claimsCollection =
        claimsHandler.retrieveClaimValues(requestClaims, claimsParameters);

    assertEquals(3, claimsCollection.size());

    for (ProcessedClaim claim : claimsCollection) {
      if (claim.getClaimType().equals(nameURI)) {
        assertEquals(1, claim.getValues().size());
        assertEquals("Guest", claim.getValues().get(0));
      } else if (claim.getClaimType().equals(emailURI)) {
        assertEquals(3, claim.getValues().size());
        List<Object> values = claim.getValues();
        assertEquals("Guest@guest.com", values.get(0));
        assertEquals("someguy@somesite.com", values.get(1));
        assertEquals("somedude@cool.com", values.get(2));
      } else if (claim.getClaimType().equals(new URI("IpAddress"))) {
        assertEquals("127.0.0.1", claim.getValues().get(0));
      }
      assertFalse(claim.getClaimType().equals(fooURI));
    }

    claimsParameters = new ClaimsParameters();
    claimsCollection = claimsHandler.retrieveClaimValues(requestClaims, claimsParameters);

    assertEquals(2, claimsCollection.size());

    claimsParameters = new ClaimsParameters();
    claimsParameters.setPrincipal(new CustomTokenPrincipal("SomeValue"));
    claimsCollection = claimsHandler.retrieveClaimValues(requestClaims, claimsParameters);

    assertEquals(2, claimsCollection.size());
  }
}
