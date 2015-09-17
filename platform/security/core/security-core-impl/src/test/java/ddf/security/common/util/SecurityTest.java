/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.security.common.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.shiro.subject.PrincipalCollection;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;

@PrepareForTest(FrameworkUtil.class)
public class SecurityTest {

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Test
    public void testGetSubjectNoSecurityManager() throws Exception {
        configureMockForSecurityManager(null);
        Subject subject = Security.getSubject("username", "password");
        assertThat(subject, is(equalTo(null)));
    }

    @Test
    public void testGetSubjectInvalidUsernamePassword() throws Exception {
        SecurityManager sm = mock(SecurityManager.class);
        when(sm.getSubject(any())).thenThrow(new SecurityServiceException("Error"));

        configureMockForSecurityManager(sm);

        Subject subject = Security.getSubject("username", "password");
        assertThat(subject, is(equalTo(null)));
    }

    @Test
    public void testGetSubject() throws Exception {
        SecurityManager sm = mock(SecurityManager.class);
        Subject smSubject = mock(Subject.class);
        when(sm.getSubject(any())).thenReturn(smSubject);

        configureMockForSecurityManager(sm);

        Subject subject = Security.getSubject("username", "password");
        assertThat(subject, not(equalTo(null)));
    }

    @Test
    public void testTokenAboutToExpire() throws Exception {
        Subject subject = mock(Subject.class);
        SecurityAssertion assertion = mock(SecurityAssertion.class);
        PrincipalCollection pc = mock(PrincipalCollection.class);
        SecurityToken st = mock(SecurityToken.class);
        when(st.isAboutToExpire(anyLong())).thenReturn(true);

        assertThat(Security.tokenAboutToExpire(null), equalTo(true));
        assertThat(Security.tokenAboutToExpire(subject), equalTo(true));
        when(subject.getPrincipals()).thenReturn(pc);
        assertThat(Security.tokenAboutToExpire(subject), equalTo(true));
        when(pc.oneByType(any(Class.class))).thenReturn(assertion);
        when(assertion.getSecurityToken()).thenReturn(st);
        assertThat(Security.tokenAboutToExpire(subject), equalTo(true));
        when(st.isAboutToExpire(anyLong())).thenReturn(false);
        assertThat(Security.tokenAboutToExpire(subject), equalTo(false));
    }

    @Test
    public void testJavaSubjectDoesNotHasAdminRole() throws Exception {
        javax.security.auth.Subject subject = new javax.security.auth.Subject();
        javax.security.auth.Subject.doAs(subject, new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                assertThat(Security.javaSubjectHasAdminRole(), equalTo(false));
                return null;
            }
        });
    }

    @Test
    public void testJavaSubjectHasAdminRole() throws Exception {
        Set<Principal> principals = new HashSet<>();
        principals.add(new RolePrincipal("admin"));
        javax.security.auth.Subject subject = new javax.security.auth.Subject(true, principals,
                new HashSet(), new HashSet());

        javax.security.auth.Subject.doAs(subject, new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                assertThat(Security.javaSubjectHasAdminRole(), equalTo(true));
                return null;
            }
        });
    }

    @Test
    public void testGetSystemSubject() throws Exception {

        setSystemProps();

        Dictionary<String, Object> props = new Hashtable<>();
        props.put("host", "server");

        configurMocksForBundleContext(props);

        assertThat(Security.getSystemSubject(), not(equalTo(null)));
    }

    @Test
    public void testGetSystemSubjectBadAlias() throws Exception {

        setSystemProps();

        Dictionary<String, Object> props = new Hashtable<>();
        props.put("host", "bad-alias");
        configurMocksForBundleContext(props);

        assertThat(Security.getSystemSubject(), equalTo(null));
    }

    private void configureMockForSecurityManager(SecurityManager sm) {
        PowerMockito.mockStatic(FrameworkUtil.class);
        Bundle bundle = mock(Bundle.class);
        when(FrameworkUtil.getBundle(any(Class.class))).thenReturn(bundle);
        BundleContext bc = mock(BundleContext.class);
        when(bundle.getBundleContext()).thenReturn(bc);
        ServiceReference ref = mock(ServiceReference.class);
        when(bc.getServiceReference(any(Class.class))).thenReturn(ref);
        when(bc.getService(ref)).thenReturn(sm);
    }

    private void setSystemProps() throws Exception {
        System.setProperty("javax.net.ssl.keyStoreType", "JKS");
        System.setProperty("javax.net.ssl.keyStore",
                getClass().getResource("/secureKeystore.jks").toURI().getPath());
        System.setProperty("javax.net.ssl.keyStorePassword", "password");
        System.setProperty("ddf.home", "/ddf/home");
    }

    private void configurMocksForBundleContext(Dictionary<String, Object> props) throws Exception {
        Subject subject = mock(Subject.class);
        PowerMockito.mockStatic(FrameworkUtil.class);
        Bundle bundle = mock(Bundle.class);
        when(FrameworkUtil.getBundle(any(Class.class))).thenReturn(bundle);
        BundleContext bc = mock(BundleContext.class);
        when(bundle.getBundleContext()).thenReturn(bc);
        ServiceReference adminRef = mock(ServiceReference.class);
        ConfigurationAdmin configAdmin = mock(ConfigurationAdmin.class);
        Configuration config = mock(Configuration.class);
        when(config.getProperties()).thenReturn(props);
        when(configAdmin.getConfiguration(anyString(), anyString())).thenReturn(config);
        when(bc.getService(adminRef)).thenReturn(configAdmin);
        ServiceReference securityRef = mock(ServiceReference.class);
        SecurityManager securityManager = mock(SecurityManager.class);
        when(securityManager.getSubject(any())).thenReturn(subject);
        when(bc.getService(securityRef)).thenReturn(securityManager);
        when(bc.getServiceReference(ConfigurationAdmin.class)).thenReturn(adminRef);
        when(bc.getServiceReference(SecurityManager.class)).thenReturn(securityRef);
    }
}