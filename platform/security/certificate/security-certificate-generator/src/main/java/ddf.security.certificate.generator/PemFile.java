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

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.*;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Facade wrapper for the Bouncy Castle PEMParser. PEM encryption typically supports DES/TripleDES.
 * AES and Blowfish were added later on but not supported by all implementations.
 */
public class PemFile extends SecurityFileFacade {

    //Declare member attributes
    protected PEMParser pem;

    //Declare password private so a malicious subclass cannot gain access to it.
    private char[] password;

    //Do not use constructor publicly. Use getInstance method to instantiate this class.
    public PemFile(Reader reader, char[] password) {
        this.pem = new PEMParser(reader);
        this.password = password;
    }

    /**
     * Create new instance. Validate file exists and has the expected format. Use this factory method if the
     * PEM file is encrypted.
     *
     * @param filePath the file path to an existing PEM file. Assumes local file and not URI.
     * @param password - if the PEM file is encrypted, provide the password. If the PEM file is not encrypted,
     *                 pass null. Password is stored as a character array to keep it out of
     *                 the String pool to make it harder to recover the password from a heap dump.
     * @return fully formed instance of the class
     * @throws FileNotFoundException
     */
    public static PemFile getInstance(String filePath, char[] password) throws FileNotFoundException {
        File file = createFileObject(filePath);
        return new PemFile(new FileReader(file), password);
    }

    /**
     * Create new instance. Convenience methods for unencrypted PEM files.
     *
     * @param filePath the file path to an existing PEM file. Assumes local file and not URI.
     * @return fully formed instance of the class
     * @throws FileNotFoundException
     */
    public static PemFile getInstance(String filePath) throws FileNotFoundException {
        return getInstance(filePath, null);
    }

    /**
     * Extract an X509 certificate from the PEM file. This implementation assumes the certificate is the first object
     * in the PEM file. hrow an exception if a certificate cannot be extracted from the object.
     *
     * @return X509Certificate
     * @throws IOException
     * @throws CertificateException
     */
    public X509Certificate getCertificate() throws IOException, CertificateException {

        X509CertificateHolder holder = (X509CertificateHolder) readObject();
        return certificateConverter().getCertificate(holder);

    }

    /**
     * Extract private key from the PEM file. This implementation assumes the private key is the first
     * object in the PEM file. Throw an exception if a private key cannot be extracted from the object.
     *
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
        if (pemObject instanceof PKCS8EncryptedPrivateKeyInfo)
            return extractPrivateKey((PKCS8EncryptedPrivateKeyInfo) pemObject);

        //If the method has not returned, the type of PEM object read from the file is not yet supported.
        //Keep adding more if..instanceof... statements to support more PEM object types.
        throw new IOException(String.format("The PEM object type %s is not recognized as a private key", pemObject.getClass()));
    }

    protected PrivateKey extractPrivateKey(PKCS8EncryptedPrivateKeyInfo kp) throws IOException {
        return extractPrivateKey(decrypt(kp));
    }

    protected PrivateKey extractPrivateKey(PEMEncryptedKeyPair keyPair) throws IOException {
        return extractPrivateKey(decrypt(keyPair));
    }

    protected PrivateKey extractPrivateKey(PEMKeyPair keyPair) throws PEMException {
        return extractKeyPair(keyPair).getPrivate();
    }

    protected PrivateKey extractPrivateKey(PrivateKeyInfo keyInfo) throws PEMException {
        return pemKeyConverter().getPrivateKey(keyInfo);
    }


    protected KeyPair extractKeyPair(PEMKeyPair keyPair) throws PEMException {
        return pemKeyConverter().getKeyPair(keyPair);
    }

    protected Object readObject() throws IOException {
        return pem.readObject();
    }

    protected PEMKeyPair decrypt(PEMEncryptedKeyPair keyPair) throws IOException {
        return keyPair.decryptKeyPair(pemDecryptor());
    }

    protected PrivateKeyInfo decrypt(PKCS8EncryptedPrivateKeyInfo keyInfo) throws IOException {

        try {
            return keyInfo.decryptPrivateKeyInfo(pkcs8Decryptor());
        } catch (PKCSException e) {
            throw new IOException("Cannot decrypt private key", e);
        } catch (OperatorCreationException e) {
            throw new IOException("Cannot decrypt private key", e);
        }
    }

    private InputDecryptorProvider pkcs8Decryptor() throws OperatorCreationException {
        return new JceOpenSSLPKCS8DecryptorProviderBuilder().setProvider(BC).build(password);
    }

    private PEMDecryptorProvider pemDecryptor() {
        return new JcePEMDecryptorProviderBuilder().setProvider(BC).build(password);
    }

    protected JcaX509CertificateConverter certificateConverter() {
        return new JcaX509CertificateConverter().setProvider(BC);
    }

    protected JcaPEMKeyConverter pemKeyConverter() {
        return new JcaPEMKeyConverter().setProvider(BC);
    }
}
