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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.LoginService;
import org.junit.Before;
import org.junit.Test;

public class SecurityAuthServiceTest {

  private SecurityAuthService service;

  @Before
  public void setUp() {
    service = new SecurityAuthService();
  }

  @Test
  public void returnsJettyAuthenticatorForMatchingMethodAndInterface() {
    Authenticator result =
        service.getAuthenticatorService(JettyAuthenticator.DDF_AUTH_METHOD, Authenticator.class);
    assertThat(result, instanceOf(JettyAuthenticator.class));
  }

  @Test
  public void returnsNullForNonMatchingMethod() {
    Authenticator result = service.getAuthenticatorService("BASIC", Authenticator.class);
    assertThat(result, nullValue());
  }

  @Test
  public void returnsNullForNonAuthenticatorInterface() {
    Object result =
        service.getAuthenticatorService(JettyAuthenticator.DDF_AUTH_METHOD, LoginService.class);
    assertThat(result, nullValue());
  }
}
