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

import org.apache.commons.lang3.Validate;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

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
    ContentSigner contentSigner;
    PkiTools pkiTools = new PkiTools();

    /**
     * @param derCertificate
     * @param derPrivateKey
     */
    public CertificateAuthority(byte[] derCertificate, byte[] derPrivateKey) {
        initialize(
                pkiTools.derToCertificate(derCertificate),
                pkiTools.derToPrivateKey(derPrivateKey));
    }

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

    public PrivateKeyEntry sign(CertificateSigningRequest csr) {
        //Converters, holders, and builders! Oh my!
        JcaX509v3CertificateBuilder builder = csr.getCertificateBuilder(getCertificate());
        X509CertificateHolder holder = builder.build(getContentSigner());
        JcaX509CertificateConverter converter = new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);
        X509Certificate signedCert;
        try {
            signedCert = converter.getCertificate(holder);
        } catch (CertificateException e) {
            throw new CertificateGeneratorException("Could not create signed certificate.", e.getCause());
        }

        return new PrivateKeyEntry(signedCert, csr.getSubjectPrivateKey(), getCertificate());
    }

    /**
     * Get an object that can be used to digitally sign certificates, messages, or anything else.
     *
     * @return ContentSigner
     */
    ContentSigner getContentSigner() {
        return contentSigner;
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
        Validate.notNull("The issuer's certificate cannot be null");
        Validate.notNull("The issuer's private key cannot be null");
        issuerPrivateKey = privateKey;
        issuerCert = cert;
        try {
            contentSigner = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).setProvider(BouncyCastleProvider.PROVIDER_NAME).build(getPrivateKey());
        } catch (Exception e) {
            throw new CertificateGeneratorException("Cannot create content signer of certificate authority", e);
        }
    }
}