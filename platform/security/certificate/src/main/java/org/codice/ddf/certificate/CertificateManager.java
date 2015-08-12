/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.certificate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

    /*+----------------------------------------------------------------------
     ||
     ||  Class: CertificateManager
     ||
     ||        Purpose:  Collection of static methods that manages X509Certificate creation and signing.
     ||
     ||  Inherits From:  None
     ||
     ||     Interfaces:  None
     ||
     |+-----------------------------------------------------------------------
     ||
     ||      Constants:  MILLIS_IN_DAY - Number of milliseconds in a day
     ||                  MILLIS_IN_YEAR - Number of milliseconds in a year
     ||
     |+-----------------------------------------------------------------------
     ||
     ||  Class Methods:     X509v3CertificateBuilder    createDefaultX509SigningRequest(PublicKey)
     ||                     X509v3CertificateBuilder    createCustomX509SigningRequest(String, String, int, PublicKey)
     ||                     X509v3CertificateBuilder    createCustomX509SigningRequest(String, String, String, String, String, String, String, String, String, String, int, PublicKey)
     ||                     X509Certificate             signX509CertificateSHA256RSA(X509v3CertificateBuilder, PrivateKey)
     ||                     void                        registerBCSecurityProvider()
     ||                     String                      getHostName()
     ||
     ||
     ++-----------------------------------------------------------------------*/

public class CertificateManager {
    private static final long MILLIS_IN_DAY = 86400000;

    private static final long MILLIS_IN_YEAR = 31536000000L;

    private static final Logger LOGGER = LoggerFactory.getLogger(CertificateManager.class);

    /**
     * ---------------------------------------------------------------------
     * Method: createDefaultX509SigningRequest
     * <p/>
     * Purpose:  Creates a signing request with default issuer and subject information
     * <p/>
     * Pre-condition: None
     * <p/>
     * Post-condition: Issuer information, subject information, and the encoded public key have been added to the returned signing request. Valid for 3 years.
     *
     * @param pubKey the PublicKey to be associated with the certificate signing request
     * @return X509v3CertificateBuilder with the issuer, subject, and encoded public key set.
     * -------------------------------------------------------------------
     */

    public static X509v3CertificateBuilder createDefaultX509SigningRequest(PublicKey pubKey) {
        X509v3CertificateBuilder certBuilder;
        Long currentTimeMillis = System.currentTimeMillis();
        X500Name theIssuer = new X500Name(
                "C=US,ST=AZ,O=DDF,OU=Dev,CN=DDF Demo Root CA,E=ddfrootca@example.org");
        X500Name theSubject = new X500Name(
                "CN=" + getHostName() + ", OU=Conn, OU=Dev, OU=DevT, O=U.S., C=US");
        certBuilder = new X509v3CertificateBuilder(theIssuer,
                new BigInteger(currentTimeMillis.toString()),
                (new Date(currentTimeMillis - MILLIS_IN_DAY)),
                (new Date(currentTimeMillis + (MILLIS_IN_YEAR * 3))), theSubject,
                SubjectPublicKeyInfo.getInstance(pubKey.getEncoded()));
        return certBuilder;
    }

    /**
     * ---------------------------------------------------------------------
     * Method: createCustomX509SigningRequest
     * <p/>
     * Purpose:  Creates a signing request with custom Issuer, Subject, and validity period data.
     * <p/>
     * Pre-condition: None
     * <p/>
     * Post-condition: Issuer information, subject information, and the encoded public key have been added to the returned signing request. This is valid for the number of days specified by numDaysValid parameter.
     *
     * @param issuerAttributes  String that contains a comma delimited list of certificate attributes such as CN, OU, and C for the issuer.
     * @param subjectAttributes String that contains a comma delimited list of certificate attributes such as CN, OU, and C for the subject.
     * @param numDaysValid      Integer representing the number of days for the certificate to be valid.
     * @param pubKey            The PublicKey to be associated with the certificate signing request.
     * @return X509v3CertificateBuilder signing request with the issuer, subject, and date fields set
     * -------------------------------------------------------------------
     */

