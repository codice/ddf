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
package ddf.ldap.ldaplogin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.security.auth.login.LoginException;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.LDAPConnectionFactory;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.responses.BindResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PowerMockRunner.class)
@PrepareForTest(LDAPConnectionFactory.class)
public class SslLdapLoginModuleTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(SslLdapLoginModule.class);

  @Test
  public void testUnsuccessfulConnectionBind1() throws LoginException {
    LDAPConnectionFactory mockedConnectionFactory = PowerMockito.mock(LDAPConnectionFactory.class);
    BindResult mockedBindResult = mock(BindResult.class);
    when(mockedBindResult.isSuccess()).thenReturn(false);
    Connection mockedConnection = mock(Connection.class);
    SslLdapLoginModule testLoginModule = mock(SslLdapLoginModule.class);
    try {
      when(mockedConnectionFactory.getConnection()).thenReturn(mockedConnection);
      when(mockedConnection.bind(anyString(), any(char[].class))).thenReturn(mockedBindResult);
      when(testLoginModule.createLdapConnectionFactory(any(String.class), any(Boolean.class)))
          .thenReturn(mockedConnectionFactory);
    } catch (LdapException e) {
      LOGGER.debug("LDAP exception", e);
    }

    Boolean loginBool = testLoginModule.doLogin();
    assertThat(loginBool, is(false));
  }

  @Test(expected = LoginException.class)
  public void testBadCharacters() throws LoginException {
    SslLdapLoginModule sslLdapLoginModule = new SslLdapLoginModule();
    sslLdapLoginModule.validateUsername("<user>");
  }

  @Test
  public void testGoodCharacters() throws LoginException {
    SslLdapLoginModule sslLdapLoginModule = new SslLdapLoginModule();
    sslLdapLoginModule.validateUsername(
        "abcdefghijklmnopqrstuvwxyz1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ");
  }
}
