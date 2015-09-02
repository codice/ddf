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

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigInteger;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)

public class CertificateGenerationTest {

    public static final DateTime THE_FUTURE = DateTime.now().plusYears(1);
    public static final DateTime THE_PAST = DateTime.now().minusYears(1);

    private PkiTools tools = new PkiTools();

    @Mock
    private JcaX509v3CertificateBuilder certificateBuilder;

    @Mock
    private X509CertificateHolder certificateHolder;

    @Mock
    private JcaX509CertificateConverter certificateConverter;

    @Mock
    private X509Certificate mockCertificate;

    protected String getPathTo(String path) {
        return getClass().getClassLoader().getResource(path).getPath();
    }


    @Test(expected = IllegalArgumentException.class)
    public void serialNumberIsNegative() throws Exception {
        new CertificateSigningRequest().setSerialNumber(-1);
    }

    //Test building incomplete instance
    @Test(expected = CertificateGeneratorException.class)
    public void notAfterDateIsNull() throws Exception {
        CertificateSigningRequest csr = new CertificateSigningRequest();
        csr.build();
    }

    //Test building incomplete instance
    @Test(expected = CertificateGeneratorException.class)
    public void notAfterDateIsInThePast() throws Exception {
        CertificateSigningRequest csr = new CertificateSigningRequest();
        csr.setNotAfter(THE_PAST);
        csr.build();
    }

    //Test building incomplete instance
    @Test(expected = CertificateGeneratorException.class)
    public void subjectKeysAreNull() throws Exception {
        CertificateSigningRequest csr = new CertificateSigningRequest();
        csr.setNotAfter(THE_FUTURE);
        csr.build();
    }

    @Test(expected = CertificateGeneratorException.class)
    public void effectiveDateIsWrong() throws Exception {
        CertificateSigningRequest csr = new CertificateSigningRequest();
        csr.setNotAfter(THE_FUTURE);
        csr.setCommonNameToHostname();
        csr.setNotBefore(THE_FUTURE.plusDays(1));
        csr.build();
    }


    //Test building incomplete instance.
    @Test(expected = CertificateGeneratorException.class)
    public void certificateAuthorityIsMissing() throws Exception {
        CertificateSigningRequest csr = new CertificateSigningRequest();
        csr.setNotAfter(THE_FUTURE);
        csr.setCommonNameToHostname();
        csr.build();
    }

    //Add newly created private key and certificate (chain) to a keystore
    @Test
    public void installCertificate() throws Exception {

        //Instantiate DDF Demo CA's certificate
        final X509Certificate cert = tools.pemToCertificate(CertificateAuthority.pemDemoCaCertificate);

        //Instantiate DDF DEmo CA's private key
        PrivateKey privateKey = tools.pemToPrivateKey(CertificateAuthority.pemDemoCaPrivateKey);

        //Instantiate DDF Demo CA
        CertificateAuthority ca = new CertificateAuthority(cert, privateKey);

        //Create new signing request and let DDF Demo CA sign it
        CertificateSigningRequest csr1 = new CertificateSigningRequest() {
            JcaX509v3CertificateBuilder getCertificateBuilder(X509Certificate certificate, BigInteger serialNumber, Date notBefore, Date notAfter, X500Name subjectName, PublicKey key) {
                assertThat(certificate, sameInstance(cert));
                return certificateBuilder;
            }

            JcaX509CertificateConverter getCertificateConverter() {
                return certificateConverter;
            }

        };
        csr1.setNotAfter(THE_FUTURE);

        KeyPair keyPair = tools.generateRsaKeyPair();
        csr1.useSubjectKeyPair(keyPair);
        csr1.setCommonNameToHostname();
        csr1.setCertificateAuthority(ca);

        when(certificateBuilder.build(ca.getContentSigner())).thenReturn(certificateHolder);
        when(certificateConverter.getCertificate(certificateHolder)).thenReturn(mockCertificate);
        csr1.build();
        verify(certificateBuilder).build(ca.getContentSigner());

        CertificateSigningRequest csr = csr1;
        PrivateKey subjectPrivateKey = csr.getPrivateKey();
        X509Certificate chain[] = csr.getCertificateChain();
        KeyStoreFile keyStoreFile = KeyStoreFile.newInstance(getPathTo("keystore-password_changeit.jks"), "changeit".toCharArray());
        keyStoreFile.addEntry("alias", subjectPrivateKey, chain);
        PrivateKey key = keyStoreFile.getPrivateKey("alias");
        assertNotNull(key);
        Certificate[] certs = keyStoreFile.getCertificateChain("alias");
        assertNotNull(certs);
        keyStoreFile.save();
    }

    private CertificateSigningRequest getCsr() throws CertificateException, UnknownHostException {
        //Instantiate DDF Demo CA's certificate
        X509Certificate cert = tools.pemToCertificate(CertificateAuthority.pemDemoCaCertificate);

        //Instantiate DDF DEmo CA's private key
        PrivateKey privateKey = tools.pemToPrivateKey(CertificateAuthority.pemDemoCaPrivateKey);

        //Instantiate DDF Demo CA
        CertificateAuthority ca = new CertificateAuthority(cert, privateKey);

        //Create new signing request and let DDF Demo CA sign it
        CertificateSigningRequest csr = new CertificateSigningRequest();
        csr.setNotAfter(THE_FUTURE);

        KeyPair keyPair = tools.generateRsaKeyPair();
        csr.useSubjectKeyPair(keyPair);
        csr.setCommonNameToHostname();
        csr.setCertificateAuthority(ca);
        csr.build();
        return csr;
    }

    private void createSignedCertificate() throws Exception {
        getCsr().getSignedCertificate();
    }

}


