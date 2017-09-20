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
package org.codice.ddf.security.sts.claims.property;

import ddf.security.PropertiesLoader;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.x500.X500Principal;
import org.apache.cxf.rt.security.claims.Claim;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.sts.claims.ClaimsHandler;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.ProcessedClaim;
import org.apache.cxf.sts.claims.ProcessedClaimCollection;
import org.apache.cxf.sts.token.realm.RealmSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertyFileClaimsHandler implements ClaimsHandler, RealmSupport {

  private static final Logger LOGGER = LoggerFactory.getLogger(PropertyFileClaimsHandler.class);

  private String propertyFileLocation;

  private Map<String, String> userMapping;

  private List<String> supportedRealms;

  private String realm;

  private String roleClaimType;

  private String idClaimType;

  @Override
  public List<URI> getSupportedClaimTypes() {
    List<URI> uriList = new ArrayList<>();
    uriList.add(URI.create(roleClaimType));
    uriList.add(URI.create(idClaimType));
    return uriList;
  }

  @Override
  public ProcessedClaimCollection retrieveClaimValues(
      ClaimCollection claims, ClaimsParameters parameters) {
    ProcessedClaimCollection claimsColl = new ProcessedClaimCollection();
    Principal principal = parameters.getPrincipal();
    boolean needsRoleClaim = false;
    boolean needsIdClaim = false;
    for (Claim claim : claims) {
      if (roleClaimType.equals(claim.getClaimType().toString())) {
        needsRoleClaim = true;
      } else if (idClaimType.equals(claim.getClaimType().toString())) {
        needsIdClaim = true;
      } else {
        LOGGER.debug("Unsupported claim: {}", claim.getClaimType());
      }
    }
    String user = getUser(principal);
    if (user == null) {
      LOGGER.info(
          "Could not determine user name, possible authentication error. Returning no claims.");
      return claimsColl;
    }

    if (needsRoleClaim) {
      String userAttributes = userMapping.get(user);
      if (userAttributes != null) {
        String[] attributes = userAttributes.split(",");
        ProcessedClaim c = new ProcessedClaim();
        c.setClaimType(URI.create(roleClaimType));
        c.setPrincipal(principal);
        for (int i = 1; i < attributes.length; i++) {
          c.addValue(attributes[i]);
        }
        claimsColl.add(c);
      }
    }

    if (needsIdClaim) {
      ProcessedClaim idClaim = new ProcessedClaim();
      idClaim.setClaimType(URI.create(idClaimType));
      idClaim.setPrincipal(principal);
      idClaim.addValue(user);
      claimsColl.add(idClaim);
    }

    return claimsColl;
  }

  /**
   * Obtains the user name from the principal.
   *
   * @param principal Describing the current user that should be used for retrieving claims.
   * @return the user name if the principal has one, null if no name is specified or if principal is
   *     null.
   */
  public String getUser(Principal principal) {
    String user = null;
    if (principal instanceof KerberosPrincipal) {
      KerberosPrincipal kp = (KerberosPrincipal) principal;
      StringTokenizer st = new StringTokenizer(kp.getName(), "@");
      user = st.nextToken();
    } else if (principal instanceof X500Principal) {
      X500Principal x500p = (X500Principal) principal;
      StringTokenizer st = new StringTokenizer(x500p.getName(), ",");
      while (st.hasMoreElements()) {
        // token is in the format:
        // syntaxAndUniqueId
        // cn
        // ou
        // o
        // loc
        // state
        // country
        String[] strArr = st.nextToken().split("=");
        if (strArr.length > 1 && strArr[0].equalsIgnoreCase("cn")) {
          user = strArr[1];
          break;
        }
      }
    } else if (principal != null) {
      user = principal.getName();
    }

    return user;
  }

  @Override
  public List<String> getSupportedRealms() {
    return supportedRealms;
  }

  @Override
  public String getHandlerRealm() {
    return realm;
  }

  public String getPropertyFileLocation() {
    return propertyFileLocation;
  }

  public void setPropertyFileLocation(String propertyFileLocation) {
    if (propertyFileLocation != null
        && !propertyFileLocation.isEmpty()
        && !propertyFileLocation.equals(this.propertyFileLocation)) {
      userMapping = PropertiesLoader.toMap(PropertiesLoader.loadProperties(propertyFileLocation));
    }
    this.propertyFileLocation = propertyFileLocation;
  }

  public String getRoleClaimType() {
    return roleClaimType;
  }

  public void setRoleClaimType(String roleClaimType) {
    this.roleClaimType = roleClaimType;
  }

  public String getIdClaimType() {
    return idClaimType;
  }

  public void setIdClaimType(String idClaimType) {
    this.idClaimType = idClaimType;
  }

  public void setRealm(String realm) {
    this.realm = realm;
  }
}
