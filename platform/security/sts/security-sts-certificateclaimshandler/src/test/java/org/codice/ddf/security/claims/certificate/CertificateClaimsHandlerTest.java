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
package org.codice.ddf.security.claims.certificate;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import ddf.security.SubjectUtils;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.cxf.rt.security.claims.Claim;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.ProcessedClaimCollection;
import org.junit.Test;

public class CertificateClaimsHandlerTest {

  private static final String EMAIL_CLAIM_NAME = "Email";
  private static final String COUNTRY_CLAIM_NAME = "Country";
  private static final String COUNTRY_CLAIM_URI_VALUE = "USA";
  private static final String EMAIL_ADDRESS_CLAIM_URI_VALUE = "local@localhost";

  private ClaimCollection getClaims() throws URISyntaxException {
    ClaimCollection claims = new ClaimCollection();
    Claim claim = new Claim();
    claim.setClaimType(new URI(SubjectUtils.COUNTRY_CLAIM_URI));
    claims.add(claim);
    Claim claim1 = new Claim();
    claim1.setClaimType(new URI(SubjectUtils.EMAIL_ADDRESS_CLAIM_URI));
    claims.add(claim1);
    return claims;
  }

  @Test
  public void testGetSupportedClaimTypes() {
    CertificateClaimsHandler certificateClaimsHandler = new CertificateClaimsHandler();
    List<String> supportedClaimTypes = certificateClaimsHandler.getSupportedClaimTypes();
    assertThat(supportedClaimTypes.size(), is(2));
  }

  @Test
  public void testRetrieveClaimValuesNoCertValues() throws URISyntaxException {
    CertificateClaimsHandler certificateClaimsHandler = new CertificateClaimsHandler();
    ClaimCollection claims = getClaims();
    ClaimsParameters parameters = new ClaimsParameters();
    parameters.setPrincipal(mock(Principal.class));
    ProcessedClaimCollection processedClaims =
        certificateClaimsHandler.retrieveClaimValues(claims, parameters);
    assertThat(processedClaims.size(), is(0));
  }

  @Test
  public void testRetrieveClaimValuesWithCertValues() throws URISyntaxException {
    CertificateClaimsHandler certificateClaimsHandler = new CertificateClaimsHandler();
    ClaimCollection claims = getClaims();
    ClaimsParameters parameters = new ClaimsParameters();
    parameters.setPrincipal(mock(Principal.class));
    Map<String, Object> map = new HashMap<>();
    map.put(SubjectUtils.EMAIL_ADDRESS_CLAIM_URI, EMAIL_ADDRESS_CLAIM_URI_VALUE);
    map.put(SubjectUtils.COUNTRY_CLAIM_URI, COUNTRY_CLAIM_URI_VALUE);
    parameters.setAdditionalProperties(map);
    ProcessedClaimCollection processedClaims =
        certificateClaimsHandler.retrieveClaimValues(claims, parameters);
    assertThat(processedClaims.size(), is(2));
    assertThat(
        processedClaims.stream().map(Claim::getClaimType).collect(Collectors.toList()),
        containsInAnyOrder(SubjectUtils.EMAIL_ADDRESS_CLAIM_URI, SubjectUtils.COUNTRY_CLAIM_URI));
  }

