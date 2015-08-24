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

public class CertificateGeneratorUtilities {

    public static String BC = BouncyCastleProvider.PROVIDER_NAME;

    /**
     * Convert a byte array to a Java String.
     *
     * @param bytes DER encoded bytes
     * @return PEM encoded bytes
     */
    public static String bytesToString(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Convert a Java String to a byte array
     *
     * @param string PEM encoded bytes
     * @return DER encoded bytes
     */
    public static byte[] stringToBytes(String string) {
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

        return InetAddress.getLocalHost().getHostName();

    }

    public static String certificateToString(X509Certificate cert) throws CertificateEncodingException {
        return bytesToString(cert.getEncoded());
    }

    public static X509Certificate stringToCertificate(String certString) throws CertificateException {
        CertificateFactory cf = new CertificateFactory();
        ByteArrayInputStream in = new ByteArrayInputStream(stringToBytes(certString));
        X509Certificate cert = (X509Certificate) cf.engineGenerateCertificate(in);
        return cert;
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
            return getRsaKeyFactory().generatePrivate(new PKCS8EncodedKeySpec(stringToBytes(keyString)));
        } catch (Exception e) {
            throw new CertificateGeneratorException.InvalidKey("Could not convert String to Private Key", e.getCause());
        }
    }

    //DDF uses RSA asymmetric keys
    protected static KeyFactory getRsaKeyFactory() throws NoSuchProviderException, NoSuchAlgorithmException {
        return KeyFactory.getInstance("RSA", BC);
    }

    /**
     * Create an X500 name object from simple strings. Currently only uses the common name attribute.
     */
    public static X500Name makeDistinguishedName(String commonName) {
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

    /**
     * Convert an instance of Key to a (PEM-encoded) instance of Java String.
     */
    public String keyToString(Key key) {
        return bytesToString(key.getEncoded());
    }
}