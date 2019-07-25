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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import ddf.security.assertion.Attribute;
import ddf.security.assertion.SecurityAssertion;
import java.util.Arrays;
import java.util.Iterator;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.PrincipalCollection;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class GuestRealmTest {

  private static GuestRealm guestRealm;

  @BeforeClass
  public static void setup() {
    guestRealm = new GuestRealm();
    guestRealm.setAttributes(
        Arrays.asList("claim1=value1", "claim2=value2|value3", "bad", ":=invalid"));
  }

  @Test
  public void testSupportsNull() {
    boolean supports = guestRealm.supports(null);

    assertFalse(supports);
  }

  @Test
  public void testSupportsBaseGuestAllowed() {
    BaseAuthenticationToken baseAuthenticationToken =
        new MockBaseAuthenticationToken("principal", "credentials", "0.0.0.0");
    baseAuthenticationToken.setAllowGuest(true);

    boolean supports = guestRealm.supports(baseAuthenticationToken);

    assertTrue(supports);
  }

  @Test
  public void testSupportsNotBase() {
    AuthenticationToken authenticationToken =
        new AuthenticationToken() {
          @Override
          public Object getPrincipal() {
            return "principal";
          }

          @Override
          public Object getCredentials() {
            return "credentials";
          }
        };
    boolean supports = guestRealm.supports(authenticationToken);

    assertFalse(supports);
  }

  @Test
  public void testSupportsBaseGuestNotAllowed() {
    BaseAuthenticationToken baseAuthenticationToken =
        new MockBaseAuthenticationToken("principal", "credentials", "0.0.0.0");
    baseAuthenticationToken.setAllowGuest(false);

    boolean supports = guestRealm.supports(baseAuthenticationToken);

    assertFalse(supports);
  }

  @Ignore
  @Test
  public void testDoGetAuthenticationInfo() {
    BaseAuthenticationToken baseAuthenticationToken =
        new MockBaseAuthenticationToken("principal", "credentials", "0.0.0.0");
    baseAuthenticationToken.setAllowGuest(true);

    AuthenticationInfo authenticationInfo =
        guestRealm.doGetAuthenticationInfo(baseAuthenticationToken);

    assertEquals(baseAuthenticationToken.getCredentials(), authenticationInfo.getCredentials());

    PrincipalCollection principals = authenticationInfo.getPrincipals();

    assertEquals(2, principals.asList().size());

    Iterator iterator = principals.iterator();

    assertEquals("Guest@0.0.0.0", iterator.next());

    Object next = iterator.next();

    assertTrue(next instanceof SecurityAssertion);

    SecurityAssertion securityAssertion = (SecurityAssertion) next;

    assertEquals(2, securityAssertion.getAttributeStatements().get(0).getAttributes().size());

    boolean claim1 = false;
    boolean claim2 = false;
    boolean claim3 = false;
    boolean claim4 = false;
    for (Attribute attribute : securityAssertion.getAttributeStatements().get(0).getAttributes()) {
      if (attribute.getName().equals("claim1")) {
        claim1 = true;
        assertEquals("value1", attribute.getValues().get(0));
      }
      if (attribute.getName().equals("claim2")) {
        claim2 = true;
        assertTrue(attribute.getValues().stream().anyMatch(v -> v.equals("value2")));
        assertTrue(attribute.getValues().stream().anyMatch(v -> v.equals("value3")));
      }
      if (attribute.getName().equals(":")) {
        claim3 = true;
      }
      if (attribute.getName().equals("bad")) {
        claim4 = true;
      }
    }
    assertTrue(claim1);
    assertTrue(claim2);
    assertFalse(claim3);
    assertFalse(claim4);

    AuthenticationInfo newAuthenticationInfo =
        guestRealm.doGetAuthenticationInfo(baseAuthenticationToken);

    assertNotSame(authenticationInfo, newAuthenticationInfo);
  }

  class MockBaseAuthenticationToken extends BaseAuthenticationToken {
    public MockBaseAuthenticationToken(Object principal, Object credentials, String ip) {
      super(principal, credentials, ip);
    }
  }
}
