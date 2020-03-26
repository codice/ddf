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
package org.codice.ddf.security.pki.realm;

import ddf.security.assertion.Attribute;
import ddf.security.assertion.AttributeStatement;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.assertion.impl.AttributeDefault;
import ddf.security.assertion.impl.AttributeStatementDefault;
import ddf.security.assertion.impl.DefaultSecurityAssertionBuilder;
import ddf.security.claims.Claim;
import ddf.security.claims.ClaimsCollection;
import ddf.security.claims.ClaimsHandler;
import ddf.security.claims.impl.ClaimsParametersImpl;
import ddf.security.impl.SubjectUtils;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.security.auth.x500.X500Principal;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.codice.ddf.security.handler.AuthenticationTokenType;
import org.codice.ddf.security.handler.BaseAuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PKIRealm extends AuthenticatingRealm {

  private static final Logger LOGGER = LoggerFactory.getLogger(PKIRealm.class);

  private List<ClaimsHandler> claimsHandlers = new ArrayList<>();

  private Duration fourHours = Duration.ofHours(4);

  /** Determine if the supplied token is supported by this realm. */
  @Override
  public boolean supports(AuthenticationToken token) {
    if (!(token instanceof BaseAuthenticationToken)) {
      LOGGER.debug(
          "The supplied authentication token is not an instance of BaseAuthenticationToken. Sending back not supported.");
      return false;
    }

    BaseAuthenticationToken authToken = (BaseAuthenticationToken) token;

    Object credentials = authToken.getCredentials();
    Object principal = authToken.getPrincipal();

    if (authToken.getType() != AuthenticationTokenType.PKI) {
      LOGGER.debug(
          "The supplied authentication token has null/empty credentials. Sending back no supported.");
      return false;
    }

    if (credentials instanceof X509Certificate[] && principal instanceof X500Principal) {
      LOGGER.debug("Token {} is supported by {}.", token.getClass(), PKIRealm.class.getName());
      return true;
    }

    return false;
  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token)
      throws AuthenticationException {
    X500Principal principal = (X500Principal) token.getPrincipal();
    X509Certificate[] certs = (X509Certificate[]) token.getCredentials();

    SimpleAuthenticationInfo simpleAuthenticationInfo = new SimpleAuthenticationInfo();
    SimplePrincipalCollection principalCollection =
        createPrincipalCollectionFromCertificate(principal);
    simpleAuthenticationInfo.setPrincipals(principalCollection);
    simpleAuthenticationInfo.setCredentials(certs);

    return simpleAuthenticationInfo;
  }

  private SimplePrincipalCollection createPrincipalCollectionFromCertificate(
      X500Principal principal) {
    SimplePrincipalCollection principals = new SimplePrincipalCollection();
    DefaultSecurityAssertionBuilder assertionBuilder = new DefaultSecurityAssertionBuilder();
    AttributeStatement attributeStatement = new AttributeStatementDefault();
    HashMap<String, Object> properties = createProperties(principal);
    for (ClaimsHandler claimsHandler : claimsHandlers) {
      ClaimsCollection claims =
          claimsHandler.retrieveClaims(
              new ClaimsParametersImpl(principal, Collections.singleton(principal), properties));
      mergeClaimsToAttributes(attributeStatement, claims);
    }
    final Instant now = Instant.now();

    SecurityAssertion assertion =
        assertionBuilder
            .addAttributeStatement(attributeStatement)
            .userPrincipal(principal)
            .weight(SecurityAssertion.LOCAL_AUTH_WEIGHT)
            .issuer("DDF")
            .notBefore(Date.from(now))
            .notOnOrAfter(Date.from(now.plus(fourHours)))
            .build();

    principals.add(assertion, "PKI");

    return principals;
  }

  private HashMap<String, Object> createProperties(X500Principal subjectX500Principal) {
    HashMap<String, Object> props = new HashMap<>();
    try {
      String emailAddress = SubjectUtils.getEmailAddress(subjectX500Principal);
      if (emailAddress != null) {
        props.put(SubjectUtils.EMAIL_ADDRESS_CLAIM_URI, emailAddress);
      }
      String country = SubjectUtils.getCountry(subjectX500Principal);
      if (country != null) {
        props.put(SubjectUtils.COUNTRY_CLAIM_URI, country);
      }
    } catch (Exception e) {
      LOGGER.debug("Unable to set email address or country from certificate.", e);
    }
    return props;
  }

  private void mergeClaimsToAttributes(
      AttributeStatement attributeStatement, ClaimsCollection claims) {
    for (Claim claim : claims) {
      Attribute newAttr = new AttributeDefault();
      newAttr.setName(claim.getName());
      newAttr.setValues(claim.getValues());
      boolean found = false;
      for (Attribute attribute : attributeStatement.getAttributes()) {
        if (attribute.getName().equals(newAttr.getName())) {
          found = true;
          for (String value : newAttr.getValues()) {
            attribute.addValue(value);
          }
        }
      }
      if (!found) {
        attributeStatement.addAttribute(newAttr);
      }
    }
  }

  public void setClaimsHandlers(List<ClaimsHandler> claimsHandlers) {
    this.claimsHandlers = claimsHandlers;
  }
}
