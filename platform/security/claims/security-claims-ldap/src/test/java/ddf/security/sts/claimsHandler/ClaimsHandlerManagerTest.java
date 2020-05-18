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
package ddf.security.sts.claimsHandler;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.security.audit.SecurityLogger;
import ddf.security.claims.ClaimsHandler;
import ddf.security.encryption.EncryptionService;
import java.util.Dictionary;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/** Tests out the ClaimsHandlerManager. */
public class ClaimsHandlerManagerTest {

  private BundleContext context;

  private EncryptionService encryptService;

  private ServiceRegistration<ClaimsHandler> handlerReg;

  /** Create a new BundleContext, EncryptionService, and ServiceRegistration before each test. */
  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {
    context = mock(BundleContext.class);
    encryptService = mock(EncryptionService.class);
    handlerReg = mock(ServiceRegistration.class);
    when(context.registerService(
            eq(ClaimsHandler.class),
            any(ClaimsHandler.class),
            Matchers.<Dictionary<String, Object>>any()))
        .thenReturn(handlerReg);
  }

  /** Test registration of the role and ldap claims handler. */
  @Test
  public void registerHandlers() {

    ClaimsHandlerManager manager =
        new ClaimsHandlerManager(encryptService) {
          @Override
          protected BundleContext getContext() {
            return context;
          }
        };

    System.setProperty(
        "https.cipherSuites",
        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA,TLS_DHE_RSA_WITH_AES_128_CBC_SHA,TLS_DHE_DSS_WITH_AES_128_CBC_SHA,TLS_RSA_WITH_AES_128_CBC_SHA");
    System.setProperty("https.protocols", "TLSv1.1,TLSv1.2");

    manager.setSecurityLogger(mock(SecurityLogger.class));
    manager.setLdapBindUserDn("cn=admin");
    manager.setUserBaseDn("ou=users,dc=example,dc=com");
    manager.setGroupBaseDn("ou=groups,dc=example,dc=com");
    manager.setLoginUserAttribute("uid");
    manager.setMembershipUserAttribute("uid");
    manager.setUrl("ldap://ldap:1389");
    manager.setStartTls(false);
    manager.setLdapBindUserDn("cn=admin");
    manager.setObjectClass("ou=users,dc=example,dc=com");
    manager.setMemberNameAttribute("member");
    manager.setPassword("secret");
    manager.setPropertyFileLocation("etc/ws-security/attributeMap.properties");
    manager.setOverrideCertDn(false);
    manager.configure();

    // verify initial registration
    verify(context, times(2))
        .registerService(
            eq(ClaimsHandler.class),
            any(ClaimsHandler.class),
            Matchers.<Dictionary<String, Object>>any());
    verify(handlerReg, never()).unregister();
  }
}
