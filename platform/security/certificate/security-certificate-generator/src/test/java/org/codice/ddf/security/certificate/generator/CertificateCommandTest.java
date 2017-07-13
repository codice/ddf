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
package org.codice.ddf.security.certificate.generator;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNamesBuilder;
import org.bouncycastle.cert.X509CertificateHolder;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import ddf.security.SecurityConstants;

@RunWith(MockitoJUnitRunner.class)
public class CertificateCommandTest {
    private static final String[] SANs = new String[] {"IP:1.2.3.4", "DNS:A"};

    private static final String SANs_ARG = "IP:1.2.3.4,DNS:A";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File systemKeystoreFile = null;

    private static void validateCertificateHasNoSAN(KeyStoreFile ksf, String alias)
            throws Exception {
        final KeyStore.Entry ke = ksf.getEntry(alias);

        assertThat(ke, instanceOf(KeyStore.PrivateKeyEntry.class));
        final KeyStore.PrivateKeyEntry pke = (KeyStore.PrivateKeyEntry) ke;
        final Certificate c = pke.getCertificate();
        final X509CertificateHolder holder = new X509CertificateHolder(c.getEncoded());

        MatcherAssert.assertThat("There should be no subject alternative name extension",
                holder.getExtension(org.bouncycastle.asn1.x509.Extension.subjectAlternativeName),
                nullValue(org.bouncycastle.asn1.x509.Extension.class));
    }

    private static void validateCertificateHasSANs(KeyStoreFile ksf, String alias)
            throws Exception {
        final KeyStore.Entry ke = ksf.getEntry(alias);

        assertThat(ke, instanceOf(KeyStore.PrivateKeyEntry.class));
        final KeyStore.PrivateKeyEntry pke = (KeyStore.PrivateKeyEntry) ke;
        final Certificate c = pke.getCertificate();
        final X509CertificateHolder holder = new X509CertificateHolder(c.getEncoded());
        final org.bouncycastle.asn1.x509.Extension csn =
                holder.getExtension(org.bouncycastle.asn1.x509.Extension.subjectAlternativeName);

        MatcherAssert.assertThat(csn.getParsedValue()
                        .toASN1Primitive()
                        .getEncoded(ASN1Encoding.DER),
                equalTo(new GeneralNamesBuilder().addName(new GeneralName(GeneralName.iPAddress,
                        "1.2.3.4"))
                        .addName(new GeneralName(GeneralName.dNSName, "A"))
                        .build()
                        .getEncoded(ASN1Encoding.DER)));
    }

    private static void validateKeyStore(String alias, boolean hasSAN) throws Exception {
        final KeyStoreFile ksf = CertificateCommand.getKeyStoreFile();

        assertThat(ksf.aliases()
                .size(), is(2));
        if (hasSAN) {
            validateCertificateHasSANs(ksf, alias);
        } else {
            validateCertificateHasNoSAN(ksf, alias);
        }
    }

    @Before
    public void setup() throws IOException {
        this.systemKeystoreFile = temporaryFolder.newFile("serverKeystore.jks");
        final FileOutputStream systemKeyOutStream = new FileOutputStream(systemKeystoreFile);
        final InputStream systemKeyStream = CertificateGenerator.class.getResourceAsStream(
                "/serverKeystore.jks");

        IOUtils.copy(systemKeyStream, systemKeyOutStream);
        IOUtils.closeQuietly(systemKeyOutStream);
        IOUtils.closeQuietly(systemKeyStream);

        System.setProperty(SecurityConstants.KEYSTORE_TYPE, "jks");
        System.setProperty(SecurityConstants.KEYSTORE_PATH, systemKeystoreFile.getAbsolutePath());
        System.setProperty(SecurityConstants.KEYSTORE_PASSWORD, "changeit");
    }

    @Test
    public void testConfigureDemoCertWithoutSAN() throws Exception {
        final KeyStoreFile ksf = CertificateCommand.getKeyStoreFile();

        assertThat(ksf.aliases()
                .size(), is(2));
        assertThat(ksf.isKey("my-fqdn"), is(false));

        assertThat(CertificateCommand.configureDemoCert("my-fqdn", null), equalTo("CN=my-fqdn"));

        validateKeyStore("my-fqdn", false);
    }

