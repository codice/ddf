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
package org.codice.ddf.security.claims.cas;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.cxf.rt.security.claims.Claim;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.ProcessedClaimCollection;
import org.junit.Test;

public class CasClaimsHandlerTest {

  public static final String NAME_IDENTIFIER_CLAIM_URI =
      "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier";
  public static final String DISTINGUISHED_NAME_URI =
      "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/x500distinguishedname";
  public static final String ROLE_CLAIM_URI =
      "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role";
  public static final String GROUP_CLAIM_URI =
      "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/group";

  private Map<String, Object> getAdditionalProperties() {
    Map<String, Object> additionalProperties = new HashMap<>();
    additionalProperties.put("samaccountname", "testuser1");
    additionalProperties.put("distinguishedname", "CN=Test User,OU=Users,DC=example,DC=com");
    additionalProperties.put("role", "[admin]");
    additionalProperties.put("group", "[server admins, test users]");
    return additionalProperties;
  }

  private ClaimCollection getClaims() throws URISyntaxException {
    ClaimCollection claimColl = new ClaimCollection();
    Claim claim1 = new Claim();
    Claim claim2 = new Claim();
    claim1.setClaimType(new URI(NAME_IDENTIFIER_CLAIM_URI));
    claim2.setClaimType(new URI(DISTINGUISHED_NAME_URI));
    claimColl.add(claim1);
    claimColl.add(claim2);
    return claimColl;
  }

  private List<String> getAttributeMap() {
    return Arrays.asList(
        String.format("%s=%s", NAME_IDENTIFIER_CLAIM_URI, "samaccountname"),
        String.format("%s=%s", DISTINGUISHED_NAME_URI, "distinguishedname"),
        String.format("%s=%s", ROLE_CLAIM_URI, "role"),
        String.format("%s=%s", GROUP_CLAIM_URI, "group"));
  }

  @Test
  public void testGetSupportedClaimTypes() {
    CasClaimsHandler handler = new CasClaimsHandler();
    List<String> claims = Arrays.asList("NameIdentifier", "Email", "Role", "Groups");
    handler.setSupportedClaims(claims);
    assertThat(handler.getSupportedClaimTypes().size(), is(4));
  }

  @Test
  public void testGetSupportedClaimTypesInvalidURI() {
    CasClaimsHandler handler = new CasClaimsHandler();
    List<String> claims = Arrays.asList("control\\", "space ", "validURI");
    handler.setSupportedClaims(claims);
    assertThat(handler.getSupportedClaimTypes().size(), is(1));
  }

  @Test
  public void testSetAttributeMapValid() {
    CasClaimsHandler handler = new CasClaimsHandler();
    List<String> attributeMapEntries =
        Arrays.asList(
            "key=value",
            "equals\\=in\\=key=equals\\=in\\=value",
            "special=\\u0063\\u0068\\u0061\\u0072\\u0073");
    handler.setAttributeMap(attributeMapEntries);
    Map<String, String> attributeMap = handler.getAttributeMap();
    assertThat(attributeMap.size(), is(3));
    assertThat(attributeMap.get("key"), is("value"));
    assertThat(attributeMap.get("equals=in=key"), is("equals=in=value"));
    assertThat(attributeMap.get("special"), is("chars"));
  }

  @Test
  public void testSetAttributeMapInvalid() {
    CasClaimsHandler handler = new CasClaimsHandler();
    List<String> attributeMapEntries = Arrays.asList("", "colon:delimiter", "key=value");
    handler.setAttributeMap(attributeMapEntries);
    Map<String, String> attributeMap = handler.getAttributeMap();
    assertThat(attributeMap.size(), is(1));
  }

  @Test
  public void testRetrieveClaimValuesSinglevalueAttributes() throws URISyntaxException {
    CasClaimsHandler handler = new CasClaimsHandler();
    ClaimsParameters parameters = new ClaimsParameters();
    parameters.setPrincipal(mock(Principal.class));
    parameters.setAdditionalProperties(getAdditionalProperties());
    handler.setAttributeMap(getAttributeMap());
    ProcessedClaimCollection processedClaims = handler.retrieveClaimValues(getClaims(), parameters);
    assertThat(processedClaims.size(), is(2));

    for (Claim claim : processedClaims) {
      switch (claim.getClaimType().toString()) {
        case NAME_IDENTIFIER_CLAIM_URI:
          assertThat(claim.getValues(), hasItem("testuser1"));
          break;
        case DISTINGUISHED_NAME_URI:
          assertThat(claim.getValues(), hasItem("CN=Test User,OU=Users,DC=example,DC=com"));
          break;
      }
    }
  }

