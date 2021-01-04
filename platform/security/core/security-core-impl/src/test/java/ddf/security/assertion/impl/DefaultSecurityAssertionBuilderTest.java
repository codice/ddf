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

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import ddf.security.assertion.AttributeStatement;
import ddf.security.assertion.AuthenticationStatement;
import ddf.security.assertion.SecurityAssertion;
import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.junit.Test;

public class DefaultSecurityAssertionBuilderTest {

  @Test
  public void testDefaultSecurityAssertionBuilder() {
    Principal principal = mock(Principal.class);
    AttributeStatement attributeStatement = mock(AttributeStatement.class);
    AuthenticationStatement authenticationStatement = mock(AuthenticationStatement.class);
    Object token = new Object();
    Date notBefore = Date.from(Instant.now());
    Date notOnOrAfter = Date.from(Instant.now().plus(Duration.ofMinutes(1)));

    DefaultSecurityAssertionBuilder builder = new DefaultSecurityAssertionBuilder();
    SecurityAssertion assertion =
        builder
            .userPrincipal(principal)
            .addPrincipal(principal)
            .issuer("test")
            .addAttributeStatement(attributeStatement)
            .addAuthnStatement(authenticationStatement)
            .addSubjectConfirmation("subjectConfirmation")
            .tokenType("testToken")
            .token(token)
            .notBefore(notBefore)
            .notOnOrAfter(notOnOrAfter)
            .weight(7)
            .build();

    assertThat(assertion.getPrincipal(), is(principal));
    assertThat(assertion.getPrincipals(), hasItem(principal));
    assertThat(assertion.getIssuer(), is("test"));
    assertThat(assertion.getAttributeStatements(), hasItem(attributeStatement));
    assertThat(assertion.getSubjectConfirmations(), hasItem("subjectConfirmation"));
    assertThat(assertion.getTokenType(), is("testToken"));
    assertThat(assertion.getToken(), is(token));
    assertThat(assertion.getNotBefore(), is(notBefore));
    assertThat(assertion.getNotOnOrAfter(), is(notOnOrAfter));
    assertThat(assertion.getWeight(), is(7));
  }

  @Test(expected = NullPointerException.class)
  public void testDefaultSecurityAssertionBuilderNoUserPrincipal() {
    DefaultSecurityAssertionBuilder builder = new DefaultSecurityAssertionBuilder();
    builder
        .addPrincipal(mock(Principal.class))
        .issuer("test")
        .addAttributeStatement(mock(AttributeStatementDefault.class))
        .notBefore(Date.from(Instant.now()))
        .notOnOrAfter(Date.from(Instant.now().plus(Duration.ofMinutes(1))))
        .build();
  }
}
