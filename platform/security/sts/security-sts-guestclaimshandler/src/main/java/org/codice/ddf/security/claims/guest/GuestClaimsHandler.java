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

import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.cxf.rt.security.claims.Claim;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.sts.claims.ClaimsHandler;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.ProcessedClaim;
import org.apache.cxf.sts.claims.ProcessedClaimCollection;
import org.apache.cxf.sts.token.realm.RealmSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides claims for a guest auth token. */
public class GuestClaimsHandler implements ClaimsHandler, RealmSupport {

  private static final Logger LOGGER = LoggerFactory.getLogger(GuestClaimsHandler.class);

  private Map<URI, List<String>> claimsMap = new HashMap<>();

  private List<String> supportedRealms;

  private String realm;

  public GuestClaimsHandler() {
    LOGGER.debug("Starting GuestClaimsHandler");
  }

  public void setAttributes(List<String> attributes) {
    if (attributes != null) {
      LOGGER.debug("Attribute value list was set.");
      List<String> attrs = new ArrayList<>(attributes.size());
      attrs.addAll(attributes);
      initClaimsMap(attrs);
    } else {
      LOGGER.debug("Set attribute value list was null");
    }
  }

  public Map<URI, List<String>> getClaimsMap() {
    return Collections.unmodifiableMap(claimsMap);
  }

  private void initClaimsMap(List<String> attributes) {
    for (String attr : attributes) {
      String[] claimMapping = attr.split("=");
      if (claimMapping.length == 2) {
        try {
          List<String> values = new ArrayList<>();
          if (claimMapping[1].contains("|")) {
            String[] valsArr = claimMapping[1].split("\\|");
            Collections.addAll(values, valsArr);
          } else {
            values.add(claimMapping[1]);
          }
          claimsMap.put(new URI(claimMapping[0]), values);
        } catch (URISyntaxException e) {
          LOGGER.info(
              "Claims mapping cannot be converted to a URI. This claim will be excluded: {}",
              attr,
              e);
        }
      } else {
        LOGGER.warn("Invalid claims mapping entered for guest user: {}", attr);
      }
    }
  }

  @Override
  public List<URI> getSupportedClaimTypes() {
    return new ArrayList<>(claimsMap.keySet());
  }

  @Override
  public ProcessedClaimCollection retrieveClaimValues(
      ClaimCollection claims, ClaimsParameters parameters) {
    ProcessedClaimCollection claimsColl = new ProcessedClaimCollection();
    Principal principal = parameters.getPrincipal();
    for (Claim claim : claims) {
      URI claimType = claim.getClaimType();
      List<String> value = claimsMap.get(claimType);
      if (value != null) {
        ProcessedClaim c = new ProcessedClaim();
        c.setClaimType(claimType);
        c.setPrincipal(principal);
        for (String val : value) {
          c.addValue(val);
        }
        claimsColl.add(c);
      }
    }

    Map<String, Object> additionalProperties = parameters.getAdditionalProperties();
    if (additionalProperties != null) {
      for (Entry<String, Object> additionalProperty : additionalProperties.entrySet()) {
        try {
          ProcessedClaim pc = new ProcessedClaim();
          pc.setClaimType(new URI(additionalProperty.getKey()));
          pc.setPrincipal(principal);
          pc.addValue(additionalProperty.getValue());
          claimsColl.add(pc);
        } catch (URISyntaxException e) {
          LOGGER.info(
              "Claims mapping cannot be converted to a URI. {} claim will be excluded",
              additionalProperty.getKey());
        }
      }
    }

    return claimsColl;
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
