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
import ddf.security.claims.ClaimsCollection;
import ddf.security.claims.ClaimsParameters;
import ddf.security.claims.impl.ClaimsParametersImpl;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Test;

public class CertificateClaimsHandlerTest {

  @Test
  public void testRetrieveClaimValuesNoCertValues() throws URISyntaxException {
    CertificateClaimsHandler certificateClaimsHandler = new CertificateClaimsHandler();
    ClaimsParameters parameters =
        new ClaimsParametersImpl(mock(Principal.class), new HashSet<>(), new HashMap<>());
    ClaimsCollection processedClaims = certificateClaimsHandler.retrieveClaims(parameters);
    assertThat(processedClaims.size(), is(0));
  }

  @Test
  public void testRetrieveClaimValuesWithCertValues() throws URISyntaxException {
    CertificateClaimsHandler certificateClaimsHandler = new CertificateClaimsHandler();
    Map<String, Object> map = new HashMap<>();
    map.put(SubjectUtils.EMAIL_ADDRESS_CLAIM_URI, "local@localhost");
    map.put(SubjectUtils.COUNTRY_CLAIM_URI, "USA");
    ClaimsParameters parameters =
        new ClaimsParametersImpl(mock(Principal.class), new HashSet<>(), map);
    ClaimsCollection processedClaims = certificateClaimsHandler.retrieveClaims(parameters);
    assertThat(processedClaims.size(), is(2));
    assertThat(
        processedClaims
            .stream()
            .map(ddf.security.claims.Claim::getName)
            .collect(Collectors.toList()),
        containsInAnyOrder(SubjectUtils.EMAIL_ADDRESS_CLAIM_URI, SubjectUtils.COUNTRY_CLAIM_URI));
  }

  @Test
  public void testRetrieveClaimValuesWithAltNamesRequested() throws URISyntaxException {
    CertificateClaimsHandler certificateClaimsHandler = new CertificateClaimsHandler();
    certificateClaimsHandler.setCountryClaim("Country");
    certificateClaimsHandler.setEmailClaim("Email");
    Map<String, Object> map = new HashMap<>();
    map.put(SubjectUtils.EMAIL_ADDRESS_CLAIM_URI, "local@localhost");
    map.put(SubjectUtils.COUNTRY_CLAIM_URI, "USA");
    ClaimsParameters parameters =
        new ClaimsParametersImpl(mock(Principal.class), new HashSet<>(), map);
    ClaimsCollection processedClaims = certificateClaimsHandler.retrieveClaims(parameters);
    assertThat(processedClaims.size(), is(2));
    assertThat(
        processedClaims
            .stream()
            .map(ddf.security.claims.Claim::getName)
            .collect(Collectors.toList()),
        containsInAnyOrder("Email", "Country"));
  }

  @Test
  public void testRetrieveClaimValuesWithEmail() throws URISyntaxException {
    CertificateClaimsHandler certificateClaimsHandler = new CertificateClaimsHandler();
    Map<String, Object> map = new HashMap<>();
    map.put(SubjectUtils.EMAIL_ADDRESS_CLAIM_URI, "local@localhost");
    ClaimsParameters parameters =
        new ClaimsParametersImpl(mock(Principal.class), new HashSet<>(), map);
    ClaimsCollection processedClaims = certificateClaimsHandler.retrieveClaims(parameters);
    assertThat(processedClaims.size(), is(1));
    assertThat(
        processedClaims
            .stream()
            .map(ddf.security.claims.Claim::getName)
            .collect(Collectors.toList()),
        containsInAnyOrder(SubjectUtils.EMAIL_ADDRESS_CLAIM_URI));
  }

  @Test
  public void testRetrieveClaimValuesWithCountry() throws URISyntaxException {
    CertificateClaimsHandler certificateClaimsHandler = new CertificateClaimsHandler();
    Map<String, Object> map = new HashMap<>();
    map.put(SubjectUtils.COUNTRY_CLAIM_URI, "USA");
    ClaimsParameters parameters =
        new ClaimsParametersImpl(mock(Principal.class), new HashSet<>(), map);
    ClaimsCollection processedClaims = certificateClaimsHandler.retrieveClaims(parameters);
    assertThat(processedClaims.size(), is(1));
    assertThat(
        processedClaims
            .stream()
            .map(ddf.security.claims.Claim::getName)
            .collect(Collectors.toList()),
        containsInAnyOrder(SubjectUtils.COUNTRY_CLAIM_URI));
  }
}
