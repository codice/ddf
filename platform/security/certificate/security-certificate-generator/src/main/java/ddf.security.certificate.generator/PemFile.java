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

import java.io.*;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.*;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Facade wrapper for the Bouncy Castle PEMParser.
 */
public class PemFile extends SecurityFileFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(PemFile.class);

    //Declare member attributes
    protected PEMParser pem;

    //Declare password private so that a malicious subclass cannot gain access to it.
    private char[] password;

    //Do not use constructor. Use get method to instantiate this class.
    protected PemFile() {

    }

    /**
     * Create new instance. Validate file exists and has the expected format.
     *
     * @param filePath the file path to an existing PEM file. Assumes local file and not URI.
     * @param password - if the PEM file is encrypted, provide the password. If the PEM file is not encrypted,
     *                 pass null. Password is stored as a character array to keep it out of
     *                 the String pool to make it harder to recover the password from a heap dump.
     * @return fully formed instance of the class
     * @throws FileNotFoundException
     */
    public static PemFile get(String filePath, char[] password) throws FileNotFoundException {
        File file = createFileObject(filePath);
        char[] pw = formatPassword(password);
        PemFile facade = new PemFile();
        facade.pem = new PEMParser(new FileReader(file));
        return facade;
    }

    /**
     * Extract private key from the PEM file. This implementation assumes the private key is the first
     * object in the PEM file. Throw an exception if a private key cannot be extracted from the object.
     * @return PrivateKey
     * @throws IOException
     */
    public PrivateKey getPrivateKey() throws IOException {

        //Grab first object out of the PEM file.
        Object pemObject = readObject();

        //Oh for the love of God! Really?
        //o.getClass().cast(o)
        if (pemObject instanceof PEMEncryptedKeyPair)
            return extractPrivateKey((PEMEncryptedKeyPair) pemObject);
        if (pemObject instanceof PEMKeyPair)
            return extractPrivateKey((PEMKeyPair) pemObject);
        if (pemObject instanceof PrivateKeyInfo)
            return extractPrivateKey((PrivateKeyInfo) pemObject);

        //If the method has not returned, the type of PEM object read from the file is not yet supported.
        //Keep adding more if..instanceof... statements to support more PEM object types.
        throw new IOException(String.format("The PEM object type %s is not recognized as a private key", pemObject.getClass()));
    }


    public X509Certificate getCertificate() {

        return null;
    }

//    public static X509Certificate loadX509FromPEM(String filePath) {
//        X509Certificate cert = null;
//        try {
//            JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter()
//                    .setProvider("BC");
//            PEMParser pp = new PEMParser(new InputStreamReader(new FileInputStream(filePath)));
//            Object o = pp.readObject();
//            if (o instanceof X509CertificateHolder) {
//                X509CertificateHolder ch = (X509CertificateHolder) o;
//                cert = certConverter.getCertificate(ch);
//            } else {
//                LOGGER.error("File does not contain valid x509!");
//                throw new IOException();
//            }
//        } catch (CertificateException | IOException e) {
//            LOGGER.error(e.getMessage(), e);
//        }
//        return cert;
//    }


    protected Object readObject() throws IOException {
        return pem.readObject();
    }

    protected JcaPEMKeyConverter getKeyConverter() {
        return new JcaPEMKeyConverter().setProvider("BC");
    }

    protected PEMKeyPair decrypt(PEMEncryptedKeyPair kp) throws IOException {
        return kp.decryptKeyPair(new JcePEMDecryptorProviderBuilder().build(password));
    }

    protected PrivateKey extractPrivateKey(PEMEncryptedKeyPair kp) throws IOException {
        return extractPrivateKey(decrypt(kp));
    }

    protected PrivateKey extractPrivateKey(PEMKeyPair kp) throws PEMException {
        return extractKeyPair(kp).getPrivate();
    }

    protected PrivateKey extractPrivateKey(PrivateKeyInfo kp) throws PEMException {
        return getKeyConverter().getPrivateKey(kp);
    }

    protected KeyPair extractKeyPair(PEMKeyPair kp) throws PEMException {
        return getKeyConverter().getKeyPair(kp);
    }
}
