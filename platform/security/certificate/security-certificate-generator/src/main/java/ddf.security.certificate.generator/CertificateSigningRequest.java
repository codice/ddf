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
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.X509Certificate;

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

    public static final int VALID_YEARS = 100;
    private static final Logger LOGGER = LoggerFactory.getLogger(CertificateSigningRequest.class);

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    protected DateTime notBefore;
    protected DateTime notAfter;
    protected X500Name subjectName;
    protected KeyPair subjectKeyPair;
    protected BigInteger serialNumber;
    PkiTools pkiTools = new PkiTools();

    public CertificateSigningRequest() {
        initialize();
    }

    public PrivateKey getSubjectPrivateKey() {
        return subjectKeyPair.getPrivate();
    }

    public PublicKey getSubjectPublicKey() {
        return subjectKeyPair.getPublic();
    }


    /**
     * Create a distinguished name for the certificate's subject. Currently, only the common name (sub-attribute of
     * the distinguished name) is supported. The common name should be the <b>fully qualified domain name</b> of the
     * certificate's subject (i.e. the server). For example, <i>server.subdomain.domain.tld</i>.
     *
     * @param name subject's common name attribute (
     */
    public void setCommonName(String name) {
        Validate.notNull("Subject common name of certificate signing request cannot be null");
        subjectName = pkiTools.makeDistinguishedName(name);
    }

    /**
     * Use hostname of the network node running the JBM as the subject's common name.
     * The most common use case is the DNS name of the server is the X509 common name.
     * SSL/TLS uses the common name as part of the secure connection handshake and if the name is wrong, TLS will
     * not trust the connection. There is no guarantee that the {@link PkiTools#getHostName()}
     * returns the correct result in all cases. Use {@link #setCommonName(String)} to manually set
     * the common name for the certificate's subject.
     */
    public void setCommonNameToHostname() {
        String hname = pkiTools.getHostName();
        LOGGER.info("Creating X509 certificate CN=%s", hname);
        setCommonName(pkiTools.getHostName());
    }

    /**
     * The validity period for a certificate is the period of time from notBefore through notAfter, inclusive.
     *
     * @param date expiration date of the certificate
     */
    public void setNotBefore(DateTime date) {
        Validate.notNull(date, "Certificate 'not before' date cannot be null");
        Validate.isTrue(date.isBefore(notAfter), "Certificate 'not after' date must come after 'not before' date");
        notBefore = date;
    }

    /**
     * The validity period for a certificate is the period of time from notBefore through notAfter, inclusive.
     *
     * @param date effective date
     */
    public void setNotAfter(DateTime date) {
        Validate.notNull(date, "Certificate 'not after' date cannot be null");
        Validate.isTrue(date.isAfter(notBefore), "Certificate 'not after' date must come after 'not before' date");
        notAfter = date;
    }

    /**
     * Set the serial number of the certificate. The serial number is arbitrary, but should not be negative.
     *
     * @param number arbitrary serial number
     */
    public void setSerialNumber(long number) {
        Validate.isTrue(number > 0, "Serial number for X509 certificate should not be negative");
        serialNumber = BigInteger.valueOf(number);
    }

    /**
     * Set the subject's public and private keypair. Only the public key is used. However, the the most common use
     * case is to generate a new keypair when creating a certificate. The client code will need to retrieve the
     * the private key for later use. This method is provided for situations when an existing  keypair
     * should be used to generate the certificate.
     *
     * @param keyPair RSA public/private keys
     */
    public void setSubjectKeyPair(KeyPair keyPair) {
        Validate.notNull(keyPair, "Subject public/private keypair cannot be null");
        subjectKeyPair = keyPair;
    }

    JcaX509v3CertificateBuilder getCertificateBuilder(X509Certificate certificate) {
        return new JcaX509v3CertificateBuilder(
                certificate,
                serialNumber,
                notBefore.toDate(),
                notAfter.toDate(),
                subjectName,
                getSubjectPublicKey());
    }

    //Set reasonable defaults
    void initialize() {
        serialNumber = BigInteger.valueOf(System.currentTimeMillis());
        notBefore = DateTime.now().minusDays(1);
        notAfter = notBefore.plusYears(VALID_YEARS);
        subjectKeyPair = pkiTools.generateRsaKeyPair();
    }
}