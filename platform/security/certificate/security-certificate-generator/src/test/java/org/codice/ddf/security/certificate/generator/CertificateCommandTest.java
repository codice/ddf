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
package org.codice.ddf.security.certificate.generator;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import ddf.security.SecurityConstants;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNamesBuilder;
import org.bouncycastle.cert.X509CertificateHolder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CertificateCommandTest {
  private static final String[] SANS = new String[] {"IP:1.2.3.4", "DNS:A"};

  private static final String SANS_ARG = "IP:1.2.3.4,DNS:A";

  private static final String FQDN = "my-fqdn";

  private static final String CN_FROM_DN = "John Whorfin";

  private static final String IP_FROM_SAN = "1.2.3.4";

  private static final String DNS_FROM_SAN = "A";

  private static final String[] CERTIFICATE_DN =
      new String[] {"cn=John Whorfin", "o=Yoyodyne", "l=San Narciso", "st=California", "c=US"};

  private static final String CERTIFICATE_CN =
      "CN=John Whorfin,O=Yoyodyne,L=San Narciso,ST=California,C=US";

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  /**
   * Expected SAN general name is now a function of the CN, since a SAN representing the CN is now
   * required.
   */
  private static byte[] expectedSanGeneralName(String alias, boolean withAdditionalSans) {
    try {
      GeneralNamesBuilder builder =
          new GeneralNamesBuilder().addName(new GeneralName(GeneralName.dNSName, alias));
      if (withAdditionalSans) {
        builder
            .addName(new GeneralName(GeneralName.iPAddress, IP_FROM_SAN))
            .addName(new GeneralName(GeneralName.dNSName, DNS_FROM_SAN));
      }
      return builder.build().getEncoded(ASN1Encoding.DER);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static void validateSans(KeyStoreFile ksf, String alias, boolean withAdditionalSans)
      throws Exception {
    final KeyStore.Entry ke = ksf.getEntry(alias);
    assertThat(ke, instanceOf(KeyStore.PrivateKeyEntry.class));

    final KeyStore.PrivateKeyEntry pke = (KeyStore.PrivateKeyEntry) ke;
    final Certificate c = pke.getCertificate();
    final X509CertificateHolder holder = new X509CertificateHolder(c.getEncoded());
    final Extension csn = holder.getExtension(Extension.subjectAlternativeName);

    assertThat(
        csn.getParsedValue().toASN1Primitive().getEncoded(ASN1Encoding.DER),
        equalTo(expectedSanGeneralName(alias, withAdditionalSans)));
  }

  private static void validateKeyStore(String alias, boolean withAdditionalSans) throws Exception {
    final KeyStoreFile ksf = CertificateCommand.getKeyStoreFile();
    assertThat(ksf.aliases().size(), is(2));
    validateSans(ksf, alias, withAdditionalSans);
  }

  @Before
  public void setup() throws IOException {
    final File systemKeystoreFile = temporaryFolder.newFile("serverKeystore.jks");
    final FileOutputStream systemKeyOutStream = new FileOutputStream(systemKeystoreFile);
    final InputStream systemKeyStream =
        CertificateGenerator.class.getResourceAsStream("/serverKeystore.jks");

    IOUtils.copy(systemKeyStream, systemKeyOutStream);
    IOUtils.closeQuietly(systemKeyOutStream);
    IOUtils.closeQuietly(systemKeyStream);

    System.setProperty(SecurityConstants.KEYSTORE_TYPE, "jks");
    System.setProperty(SecurityConstants.KEYSTORE_PATH, systemKeystoreFile.getAbsolutePath());
    System.setProperty(SecurityConstants.KEYSTORE_PASSWORD, "changeit");
  }

  @Test
  public void testConfigureDemoCertWithoutSan() throws Exception {
    final KeyStoreFile ksf = CertificateCommand.getKeyStoreFile();

    assertThat(ksf.aliases().size(), is(2));
    assertThat(ksf.isKey(FQDN), is(false));

    assertThat(CertificateCommand.configureDemoCert(FQDN, null), equalTo("CN=" + FQDN));

    validateKeyStore(FQDN, false);
  }

  @Test
  public void testConfigureDemoCertWithSans() throws Exception {
    final KeyStoreFile ksf = CertificateCommand.getKeyStoreFile();

    assertThat(ksf.aliases().size(), is(2));
    assertThat(ksf.isKey(FQDN), is(false));

    assertThat(
        CertificateCommand.configureDemoCert(FQDN, CertificateCommandTest.SANS),
        equalTo("CN=" + FQDN));

    validateKeyStore(FQDN, true);
  }

  @Test
  public void testConfigureDemoCertWithDnAndWithoutSan() throws Exception {
    final KeyStoreFile ksf = CertificateCommand.getKeyStoreFile();

    assertThat(ksf.aliases().size(), is(2));
    assertThat(ksf.isKey(CertificateCommandTest.CERTIFICATE_CN), is(false));

    assertThat(
        CertificateCommand.configureDemoCertWithDN(CertificateCommandTest.CERTIFICATE_DN, null),
        equalTo(CertificateCommandTest.CERTIFICATE_CN));

    validateKeyStore(CN_FROM_DN, false);
  }

  @Test
  public void testConfigureDemoCertWithDnAndSans() throws Exception {
    final KeyStoreFile ksf = CertificateCommand.getKeyStoreFile();

    assertThat(ksf.aliases().size(), is(2));
    assertThat(ksf.isKey(CertificateCommandTest.CERTIFICATE_CN), is(false));

    assertThat(
        CertificateCommand.configureDemoCertWithDN(
            CertificateCommandTest.CERTIFICATE_DN, CertificateCommandTest.SANS),
        equalTo(CertificateCommandTest.CERTIFICATE_CN));

    validateKeyStore(CN_FROM_DN, true);
  }

  @Test
  public void testMainWithCNAndWithoutSan() throws Exception {
    CertificateCommand.main(new String[] {"-cn", FQDN});

    validateKeyStore(FQDN, false);
  }

  @Test
  public void testMainWithCNAndSans() throws Exception {
    CertificateCommand.main(new String[] {"-cn", FQDN, "-san", CertificateCommandTest.SANS_ARG});

    validateKeyStore(FQDN, true);
  }

  @Test
  public void testMainWithCNAndSansReversedArguments() throws Exception {
    CertificateCommand.main(new String[] {"-san", CertificateCommandTest.SANS_ARG, "-cn", FQDN});

    validateKeyStore(FQDN, true);
  }

  @Test
  public void testMainWithDnAndWithoutSan() throws Exception {
    CertificateCommand.main(new String[] {"-dn", CertificateCommandTest.CERTIFICATE_CN});

    validateKeyStore(CN_FROM_DN, false);
  }

  @Test
  public void testMainWithDnAndSans() throws Exception {
    CertificateCommand.main(
        new String[] {
          "-dn", CertificateCommandTest.CERTIFICATE_CN, "-san", CertificateCommandTest.SANS_ARG
        });

    validateKeyStore(CN_FROM_DN, true);
  }

  @Test
  public void testMainWithDnAndSansReversedArguments() throws Exception {
    CertificateCommand.main(
        new String[] {
          "-san", CertificateCommandTest.SANS_ARG, "-dn", CertificateCommandTest.CERTIFICATE_CN
        });

    validateKeyStore(CN_FROM_DN, true);
  }

  @Test(expected = RuntimeException.class)
  public void testMainWithTooFewArguments() throws Exception {
    CertificateCommand.main(new String[] {"something"});
  }

  @Test(expected = RuntimeException.class)
  public void testMainWithTooFewArgumentsWithSan() throws Exception {
    CertificateCommand.main(new String[] {"-san", CertificateCommandTest.SANS_ARG, "something"});
  }

  @Test(expected = RuntimeException.class)
  public void testMainWithTooManyArguments() throws Exception {
    CertificateCommand.main(new String[] {"something", "wicked", "is", "coming", "to", "town"});
  }
}
