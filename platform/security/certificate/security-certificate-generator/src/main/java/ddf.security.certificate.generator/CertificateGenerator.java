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
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class CertificateGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CertificateGenerator.class);
    private static String BC = BouncyCastleProvider.PROVIDER_NAME;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    protected org.joda.time.DateTime effectiveDate;
    protected org.joda.time.DateTime expirationDate;
    protected PrivateKey issuerPrivateKey;
    protected X509Certificate issuerCert;
    protected X509Certificate signedCert;
    protected int keyPairLength;
    protected X500Name subjectDistinguishedName;
    protected KeyPair subjectKeyPair;
    protected BigInteger serialNumber;
    protected Boolean useNewKeys;
    protected X509v3CertificateBuilder certificateSigningRequest;

    CertificateGenerator() {
        initialize();
    }

    public static String getHostName() throws UnknownHostException {

        return InetAddress.getLocalHost().getHostName();

    }

    //Set reasonable defaults
    protected void initialize() {
        keyPairLength = 1024;
        serialNumber = BigInteger.valueOf(System.currentTimeMillis());
        effectiveDate = DateTime.now().minusDays(1);
        useNewKeys = false;

        //TODO: It might not be a good idea to pick the end effective date for the user.
        // expirationDate = DateTime.now().plusYears(5);

    }

    public CertificateGenerator build() throws NoSuchProviderException, NoSuchAlgorithmException, CertificateException, OperatorCreationException {
        //Validate inputs. Throw exceptions as necessary.
        //Generate signed certificate.
        if (useNewKeys) {
            generateNewKeys();
        }

        //Create and sign the Certificate Signing Request
        createCsr().signCsr();

        //fluid interface
        return this;
    }

    protected CertificateGenerator createCsr() {
        certificateSigningRequest = new JcaX509v3CertificateBuilder(
                issuerCert,
                serialNumber,
                effectiveDate.toDate(),
                expirationDate.toDate(),
                subjectDistinguishedName,
                subjectKeyPair.getPublic());

        //fluid interface
        return this;
    }

    protected CertificateGenerator signCsr() throws OperatorCreationException, CertificateException {

        X509CertificateHolder holder = certificateSigningRequest.build(jcaSigner());
        signedCert = jcaConverter().getCertificate(holder);

        //fluid interface
        return this;
    }

    //Sign the certificate

    public PrivateKey getSubjectPrivateKey() {
        return subjectKeyPair.getPrivate();
    }

    protected void generateNewKeys() throws NoSuchProviderException, NoSuchAlgorithmException {

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA", BC);
        keyGen.initialize(keyPairLength);
        setSubjectKeyPair(keyGen.generateKeyPair());
    }

    public CertificateGenerator useNewKeys() {
        useNewKeys = true;

        //fluid interface
        return this;
    }

    // For a Subject, the common name should be the server's DNS name.
    public CertificateGenerator setSubjectDistinguishedName(String name) {
        subjectDistinguishedName = makeDistinguishedName(name);

        //fluid interface
        return this;
    }

    public CertificateGenerator setSubjectToHostname() throws UnknownHostException {
        return setSubjectDistinguishedName(getHostName());
    }

    public CertificateGenerator setSubjectKeyPair(KeyPair keyPair) {
        subjectKeyPair = keyPair;
        return this;
    }

    public CertificateGenerator setIssuerPrivateKey(PrivateKey privateKey) {
        issuerPrivateKey = privateKey;
        return this;
    }

    public CertificateGenerator setIssuerCert(X509Certificate cert) {
        issuerCert = cert;
        return this;
    }

    public CertificateGenerator setEffectiveDate(org.joda.time.DateTime date) {
        effectiveDate = date;
        return this;
    }

    public CertificateGenerator setExpirationDate(org.joda.time.DateTime date) {
        expirationDate = date;
        return this;
    }

    public CertificateGenerator setSerialNumber(BigInteger number) {
        serialNumber = number;
        return this;
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

    protected JcaX509CertificateConverter jcaConverter() {
        return new JcaX509CertificateConverter().setProvider(BC);
    }

    protected ContentSigner jcaSigner() throws OperatorCreationException {
        return new JcaContentSignerBuilder("SHA256WithRSAEncryption").setProvider(BC).build(issuerPrivateKey);
    }

}