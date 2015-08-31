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
 **/

package ddf.security.certificate.generator;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.RFC4519Style;
import org.bouncycastle.jcajce.provider.asymmetric.x509.CertificateFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * This class is a home for helper functions that were did not belong to other classes.
 */


// Considered making more wrapper classes-- for example, KeyWrapper that would add methods like
//  "asString" or static method "fromString" to create new instances. But there
//  are already too many classes and too many wrappers. Ultimately, I couldn't justify the
//    added complexity of that approach.

public class PkiTools {

    public static final int RSA_KEY_LENGTH = 2048;
    public static final String ALGORITHM = "RSA";
    private static final Logger LOGGER = LoggerFactory.getLogger(KeyStoreFile.class);

    /**
     * Convert a byte array to a Java String.
     *
     * @param bytes DER encoded bytes
     * @return PEM encoded bytes
     */
    public static String bytesToBase64String(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Convert a Java String to a byte array
     *
     * @param string PEM encoded bytes
     * @return DER encoded bytes
     */
    public static byte[] base64StringToBytes(String string) {
        return Base64.getDecoder().decode(string);
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

        return InetAddress.getLocalHost().getCanonicalHostName();

    }

    /**
     * Given an X509 certificate, return a PEM encoded string representation of the certificate.
     *
     * @param cert certificate
     * @return PEM encoded String
     * @throws CertificateEncodingException
     */
    public static String certificateToString(X509Certificate cert) throws CertificateEncodingException {
        return bytesToBase64String(cert.getEncoded());
    }

    public static X509Certificate stringToCertificate(String certString) throws CertificateException {
        CertificateFactory cf = new CertificateFactory();
        ByteArrayInputStream in = new ByteArrayInputStream(base64StringToBytes(certString));
        return (X509Certificate) cf.engineGenerateCertificate(in);
    }

    /**
     * Convert a Java String to an  private key
     *
     * @param keyString encoded RSA private key. Assume PKCS#8 format
     * @return Instance of PrivateKey
     * @throws CertificateGeneratorException Raise exception if conversion was not successful
     */
    public static PrivateKey stringToPrivateKey(String keyString) throws CertificateGeneratorException {
        try {
            return getRsaKeyFactory().generatePrivate(new PKCS8EncodedKeySpec(base64StringToBytes(keyString)));
        } catch (Exception e) {
            throw new CertificateGeneratorException("Could not convert String to Private Key", e.getCause());
        }
    }

    /**
     * Create an X500 name with a single populated attribute, the "common name". An X500 name object details the
     * identity of a machine, person, or organization. The name object is used as the "subject" of a certificate.
     * SSL/TLS typically uses a subject's common name as the DNS name for a machine and this name must be correct
     * or SSl/TLS will not trust the machine's certificate.
     * <p>
     * TLS can use a different set of attributes to, the Subject Alternative Names. SANs are extensions to the
     * X509 specification and can include IP addresses, DNS names and other machine information. This package does
     * not use SANs.
     *
     * @param commonName the fully qualified host name of the end entity
     * @return X500 name object with common name attribute set
     * @see <a href="https://www.ietf.org/rfc/rfc4514.txt">RFC 4514, section 'LDAP: Distinguished Names'</a>
     * @see <a href="https://tools.ietf.org/html/rfc4519">RFC 4519 details the exact construction of distinguished names</a>
     * @see <a href="https://en.wikipedia.org/wiki/SubjectAltName">Subject Alternative Names on Wikipedia'</a>
     */
    public static X500Name makeDistinguishedName(String commonName) {

        if (commonName == null) {
            throw new IllegalArgumentException("Certificate common name cannot be null");
        }

        if (commonName.isEmpty()) {
            LOGGER.warn("Setting certificate common name to empty string. This could result in an unusable TLS certificate.");
        }

        X500NameBuilder nameBuilder = new X500NameBuilder(RFC4519Style.INSTANCE);

        //Add more nameBuilder.addRDN(....) statements to support more X500 attributes.
        nameBuilder.addRDN(RFC4519Style.cn, commonName);

        return nameBuilder.build();
    }


    /**
     * @param key object
     * @return PEM encoded string represents the bytes of the key
     */
    public static String keyToString(Key key) {
        return bytesToBase64String(key.getEncoded());
    }

    /**
     * Generate new RSA public/private key pair with 2048 bit key
     *
     * @return new generated key pair
     * @throws CertificateGeneratorException
     */
    public static KeyPair generateRsaKeyPair() throws CertificateGeneratorException {
        KeyPairGenerator keyGen;
        try {
            keyGen = KeyPairGenerator.getInstance(ALGORITHM, BouncyCastleProvider.PROVIDER_NAME);
        } catch (Exception e) {
            throw new CertificateGeneratorException("Failed to generate new public/private key pair.", e);
        }
        keyGen.initialize(RSA_KEY_LENGTH);
        return keyGen.generateKeyPair();
    }


    private static KeyFactory getRsaKeyFactory() throws GeneralSecurityException {
        return KeyFactory.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
    }

}