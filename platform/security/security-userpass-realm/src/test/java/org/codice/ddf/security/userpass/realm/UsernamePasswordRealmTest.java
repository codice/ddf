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
package org.codice.ddf.security.userpass.realm;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.security.assertion.Attribute;
import ddf.security.assertion.AttributeStatement;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.claims.ClaimsCollection;
import ddf.security.claims.ClaimsHandler;
import ddf.security.claims.impl.ClaimImpl;
import ddf.security.claims.impl.ClaimsCollectionImpl;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.jaas.config.JaasRealm;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.codice.ddf.security.handler.api.AuthenticationTokenFactory;
import org.codice.ddf.security.handler.api.AuthenticationTokenType;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.junit.Before;
import org.junit.Test;

public class UsernamePasswordRealmTest {
  UsernamePasswordRealm upRealm =
      new UsernamePasswordRealm() {
        protected Subject login(String username, String password, String realmName)
            throws LoginException {
          if (realmName.equals("realm")) {
            HashSet<Principal> principals = new HashSet<>();
            UserPrincipal principal = mock(UserPrincipal.class);
            RolePrincipal rolePrincipal = mock(RolePrincipal.class);
            when(rolePrincipal.getName()).thenReturn("manager");
            when(principal.getName()).thenReturn("admin");
            principals.add(principal);
            principals.add(rolePrincipal);
            return new Subject(true, principals, new HashSet<>(), new HashSet<>());
          }
          throw new LoginException();
        }
      };

  @Before
  public void setup() {
    List<ClaimsHandler> claimsHandlers = new ArrayList<>();
    claimsHandlers.add(mock(ClaimsHandler.class));
    claimsHandlers.add(mock(ClaimsHandler.class));
    ClaimsCollection claims1 = new ClaimsCollectionImpl();
    ClaimImpl email1 = new ClaimImpl("email");
    email1.addValue("test@example.com");
    claims1.add(email1);
    ClaimsCollection claims2 = new ClaimsCollectionImpl();
    ClaimImpl email2 = new ClaimImpl("email");
    email2.addValue("tester@example.com");
    claims2.add(email2);
    when(claimsHandlers.get(0).retrieveClaims(any())).thenReturn(claims1);
    when(claimsHandlers.get(1).retrieveClaims(any())).thenReturn(claims2);
    upRealm.setClaimsHandlers(claimsHandlers);

    JaasRealm jaasRealm = mock(JaasRealm.class);
    when(jaasRealm.getName()).thenReturn("realm");
    upRealm.realmList.add(jaasRealm);
  }

  @Test
  public void testSupportsGood() {
    BaseAuthenticationToken authenticationToken = mock(BaseAuthenticationToken.class);
    when(authenticationToken.getCredentials()).thenReturn(new Object());
    when(authenticationToken.getType()).thenReturn(AuthenticationTokenType.USERNAME);
    boolean supports = upRealm.supports(authenticationToken);
    assertTrue(supports);
  }

  @Test
  public void testSupportsBad() {
    AuthenticationToken authenticationToken = mock(AuthenticationToken.class);
    boolean supports = upRealm.supports(authenticationToken);
    assertFalse(supports);

    authenticationToken = mock(BaseAuthenticationToken.class);
    supports = upRealm.supports(authenticationToken);
    assertFalse(supports);

    when(authenticationToken.getCredentials()).thenReturn(new Object());
    supports = upRealm.supports(authenticationToken);
    assertFalse(supports);
  }

  @Test
  public void testDoGetAuthenticationInfo() {
    AuthenticationTokenFactory authenticationTokenFactory = new AuthenticationTokenFactory();
    AuthenticationToken authenticationToken =
        authenticationTokenFactory.fromUsernamePassword("admin", "pass", "0.0.0.0");

    AuthenticationInfo authenticationInfo = upRealm.doGetAuthenticationInfo(authenticationToken);
    SecurityAssertion assertion =
        authenticationInfo.getPrincipals().oneByType(SecurityAssertion.class);
    assertNotNull(assertion);
    assertThat(assertion.getPrincipal().getName(), is("admin"));

    AttributeStatement attributeStatement = assertion.getAttributeStatements().get(0);
    assertNotNull(attributeStatement);
    assertThat(attributeStatement.getAttributes().size(), greaterThan(0));
    Attribute attribute = attributeStatement.getAttributes().get(0);
    assertThat(attribute.getName(), is("email"));
    assertThat(attribute.getValues().size(), is(2));
    assertThat(attribute.getValues(), contains("tester@example.com", "test@example.com"));
  }
}
