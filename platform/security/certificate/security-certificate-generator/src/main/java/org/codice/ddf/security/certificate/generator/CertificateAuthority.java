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

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.commons.lang.Validate;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 * Models an X509 certificate authority. Construct a new instance given an X509 certificate and an RSA private key.
 * Use @sign to sign content.
 */
public class CertificateAuthority {

    public static final String SIGNATURE_ALGORITHM = "SHA256WithRSAEncryption";

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    PrivateKey issuerPrivateKey;

    X509Certificate issuerCert;

    /**
     * Create fully initialized instance of a Certificate Authority.
     *
     * @param certificate certificate authority's X509 certificate
     * @param privateKey  certificate authority's private RSA key
     */
    public CertificateAuthority(X509Certificate certificate, PrivateKey privateKey) {
        initialize(certificate, privateKey);
    }

    //Use public constructors
    CertificateAuthority() {
    }

    public KeyStore.PrivateKeyEntry sign(CertificateSigningRequest csr) {
        X509Certificate signedCert;

        try {
            //Converters, holders, and builders! Oh my!
            JcaX509v3CertificateBuilder builder = csr.newCertificateBuilder(getCertificate());
            X509CertificateHolder holder = builder.build(getContentSigner());
            JcaX509CertificateConverter converter = newCertConverter();

            signedCert = converter.getCertificate(holder);
        } catch (CertIOException e) {
            throw new CertificateGeneratorException("Could not create signed certificate.", e);
        } catch (CertificateException e) {
            throw new CertificateGeneratorException("Could not create signed certificate.",
                    e.getCause());
        }

        X509Certificate[] chain = new X509Certificate[2];
        chain[0] = signedCert;
        chain[1] = getCertificate();

        return new KeyStore.PrivateKeyEntry(csr.getSubjectPrivateKey(), chain);
    }

    JcaX509CertificateConverter newCertConverter() {
        return new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);
    }

    ContentSigner getContentSigner() {
        try {
            return new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .build(getPrivateKey());
        } catch (Exception e) {
            throw new CertificateGeneratorException(
                    "Cannot create content signer of certificate authority",
                    e);
        }
    }

    /**
     * Get the Certificate Authority's X509 certificate.
     *
     * @return Certificate Authority's X509 certificate.
     */
    X509Certificate getCertificate() {
        return issuerCert;
    }

    PrivateKey getPrivateKey() {
        return issuerPrivateKey;
    }

    void initialize(X509Certificate cert, PrivateKey privateKey) {
        Validate.isTrue(cert != null, "The issuer's certificate cannot be null");
        Validate.isTrue(privateKey != null, "The issuer's private key cannot be null");
        issuerPrivateKey = privateKey;
        issuerCert = cert;
    }
}