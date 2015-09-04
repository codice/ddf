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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.List;


/**
 * Facade class for a Java Keystore (JKS) file. Exposes a few high-level behaviors to abstract away the
 * complexity of JCA/JCE, as well as file I/O operations.
 **/
public class KeyStoreFile extends SecurityFileFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeyStoreFile.class);
    protected KeyStore keyStore;
    protected File file;
    private char[] password;

    /**
     * Load instance of a keystore file from local storage and return a fully initialized instance of the class.
     * Use a factory method to allow for future possibility of creating an instance of the class that is not already
     * a file.
     *
     * @param filePath path to the keystore file
     * @param password password to unlock the keystore file
     * @return instance of KeyStoreFile
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public static KeyStoreFile newInstance(String filePath, char[] password) throws IOException, GeneralSecurityException {

        KeyStoreFile facade = new KeyStoreFile();
        File file;
        KeyStore keyStore = newKeyStore();

        file = createFileObject(filePath);

        char[] pw = formatPassword(password);

        try (FileInputStream resource = new FileInputStream(file)) {
            keyStore.load(resource, pw);
        }

        facade.file = file;
        facade.keyStore = keyStore;
        facade.password = pw;
        return facade;
    }

    static KeyStore newKeyStore() throws KeyStoreException {

        String type = System.getProperty("javax.net.ssl.keyStoreType");

        if (type == null) {
            type = KeyStore.getDefaultType();
            LOGGER.info("System property javax.net.ssl.keyStoreType not set. Using default keyStore type " + type);
        }

        return KeyStore.getInstance(type);
    }

    /**
     * Create an instance of private key entry class and add it to the keyStore.
     * Oracle's Java documentation describes a private key entry:
     * This type of entry holds a cryptographic PrivateKey, which is optionally stored in a protected format
     * to prevent unauthorized access. It is also accompanied by a certificate chain for the corresponding
     * public key.<p>
     * NOTE: The private key will be encrypted with THE SAME PASSWORD THAT DECRYPTS THE KEYSTORE.
     */
    public void addEntry(String alias, PrivateKeyEntry pkEntry) {
        try {
            addEntry(alias, pkEntry.getSubjectPrivateKey(), pkEntry.getCertificateChain());
        } catch (KeyStoreException e) {
            throw new CertificateGeneratorException("Failed to store entry in keystore", e);
        }
    }

    /**
     * Return the aliases of the items in the key store. Return null if there is an error.
     *
     * @return List of aliases in keystore or null
     */
    public List<String> aliases() throws KeyStoreException {

        return Collections.list(keyStore.aliases());
    }

    /**
     * Return the certificate associated with this alias. if the certificate cannot retrieved, return null
     *
     * @param alias the name of entry in the keystore
     * @return Certificate or null
     */
    public Certificate getCertificate(String alias) {

        Certificate cert = null;

        try {
            cert = keyStore.getCertificate(alias);
        } catch (KeyStoreException e) {
            LOGGER.warn("Could get retrieve certificate named {}", alias);
        }

        return cert;
    }

    /**
     * Return the certificate chain at the given alias. Caller is responsible for verifying the alias is correct.
     *
     * @param alias name of entry in keystore
     * @return array of certificates
     */
    public Certificate[] getCertificateChain(String alias) {
        Certificate[] chain = null;
        try {
            chain = keyStore.getCertificateChain(alias);

        } catch (KeyStoreException e) {
            LOGGER.warn(String.format("Failed to recover certificate chain with alias '%s'", alias));
        }

        return chain;
    }

    /**
     * Attempt to recover a private key from the keystore.
     * ASSUMES the key is encrypted with the same password that encrypts the key store.
     * If key cannot be recovered (key is missing, password is incorrect, encryption is too strong, or other),
     * return null
     *
     * @param alias the name of the entry in the keystore
     * @return instance of PrivateKey or null
     */
    public PrivateKey getPrivateKey(String alias) {
        try {
            return (PrivateKey) keyStore.getKey(alias, password);
        } catch (Exception e) {
            LOGGER.warn("Failed to recover key named '{}'", alias);

        }
        return null;
    }

    /**
     * Remove the key store entry at the given alias. If the alias does not exist, log that it does not exist.
     *
     * @param alias the name of the entry in the keystore
     */
    public void removeEntry(String alias) {
        try {
            keyStore.deleteEntry(alias);
        } catch (KeyStoreException e) {
            LOGGER.info("Attempted to remove key named '{}' from keyStore. No such such key ", alias);
        }
    }

    /**
     * Save the keyStore to the original file and encrypt it with the original password.
     *
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public void save() throws GeneralSecurityException, IOException {

        //Use the try-with-resources statement. If an exception is raised, rethrow the exception.
        try (FileOutputStream fd = new FileOutputStream(file)) {
            keyStore.store(fd, password);
        }
    }

    void addEntry(String alias, PrivateKey privateKey, Certificate[] chain) throws KeyStoreException {

        keyStore.setKeyEntry(alias, privateKey, password, chain);
    }
}
