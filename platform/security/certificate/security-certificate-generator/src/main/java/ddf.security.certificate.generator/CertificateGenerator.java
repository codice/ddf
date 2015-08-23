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
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class CertificateGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CertificateGenerator.class);
    private static String BC = BouncyCastleProvider.PROVIDER_NAME;

    static {

        Security.addProvider(new BouncyCastleProvider());
    }

    protected org.joda.time.DateTime notBefore;
    protected org.joda.time.DateTime notAfter;
    protected PrivateKey issuerPrivateKey;
    protected X509Certificate issuerCert;
    protected X509Certificate signedCert;
    protected int keyPairLength;
    protected X500Name subjectDistinguishedName;
    protected KeyPair subjectKeyPair;
    protected BigInteger serialNumber;
    protected Boolean useNewKeys;
    protected X509v3CertificateBuilder certificateSigningRequest;
    private String pemPriv = "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBALVtFJIVYgb+07/jBZ1KXZVCxuf0hUoOMOw2vYJ8VqhS755Sf74qRcVaPm8BcrWVG80OdutXtzP+ylnO/tjmr+myxsKnpodXZcLqCzQE58rh57bFJRAJSjqJjny+JBSy0MdI3NtJS3yVmrUgZRVHdIquYBPMjxIxgRsT230F1MnfAgMBAAECgYEApZmHaUAzVgdL6J6kBUpX2WI2hIrhDxOc/D+LA4vS3Zm2NmE/UKjtPpJ84n4D4lLUKXvGeFJ8Wu16bjdOz1Thw3kfahTIqJdU4ppZ9ftUR0M1d3gEUVh1nd6zfJRGTR/knyvKInL8K0UKpSueHuMWPSLLe9nU4N1HHYfXRui4LGECQQDhpLmON3MtdJjWLulXz59tCPXOuj8Y8Tz3pSv8zVxkWIdcgNjbIQGxHRgjxVzQQcCswXCA5yXzbkHB4TljiWWFAkEAzdV8tq94i0Bt/O/j8gTd3NvmlOWUrhr2QluvHFwssx3AL9VDk6SoqTPpIpyg7FUKkjIh7dQ2dP0C6+Y90FiNEwJAf8M5naEgAjjm4T+muCXDa4WLSQaD+6d8kexgP8A39El8O5BpOYoy3wpORNLXfsP8SNUu0o4PGwrvCMxyJj4B0QJAWmMUZ/i4G5ZIdlk1pPKkJrdeEyaZ2ra2Sz+Nrwt/CYzX92lUSoJ1GhBUoUFcnUte4AIpyhF1dHwii0rI/DPWhwJAbmxNl+UM3aO82i04e0QChFJDgmoKHNxR9muYNHQ/SEj0ULyTETcqwQjdaXVx7WJRV/5KcWwEdv3h2CP8JIzwkA==";

    CertificateGenerator() {
        initialize();
    }

    /**
     * Get the host name or DNS name that Java thinks is associated with the server running the application. This
     * method is public so client code can easily check the name and decide if it should be used in the generated
     * certificate.
     *
     * @return Hostname of this machine. Hostname should be the same as the machine's DNS name.
     * @throws UnknownHostException
     */
    public static String getHostName() throws UnknownHostException {

        return InetAddress.getLocalHost().getHostName();

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

    public void build() throws NoSuchProviderException, NoSuchAlgorithmException, CertificateException, OperatorCreationException, CertificateGeneratorException.InvalidSubject {

        PemFile p;
        PrivateKey pk;
        try {
            p = PemFile.getInstance("/Users/aaronhoffer/test/ddf-2.8.0-SNAPSHOT/etc/certs/demoCA/private/cakey-nopassword.pem");
            pk = p.getPrivateKey();
            byte[] bytes = pk.getEncoded();
            KeyFactory kf = KeyFactory.getInstance("RSA", "BC");
            PKCS8EncodedKeySpec keySpec1 = new PKCS8EncodedKeySpec(bytes);
            PrivateKey key1 = kf.generatePrivate(keySpec1);

//            String test2 = Base64.getEncoder().encodeToString(bytes);
            byte[] bytes2 = Base64.getDecoder().decode(pemPriv);
            PKCS8EncodedKeySpec keySpec2 = new PKCS8EncodedKeySpec(bytes2);
            PrivateKey key2 = kf.generatePrivate(keySpec2);



            int x = 243423;


        } catch (Exception e) {
            e.printStackTrace();
        }


        //Create and sign the Certificate Signing Request
        createCsr().signCsr();

        //Use void return because this methods marks the end of the fluent interface.
    }

    protected CertificateGenerator createCsr() throws CertificateGeneratorException {
        //If effective date is not set, this will end badly
        if (notAfter == null) {
            throw new CertificateGeneratorException.InvalidDate("Missing certificate validity date. Set the Not After attribute to specify certificate's expiration date");
        }

        //This can only happen if client code explicitly sets the value to null because the attribute is initialized at creation time.
        if (notBefore == null) {
            throw new CertificateGeneratorException.InvalidDate("Missing certificate validity date. Set the Not After attribute to specify certificate's expiration date");
        }

        if (notAfter.isBefore(notBefore)) {
            throw new CertificateGeneratorException.InvalidDate("Certificate expiration date must be later than effective date.");
        }

        if (issuerCert == null) {
            throw new CertificateGeneratorException.InvalidIssuer("Issuer certificate is null. Set issuer certificate.");
        }

        if (subjectKeyPair == null) {
            throw new CertificateGeneratorException.InvalidSubject("Subject key pair is null. Set subject key pair.");
        }

        if (subjectDistinguishedName == null || subjectDistinguishedName.toString().isEmpty()) {
            throw new CertificateGeneratorException.InvalidSubject("Subject distinguished name is null or empty. Set subject to host name or or set subject distinguished name");
        }

        certificateSigningRequest = new JcaX509v3CertificateBuilder(
                issuerCert,
                serialNumber,
                notBefore.toDate(),
                notAfter.toDate(),
                subjectDistinguishedName,
                subjectKeyPair.getPublic());

        //fluent interface
        return this;
    }

    //Sign the certificate

    protected CertificateGenerator signCsr() throws OperatorCreationException, CertificateException {

        X509CertificateHolder holder = certificateSigningRequest.build(signer());
        signedCert = certificateConverter().getCertificate(holder);

        //fluent interface
        return this;
    }

    public PrivateKey getSubjectPrivateKey() {
        return subjectKeyPair.getPrivate();
    }

    protected CertificateGenerator generateNewKeys() throws NoSuchProviderException, NoSuchAlgorithmException {

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA", BC);
        keyGen.initialize(keyPairLength);
        setSubjectKeyPair(keyGen.generateKeyPair());

        //fluent interface
        return this;
    }

    // For a Subject, the common name should be the server's DNS name.
    public CertificateGenerator setSubjectDistinguishedName(String name) {
        subjectDistinguishedName = makeDistinguishedName(name);

        //fluent interface
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

    public CertificateGenerator setNotBefore(DateTime date) {
        notBefore = date;
        return this;
    }

    public CertificateGenerator setNotAfter(DateTime date) {
        notAfter = date;
        return this;
    }

    public CertificateGenerator setSerialNumber(long number) {
        serialNumber = BigInteger.valueOf(number);
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

    protected JcaX509CertificateConverter certificateConverter() {
        return new JcaX509CertificateConverter().setProvider(BC);
    }

    protected ContentSigner signer() throws OperatorCreationException, CertificateGeneratorException.InvalidIssuer {

        if (issuerPrivateKey == null) {
            throw new CertificateGeneratorException.InvalidIssuer("The issuer's prviate key cannot be null.");
        }

        return new JcaContentSignerBuilder("SHA256WithRSAEncryption").setProvider(BC).build(issuerPrivateKey);
    }

}