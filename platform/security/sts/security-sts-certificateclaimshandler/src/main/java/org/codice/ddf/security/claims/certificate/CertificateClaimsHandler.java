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
import ddf.security.claims.Claim;
import ddf.security.claims.ClaimsCollection;
import ddf.security.claims.ClaimsHandler;
import ddf.security.claims.ClaimsParameters;
import ddf.security.claims.impl.ClaimImpl;
import ddf.security.claims.impl.ClaimsCollectionImpl;
import java.util.List;
import java.util.Map;
import org.apache.cxf.common.util.StringUtils;

public class CertificateClaimsHandler implements ClaimsHandler {

  private String emailClaim = SubjectUtils.EMAIL_ADDRESS_CLAIM_URI;

  private String countryClaim = SubjectUtils.COUNTRY_CLAIM_URI;

  @Override
  public ClaimsCollection retrieveClaims(ClaimsParameters parameters) {
    ClaimsCollection claimsColl = new ClaimsCollectionImpl();
    Map<String, Object> additionalProperties = parameters.getAdditionalProperties();
    if (additionalProperties != null) {
      if (additionalProperties.containsKey(SubjectUtils.EMAIL_ADDRESS_CLAIM_URI)) {
        buildClaim(
            claimsColl, emailClaim, additionalProperties.get(SubjectUtils.EMAIL_ADDRESS_CLAIM_URI));
      }
      if (additionalProperties.containsKey(SubjectUtils.COUNTRY_CLAIM_URI)) {
        buildClaim(
            claimsColl, countryClaim, additionalProperties.get(SubjectUtils.COUNTRY_CLAIM_URI));
      }
    }
    return claimsColl;
  }

  private void buildClaim(ClaimsCollection claimsColl, String claimType, Object value) {
    if (value == null) {
      return;
    }

    Claim claim = new ClaimImpl(claimType);

    if (value instanceof List) {
      List<String> valueList = (List<String>) value;
      valueList.forEach(claim::addValue);
    } else {
      claim.addValue(value.toString());
    }
    claimsColl.add(claim);
  }

  public void setEmailClaim(String emailClaim) {
    this.emailClaim =
        StringUtils.isEmpty(emailClaim) ? SubjectUtils.EMAIL_ADDRESS_CLAIM_URI : emailClaim;
  }

  public void setCountryClaim(String countryClaim) {
    this.countryClaim =
        StringUtils.isEmpty(countryClaim) ? SubjectUtils.COUNTRY_CLAIM_URI : countryClaim;
  }
}