    public static X509v3CertificateBuilder createCustomX509SigningRequest(String issuerAttributes,
            String subjectAttributes, int numDaysValid, PublicKey pubKey) {
        X509v3CertificateBuilder certBuilder;
        Long currentTimeMillis = System.currentTimeMillis();
        X500Name theIssuer = new X500Name(issuerAttributes);
        X500Name theSubject = new X500Name(subjectAttributes);
        certBuilder = new X509v3CertificateBuilder(theIssuer, new BigInteger("2"),
                (new Date(currentTimeMillis - MILLIS_IN_DAY)),
                (new Date(currentTimeMillis + (MILLIS_IN_DAY * numDaysValid))), theSubject,
                SubjectPublicKeyInfo.getInstance(pubKey.getEncoded()));

        return certBuilder;
    }

    /**
     * ---------------------------------------------------------------------
     * Method: signX509Certificate
     * <p/>
     * Purpose:  Signs and returns a X509Certificate given a signing request and privateKey
     * <p/>
     * Pre-condition:  The X509v3CertificateBuilder has been initialized with subject, issuer, and data information.
     * <p/>
     * Post-condition: The X509 returned is signed using the privateKey privKey
     *
     * @param signingRequest an initialized signing request
     * @param privKey        the PrivateKey used to sign the certificate
     * @return X509Certificate that has been signed with the supplied PrivateKey
     * -------------------------------------------------------------------
     */

    public static X509Certificate signX509CertificateSHA256RSA(
            X509v3CertificateBuilder signingRequest, PrivateKey privKey) {
        X509Certificate certificate = null;
        try {
            byte[] certBytes = signingRequest.build(new SHA256RSASigner(privKey)).getEncoded();
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            certificate = (X509Certificate) certificateFactory
                    .generateCertificate(new ByteArrayInputStream(certBytes));
        } catch (IOException | CertificateException e) {
            LOGGER.error(e.getMessage(), e);
        }

        return certificate;
    }

    /**
     * ---------------------------------------------------------------------
     * Method: registerBCSecurityProvider
     * <p/>
     * Purpose:  Registers BouncyCastle as a java.Security provider
     * <p/>
     * Pre-condition:  None
     * <p/>
     * Post-condition: Bouncy Castle is added to the list of java.Security Providers
     * <p/>
     * Parameters:     None
     * <p/>
     * Returns:        None
     * -------------------------------------------------------------------
     */

    public static void registerBCSecurityProvider() {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * ---------------------------------------------------------------------
     * Method: getHostName
     * <p/>
     * Purpose:  Used to retrieve the hostname of the machine
     * <p/>
     * Pre-condition:  None
     * <p/>
     * Post-condition: Hostname is returned
     * <p/>
     * Parameters:     None
     *
     * @return String representing the hostname
     * -------------------------------------------------------------------
     */

    public static String getHostName() {
        String hostName = null;
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return hostName;
    }


    /*+----------------------------------------------------------------------
    ||
    ||  Class: SHA256RSASigner
    ||
    ||        Purpose:  Implementation of ContentSigner that uses SHa256withRSA. Used to sign X509 certificates
    ||
    ||     Interfaces:  ContentSigner
    ||
    ++-----------------------------------------------------------------------*/

    private static class SHA256RSASigner implements ContentSigner {

        private static final AlgorithmIdentifier SHA_256_WITH_RSA_ID = new AlgorithmIdentifier(
                new ASN1ObjectIdentifier("1.2.840.113549.1.1.11"));

        private Signature signature;

        private ByteArrayOutputStream outputStream;

        public SHA256RSASigner(PrivateKey privateKey) {
            try {
                this.outputStream = new ByteArrayOutputStream();
                this.signature = Signature.getInstance("SHA256withRSA");
                this.signature.initSign(privateKey);
            } catch (GeneralSecurityException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

        @Override public AlgorithmIdentifier getAlgorithmIdentifier() {
            return SHA_256_WITH_RSA_ID;
        }

        @Override public OutputStream getOutputStream() {
            return outputStream;
        }

        @Override public byte[] getSignature() {
            try {
                signature.update(outputStream.toByteArray());
                return signature.sign();
            } catch (GeneralSecurityException e) {
                LOGGER.error(e.getMessage(), e);
                return null;
            }
        }
    }
}