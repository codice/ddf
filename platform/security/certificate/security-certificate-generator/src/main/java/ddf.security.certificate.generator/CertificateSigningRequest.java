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

import java.math.BigInteger;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * Model of a X509 certificate signing request. These values must be set:
 * <p><ul>
 * <li>notAfter - certificate expiration date</li>
 * <li>subjectName - typically a server's FQDN. Use {@link #setCommonNameToHostname } to automatically set this attribute to the server's FQDN</li>
 * <li>certificateAuthority - instance of {@link ddf.security.certificate.generator.CertificateAuthority} who will signed this request</li>
 * </ul><p>
 * These values may be optionally set:
 * <p><ul>
 * <li>serialNumber - arbitrary serial number</li>
 * </ul>
 *
 * @see <a href="https://www.ietf.org/rfc/rfc4514.txt">RFC 5280,  Internet X.509 Public Key Infrastructure Certificat</a>
 */
public class CertificateSigningRequest {

    static {

        Security.addProvider(new BouncyCastleProvider());
    }

    protected DateTime notBefore;
    protected DateTime notAfter;
    protected X500Name subjectName;
    protected KeyPair subjectKeyPair;
    protected BigInteger serialNumber;
    protected CertificateAuthority certificateAuthority;
    protected X509Certificate signedCertificate;
    private PkiTools pkiTools = new PkiTools();

    public CertificateSigningRequest() {
        initialize();
    }

    //Set reasonable defaults
    protected void initialize() {
        serialNumber = BigInteger.valueOf(System.currentTimeMillis());
        notBefore = DateTime.now().minusDays(1);
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
            throw new CertificateGeneratorException(
                    "Missing certificate validity date. " +
                            "Set the Not After attribute to specify certificate's expiration date");
        }

        //This can only happen if client code explicitly sets the value to null because the attribute is initialized at creation time.
        if (notBefore == null) {
            throw new CertificateGeneratorException(
                    "Missing certificate validity date. " +
                            "Set the Not After attribute to specify certificate's expiration date");
        }

        if (notAfter.isBefore(notBefore)) {
            throw new CertificateGeneratorException(
                    String.format("Certificate 'Not After' (expiration) of %s must be later than 'Not Before' (effective) of %s", notBefore, notAfter));
        }

        if (subjectKeyPair == null) {
            //Typical use case is to generate both the public and private keys within this class.
            //ASSUME that if the keypair has not been set, the user's intention is to generate a new keypair.

            KeyPair keyPair = pkiTools.generateRsaKeyPair();
            useSubjectKeyPair(keyPair);
        }

        if (subjectName == null || subjectName.toString().isEmpty()) {
            throw new CertificateGeneratorException(
                    "Subject distinguished name is null or empty. " +
                            "Set subject to host name or or set subject distinguished name");
        }

        if (certificateAuthority == null) {
            throw new CertificateGeneratorException("Certificate authority is null");
        }

        X509v3CertificateBuilder certificateBuilder = getCertificateBuilder(certificateAuthority.getCertificate(), serialNumber, notBefore.toDate(), notAfter.toDate(), subjectName, subjectKeyPair.getPublic());

        X509CertificateHolder holder =
                certificateBuilder.build(certificateAuthority.getContentSigner());

        try {
            signedCertificate = getCertificateConverter().getCertificate(holder);
        } catch (CertificateException e) {
            throw new CertificateGeneratorException("Could not create signed certificate.", e.getCause());
        }
    }

    JcaX509v3CertificateBuilder getCertificateBuilder(X509Certificate certificate, BigInteger serialNumber, Date notBefore, Date notAfter, X500Name subjectName, PublicKey key) {
        return new JcaX509v3CertificateBuilder(
                certificate,
                serialNumber,
                notBefore,
                notAfter,
                subjectName,
                key);
    }


    /**
     * Create a distinguished name for the certificate's subject. Currently, only the common name (sub-attribute of
     * the distinguished name) is supported. The common name should be the <b>fully qualified domain name</b> of the
     * certificate's subject (i.e. the server). For example, <i>server.subdomain.domain.tld</i>.
     *
     * @param name subject's common name attribute (
     */
    public void setCommonName(String name) {
        subjectName = pkiTools.makeDistinguishedName(name);

    }

    /**
     * Use hostname of the network node running the JBM as the subject's common name.
     * The most common use case is the DNS name of the server is the X509 common name.
     * SSL/TLS uses the common name as part of the secure connection handshake and if the name is wrong, TLS will
     * not trust the connection. There is no guarantee that the {@link PkiTools#getHostName()}
     * returns the correct result in all cases. Use {@link #setCommonName(String)} to manually set
     * the common name for the certificate's subject.
     *
     * @throws UnknownHostException
     */
    public void setCommonNameToHostname() throws UnknownHostException {
        setCommonName(pkiTools.getHostName());
    }

    /**
     * Set the subject's public and private keypair. Only the public key is used. However, the the most common use
     * case is to generate a new keypair when creating a certificate. The client code will need to retrieve the
     * the private key for later use. This method is provided for situations when an existing  keypair
     * should be used to generate the certificate.
     *
     * @param keyPair RSA public/private keys
     */
    public void useSubjectKeyPair(KeyPair keyPair) {
        subjectKeyPair = keyPair;
    }

    /**
     * The validity period for a certificate is the period of time from notBefore through notAfter, inclusive.
     *
     * @param date expiration date of the certificate
     */
    public void setNotBefore(DateTime date) {
        notBefore = date;
    }

    /**
     * The validity period for a certificate is the period of time from notBefore through notAfter, inclusive.
     *
     * @param date effective date
     */
    public void setNotAfter(DateTime date) {
        notAfter = date;
    }

    /**
     * Set the serial number of the certificate. The serial number is arbitrary, but should not be negative.
     *
     * @param number arbitrary serial number
     */
    public void setSerialNumber(long number) throws CertificateGeneratorException {

        if (number < 0) {
            throw new IllegalArgumentException("Serial number for X.509 certificate should not be negative");
        }
        serialNumber = BigInteger.valueOf(number);
    }

    JcaX509CertificateConverter getCertificateConverter() {
        return new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);
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

    public PublicKey getPublicKey() {
        return subjectKeyPair.getPublic();
    }
}