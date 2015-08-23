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

import java.security.cert.X509Certificate;

public class TestCertificateGenerator {

    public static final DateTime THE_FUTURE = DateTime.now().plusYears(10);

    protected String getPathTo(String path) {
        return getClass().getClassLoader().getResource(path).getPath();
    }


    protected KeyStoreFile getKeyStore() throws Exception {
        return KeyStoreFile.getInstance(getPathTo("ValidKeyStore.jks"), "changeit".toCharArray());
    }

    protected X509Certificate getCertificate() throws Exception {
        return getKeyStore().getCertificate("ddf demo root ca");
    }

    //Test building incomplete instance
    @Test(expected = CertificateGeneratorException.InvalidDate.class)
    public void badNotAfterDate() throws Exception {
        DateTime longTimeAgo = DateTime.now().minusYears(1);
        new CertificateGenerator().setNotAfter(longTimeAgo).build();
    }

    //Test building incomplete instance
    @Test(expected = CertificateGeneratorException.InvalidDate.class)
    public void missingNotAfterDate() throws Exception {
        new CertificateGenerator().build();
    }

    //Test building incomplete instance
    @Test(expected = CertificateGeneratorException.InvalidIssuer.class)
    public void issuerCertificateNotSet() throws Exception {
        new CertificateGenerator().
                setNotAfter(THE_FUTURE).
                build();
    }

    //Test building incomplete instance
    @Test(expected = CertificateGeneratorException.InvalidSubject.class)
    public void subjectKeyPairNotSet() throws Exception {
        new CertificateGenerator().
                setNotAfter(THE_FUTURE).
                setIssuerCert(getCertificate()).
                build();
    }

    //Test building incomplete instance
    @Test(expected = CertificateGeneratorException.InvalidSubject.class)
    public void subjectNameInvalid() throws Exception {
        new CertificateGenerator().
                setNotAfter(THE_FUTURE).
                setIssuerCert(getCertificate()).
                generateNewKeys().
                build();
    }

    //Test building incomplete instance
    @Test(expected = CertificateGeneratorException.InvalidIssuer.class)
    public void issuerPrivateKeyNotSet() throws Exception {
        new CertificateGenerator().
                setNotAfter(THE_FUTURE).
                setIssuerCert(getCertificate()).
                generateNewKeys().
                setSubjectToHostname().
                build();
    }

    @Test
    public void xxx() throws Exception {


//        PrivateKey privateKey = PemFile.getInstance(getPathTo("demoCA-private-key-3des-encrypted-password_changeit.pem"), "changeit".toCharArray()).getPrivateKey();

        new CertificateGenerator().
                setNotAfter(THE_FUTURE).
                setIssuerCert(getCertificate()).
                generateNewKeys().
                setSubjectToHostname().
                setIssuerPrivateKey(null).
                build();
    }

}


