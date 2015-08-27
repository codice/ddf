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
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.List;


/**
 * Facade class for a Java Keystore (JKS) file. Exposes a few high-level behaviors to abstract away the
 * complexity of JCA/JCE, as well as file I/O operations.
 **/
public class KeyStoreFile extends SecurityFileFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeyStoreFile.class);
    //The object under the facade
    protected KeyStore keyStore;
    //The file representing the object under the facade
    protected File file;
    //Declare password private so that a malicious subclass cannot gain access to it.
    private char[] password;

    //Use getInstance method to create an instance, not constructor
    protected KeyStoreFile() {

    }

    /**
     * Load instance of a keystore file from local storage and return a fully initialized instance of the class.
     * Use a factory method to allow for future possibility of creating an instance of the class that is not already
     * a file.
     *
     * @param filePath path to the keystore file
     * @param password password to unlock the keystore file
     * @return instance of KeyStoreFile
     * @throws IOException
     * @throws KeyStoreException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     */
    public static KeyStoreFile getInstance(String filePath, char[] password) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {

        //Declare working variables. After initialization, add them to created instance's state.
        KeyStoreFile facade = new KeyStoreFile();
        File file;
        KeyStore keyStore;

        //Run the gauntlet of validation!
        file = createFileObject(filePath);

        // Store state in class members and
        // convert null password to zero-length character array
        char[] pw = formatPassword(password);

        // Create new keyStore object and store it
        keyStore = createSecurityObject();

        // Attempt to load the keyStore to validate password.
        // Case 1: File is readable, but is not a valid keyStore file.
        //  The cause of exception will be "Invalid keyStore format"
        // Case 2: File is valid keyStore, but password is not valid.
        // The cause of the exception will be UnrecoverableKeyException.
        try (FileInputStream resource = new FileInputStream(file)) {
            keyStore.load(resource, pw);
        }

        // The keyStore should now be successfully loaded from a file.
        // Return initialized instance.
        facade.file = file;
        facade.keyStore = keyStore;
        facade.password = pw;
        return facade;
    }

    //Create a new instance of a KeyStore object.
    protected static KeyStore createSecurityObject() throws KeyStoreException {

        //Attempt to find the proper keyStore type
        String type = System.getProperty("javax.net.ssl.keyStoreType");

        //If the keyStore type is not set, log a warning and use the default keyStore type.
        if (type == null) {
            type = KeyStore.getDefaultType();
            LOGGER.info("System property javax.net.ssl.keyStoreType not set. Using default keyStore type " + type);
        }

        return KeyStore.getInstance(type);
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
     * Create an instance of private key entry class and add it to the keyStore.
     * Oracle's Java documentation describes a private key entry:
     * This type of entry holds a cryptographic PrivateKey, which is optionally stored in a protected format
     * to prevent unauthorized access. It is also accompanied by a certificate chain for the corresponding
     * public key.<p>
     * NOTE: The private key will be encrypted with THE SAME PASSWORD THAT DECRYPTS THE KEYSTORE.
     *
     * @param alias      of entry
     * @param chain      list of certificates from the end entity to the anchor certificate
     * @param privateKey of end entity
     */
    public void addEntry(String alias, PrivateKey privateKey, Certificate[] chain) throws KeyStoreException {

        keyStore.setKeyEntry(alias, privateKey, password, chain);
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
     * Save the keyStore to the original file and encrypt it with the original password.
     *
     * @throws CertificateException     a certificates included in the keyStore data could not be stored
     * @throws NoSuchAlgorithmException the appropriate data integrity algorithm could not be found
     * @throws KeyStoreException        the keyStore has not been initialized (loaded)
     * @throws IOException              here was an I/O problem with data
     */
    public void save() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {

        //Use the try-with-resources statement. If an exception is raised, rethrow the exception.
        try (FileOutputStream fd = new FileOutputStream(file)) {
            keyStore.store(fd, password);
        } catch (Exception e) {
            throw e;
        }
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
     * @param alias
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
}
