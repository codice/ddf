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
import ddf.security.claims.ClaimsCollection;
import ddf.security.claims.ClaimsHandler;
import ddf.security.claims.impl.ClaimImpl;
import ddf.security.claims.impl.ClaimsCollectionImpl;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import javax.security.auth.x500.X500Principal;
import org.apache.shiro.authc.AuthenticationInfo;
import org.codice.ddf.security.handler.api.AuthenticationTokenType;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PKIRealmTest {

  PKIRealm pkiRealm = new PKIRealm();

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
    pkiRealm.setClaimsHandlers(claimsHandlers);
  }

  @Test
  public void testSupportsGood() {
    BaseAuthenticationToken authenticationToken = mock(BaseAuthenticationToken.class);
    when(authenticationToken.getCredentials()).thenReturn(new X509Certificate[1]);
    when(authenticationToken.getPrincipal()).thenReturn(new X500Principal("cn=test"));
    when(authenticationToken.getType()).thenReturn(AuthenticationTokenType.PKI);
    boolean supports = pkiRealm.supports(authenticationToken);
    assertTrue(supports);
  }

  @Test
  public void testSupportsBad() {
    BaseAuthenticationToken authenticationToken = mock(BaseAuthenticationToken.class);
    boolean supports = pkiRealm.supports(authenticationToken);
    assertFalse(supports);

    when(authenticationToken.getCredentials()).thenReturn(new Object());
    when(authenticationToken.getPrincipal()).thenReturn(new Object());
    supports = pkiRealm.supports(authenticationToken);
    assertFalse(supports);

    when(authenticationToken.getType()).thenReturn(AuthenticationTokenType.SAML);
    supports = pkiRealm.supports(authenticationToken);
    assertFalse(supports);

    when(authenticationToken.getCredentials()).thenReturn(new X509Certificate[1]);
    when(authenticationToken.getType()).thenReturn(AuthenticationTokenType.PKI);
    supports = pkiRealm.supports(authenticationToken);
    assertFalse(supports);

    when(authenticationToken.getCredentials()).thenReturn(new Object());
    when(authenticationToken.getPrincipal()).thenReturn(new X500Principal("cn=test"));
    supports = pkiRealm.supports(authenticationToken);
    assertFalse(supports);
  }

  @Test
  public void testDoGetAuthenticationInfo() {
    BaseAuthenticationToken authenticationToken = mock(BaseAuthenticationToken.class);
    X509Certificate[] certificates = new X509Certificate[1];
    certificates[0] = mock(X509Certificate.class);
    X500Principal x500Principal = new X500Principal("cn=myxman,ou=someunit,o=someorg");
    when(authenticationToken.getCredentials()).thenReturn(certificates);
    when(authenticationToken.getPrincipal()).thenReturn(x500Principal);
    when(authenticationToken.getType()).thenReturn(AuthenticationTokenType.PKI);

    AuthenticationInfo authenticationInfo = pkiRealm.doGetAuthenticationInfo(authenticationToken);

    assertThat(authenticationInfo.getCredentials(), is(certificates));
    SecurityAssertion assertion =
        authenticationInfo.getPrincipals().oneByType(SecurityAssertion.class);
    assertNotNull(assertion);
    assertThat(assertion.getPrincipal(), is(x500Principal));

    AttributeStatement attributeStatement = assertion.getAttributeStatements().get(0);
    assertNotNull(attributeStatement);
    assertThat(attributeStatement.getAttributes().size(), greaterThan(0));
    Attribute attribute = attributeStatement.getAttributes().get(0);
    assertThat(attribute.getName(), is("email"));
    assertThat(attribute.getValues().size(), is(2));
    assertThat(attribute.getValues(), contains("tester@example.com", "test@example.com"));
  }
}
