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

import org.joda.time.DateTime;
import org.junit.Test;

import java.net.UnknownHostException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import static org.junit.Assert.assertNotNull;

public class TestCertificateGeneration {

    public static final DateTime THE_FUTURE = DateTime.now().plusYears(1);
    public static final DateTime THE_PAST = DateTime.now().minusYears(1);

    protected String getPathTo(String path) {
        return getClass().getClassLoader().getResource(path).getPath();
    }


    @Test(expected = CertificateGeneratorException.InvalidSerialNumber.class)
    public void serialNumberIsNegative() throws Exception {
        new CertificateSigningRequest().setSerialNumber(-1);
    }

    //Test building incomplete instance
    @Test(expected = CertificateGeneratorException.InvalidDate.class)
    public void notAfterDateIsNull() throws Exception {
        CertificateSigningRequest csr = new CertificateSigningRequest();
        csr.build();
    }

    //Test building incomplete instance
    @Test(expected = CertificateGeneratorException.InvalidDate.class)
    public void notAfterDateIsInThePast() throws Exception {
        CertificateSigningRequest csr = new CertificateSigningRequest();
        csr.setNotAfter(THE_PAST);
        csr.build();
    }

    //Test building incomplete instance
    @Test(expected = CertificateGeneratorException.InvalidSubject.class)
    public void subjectKeysAreNull() throws Exception {
        CertificateSigningRequest csr = new CertificateSigningRequest();
        csr.setNotAfter(THE_FUTURE);
        csr.build();
    }

    //Test building incomplete instance
    @Test(expected = CertificateGeneratorException.InvalidCertificateAuthority.class)
    public void certificateAuthorityIsMissing() throws Exception {
        CertificateSigningRequest csr = new CertificateSigningRequest();
        csr.setNotAfter(THE_FUTURE);
        csr.setSubjectNameToHostname();
        csr.build();
    }

    //Happy path!
    @Test
    public void createSignedCertificate() throws Exception {
        getCsr().getSignedCertificate();
    }

    //Add newly created private key and certificate (chain) to a keystore
    @Test
    public void installCertificate() throws Exception {
        CertificateSigningRequest csr = getCsr();
        PrivateKey subjectPrivateKey = csr.getPrivateKey();
        X509Certificate chain[] = csr.getCertificateChain();
        KeyStoreFile keyStore = KeyStoreFile.getInstance(getPathTo("keystore-password_changeit.jks"), "changeit".toCharArray());
        keyStore.addEntry("alias", subjectPrivateKey, chain);
        PrivateKey key = keyStore.getPrivateKey("alias");
        assertNotNull(key);
        Certificate[] certs = keyStore.getCertificateChain("alias");
        assertNotNull(certs);
        keyStore.save();
    }

    protected CertificateSigningRequest getCsr() throws CertificateException, UnknownHostException {
        //Instantiate DDF Demo CA's certificate
        X509Certificate cert = CertificateGeneratorUtilities.stringToCertificate(CertificateAuthority.certificatePem);

        //Instantiate DDF DEmo CA's private key
        PrivateKey privateKey = CertificateGeneratorUtilities.stringToPrivateKey(CertificateAuthority.privateKeyPem);

        //Instantiate DDF Demo CA
        CertificateAuthority ca = new CertificateAuthority(cert, privateKey);

        //Create new signing request and let DDF Demo CA sign it
        CertificateSigningRequest csr = new CertificateSigningRequest();
        csr.setNotAfter(THE_FUTURE);
        csr.generateNewKeys();
        csr.setSubjectNameToHostname();
        csr.setCertificateAuthority(ca);
        csr.build();
        return csr;
    }


}