  @Test
  public void testRetrieveClaimValuesWithAltNamesRequested() throws URISyntaxException {
    CertificateClaimsHandler certificateClaimsHandler = new CertificateClaimsHandler();
    certificateClaimsHandler.setCountryClaim(COUNTRY_CLAIM_NAME);
    certificateClaimsHandler.setEmailClaim(EMAIL_CLAIM_NAME);
    ClaimCollection claims = new ClaimCollection();
    Claim claim = new Claim();
    claim.setClaimType(new URI(COUNTRY_CLAIM_NAME));
    claims.add(claim);
    Claim claim1 = new Claim();
    claim1.setClaimType(new URI(EMAIL_CLAIM_NAME));
    claims.add(claim1);
    ClaimsParameters parameters = new ClaimsParameters();
    parameters.setPrincipal(mock(Principal.class));
    Map<String, Object> map = new HashMap<>();
    map.put(SubjectUtils.EMAIL_ADDRESS_CLAIM_URI, EMAIL_ADDRESS_CLAIM_URI_VALUE);
    map.put(SubjectUtils.COUNTRY_CLAIM_URI, COUNTRY_CLAIM_URI_VALUE);
    parameters.setAdditionalProperties(map);
    ProcessedClaimCollection processedClaims =
        certificateClaimsHandler.retrieveClaimValues(claims, parameters);
    assertThat(processedClaims.size(), is(2));
    assertThat(
        processedClaims.stream().map(Claim::getClaimType).collect(Collectors.toList()),
        containsInAnyOrder(EMAIL_CLAIM_NAME, COUNTRY_CLAIM_NAME));
  }

  @Test
  public void testRetrieveClaimValuesWithAltNamesNotRequested() throws URISyntaxException {
    CertificateClaimsHandler certificateClaimsHandler = new CertificateClaimsHandler();
    certificateClaimsHandler.setCountryClaim(COUNTRY_CLAIM_NAME);
    certificateClaimsHandler.setEmailClaim(EMAIL_CLAIM_NAME);
    ClaimCollection claims = getClaims();
    ClaimsParameters parameters = new ClaimsParameters();
    parameters.setPrincipal(mock(Principal.class));
    Map<String, Object> map = new HashMap<>();
    map.put(SubjectUtils.EMAIL_ADDRESS_CLAIM_URI, EMAIL_ADDRESS_CLAIM_URI_VALUE);
    map.put(SubjectUtils.COUNTRY_CLAIM_URI, COUNTRY_CLAIM_URI_VALUE);
    parameters.setAdditionalProperties(map);
    ProcessedClaimCollection processedClaims =
        certificateClaimsHandler.retrieveClaimValues(claims, parameters);
    assertThat(processedClaims.size(), is(0));
  }

  @Test
  public void testRetrieveClaimValuesWithEmail() throws URISyntaxException {
    CertificateClaimsHandler certificateClaimsHandler = new CertificateClaimsHandler();
    ClaimCollection claims = getClaims();
    ClaimsParameters parameters = new ClaimsParameters();
    parameters.setPrincipal(mock(Principal.class));
    Map<String, Object> map = new HashMap<>();
    map.put(SubjectUtils.EMAIL_ADDRESS_CLAIM_URI, EMAIL_ADDRESS_CLAIM_URI_VALUE);
    parameters.setAdditionalProperties(map);
    ProcessedClaimCollection processedClaims =
        certificateClaimsHandler.retrieveClaimValues(claims, parameters);
    assertThat(processedClaims.size(), is(1));
    assertThat(
        processedClaims.stream().map(Claim::getClaimType).collect(Collectors.toList()),
        containsInAnyOrder(SubjectUtils.EMAIL_ADDRESS_CLAIM_URI));
  }

  @Test
  public void testRetrieveClaimValuesWithCountry() throws URISyntaxException {
    CertificateClaimsHandler certificateClaimsHandler = new CertificateClaimsHandler();
    ClaimCollection claims = getClaims();
    ClaimsParameters parameters = new ClaimsParameters();
    parameters.setPrincipal(mock(Principal.class));
    Map<String, Object> map = new HashMap<>();
    map.put(SubjectUtils.COUNTRY_CLAIM_URI, COUNTRY_CLAIM_URI_VALUE);
    parameters.setAdditionalProperties(map);
    ProcessedClaimCollection processedClaims =
        certificateClaimsHandler.retrieveClaimValues(claims, parameters);
    assertThat(processedClaims.size(), is(1));
    assertThat(
        processedClaims.stream().map(Claim::getClaimType).collect(Collectors.toList()),
        containsInAnyOrder(SubjectUtils.COUNTRY_CLAIM_URI));
  }
}
