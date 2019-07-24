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
package ddf.catalog.ftp;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.security.Subject;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.apache.ftpserver.ftplet.Authentication;
import org.apache.ftpserver.ftplet.AuthenticationFailedException;
import org.apache.ftpserver.usermanager.AnonymousAuthentication;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;
import org.apache.ftpserver.usermanager.impl.UserMetadata;
import org.codice.ddf.security.policy.context.ContextPolicyManager;
import org.junit.Before;
import org.junit.Test;

public class UserManagerImplTest {

  private static final String USER = "user";

  private static final String PASSWORD = "password";

  private UserManagerImpl userManager;

  private SecurityManager securityManager;

  private ContextPolicyManager contextPolicyManager;

  @Before
  public void setUp() {
    securityManager = mock(SecurityManager.class);
    contextPolicyManager = mock(ContextPolicyManager.class);
    when(contextPolicyManager.getGuestAccess()).thenReturn(true);
    userManager = new UserManagerImpl(securityManager, contextPolicyManager);
  }

  @Test(expected = AuthenticationFailedException.class)
  public void wrongAuthType() throws AuthenticationFailedException {
    AnonymousAuthentication anonAuthentication = mock(AnonymousAuthentication.class);

    userManager.authenticate(anonAuthentication);
  }

  @Test(expected = AuthenticationFailedException.class)
  public void nullShiroSubject()
      throws SecurityServiceException, AuthenticationFailedException, UnknownHostException {
    UsernamePasswordAuthentication upa = mock(UsernamePasswordAuthentication.class);

    when(upa.getUsername()).thenReturn(USER);
    when(upa.getPassword()).thenReturn(PASSWORD);
    when(securityManager.getSubject(upa)).thenReturn(null);
    UserMetadata userMetadata = new UserMetadata();
    userMetadata.setInetAddress(InetAddress.getLocalHost());
    when(upa.getUserMetadata()).thenReturn(userMetadata);

    userManager.authenticate(upa);
  }

  @Test(expected = AuthenticationFailedException.class)
  public void shiroUnsupportedAuthentication()
      throws SecurityServiceException, AuthenticationFailedException, UnknownHostException {
    UsernamePasswordAuthentication upa = mock(UsernamePasswordAuthentication.class);

    when(upa.getUsername()).thenReturn(USER);
    when(upa.getPassword()).thenReturn(PASSWORD);
    when(securityManager.getSubject(any(Authentication.class)))
        .thenThrow(SecurityServiceException.class);
    UserMetadata userMetadata = new UserMetadata();
    userMetadata.setInetAddress(InetAddress.getLocalHost());
    when(upa.getUserMetadata()).thenReturn(userMetadata);

    userManager.authenticate(upa);
  }

  @Test
  public void authenticationSuccess()
      throws SecurityServiceException, AuthenticationFailedException, UnknownHostException {
    UsernamePasswordAuthentication upa = mock(UsernamePasswordAuthentication.class);
    Subject subject = mock(Subject.class);

    when(upa.getUsername()).thenReturn(USER);
    when(upa.getPassword()).thenReturn(PASSWORD);
    when(securityManager.getSubject(any(Authentication.class))).thenReturn(subject);
    userManager.setKarafLocalRoles("admin,localhost");
    UserMetadata userMetadata = new UserMetadata();
    userMetadata.setInetAddress(InetAddress.getLocalHost());
    when(upa.getUserMetadata()).thenReturn(userMetadata);

    assertEquals(userManager.createUser(USER, subject), userManager.authenticate(upa));
  }
}
