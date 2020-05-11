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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.GeneralName;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PkiToolsTest {

  @Mock private X509Certificate mockCert;

  @Mock private PrivateKey mockKey;

  @Test(expected = CertificateGeneratorException.class)
  public void testDerToPrivateKey() {
    PkiTools.derToPrivateKey(new byte[] {0});
  }

  @Test(expected = IllegalArgumentException.class)
  public void nameIsNull() throws IllegalArgumentException {
    PkiTools.makeDistinguishedName(null);
  }

  @Test
  public void nameIsEmptyString() throws CertificateEncodingException {

    X500Name name = PkiTools.makeDistinguishedName("");
    assertThat(name.toString(), equalTo("cn="));
  }

  @Test
  public void nameIsNotEmpty() throws CertificateEncodingException {
    String host = "host.domain.tld";
    X500Name name = PkiTools.makeDistinguishedName(host);
    assertThat(name.toString(), equalTo("cn=" + host));
  }

  @Test(expected = IllegalArgumentException.class)
  public void dnIsNull() throws CertificateEncodingException {
    PkiTools.convertDistinguishedName((String[]) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void dnIsEmpty() throws CertificateEncodingException {
    PkiTools.convertDistinguishedName("");
  }

  @Test(expected = IllegalArgumentException.class)
  public void dnIsNotValidFormat() throws CertificateEncodingException {
    PkiTools.convertDistinguishedName("cnIsSomething", "l=london");
  }

  @Test(expected = IllegalArgumentException.class)
  public void dnHasInvalidRDN() throws CertificateEncodingException {
    PkiTools.convertDistinguishedName("cnxxx=IsSomething", "l=london");
  }

  @Test
  public void dnIsValidFormat() throws CertificateEncodingException {
    X500Name name =
        PkiTools.convertDistinguishedName(
            "cn=john.smith", "o=police box", "o = Tardis", "l= London", "c=UK");
    assertThat(name.getRDNs(BCStyle.CN)[0].getFirst().getValue().toString(), equalTo("john.smith"));
    assertThat(name.getRDNs(BCStyle.O).length, equalTo(2));
    assertThat(name.getRDNs(BCStyle.C)[0].getFirst().getValue().toString(), equalTo("UK"));
  }

  @Test
  public void convertCertificate() throws CertificateException {
    String originalCert = DemoCertificateAuthority.pemDemoCaCertificate;
    assertThat(originalCert, not(equalTo("")));
    assertThat(
        PkiTools.certificateToPem(PkiTools.pemToCertificate(originalCert)), equalTo(originalCert));
  }

  @Test
  public void hostName() {
    assertThat(PkiTools.getHostName(), not(equalTo("")));
  }

  @Test(expected = CertificateGeneratorException.class)
  public void exception() {
    throw new CertificateGeneratorException("", new Exception());
  }

  private String getPathTo(String path) {
    return getClass().getClassLoader().getResource(path).getPath();
  }

  @Test
  public void testFormatPassword() throws Exception {
    Assert.assertThat(
        "formatPassword() failed to return empty character array",
        PkiTools.formatPassword(null),
        instanceOf(char[].class));

    char[] pw = "password".toCharArray();
    Assert.assertThat(
        "formatPassword() should not modify the password",
        new String(PkiTools.formatPassword(pw)),
        equalTo("password"));
  }

  // Null path to keyStore file.
  @Test(expected = IllegalArgumentException.class)
  public void nullPath() throws Exception {
    PkiTools.createFileObject(null);
  }

  // Test constructor. Invalid path to keyStore file.
  @Test(expected = FileNotFoundException.class)
  public void invalidPath() throws Exception {
    PkiTools.createFileObject("");
  }

  // Test Constructor. Path is a directory, not a file.
  @Test(expected = FileNotFoundException.class)
  public void pathIsDirectory() throws Exception {
    String anyDirectory = getPathTo("");
    PkiTools.createFileObject("");
  }

  @Test
  public void realFile() throws IOException {
    assertThat(
        "Should have returned a new File object. Is the file in the test resources directory?",
        PkiTools.createFileObject(getPathTo("not_keystore.jks")),
        instanceOf(File.class));
  }

  @Test(expected = CertificateGeneratorException.class)
  public void badKey() {
    PkiTools.pemToPrivateKey("YmFkc3RyaW5n");
  }

  @Test(expected = CertificateGeneratorException.class)
  public void badCert() {
    PkiTools.pemToCertificate("YmFkc3RyaW5n");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDerToCertificate() {

    PkiTools.derToCertificate(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDerToPem() {
    PkiTools.derToPem(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullToPrivateKey() {
    PkiTools.derToPrivateKey(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCertificateToPem() {
    PkiTools.certificateToPem(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testKeyToDer() {
    PkiTools.keyToDer(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPemToDer() {
    PkiTools.pemToDer(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testKeyToPem() {
    PkiTools.keyToPem(null);
  }

  @Test(expected = CertificateGeneratorException.class)
  public void test() {
    PkiTools.certificateToPem(mockCert);
  }

  @Test
  public void testKeyConversion() {
    when(mockKey.getEncoded()).thenReturn(new byte[] {0});
    PkiTools.keyToDer(mockKey);
  }

  @Test(expected = CertificateGeneratorException.class)
  public void testDerToCert() {
    PkiTools.derToCertificate(new byte[] {0});
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMakeGeneralNameNullName() {
    PkiTools.makeGeneralName(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMakeGeneralNameMissingSeparator() {
    final String name = "A";

    PkiTools.makeGeneralName(name);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMakeGeneralNameEmptyValue() {
    final String name = "A:";

    PkiTools.makeGeneralName(name);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMakeGeneralNameUnkownTag() {
    final String name = "A:A";

    PkiTools.makeGeneralName(name);
  }

  @Test
  public void testMakeGeneralNameForEmail() {
    final String value = "a@host.com";

    final GeneralName gname = PkiTools.makeGeneralName("email:" + value);

    assertThat(gname.getTagNo(), equalTo(GeneralName.rfc822Name));
    assertThat(gname.getName().toString(), equalTo(value));
  }

  @Test
  public void testMakeGeneralNameForURI() {
    final String value = "http://ocsp.my.host/";

    final GeneralName gname = PkiTools.makeGeneralName("URI:" + value);

    assertThat(gname.getTagNo(), equalTo(GeneralName.uniformResourceIdentifier));
    assertThat(gname.getName().toString(), equalTo(value));
  }

  @Test
  public void testMakeGeneralNameForRID() {
    final String value = "0.2.1.4";

    final GeneralName gname = PkiTools.makeGeneralName("RID:" + value);

    assertThat(gname.getTagNo(), equalTo(GeneralName.registeredID));
    assertThat(gname.getName().toString(), equalTo(value));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMakeGeneralNameForRIDWithInvalidID() {
    final String value = "3.2.1.4";

    PkiTools.makeGeneralName("RID:" + value);
  }

  @Test
  public void testMakeGeneralNameForDNS() {
    final String value = "A";

    final GeneralName gname = PkiTools.makeGeneralName("DNS:" + value);

    assertThat(gname.getTagNo(), equalTo(GeneralName.dNSName));
    assertThat(gname.getName().toString(), equalTo(value));
  }

  @Test
  public void testMakeGeneralNameForIP() {
    final String value = "1.2.3.4";

    final GeneralName gname = PkiTools.makeGeneralName("IP:" + value);

    assertThat(gname.getTagNo(), equalTo(GeneralName.iPAddress));
    assertThat(gname.getName().toString(), equalTo("#01020304"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMakeGeneralNameForIPWithInvalidIP() {
    final String value = "1.2.3";

    PkiTools.makeGeneralName("IP:" + value);
  }

  @Test
  public void testMakeGeneralNameForDirName() {
    final String value = "C=UK+CN=My Name+OU=My Unit+O=My Organization";

    final GeneralName gname = PkiTools.makeGeneralName("dirName:" + value);

    assertThat(gname.getTagNo(), equalTo(GeneralName.directoryName));
    assertThat(gname.getName().toString(), equalTo(value));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMakeGeneralNameForDirNameWithInvalidName() {
    final String value = "A";

    PkiTools.makeGeneralName("dirName:" + value);
  }
}
