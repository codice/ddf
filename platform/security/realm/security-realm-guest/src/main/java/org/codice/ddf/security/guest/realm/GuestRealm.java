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
package org.codice.ddf.security.guest.realm;

import ddf.security.assertion.SecurityAssertion;
import ddf.security.assertion.impl.AttributeDefault;
import ddf.security.assertion.impl.AttributeStatementDefault;
import ddf.security.assertion.impl.DefaultSecurityAssertionBuilder;
import ddf.security.common.audit.SecurityLogger;
import ddf.security.principal.GuestPrincipal;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.codice.ddf.security.handler.api.GuestAuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GuestRealm extends AuthenticatingRealm {

  private static final Logger LOGGER = LoggerFactory.getLogger(GuestRealm.class);

  private Map<URI, List<String>> claimsMap = new HashMap<>();

  /** Determine if the supplied token is supported by this realm. */
  @Override
  public boolean supports(AuthenticationToken token) {
    boolean supported =
        token != null
            && token.getCredentials() != null
            && token instanceof BaseAuthenticationToken
            && (((BaseAuthenticationToken) token).getAllowGuest()
                || token instanceof GuestAuthenticationToken);

    if (supported) {
      LOGGER.debug("Token {} is supported by {}.", token.getClass(), GuestRealm.class.getName());
    } else if (token != null) {
      LOGGER.debug(
          "Token {} is not supported by {}.", token.getClass(), GuestRealm.class.getName());
    } else {
      LOGGER.debug("The supplied authentication token is null. Sending back not supported.");
    }

    return supported;
  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken)
      throws AuthenticationException {
    BaseAuthenticationToken baseAuthenticationToken = (BaseAuthenticationToken) authenticationToken;
    SimpleAuthenticationInfo simpleAuthenticationInfo = new SimpleAuthenticationInfo();
    SimplePrincipalCollection principals = createPrincipalFromToken(baseAuthenticationToken);
    simpleAuthenticationInfo.setPrincipals(principals);
    simpleAuthenticationInfo.setCredentials(authenticationToken.getCredentials());

    SecurityLogger.audit(
        "Guest assertion generated for IP address: " + baseAuthenticationToken.getIpAddress());
    return simpleAuthenticationInfo;
  }

  private SimplePrincipalCollection createPrincipalFromToken(BaseAuthenticationToken token) {
    SimplePrincipalCollection principals = new SimplePrincipalCollection();
    DefaultSecurityAssertionBuilder defaultSecurityAssertionBuilder =
        new DefaultSecurityAssertionBuilder();
    Set<Map.Entry<URI, List<String>>> entries = claimsMap.entrySet();
    AttributeStatementDefault attributeStatement = new AttributeStatementDefault();
    for (Map.Entry<URI, List<String>> entry : entries) {
      AttributeDefault attribute = new AttributeDefault();
      attribute.setName(entry.getKey().toString());
      for (String value : entry.getValue()) {
        attribute.addValue(value);
      }
      attributeStatement.addAttribute(attribute);
    }
    defaultSecurityAssertionBuilder.addAttributeStatement(attributeStatement);
    defaultSecurityAssertionBuilder.addPrincipal(new GuestPrincipal(token.getIpAddress()));
    defaultSecurityAssertionBuilder.issuer("local");
    defaultSecurityAssertionBuilder.notBefore(new Date());
    // We don't really care how long it is "valid" for
    defaultSecurityAssertionBuilder.notOnOrAfter(new Date(new Date().getTime() + 14400000L));
    defaultSecurityAssertionBuilder.token(token);
    defaultSecurityAssertionBuilder.tokenType("guest");
    SecurityAssertion securityAssertion = defaultSecurityAssertionBuilder.build();

    Principal principal = securityAssertion.getPrincipal();
    if (principal != null) {
      principals.add(principal.getName(), getName());
    }
    principals.add(securityAssertion, getName());
    return principals;
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
}
