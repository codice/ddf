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
package ddf.security.assertion.impl;

import ddf.security.assertion.AttributeStatement;
import ddf.security.assertion.AuthenticationStatement;
import ddf.security.assertion.SecurityAssertion;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecurityAssertionDefault implements SecurityAssertion {

  private static final Logger LOGGER = LoggerFactory.getLogger(SecurityAssertionDefault.class);

  Set<Principal> principals = new HashSet<>();

  String issuer;

  List<AttributeStatement> attributeStatements = new ArrayList<>();

  List<AuthenticationStatement> authenticationStatements = new ArrayList<>();

  List<String> subjectConfirmations = new ArrayList<>();

  String tokenType;

  Object token;

  Date notBefore;

  Date notOnOrAfter;

  int weight = SecurityAssertion.NO_AUTH_WEIGHT;

  @Override
  public Principal getPrincipal() {
    return principals.iterator().next();
  }

  @Override
  public String getIssuer() {
    return issuer;
  }

  @Override
  public List<AttributeStatement> getAttributeStatements() {
    return attributeStatements;
  }

  @Override
  public List<AuthenticationStatement> getAuthnStatements() {
    return authenticationStatements;
  }

  @Override
  public List<String> getSubjectConfirmations() {
    return subjectConfirmations;
  }

  @Override
  public Set<Principal> getPrincipals() {
    return principals;
  }

  @Override
  public String getTokenType() {
    return tokenType;
  }

  @Override
  public Object getToken() {
    return token;
  }

  @Override
  public Date getNotBefore() {
    return notBefore;
  }

  @Override
  public Date getNotOnOrAfter() {
    return notOnOrAfter;
  }

  @Override
  public int getWeight() {
    return weight;
  }

  @Override
  public boolean isPresentlyValid() {
    Date now = new Date();

    if (getNotBefore() != null && now.before(getNotBefore())) {
      LOGGER.debug("Security Assertion Time Bound Check Failed.");
      LOGGER.debug("\t Checked time of {} is before the NotBefore time of {}", now, getNotBefore());
      return false;
    }

    if (getNotOnOrAfter() != null
        && (now.equals(getNotOnOrAfter()) || now.after(getNotOnOrAfter()))) {
      LOGGER.debug("Security Assertion Time Bound Check Failed.");
      LOGGER.debug(
          "\t Checked time of {} is equal to or after the NotOnOrAfter time of {}",
          now,
          getNotOnOrAfter());
      return false;
    }

    return true;
  }
}
