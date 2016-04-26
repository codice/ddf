/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.security;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.function.Predicate;

import javax.security.auth.x500.X500Principal;

import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

/**
 * Tests out the SubjectUtils class
 */
public class SubjectUtilsTest {

    private static final String TEST_NAME = "test123";

    private static final String DEFAULT_NAME = "default";

    private Principal principal;

    private X500Principal dnPrincipal;

    @Before
    public void setup()
            throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        try (InputStream systemKeyStream = SubjectUtilsTest.class.getResourceAsStream(
                "/serverKeystore.jks")) {
            KeyStore keyStore = KeyStore.getInstance("jks");
            keyStore.load(systemKeyStream, "changeit".toCharArray());
            X509Certificate localhost = (X509Certificate) keyStore.getCertificate("localhost");
            principal = localhost.getSubjectDN();
        }

        dnPrincipal = new X500Principal("CN=Foo,OU=Engineering,OU=Dev,O=DDF,ST=AZ,C=US");
    }

    @Test
    public void testGetName() {
        org.apache.shiro.subject.Subject subject;
        org.apache.shiro.mgt.SecurityManager secManager = new DefaultSecurityManager();
        PrincipalCollection principals = new SimplePrincipalCollection(TEST_NAME, "testrealm");
        subject = new Subject.Builder(secManager).principals(principals)
                .session(new SimpleSession())
                .authenticated(true)
                .buildSubject();
        assertEquals(TEST_NAME, SubjectUtils.getName(subject));
    }

    @Test
    public void testGetDefaultName() {
        org.apache.shiro.subject.Subject subject;
        org.apache.shiro.mgt.SecurityManager secManager = new DefaultSecurityManager();
        PrincipalCollection principals = new SimplePrincipalCollection();
        subject = new Subject.Builder(secManager).principals(principals)
                .session(new SimpleSession())
                .authenticated(true)
                .buildSubject();
        assertEquals(DEFAULT_NAME, SubjectUtils.getName(subject, DEFAULT_NAME));
        assertEquals(DEFAULT_NAME, SubjectUtils.getName(null, DEFAULT_NAME));
    }

    @Test
    public void testGetCommonName() {
        assertThat(SubjectUtils.getCommonName(new X500Principal(principal.getName())),
                is("localhost"));
    }

    @Test
    public void testFilterDNKeepOne() {
        Predicate<RDN> predicate = rdn -> rdn.getTypesAndValues()[0].getType()
                .equals(BCStyle.CN);
        String baseDN = SubjectUtils.filterDN(dnPrincipal, predicate);
        assertThat(baseDN, is("CN=Foo"));
    }

    @Test
    public void testFilterDNDropOne() {
        Predicate<RDN> predicate = rdn -> !rdn.getTypesAndValues()[0].getType()
                .equals(BCStyle.CN);
        String baseDN = SubjectUtils.filterDN(dnPrincipal, predicate);
        assertThat(baseDN, is("OU=Engineering,OU=Dev,O=DDF,ST=AZ,C=US"));
    }

    @Test
    public void testFilterDNDropTwo() {
        Predicate<RDN> predicate = rdn -> !ImmutableSet.of(BCStyle.C, BCStyle.ST)
                .contains(rdn.getTypesAndValues()[0].getType());
        String baseDN = SubjectUtils.filterDN(dnPrincipal, predicate);
        assertThat(baseDN, is("CN=Foo,OU=Engineering,OU=Dev,O=DDF"));
    }

    @Test
    public void testFilterDNDropMultivalue() {
        Predicate<RDN> predicate = rdn -> !rdn.getTypesAndValues()[0].getType()
                .equals(BCStyle.OU);
        String baseDN = SubjectUtils.filterDN(dnPrincipal, predicate);
        assertThat(baseDN, is("CN=Foo,O=DDF,ST=AZ,C=US"));
    }

    @Test
    public void testFilterDNRemoveAll() {
        Predicate<RDN> predicate = rdn -> !ImmutableSet.of(BCStyle.OU, BCStyle.CN, BCStyle.O,
                BCStyle.ST, BCStyle.C)
                .contains(rdn.getTypesAndValues()[0].getType());
        String baseDN = SubjectUtils.filterDN(dnPrincipal, predicate);
        assertThat(baseDN, is(""));
    }
}
