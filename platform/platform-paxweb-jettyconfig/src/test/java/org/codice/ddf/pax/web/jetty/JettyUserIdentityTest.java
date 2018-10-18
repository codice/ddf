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
package org.codice.ddf.pax.web.jetty;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;
import javax.security.auth.Subject;
import org.junit.Test;

public class JettyUserIdentityTest {

  @Test
  public void getEmptyUserPrincipalTest() {
    Set<Principal> principalSet = new HashSet<>();
    HashSet emptySet = new HashSet();
    Subject subject = new Subject(true, principalSet, emptySet, emptySet);
    JettyUserIdentity jettyUserIdentity = new JettyUserIdentity(subject);
    assertThat(jettyUserIdentity.getUserPrincipal(), is(nullValue()));
  }

  @Test
  public void getUserPrincipalTest() {
    Set<Principal> principalSet = new HashSet<>();
    Principal principal = () -> "";
    principalSet.add(principal);
    HashSet emptySet = new HashSet();
    Subject subject = new Subject(true, principalSet, emptySet, emptySet);
    JettyUserIdentity jettyUserIdentity = new JettyUserIdentity(subject);
    assertThat(jettyUserIdentity.getUserPrincipal(), is(principal));
  }
}