  @Test
  public void testRetrieveClaimValuesMultivalueAttributes() throws URISyntaxException {
    CasClaimsHandler handler = new CasClaimsHandler();
    ClaimsParameters parameters = new ClaimsParameters();
    parameters.setPrincipal(mock(Principal.class));
    parameters.setAdditionalProperties(getAdditionalProperties());
    handler.setAttributeMap(getAttributeMap());
    ClaimCollection claimColl = new ClaimCollection();
    Claim claim1 = new Claim();
    Claim claim2 = new Claim();
    claim1.setClaimType(new URI(ROLE_CLAIM_URI));
    claim2.setClaimType(new URI(GROUP_CLAIM_URI));
    claimColl.add(claim1);
    claimColl.add(claim2);
    ProcessedClaimCollection processedClaims = handler.retrieveClaimValues(claimColl, parameters);
    assertThat(processedClaims.size(), is(2));

    for (Claim claim : processedClaims) {
      switch (claim.getClaimType().toString()) {
        case ROLE_CLAIM_URI:
          assertThat(claim.getValues(), hasItem("admin"));
          break;
        case GROUP_CLAIM_URI:
          assertThat(claim.getValues(), hasItem("server admins"));
          assertThat(claim.getValues(), hasItem("test users"));
          break;
      }
    }
  }

  @Test
  public void testRetrieveClaimValuesNoMapping() throws URISyntaxException {
    CasClaimsHandler handler = new CasClaimsHandler();
    ClaimsParameters parameters = new ClaimsParameters();
    parameters.setPrincipal(mock(Principal.class));
    Map<String, Object> additionalProperties = new HashMap<>();
    additionalProperties.put(NAME_IDENTIFIER_CLAIM_URI, "testuser1");
    additionalProperties.put(DISTINGUISHED_NAME_URI, "CN=Test User,OU=Users,DC=example,DC=com");
    parameters.setAdditionalProperties(additionalProperties);
    handler.setAttributeMap(new ArrayList<>());
    ProcessedClaimCollection processedClaims = handler.retrieveClaimValues(getClaims(), parameters);
    assertThat(processedClaims.size(), is(2));

    for (Claim claim : processedClaims) {
      switch (claim.getClaimType().toString()) {
        case NAME_IDENTIFIER_CLAIM_URI:
          assertThat(claim.getValues(), hasItem("testuser1"));
          break;
        case DISTINGUISHED_NAME_URI:
          assertThat(claim.getValues(), hasItem("CN=Test User,OU=Users,DC=example,DC=com"));
          break;
      }
    }
  }

  @Test
  public void testRetrieveClaimValuesNoAttributes() throws URISyntaxException {
    CasClaimsHandler handler = new CasClaimsHandler();
    ClaimsParameters parameters = new ClaimsParameters();
    parameters.setPrincipal(mock(Principal.class));
    parameters.setAdditionalProperties(null);
    handler.setAttributeMap(getAttributeMap());
    ProcessedClaimCollection claimColl = handler.retrieveClaimValues(getClaims(), parameters);
    assertTrue(claimColl.isEmpty());
  }

  @Test
  public void testRetrieveClaimValuesNoClaims() {
    CasClaimsHandler handler = new CasClaimsHandler();
    ClaimsParameters parameters = new ClaimsParameters();
    parameters.setPrincipal(mock(Principal.class));
    parameters.setAdditionalProperties(getAdditionalProperties());
    handler.setAttributeMap(getAttributeMap());
    ProcessedClaimCollection claimColl =
        handler.retrieveClaimValues(new ClaimCollection(), parameters);
    assertTrue(claimColl.isEmpty());
  }

  @Test
  public void testRetrievedClaimValuesDuplicateClaims() throws URISyntaxException {
    CasClaimsHandler handler = new CasClaimsHandler();
    ClaimsParameters parameters = new ClaimsParameters();
    parameters.setPrincipal(mock(Principal.class));
    parameters.setAdditionalProperties(getAdditionalProperties());
    handler.setAttributeMap(getAttributeMap());
    ClaimCollection claimColl = new ClaimCollection();
    Claim claim = new Claim();
    claim.setClaimType(new URI(ROLE_CLAIM_URI));
    claimColl.add(claim);
    claimColl.add(claim);
    ProcessedClaimCollection processedClaims = handler.retrieveClaimValues(claimColl, parameters);
    assertThat(processedClaims.size(), is(1));
  }
}
