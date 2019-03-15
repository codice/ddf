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
import java.util.Date;

public class DefaultSecurityAssertionBuilder {

  private SecurityAssertionDefault securityAssertion;

  public DefaultSecurityAssertionBuilder() {
    securityAssertion = new SecurityAssertionDefault();
  }

  public DefaultSecurityAssertionBuilder addPrincipal(Principal principal) {
    securityAssertion.principals.add(principal);
    return this;
  }

  public DefaultSecurityAssertionBuilder issuer(String issuer) {
    securityAssertion.issuer = issuer;
    return this;
  }

  public DefaultSecurityAssertionBuilder addAttributeStatement(
      AttributeStatement attributeStatement) {
    securityAssertion.attributeStatements.add(attributeStatement);
    return this;
  }

  public DefaultSecurityAssertionBuilder addAuthnStatement(
      AuthenticationStatement authenticationStatement) {
    securityAssertion.authenticationStatements.add(authenticationStatement);
    return this;
  }

  public DefaultSecurityAssertionBuilder addSubjectConfirmation(String subjectConfirmation) {
    securityAssertion.subjectConfirmations.add(subjectConfirmation);
    return this;
  }

  public DefaultSecurityAssertionBuilder tokenType(String tokenType) {
    securityAssertion.tokenType = tokenType;
    return this;
  }

  public DefaultSecurityAssertionBuilder token(Object token) {
    securityAssertion.token = token;
    return this;
  }

  public DefaultSecurityAssertionBuilder notBefore(Date notBefore) {
    securityAssertion.notBefore = notBefore;
    return this;
  }

  public DefaultSecurityAssertionBuilder notOnOrAfter(Date notOnOrAfter) {
    securityAssertion.notOnOrAfter = notOnOrAfter;
    return this;
  }

  public DefaultSecurityAssertionBuilder weight(int weight) {
    securityAssertion.weight = weight;
    return this;
  }

  public SecurityAssertion build() {
    return securityAssertion;
  }
}
