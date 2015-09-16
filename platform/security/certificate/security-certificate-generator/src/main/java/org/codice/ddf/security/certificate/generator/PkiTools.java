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

package org.codice.ddf.security.certificate.generator;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import org.apache.commons.lang.Validate;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.RFC4519Style;
import org.bouncycastle.jcajce.provider.asymmetric.x509.CertificateFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a home for helper functions that did not belong to other classes.
 */
public abstract class PkiTools {

    public static final int RSA_KEY_LENGTH = 2048;

    public static final String ALGORITHM = "RSA";

    private static final Logger LOGGER = LoggerFactory.getLogger(PkiTools.class);

    /**
     * Convert a byte array to a Java String.
     *
     * @param bytes DER encoded bytes
     * @return PEM encoded bytes
     */
    public static String derToPem(byte[] bytes) {
        Validate.isTrue(bytes != null, "Argument bytes cannot be null");
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * If input is a character array, return the character array. If input is null, return a zero length
     * character array
     *
     * @param password character array
     * @return character array
     */
    static char[] formatPassword(char[] password) {
        return password == null ? new char[0] : password;
    }

    /**
     * @param filePath path to local keystore file
     * @return instance of File
     * @throws IOException
     */
    static File createFileObject(String filePath) throws IOException {

        File file;

        if (filePath == null) {
            throw new IllegalArgumentException("File path to security file is null");
        }

        file = new File(filePath);

        if (!file.exists()) {
            throw new FileNotFoundException(
                    "Cannot find security file at " + file.getAbsolutePath());
        }

        if (!file.canRead()) {
            String msg = String
                    .format("Cannot read security file (possible file permission problem)  or %s is a directory",
                            file.getAbsolutePath());
            throw new IOException(msg);
        }

        return file;
    }

    /**
     * Given an X509 certificate, return a PEM encoded string representation of the certificate.
     *
     * @param cert certificate
     * @return PEM encoded String
     */
    public static String certificateToPem(X509Certificate cert) {
        Validate.isTrue(cert != null, "Certificate cannot be null");
        try {
            return derToPem(cert.getEncoded());
        } catch (RuntimeException | CertificateEncodingException e) {
            throw new CertificateGeneratorException(
                    "Unable to convert the certificate to a PEM object", e);
        }
    }

    /**
     * Given a byte array that represents a DER encoded X509 certificate, return the certificate object
     *
     * @param certDer byte array representing a DER encoded X509 certificate
     * @return instance of X509 certificate
     */
    public static X509Certificate derToCertificate(byte[] certDer) {
        return PkiTools.pemToCertificate(derToPem(certDer));
    }

    /**
     * Given a byte array that represents a DER encoded private key, return the private key object
     *
     * @param privateKeyDer byte array representing a DER encoded private key
     * @return instance of private key
     */
    public static PrivateKey derToPrivateKey(byte[] privateKeyDer) {
        return PkiTools.pemToPrivateKey(derToPem(privateKeyDer));
    }

    /**
     * Get the host name or DNS name associated with the machine running the JVM. This
     * method is public so client code can easily check the name and decide if it should be used in the generated
     * certificate.
     *
     * @return String. Hostname of this machine. Hostname should be the same as the machine's DNS name.
     */
    public static String getHostName() {
        //getCannonicalHostName returns the IP address. getHostName is the closet Java method to getting
        // the FQDN.
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new CertificateGeneratorException("Cannot get this machine's host name", e);
        }
    }

    /**
     * Generate new RSA public/private key pair with 2048 bit key
     *
     * @return new generated key pair
     * @throws CertificateGeneratorException
     */
    public static KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator
                    .getInstance(ALGORITHM, BouncyCastleProvider.PROVIDER_NAME);
            keyGen.initialize(RSA_KEY_LENGTH);
            return keyGen.generateKeyPair();
        } catch (Exception e) {
            throw new CertificateGeneratorException(
                    "Failed to generate new public/private key pair.", e);
        }
    }

    /**
     * Serialize a Key object as a DER encoded byte array.
     *
     * @param key instance of Key object
     * @return byte[]
     */
    public static byte[] keyToDer(Key key) {
        Validate.isTrue(key != null, "Key cannot be null");
        return pemToDer(keyToPem(key));
    }

    /**
     * @param key object
     * @return PEM encoded string represents the bytes of the key
     */
    public static String keyToPem(Key key) {
        Validate.isTrue(key != null, "Key cannot be null");
        return derToPem(key.getEncoded());
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
        Validate.isTrue(commonName != null, "Certificate common name cannot be null");

        assert commonName != null;
        if (commonName.isEmpty()) {
            LOGGER.warn(
                    "Setting certificate common name to empty string. This could result in an unusable TLS certificate.");
        }

        X500NameBuilder nameBuilder = new X500NameBuilder(RFC4519Style.INSTANCE);

        //Add more nameBuilder.addRDN(....) statements to support more X500 attributes.
        nameBuilder.addRDN(RFC4519Style.cn, commonName);

        return nameBuilder.build();
    }

    /**
     * Given a PEM encoded X509 certificate, return an object representation of the certificate
     *
     * @param certString PEM encoded X509 certificate
     * @return instance of X509 certificate
     */
    public static X509Certificate pemToCertificate(String certString) {
        CertificateFactory cf = new CertificateFactory();
        ByteArrayInputStream in = new ByteArrayInputStream(PkiTools.pemToDer(certString));
        X509Certificate cert = null;
        try {
            cert = (X509Certificate) cf.engineGenerateCertificate(in);
        } catch (CertificateException e) {
            throw new CertificateGeneratorException(
                    "Cannot convert this PEM object to X509 certificate", e);
        }
        if (cert == null) {
            throw new CertificateGeneratorException(
                    "Cannot convert this PEM object to X509 certificate");
        }
        return cert;
    }

    /**
     * Convert a Java String to a byte array
     *
     * @param string PEM encoded bytes
     * @return DER encoded bytes
     */
    public static byte[] pemToDer(String string) {
        Validate.isTrue(string != null, "PEM string cannot be null");
        return Base64.getDecoder().decode(string);
    }

    /**
     * Convert a Java String to an private key
     *
     * @param keyString encoded RSA private key. Assume PKCS#8 format
     * @return Instance of PrivateKey
     */
    public static PrivateKey pemToPrivateKey(String keyString) {
        try {
            return PkiTools.getRsaKeyFactory()
                    .generatePrivate(new PKCS8EncodedKeySpec(pemToDer(keyString)));
        } catch (Exception e) {
            throw new CertificateGeneratorException("Could not convert String to Private Key",
                    e.getCause());
        }
    }

    static KeyFactory getRsaKeyFactory() throws GeneralSecurityException {
        return KeyFactory.getInstance(ALGORITHM, BouncyCastleProvider.PROVIDER_NAME);
    }
}