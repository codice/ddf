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

import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.sts.claims.ClaimsHandler;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.ProcessedClaim;
import org.apache.cxf.sts.claims.ProcessedClaimCollection;
import org.apache.cxf.sts.token.realm.RealmSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CasClaimsHandler implements ClaimsHandler, RealmSupport {

  private static final Logger LOGGER = LoggerFactory.getLogger(CasClaimsHandler.class);

  private Map<String, String> attributeMap = new HashMap<>();

  private List<String> supportedClaims = new ArrayList<>();

  private List<String> supportedRealms;

  private String realm;

  @Override
  public List<URI> getSupportedClaimTypes() {
    List<URI> supportedClaimTypes = new ArrayList<>();

    for (String claim : supportedClaims) {
      try {
        supportedClaimTypes.add(new URI(claim));
      } catch (URISyntaxException e) {
        LOGGER.error("Invalid supported claim: \"{}\"", claim);
      }
    }

    return supportedClaimTypes;
  }

  @Override
  public ProcessedClaimCollection retrieveClaimValues(
      ClaimCollection claims, ClaimsParameters parameters) {
    ProcessedClaimCollection claimsColl = new ProcessedClaimCollection();
    Principal principal = parameters.getPrincipal();
    Map<String, Object> additionalProperties = parameters.getAdditionalProperties();

    if (additionalProperties != null) {
      if (LOGGER.isTraceEnabled()) {
        StringBuffer output = new StringBuffer();
        String prefix = "\n";
        output.append("CAS attributes returned: {");
        for (Map.Entry<String, Object> entry : additionalProperties.entrySet()) {
          output.append(prefix);
          prefix = ", \n";
          output.append(entry.getKey());
          output.append("=");
          output.append(entry.getValue());
        }
        output.append("\n}");
        LOGGER.trace(output.toString());
      }

      claims
          .stream()
          .distinct()
          .forEach(
              claim -> {
                URI claimType = claim.getClaimType();
                String claimTypeString = claimType.toString();
                String mappedClaimType =
                    attributeMap.getOrDefault(claimTypeString, claimTypeString);
                buildClaim(
                    claimsColl, principal, claimType, additionalProperties.get(mappedClaimType));
              });
    }

    if (LOGGER.isTraceEnabled()) {
      StringBuffer output = new StringBuffer();
      output.append("Claims map returned: {");
      String prefix = "\n";
      for (ProcessedClaim claim : claimsColl) {
        output.append(prefix);
        prefix = ", \n";
        output.append(claim.toString());
      }
      output.append("\n}");
      LOGGER.trace(output.toString());
    }

    return claimsColl;
  }

  private void buildClaim(
      ProcessedClaimCollection claimsColl, Principal principal, URI claimType, Object value) {
    if (value == null) {
      return;
    }

    ProcessedClaim c = new ProcessedClaim();
    c.setClaimType(claimType);
    c.setPrincipal(principal);

    // Multivalued attributes have values of the form "[val1, val2, ...]". Match such strings
    // and return the substring within the brackets as group 1
    Pattern p = Pattern.compile("^\\[(.*)\\]$");
    Matcher m = p.matcher((String) value);
    if (m.matches()) {
      for (String s : m.group(1).split(",")) {
        c.addValue(s.trim());
      }
    } else {
      c.addValue(value);
    }

    claimsColl.add(c);
  }

  public Map<String, String> getAttributeMap() {
    return this.attributeMap;
  }

  public void setAttributeMap(Map<String, String> attributeMap) {
    this.attributeMap = attributeMap;
  }

  public void setAttributeMap(List<String> attributeMapEntries) {
    this.attributeMap = new HashMap<>();

    for (String entry : attributeMapEntries) {
      // Split entry at first unescaped equals. Unescaped regex: (?<!\\)(?:\\\\)*=
      String[] keyValuePair = entry.split("(?<!\\\\)(?:\\\\\\\\)*=", 2);
      if (keyValuePair.length == 2) {
        this.attributeMap.put(
            StringEscapeUtils.unescapeJava(keyValuePair[0]),
            StringEscapeUtils.unescapeJava(keyValuePair[1]));
      } else {
        LOGGER.error("Invalid attribute map entry: {}", entry);
      }
    }
  }

  public void setSupportedClaims(List<String> supportedClaims) {
    this.supportedClaims = supportedClaims;
  }

  @Override
  public List<String> getSupportedRealms() {
    return this.supportedRealms;
  }

  public void setSupportedRealms(List<String> supportedRealms) {
    this.supportedRealms = supportedRealms;
  }

  @Override
  public String getHandlerRealm() {
    return this.realm;
  }

  public void setHandlerRealm(String realm) {
    this.realm = realm;
  }
}
