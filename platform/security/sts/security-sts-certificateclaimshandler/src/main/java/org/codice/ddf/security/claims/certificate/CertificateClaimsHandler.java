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

import ddf.security.SubjectUtils;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.rt.security.claims.Claim;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.sts.claims.ClaimsHandler;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.ProcessedClaim;
import org.apache.cxf.sts.claims.ProcessedClaimCollection;
import org.apache.cxf.sts.token.realm.RealmSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CertificateClaimsHandler implements ClaimsHandler, RealmSupport {

  private static final Logger LOGGER = LoggerFactory.getLogger(CertificateClaimsHandler.class);

  private String emailClaim = SubjectUtils.EMAIL_ADDRESS_CLAIM_URI;

  private String countryClaim = SubjectUtils.COUNTRY_CLAIM_URI;

  private List<String> supportedRealms;

  private String realm;

  @Override
  public List<String> getSupportedClaimTypes() {
    try {
      // Converting to URI despite String return type to maintain the URI validation
      return Arrays.asList(new URI(emailClaim).toString(), new URI(countryClaim).toString());
    } catch (URISyntaxException e) {
      LOGGER.info("Unable to create claim URIs for certificate claims.", e);
    }
    return Collections.emptyList();
  }

  @Override
  public ProcessedClaimCollection retrieveClaimValues(
      ClaimCollection claims, ClaimsParameters parameters) {
    ProcessedClaimCollection claimsColl = new ProcessedClaimCollection();
    Principal principal = parameters.getPrincipal();
    Map<String, Object> additionalProperties = parameters.getAdditionalProperties();
    if (additionalProperties != null
        && (additionalProperties.containsKey(SubjectUtils.EMAIL_ADDRESS_CLAIM_URI)
            || additionalProperties.containsKey(SubjectUtils.COUNTRY_CLAIM_URI))) {
      for (Claim claim : claims) {
        String claimType = claim.getClaimType();
        if (emailClaim.equals(claimType)) {
          buildClaim(
              claimsColl,
              principal,
              claimType,
              additionalProperties.get(SubjectUtils.EMAIL_ADDRESS_CLAIM_URI));
        } else if (countryClaim.equals(claimType)) {
          buildClaim(
              claimsColl,
              principal,
              claimType,
              additionalProperties.get(SubjectUtils.COUNTRY_CLAIM_URI));
        }
      }
    }
    return claimsColl;
  }

  private void buildClaim(
      ProcessedClaimCollection claimsColl, Principal principal, String claimType, Object value) {
    if (value == null) {
      return;
    }

    ProcessedClaim c = new ProcessedClaim();
    c.setClaimType(claimType);
    c.setPrincipal(principal);

    if (value instanceof List) {
      List<?> valueList = (List<?>) value;
      valueList.forEach(c::addValue);
    } else {
      c.addValue(value);
    }
    claimsColl.add(c);
  }

  public void setEmailClaim(String emailClaim) {
    this.emailClaim =
        StringUtils.isEmpty(emailClaim) ? SubjectUtils.EMAIL_ADDRESS_CLAIM_URI : emailClaim;
  }

  public void setCountryClaim(String countryClaim) {
    this.countryClaim =
        StringUtils.isEmpty(countryClaim) ? SubjectUtils.COUNTRY_CLAIM_URI : countryClaim;
  }

  @Override
  public List<String> getSupportedRealms() {
    return supportedRealms;
  }

  public void setSupportedRealms(List<String> supportedRealms) {
    this.supportedRealms = supportedRealms;
  }

  @Override
  public String getHandlerRealm() {
    return realm;
  }

  public void setHandlerRealm(String realm) {
    this.realm = realm;
  }
}
