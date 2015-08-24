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
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.RFC4519Style;
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
        useNewKeys = false;

        //TODO: It might not be a good idea to pick the end effective date for the user.
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
            // Only the subject's PUBLIC KEY  is required to generate a certificate signing
            // request. However, the typical use case is to generate both the public and
            // private keys within this class.
            throw new CertificateGeneratorException.InvalidSubject(
                    "Subject key pair is null. Set subject key pair.");
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

    protected void generateNewKeys() throws CertificateGeneratorException.CannotGenerateKeyPair {

        KeyPairGenerator keyGen = null;
        try {
            keyGen = KeyPairGenerator.getInstance("RSA", BC);
        } catch (Exception e) {
            throw new CertificateGeneratorException.CannotGenerateKeyPair("Failed to generate new public/private key pair.", e);
        }
        keyGen.initialize(keyPairLength);
        useSubjectKeyPair(keyGen.generateKeyPair());
    }

    // For a Subject, the common name should be the server's DNS name.
    public void setSubjectDistinguishedName(String name) {
        subjectDistinguishedName = makeDistinguishedName(name);

    }

    public void setSubjectNameToHostname() throws UnknownHostException {
        setSubjectDistinguishedName(CertificateGeneratorUtilities.getHostName());
    }

    public void useSubjectKeyPair(KeyPair keyPair) {
        subjectKeyPair = keyPair;
    }

    public void setNotBefore(DateTime date) {
        notBefore = date;
    }

    public void setNotAfter(DateTime date) {
        notAfter = date;
    }

    public void setSerialNumber(long number) {
        serialNumber = BigInteger.valueOf(number);
    }

    // Currently only uses the common name attribute.
    protected X500Name makeDistinguishedName(String commonName) {
        // The attributes of a distinguished name that a TLS implementation must be ready to support.
        //    RFC 4514 LDAP
        //    Implementations MUST recognize AttributeType name strings (descriptors) listed in the following table,
        //    but MAY recognize other name strings.
        //
        //    String  X.500 AttributeType
        //    ------  --------------------------------------------
        //    CN      commonName (2.5.4.3)
        //    L       localityName (2.5.4.7)
        //    ST      stateOrProvinceName (2.5.4.8)
        //    O       organizationName (2.5.4.10)
        //    OU      organizationalUnitName (2.5.4.11)
        //    C       countryName (2.5.4.6)
        //    STREET  streetAddress (2.5.4.9)
        //    DC      domainComponent (0.9.2342.19200300.100.1.25)
        //    UID     userId (0.9.2342.19200300.100.1.1)
        //Build distinguished name for subject or issuer

        X500NameBuilder nameBuilder = new X500NameBuilder(RFC4519Style.INSTANCE);

        //Add more nameBuilder.addRDN(....) statements to support more X500 attributes.
        nameBuilder.addRDN(RFC4519Style.cn, commonName);

        //Turn the crank
        return nameBuilder.build();
    }

    protected JcaX509CertificateConverter getCertificateConverter() {
        return new JcaX509CertificateConverter().setProvider(BC);
    }
}