    @Test
    public void testConfigureDemoCertWithSANs() throws Exception {
        final KeyStoreFile ksf = CertificateCommand.getKeyStoreFile();

        assertThat(ksf.aliases()
                .size(), is(2));
        assertThat(ksf.isKey("my-fqdn"), is(false));

        assertThat(CertificateCommand.configureDemoCert("my-fqdn", CertificateCommandTest.SANs),
                equalTo("CN=my-fqdn"));

        validateKeyStore("my-fqdn", true);
    }

    @Test
    public void testConfigureDemoCertWithDNAndWithoutSAN() throws Exception {
        final KeyStoreFile ksf = CertificateCommand.getKeyStoreFile();

        assertThat(ksf.aliases()
                .size(), is(2));
        assertThat(ksf.isKey("CN=John Whorfin,O=Yoyodyne,L=San Narciso,ST=California,C=US"),
                is(false));

        assertThat(CertificateCommand.configureDemoCertWithDN(new String[] {"cn=John Whorfin",
                        "o=Yoyodyne", "l=San Narciso", "st=California", "c=US"}, null),
                equalTo("CN=John Whorfin,O=Yoyodyne,L=San Narciso,ST=California,C=US"));

        validateKeyStore("john whorfin", false);
    }

    @Test
    public void testConfigureDemoCertWithDNAndSANs() throws Exception {
        final KeyStoreFile ksf = CertificateCommand.getKeyStoreFile();

        assertThat(ksf.aliases()
                .size(), is(2));
        assertThat(ksf.isKey("CN=John Whorfin,O=Yoyodyne,L=San Narciso,ST=California,C=US"),
                is(false));

        assertThat(CertificateCommand.configureDemoCertWithDN(new String[] {"cn=John Whorfin",
                        "o=Yoyodyne", "l=San Narciso", "st=California", "c=US"},
                CertificateCommandTest.SANs),
                equalTo("CN=John Whorfin,O=Yoyodyne,L=San Narciso,ST=California,C=US"));

        validateKeyStore("john whorfin", true);
    }

    @Test
    public void testMainWithCNAndWithoutSAN() throws Exception {
        CertificateCommand.main(new String[] {"-cn", "my-fqdn"});

        validateKeyStore("my-fqdn", false);
    }

    @Test
    public void testMainWithCNAndSANs() throws Exception {
        CertificateCommand.main(new String[] {"-cn", "my-fqdn", "-san",
                CertificateCommandTest.SANs_ARG});

        validateKeyStore("my-fqdn", true);
    }

    @Test
    public void testMainWithCNAndSANsReversedArguments() throws Exception {
        CertificateCommand.main(new String[] {"-san", CertificateCommandTest.SANs_ARG, "-cn",
                "my-fqdn"});

        validateKeyStore("my-fqdn", true);
    }

    @Test
    public void testMainWithDNAndWithoutSAN() throws Exception {
        CertificateCommand.main(new String[] {"-dn",
                "CN=John Whorfin,O=Yoyodyne,L=San Narciso,ST=California,C=US"});

        validateKeyStore("john whorfin", false);
    }

    @Test
    public void testMainWithDNAndSANs() throws Exception {
        CertificateCommand.main(new String[] {"-dn",
                "CN=John Whorfin,O=Yoyodyne,L=San Narciso,ST=California,C=US", "-san",
                CertificateCommandTest.SANs_ARG});

        validateKeyStore("john whorfin", true);
    }

    @Test
    public void testMainWithDNAndSANsReversedArguments() throws Exception {
        CertificateCommand.main(new String[] {"-san", CertificateCommandTest.SANs_ARG, "-dn",
                "CN=John Whorfin,O=Yoyodyne,L=San Narciso,ST=California,C=US"});

        validateKeyStore("john whorfin", true);
    }

    @Test(expected = RuntimeException.class)
    public void testMainWithTooFewArguments() throws Exception {
        CertificateCommand.main(new String[] {"something"});
    }

    @Test(expected = RuntimeException.class)
    public void testMainWithTooFewArgumentsWithSAN() throws Exception {
        CertificateCommand.main(new String[] {"-san", CertificateCommandTest.SANs_ARG,
                "something"});
    }

    @Test(expected = RuntimeException.class)
    public void testMainWithTooManyArguments() throws Exception {
        CertificateCommand.main(new String[] {"something", "wicked", "is", "coming", "to", "town"});
    }

}