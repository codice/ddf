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
package org.codice.ddf.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.common.audit.SecurityLogger;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.security.PrivilegedAction;
import java.util.concurrent.Callable;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.UnavailableSecurityManagerException;
import org.apache.shiro.subject.ExecutionException;
import org.apache.shiro.subject.PrincipalCollection;
import org.codice.ddf.security.common.Security;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

@PrepareForTest({FrameworkUtil.class, SecurityUtils.class, SecurityLogger.class})
public class SecurityTest {

  @Rule public PowerMockRule rule = new PowerMockRule();

  private Security security;

  @Mock private org.apache.shiro.subject.Subject shiroSubject;

  @Mock private Subject systemSubject;

  @Mock private Callable<String> callable;

  @Before
  public void setup() throws URISyntaxException {
    System.setProperty("karaf.local.roles", "admin,local");

    initMocks(this);
    mockStatic(SecurityUtils.class);
    mockStatic(FrameworkUtil.class);
    mockStatic(SecurityLogger.class);

    when(shiroSubject.execute(callable)).thenReturn("Success!");

    System.setProperty("javax.net.ssl.keyStoreType", "JKS");
    System.setProperty(
        "javax.net.ssl.keyStore", getClass().getResource("/secureKeystore.jks").toURI().getPath());
    System.setProperty("javax.net.ssl.keyStorePassword", "password");
    System.setProperty("ddf.home", "/ddf/home");
    System.setProperty("org.codice.ddf.system.hostname", "localhost");

    System.setProperty(SecurityConstants.TRUSTSTORE_TYPE, "JKS");
    System.setProperty(
        SecurityConstants.TRUSTSTORE_PATH,
        getClass().getResource("/serverTruststore.jks").toURI().getPath());
    System.setProperty(SecurityConstants.TRUSTSTORE_PASSWORD, "changeit");
    security = Security.getInstance();
  }

  @Test
  public void testGetSubjectNoSecurityManager() throws Exception {
    configureMockForSecurityManager(null);
    Subject subject = security.getSubject("username", "password");
    assertThat(subject, is(equalTo(null)));
  }

  @Test
  public void testGetSubjectInvalidUsernamePassword() throws Exception {
    SecurityManager sm = mock(SecurityManager.class);
    when(sm.getSubject(any())).thenThrow(new SecurityServiceException("Error"));

    configureMockForSecurityManager(sm);

    Subject subject = security.getSubject("username", "password");
    assertThat(subject, is(equalTo(null)));
  }

  @Test
  public void testGetSubject() throws Exception {
    SecurityManager sm = mock(SecurityManager.class);
    Subject smSubject = mock(Subject.class);
    when(sm.getSubject(any())).thenReturn(smSubject);

    configureMockForSecurityManager(sm);

    Subject subject = security.getSubject("username", "password");
    assertThat(subject, not(equalTo(null)));
  }

  @Test
  public void testTokenAboutToExpire() throws Exception {
    Subject subject = mock(Subject.class);
    SecurityAssertion assertion = mock(SecurityAssertion.class);
    PrincipalCollection pc = mock(PrincipalCollection.class);
    SecurityToken st = mock(SecurityToken.class);
    when(st.isAboutToExpire(anyLong())).thenReturn(true);

    assertThat(security.tokenAboutToExpire(null), equalTo(true));
    assertThat(security.tokenAboutToExpire(subject), equalTo(true));
    when(subject.getPrincipals()).thenReturn(pc);
    assertThat(security.tokenAboutToExpire(subject), equalTo(true));
    when(pc.oneByType(any(Class.class))).thenReturn(assertion);
    when(assertion.getSecurityToken()).thenReturn(st);
    assertThat(security.tokenAboutToExpire(subject), equalTo(true));
    when(st.isAboutToExpire(anyLong())).thenReturn(false);
    assertThat(security.tokenAboutToExpire(subject), equalTo(false));
  }

  @Test
  public void testJavaSubjectDoesNotHaveAdminRole() throws Exception {
    javax.security.auth.Subject subject = new javax.security.auth.Subject();
    javax.security.auth.Subject.doAs(
        subject,
        (PrivilegedAction<Object>)
            () -> {
              assertThat(security.javaSubjectHasAdminRole(), equalTo(false));
              return null;
            });
  }

  @Test
  public void testJavaSubjectHasAdminRole() throws Exception {
    security.runAsAdmin(
        () -> {
          assertThat(security.javaSubjectHasAdminRole(), equalTo(true));
          return null;
        });
  }

  @Test
  public void testGetSystemSubject() throws Exception {
    configureMocksForBundleContext("server");

    security.runAsAdmin(
        () -> {
          assertThat(security.getSystemSubject(), not(equalTo(null)));
          return null;
        });
    verifyStatic();
    SecurityLogger.audit("Attempting to get System Subject");
  }

  @Test
  public void testGetSystemSubjectBadAlias() throws Exception {
    configureMocksForBundleContext("bad-alias");

    assertThat(security.getSystemSubject(), equalTo(null));
  }

  @Test
  public void testRunWithSubjectOrElevateWhenUserSubjectExists() throws Exception {
    when(SecurityUtils.getSubject()).thenReturn(shiroSubject);

    String result = security.runWithSubjectOrElevate(callable);

    assertThat(result, is("Success!"));
    verify(shiroSubject).execute(callable);
  }

