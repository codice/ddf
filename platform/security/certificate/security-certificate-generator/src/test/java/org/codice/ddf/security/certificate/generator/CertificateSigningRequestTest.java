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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNamesBuilder;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.Time;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CertificateSigningRequestTest {

  CertificateSigningRequest csr;

  private static KeyPair makeKeyPair() throws Exception {
    final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA", "SUN");
    final SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");

    keyGen.initialize(1024, random);
    return keyGen.generateKeyPair();
  }

  @Before
  public void setUp() {
    csr = new CertificateSigningRequest();
  }

  @Test
  public void testKeys() throws Exception {
    assertThat(
        "CSR failed to auto-generate RSA keypair",
        csr.getSubjectPrivateKey(),
        instanceOf(PrivateKey.class));
    assertThat(
        "CSR failed to auto-generate RSA keypair",
        csr.getSubjectPublicKey(),
        instanceOf(PublicKey.class));
    PublicKey pubKey = mock(PublicKey.class);
    PrivateKey privKey = mock(PrivateKey.class);
    KeyPair kp = new KeyPair(pubKey, privKey);
    csr.setSubjectKeyPair(kp);
    assertThat("Unable to get mock private key", csr.getSubjectPrivateKey(), sameInstance(privKey));
    assertThat("Unable to get mock public key", csr.getSubjectPublicKey(), sameInstance(pubKey));
  }

  @Test
  public void testAddSubjectAlternativeNames() {
    assertThat(
        "CSR should not have any SAN by default",
        csr.getSubjectAlternativeNames(),
        emptyCollectionOf(GeneralName.class));
    csr.addSubjectAlternativeNames("IP:1.2.3.4", "DNS:A");
    assertThat(
        csr.getSubjectAlternativeNames(),
        contains(
            new GeneralName(GeneralName.iPAddress, "1.2.3.4"),
            new GeneralName(GeneralName.dNSName, "A")));
    csr.addSubjectAlternativeNames("RID:0.2.1.4", "DNS:A");
    assertThat(
        csr.getSubjectAlternativeNames(),
        contains(
            new GeneralName(GeneralName.iPAddress, "1.2.3.4"),
            new GeneralName(GeneralName.dNSName, "A"),
            new GeneralName(GeneralName.registeredID, "0.2.1.4")));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddSubjectAlternativeNamesWithNullList() {
    csr.addSubjectAlternativeNames((String[]) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddSubjectAlternativeNamesWithNullEntry() {
    csr.addSubjectAlternativeNames(null, "IP:1.2.3.4");
  }

  @Test
  public void assertDates() {
    boolean outcome = csr.getNotAfter().isAfter(csr.getNotBefore());
    assertThat(
        "'Not after' date should never be chronologically before the 'Not before' date'",
        outcome,
        equalTo(true));
  }

  @Test(expected = IllegalArgumentException.class)
  public void badNotBeforeDate() {
    csr.setNotBefore(csr.getNotAfter().plusDays(1));
  }

  @Test(expected = IllegalArgumentException.class)
  public void badNotAfterDate() {
    csr.setNotAfter(csr.getNotBefore().minusDays(1));
  }

  @Test
  public void setValidDates() throws Exception {
    // Test default dates
    assertDates();

    // Try new dates
    DateTime effectiveDate = DateTime.now();
    csr.setNotBefore(effectiveDate);
    assertDates();
    csr.setNotAfter(effectiveDate.plusDays(1));
    assertDates();
  }

  @Test
  public void testSetSerialNumber() throws Exception {
    csr.setSerialNumber(1);
    assertThat("The serial number should be one", 1L, equalTo(csr.getSerialNumber()));
  }

  @Test
  public void subjectName() throws Exception {

    assertThat("Subject name should never be null", true, equalTo(csr.getSubjectName() != null));
    csr.setCommonName("test");
    String cn = csr.getSubjectName().toString();
    assertThat("Subject name should be 'test'", cn, endsWith("test"));
  }

  @Test
  public void subjectFromDN() throws Exception {
    assertThat("Subject name should never be null", true, equalTo(csr.getSubjectName() != null));
    csr.setDistinguishedName("CN=john.smith", "O=Tardis", "o=police box", "L=London", "C=UK");
    String subjectName = csr.getSubjectName().toString();

    assertThat(
        "Subject name should contain 'cn=john.smith'",
        subjectName,
        containsString("cn=john.smith"));
    assertThat("Subject name should contain 'o=Tardis'", subjectName, containsString("o=Tardis"));
    assertThat(
        "Subject name should contain 'o=police box'", subjectName, containsString("o=police box"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void badSerialNumber() {
    csr.setSerialNumber(-1);
  }

  @Test
  public void testNewCertificateBuilderWithoutSan() throws Exception {
    final DateTime start = DateTime.now().minusDays(1);
    final DateTime end = start.plusYears(100);
    final KeyPair kp = makeKeyPair();

    csr.setSerialNumber(1);
    csr.setNotBefore(start);
    csr.setNotAfter(end);
    csr.setCommonName("A");
    csr.setSubjectKeyPair(kp);
    final X509Certificate issuerCert = mock(X509Certificate.class);

    doReturn(new X500Principal("CN=Duke, OU=JavaSoft, O=Sun Microsystems, C=US"))
        .when(issuerCert)
        .getSubjectX500Principal();
    final JcaX509v3CertificateBuilder builder = csr.newCertificateBuilder(issuerCert);
    final X509CertificateHolder holder =
        builder.build(new DemoCertificateAuthority().getContentSigner());

    assertThat(holder.getSerialNumber(), equalTo(BigInteger.ONE));
    assertThat(holder.getNotBefore(), equalTo(new Time(start.toDate()).getDate()));
    assertThat(holder.getNotAfter(), equalTo(new Time(end.toDate()).getDate()));
    assertThat(holder.getSubject().toString(), equalTo("cn=A"));
    assertThat(
        "Unable to validate public key",
        holder.getSubjectPublicKeyInfo(),
        equalTo(SubjectPublicKeyInfo.getInstance(kp.getPublic().getEncoded())));
    assertThat(
        "There should be no subject alternative name extension",
        holder.getExtension(org.bouncycastle.asn1.x509.Extension.subjectAlternativeName),
        nullValue(org.bouncycastle.asn1.x509.Extension.class));
  }

  @Test
  public void testNewCertificateBuilderWithSan() throws Exception {
    final DateTime start = DateTime.now().minusDays(1);
    final DateTime end = start.plusYears(100);
    final KeyPair kp = makeKeyPair();

    csr.setSerialNumber(1);
    csr.setNotBefore(start);
    csr.setNotAfter(end);
    csr.setCommonName("A");
    csr.setSubjectKeyPair(kp);
    csr.addSubjectAlternativeNames("IP:1.2.3.4", "DNS:A");
    final X509Certificate issuerCert = mock(X509Certificate.class);

    doReturn(new X500Principal("CN=Duke, OU=JavaSoft, O=Sun Microsystems, C=US"))
        .when(issuerCert)
        .getSubjectX500Principal();
    final JcaX509v3CertificateBuilder builder = csr.newCertificateBuilder(issuerCert);
    final X509CertificateHolder holder =
        builder.build(new DemoCertificateAuthority().getContentSigner());

    assertThat(holder.getSerialNumber(), equalTo(BigInteger.ONE));
    assertThat(holder.getNotBefore(), equalTo(new Time(start.toDate()).getDate()));
    assertThat(holder.getNotAfter(), equalTo(new Time(end.toDate()).getDate()));
    assertThat(holder.getSubject().toString(), equalTo("cn=A"));
    assertThat(
        "Unable to validate public key",
        holder.getSubjectPublicKeyInfo(),
        equalTo(SubjectPublicKeyInfo.getInstance(kp.getPublic().getEncoded())));
    final org.bouncycastle.asn1.x509.Extension csn =
        holder.getExtension(org.bouncycastle.asn1.x509.Extension.subjectAlternativeName);

    assertThat(
        csn.getParsedValue().toASN1Primitive().getEncoded(ASN1Encoding.DER),
        equalTo(
            new GeneralNamesBuilder()
                .addName(new GeneralName(GeneralName.iPAddress, "1.2.3.4"))
                .addName(new GeneralName(GeneralName.dNSName, "A"))
                .build()
                .getEncoded(ASN1Encoding.DER)));
  }
}
