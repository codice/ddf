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

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class TestCertificateGeneration {

    public static final DateTime THE_FUTURE = DateTime.now().plusYears(1);
    public static final DateTime THE_PAST = DateTime.now().minusYears(1);

    protected String getPathTo(String path) {
        return getClass().getClassLoader().getResource(path).getPath();
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
    @Test(expected = CertificateGeneratorException.InvalidSubject.class)
    public void subjectNameIsMissing() throws Exception {
        CertificateSigningRequest csr = new CertificateSigningRequest();
        csr.setNotAfter(THE_FUTURE);
        csr.generateNewKeys();
        csr.build();
    }

    //Test building incomplete instance
    @Test(expected = CertificateGeneratorException.InvalidCertificateAuthority.class)
    public void certificateAuthorityIsMissing() throws Exception {
        CertificateSigningRequest csr = new CertificateSigningRequest();
        csr.setNotAfter(THE_FUTURE);
        csr.generateNewKeys();
        csr.setSubjectNameToHostname();
        csr.build();
    }

    @Test
    public void createSignedCertificate() throws Exception {

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

        X509Certificate signedCert = csr.getSignedCertificate();
        signedCert.checkValidity();
    }
}


