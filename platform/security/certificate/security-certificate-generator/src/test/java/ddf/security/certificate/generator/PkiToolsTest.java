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
package ddf.security.certificate.generator;

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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PkiToolsTest {

    PkiTools tools;

    @Mock
    X509Certificate mockCert;

    @Mock
    PrivateKey mockKey;

    @Test(expected = CertificateGeneratorException.class)
    public void testDerToPrivateKey() {
        tools.derToPrivateKey(new byte[] {0});
    }

    @Before
    public void setUp() {
        tools = new PkiTools();
    }

    @Test(expected = IllegalArgumentException.class)
    public void nameIsNull() throws IllegalArgumentException {
        tools.makeDistinguishedName(null);
    }

    @Test
    public void nameIsEmptyString() throws CertificateEncodingException {

        X500Name name = tools.makeDistinguishedName("");
        assertThat(name.toString(), equalTo("cn="));
    }

    @Test
    public void nameIsNotEmpty() throws CertificateEncodingException {
        String host = "host.domain.tld";
        X500Name name = tools.makeDistinguishedName(host);
        assertThat(name.toString(), equalTo("cn=" + host));
    }

    @Test
    public void convertCertificate() throws CertificateException {
        String originalCert = DemoCertificateAuthority.pemDemoCaCertificate;
        assertThat(originalCert, not(equalTo("")));
        assertThat(tools.certificateToPem(tools.pemToCertificate(originalCert)),
                equalTo(originalCert));
    }

    @Test
    public void hostName() {
        assertThat(tools.getHostName(), not(equalTo("")));
    }

    @Test(expected = CertificateGeneratorException.class)
    public void exception() {
        new CertificateGeneratorException("");
        throw new CertificateGeneratorException("", new Exception());
    }

    private String getPathTo(String path) {
        return getClass().getClassLoader().getResource(path).getPath();
    }

    @Test
    public void testFormatPassword() throws Exception {
        Assert.assertThat("formatPassword() failed to return empty character array",
                tools.formatPassword(null), instanceOf(char[].class));

        char[] pw = "password".toCharArray();
        Assert.assertThat("formatPassword() should not modify the password",
                new String(tools.formatPassword(pw)), equalTo("password"));
    }

    //Null path to keyStore file.
    @Test(expected = IllegalArgumentException.class)
    public void nullPath() throws Exception {
        tools.createFileObject(null);
    }

    //Test constructor. Invalid path to keyStore file.
    @Test(expected = FileNotFoundException.class)
    public void invalidPath() throws Exception {
        tools.createFileObject("");
    }

    //Test Constructor. Path is a directory, not a file.
    @Test(expected = FileNotFoundException.class)
    public void pathIsDirectory() throws Exception {
        String anyDirectory = getPathTo("");
        tools.createFileObject("");
    }

    @Test
    public void realFile() throws IOException {
        assertThat(
                "Should have returned a new File object. Is the file in the test resources directory?",
                tools.createFileObject(getPathTo("not_keystore.jks")), instanceOf(File.class));
    }

    @Test(expected = CertificateGeneratorException.class)
    public void badKey() {
        tools.pemToPrivateKey("YmFkc3RyaW5n");
    }

    @Test(expected = CertificateGeneratorException.class)
    public void badCert() {
        tools.pemToCertificate("YmFkc3RyaW5n");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDerToCertificate() {

        tools.derToCertificate(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDerToPem() {
        tools.derToPem(null);

    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullToPrivateKey() {
        tools.derToPrivateKey(null);

    }

    @Test(expected = IllegalArgumentException.class)
    public void testCertificateToPem() {
        tools.certificateToPem(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testKeyToDer() {
        tools.keyToDer(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPemToDer() {
        tools.pemToDer(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testKeyToPem() {
        tools.keyToPem(null);
    }

    @Test(expected = CertificateGeneratorException.class)
    public void test() {
        tools.certificateToPem(mockCert);
    }

    @Test
    public void testKeyConversion() {
        when(mockKey.getEncoded()).thenReturn(new byte[] {0});
        tools.keyToDer(mockKey);
    }

    @Test(expected = CertificateGeneratorException.class)
    public void testDerToCert() {
        tools.derToCertificate(new byte[] {0});
    }
}