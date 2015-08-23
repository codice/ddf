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

import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.cert.CertificateException;

public class TestPemFile {

    public static final String CERTIFICATE = "certificate-demoCA.pem";
    public static final String PRIVATE_KEY = "private-key-3des-encypted-password_changeit.pem";
    public static final char[] PASSWORD = "changeit".toCharArray();

    private String getPathTo(String path) {
        return getClass().getClassLoader().getResource(path).getPath();
    }
    // The basic file I/O is tested in TestKeyStoreFile.
    // Test constructor. No test to ensure PEM objects are in the file. Would have to try and
    // Read an object, test if it was null, and if it is not, reset the stream.


    @Test
    public void testEncryptedPrivateKeyNullPassword() throws FileNotFoundException {

        PemFile.getInstance(getPathTo(PRIVATE_KEY), null);
    }

    @Test(expected = IOException.class)
    public void testEncryptedPrivateKeyWrongPassword() throws IOException {

        PemFile pf = PemFile.getInstance(getPathTo(PRIVATE_KEY), "ThisIsNotPassword".toCharArray());
        pf.getPrivateKey();
    }

    @Test
    public void testEncryptedPrivateKey() throws IOException {
        PemFile pemFile = PemFile.getInstance(getPathTo(PRIVATE_KEY), PASSWORD);
        pemFile.getPrivateKey();
    }

    @Test(expected = IOException.class)
    public void testWrongPemObject() throws IOException {
        PemFile pemFile = PemFile.getInstance(getPathTo(CERTIFICATE));
        pemFile.getPrivateKey();

    }

    @Test
    public void testCertificate() throws IOException, CertificateException {
        PemFile pemFile = PemFile.getInstance(getPathTo(CERTIFICATE));
        pemFile.getCertificate();
    }
}