  @Test
  public void testRunWithSubjectOrElevateWhenSystemSubjectHasAdminRole() throws Exception {
    when(SecurityUtils.getSubject()).thenThrow(new UnavailableSecurityManagerException(""));
    when(systemSubject.execute(callable)).thenReturn("Success!");
    configureMocksForBundleContext("server");

    String result =
        security.runAsAdminWithException(() -> security.runWithSubjectOrElevate(callable));

    assertThat(result, is("Success!"));
    verifyStatic();
    SecurityLogger.auditWarn("Elevating current user permissions to use System subject");
    verifyZeroInteractions(shiroSubject);
  }

  @Test(expected = SecurityServiceException.class)
  public void testRunWithSubjectOrElevateWhenSystemSubjectDoesNotHaveAdminRole() throws Exception {
    when(SecurityUtils.getSubject()).thenThrow(new IllegalStateException());
    when(systemSubject.execute(callable)).thenReturn("Success!");
    configureMocksForBundleContext("server");

    try {
      security.runWithSubjectOrElevate(callable);
    } catch (SecurityServiceException e) {
      verifyStatic();
      SecurityLogger.audit("Current user doesn't have sufficient privileges to run this command");
      throw e;
    }
  }

  @Test
  public void testRunWithSubjectOrElevateWhenSystemSubjectIsNotAvailable() throws Exception {
    when(SecurityUtils.getSubject()).thenThrow(new IllegalStateException());
    configureMocksForBundleContext("bad-alias");

    boolean securityExceptionThrown =
        security.runAsAdmin(
            () -> {
              try {
                return security.runWithSubjectOrElevate(() -> false);
              } catch (SecurityServiceException e) {
                return true;
              } catch (Exception e) {
                return false;
              }
            });

    assertThat(securityExceptionThrown, is(true));
    verifyStatic();
    SecurityLogger.audit("Current user doesn't have sufficient privileges to run this command");
    verifyZeroInteractions(shiroSubject);
  }

  @Test
  public void testRunWithSubjectOrElevateWhenUserSubjectExistsAndCallableThrowsException()
      throws Exception {
    when(SecurityUtils.getSubject()).thenReturn(shiroSubject);
    when(shiroSubject.execute(callable))
        .thenThrow(new ExecutionException(new UnsupportedOperationException()));

    try {
      security.runWithSubjectOrElevate(callable);
      fail("InvocationTargetException expected");
    } catch (SecurityServiceException e) {
      throw e;
    } catch (InvocationTargetException e) {
      verifyStatic();
      SecurityLogger.auditWarn("Failed to execute code as System subject", e.getCause());
      assertThat(e.getCause(), is(instanceOf(UnsupportedOperationException.class)));
    }
  }

  @Test
  public void testRunWithSubjectOrElevateWhenSystemSubjectIsUsedAndCallableThrowsException()
      throws Exception {
    when(SecurityUtils.getSubject()).thenThrow(new IllegalStateException());
    when(systemSubject.execute(callable))
        .thenThrow(new ExecutionException(new UnsupportedOperationException()));
    configureMocksForBundleContext("server");

    Exception exception =
        security.runAsAdmin(
            () -> {
              try {
                security.runWithSubjectOrElevate(callable);
                fail("InvocationTargetException expected");
                return null;
              } catch (Exception e) {
                return e;
              }
            });

    assertThat(exception, is(instanceOf(InvocationTargetException.class)));
    assertThat(exception.getCause(), is(instanceOf(UnsupportedOperationException.class)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRunWithSubjectOrElevateWhenCallableIsNull() throws Exception {
    security.runWithSubjectOrElevate(null);
  }

  private void configureMockForSecurityManager(SecurityManager sm) {
    mockStatic(FrameworkUtil.class);
    Bundle bundle = mock(Bundle.class);
    when(FrameworkUtil.getBundle(any(Class.class))).thenReturn(bundle);
    BundleContext bc = mock(BundleContext.class);
    when(bundle.getBundleContext()).thenReturn(bc);
    ServiceReference ref = mock(ServiceReference.class);
    when(bc.getServiceReference(any(Class.class))).thenReturn(ref);
    when(bc.getService(ref)).thenReturn(sm);
  }

  private void configureMocksForBundleContext(String systemHostname) throws Exception {
    System.setProperty("org.codice.ddf.system.hostname", systemHostname);
    Bundle bundle = mock(Bundle.class);
    when(FrameworkUtil.getBundle(any(Class.class))).thenReturn(bundle);
    BundleContext bundleContext = mock(BundleContext.class);
    when(bundle.getBundleContext()).thenReturn(bundleContext);
    ServiceReference adminRef = mock(ServiceReference.class);
    ConfigurationAdmin configAdmin = mock(ConfigurationAdmin.class);
    Configuration config = mock(Configuration.class);
    when(configAdmin.getConfiguration(anyString(), anyString())).thenReturn(config);
    when(bundleContext.getService(adminRef)).thenReturn(configAdmin);
    ServiceReference securityRef = mock(ServiceReference.class);
    SecurityManager securityManager = mock(SecurityManager.class);
    when(securityManager.getSubject(any())).thenReturn(systemSubject);
    when(bundleContext.getService(securityRef)).thenReturn(securityManager);
    when(bundleContext.getServiceReference(ConfigurationAdmin.class)).thenReturn(adminRef);
    when(bundleContext.getServiceReference(SecurityManager.class)).thenReturn(securityRef);
  }
}
