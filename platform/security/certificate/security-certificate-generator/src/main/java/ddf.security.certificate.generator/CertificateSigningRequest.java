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
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Model of a X509 certificate signing request. See RFC 5280,  Internet X.509 Public Key Infrastructure Certificate
 * and Certificate Revocation List (CRL) Profile for more information.
 */
public class CertificateSigningRequest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CertificateSigningRequest.class);
    private static String BC = BouncyCastleProvider.PROVIDER_NAME;

    static {

        Security.addProvider(new BouncyCastleProvider());
    }

    protected DateTime notBefore;
    protected DateTime notAfter;
    protected int keyPairLength;
    protected X500Name subjectDistinguishedName;
    protected KeyPair subjectKeyPair;
    protected BigInteger serialNumber;
    protected Boolean useNewKeys;
    protected CertificateAuthority certificateAuthority;
    protected X509Certificate signedCertificate;

    public CertificateSigningRequest() {
        initialize();
    }

    //Set reasonable defaults
    protected void initialize() {
        keyPairLength = 1024;
        serialNumber = BigInteger.valueOf(System.currentTimeMillis());
        notBefore = DateTime.now().minusDays(1);
        useNewKeys = true;

        //TODO: It might not be a good idea to pick the end expiration date for the user. Force user to set expiration date.
        // notAfter = DateTime.now().plusYears(5);
    }

    public PrivateKey getPrivateKey() {
        return subjectKeyPair.getPrivate();
    }

    public void setCertificateAuthority(CertificateAuthority certificateAuthority) {
        this.certificateAuthority = certificateAuthority;
    }

    public X509Certificate getSignedCertificate() {
        return signedCertificate;
    }

    /**
     * Create a signed certificate. After this method executes, use {@link #getSignedCertificate()}
     * to retrieve the retrieve the generated certificate. If a new keypair was created for the subject, use
     * {@link #getPrivateKey()} to retrieve the private key.
     *
     * @throws CertificateGeneratorException
     */
    public void build()
            throws CertificateGeneratorException {

        //If effective date is not set, this will end badly
        if (notAfter == null) {
            throw new CertificateGeneratorException.InvalidDate(
                    "Missing certificate validity date. " +
                            "Set the Not After attribute to specify certificate's expiration date");
        }

        //This can only happen if client code explicitly sets the value to null because the attribute is initialized at creation time.
        if (notBefore == null) {
            throw new CertificateGeneratorException.InvalidDate(
                    "Missing certificate validity date. " +
                            "Set the Not After attribute to specify certificate's expiration date");
        }

        if (notAfter.isBefore(notBefore)) {
            throw new CertificateGeneratorException.InvalidDate(
                    String.format("Certificate 'Not After' (expiration) of %s must be later than 'Not Before' (effective) of %s", notBefore, notAfter));
        }

        if (subjectKeyPair == null) {
            //Typical use case is to generate both the public and private keys within this class.
            //ASSUME that if the keypair has not been set, the user's intention is to generate a new keypair.
            generateNewKeys();
        }

        if (subjectDistinguishedName == null || subjectDistinguishedName.toString().isEmpty()) {
            throw new CertificateGeneratorException.InvalidSubject(
                    "Subject distinguished name is null or empty. " +
                            "Set subject to host name or or set subject distinguished name");
        }

        if (certificateAuthority == null) {
            throw new CertificateGeneratorException.InvalidCertificateAuthority("Certificate authority is null");
        }

        //'Case the builder needs a builder?
        X509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(
                certificateAuthority.getCertificate(),
                serialNumber,
                notBefore.toDate(),
                notAfter.toDate(),
                subjectDistinguishedName,
                subjectKeyPair.getPublic());

        //What's a certificate without a certificate holder?
        X509CertificateHolder holder =
                certificateBuilder.build(certificateAuthority.getContentSigner());

        try {
            //Sign the certificate
            signedCertificate = getCertificateConverter().getCertificate(holder);
        } catch (CertificateException e) {
            throw new CertificateGeneratorException("Could not create signed certificate.", e.getCause());
        }
    }

    //TODO: This functionality is not unique to certificate signing requests. But it is not so involved that it needs its own class. Where does it belong? Static method on CertificateGenerationUtilities?
    protected void generateNewKeys() throws CertificateGeneratorException.CannotGenerateKeyPair {

        KeyPairGenerator keyGen = null;
        try {
            keyGen = KeyPairGenerator.getInstance("RSA", CertificateGeneratorUtilities.BC);
        } catch (Exception e) {
            throw new CertificateGeneratorException.CannotGenerateKeyPair("Failed to generate new public/private key pair.", e);
        }
        keyGen.initialize(keyPairLength);
        useSubjectKeyPair(keyGen.generateKeyPair());
    }

    /**
     * Create a distinguished name for the certificate's subject. Currently, only the common name (sub-attribute of
     * the distinguished name) is supported. The common name should be the <b>fully qualified domain name</b> of the
     * certificate's subject (i.e. the server). For example, <i>server.subdomain.domain.tld</i>.
     *
     * @param name subject's common name attribute (
     */
    public void setSubjectDistinguishedName(String name) {
        subjectDistinguishedName = CertificateGeneratorUtilities.makeDistinguishedName(name);

    }

    /**
     * Use hostname of the network node running the JBM as the subject's common name.
     * The most common use case is the use case is DNS entry name of the server is the X509 common name.
     * SSL/TLS uses the common name as part of the secure connection handshake and if the name is wrong, TLS will
     * not trust the connection. There is no guarantee that the {@link CertificateGeneratorUtilities#getHostName()}
     * returns the correct result in all cases. Use {@link #setSubjectDistinguishedName(String)} to manually set
     * the common name for the certificate's subject.
     *
     * @throws UnknownHostException
     */
    public void setSubjectNameToHostname() throws UnknownHostException {
        setSubjectDistinguishedName(CertificateGeneratorUtilities.getHostName());
    }

    /**
     * Set the subject's public and private keypair. Only the public key is used. However, the the most common use
     * case is to generate a new keypair when creating a certificate. The client code will need to retrieve the
     * the private key for later use. This method is provided for situations when an existing  keypair
     * should be used to generate the certificate.
     *
     * @param keyPair
     */
    public void useSubjectKeyPair(KeyPair keyPair) {
        subjectKeyPair = keyPair;
    }

    /**
     * The validity period for a certificate is the period of time from notBefore through notAfter, inclusive.
     *
     * @param date
     */
    public void setNotBefore(DateTime date) {
        notBefore = date;
    }

    /**
     * The validity period for a certificate is the period of time from notBefore through notAfter, inclusive.
     *
     * @param date
     */
    public void setNotAfter(DateTime date) {
        notAfter = date;
    }

    /**
     * Set the serial number of the certificate. The serial number is arbitrary, but should not be negative.
     *
     * @param number
     */
    public void setSerialNumber(long number) throws CertificateGeneratorException.InvalidSerialNumber {

        if(number < 0) {
            throw new CertificateGeneratorException.InvalidSerialNumber("Serial number for X.509 certificate should not be negative");
        }
        serialNumber = BigInteger.valueOf(number);
    }

    protected JcaX509CertificateConverter getCertificateConverter() {
        return new JcaX509CertificateConverter().setProvider(BC);
    }

    /**
     * The subject and issuers' certificates
     *
     * @return the certificate chain to be used in the keystore entry
     */
    public X509Certificate[] getCertificateChain() {
        X509Certificate[] chain = new X509Certificate[2];
        chain[0] = getSignedCertificate();
        chain[1] = certificateAuthority.getCertificate();
        return chain;
    }
}