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
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Facade class for a Java Keystore (JKS) file. Exposes a few high-level behaviors to abstract away the
 * complexity of JCA/JCE, as well as file I/O operations and exceptions.
 **/
public class KeyStoreFile extends SecurityFileFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeyStoreFile.class);

    //Declare password private so that a malicious subclass cannot gain access to it.
    private char[] password;

    //The object under the facade
    protected KeyStore keystore;

    //The file representing the object under the facade
    protected File file;

    //Use get method to create an instance, not constructor
    protected KeyStoreFile() {

    }

    public static KeyStoreFile get(String filePath, char[] password) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {

        //Declare working variables. After initialization, add them to created instance's state.
        KeyStoreFile facade = new KeyStoreFile();
        File file;
        KeyStore keyStore;

        //Run the gauntlet of validation!
        file = createFileObject(filePath);

        // Store state in class members and
        // convert null password to zero-length character array
        char[] pw = formatPassword(password);

        // Create new keystore object and store it
        keyStore = createSecurityObject();

        // Attempt to load the keystore to validate password.
        // Case 1: File is readable, but is not a valid keystore file.
        //  The cause of exception will be "Invalid keystore format"
        // Case 2: File is valid keystore, but password is not valid.
        // The cause of the exception will be UnrecoverableKeyException.
        try (FileInputStream resource = new FileInputStream(file)) {
            keyStore.load(resource, pw);
        }

        // The keystore should now be successfully loaded from a file.
        // Return initialized instance.
        facade.file = file;
        facade.keystore = keyStore;
        facade.password = pw;
        return facade;
    }

    /**
     * Create an instance of private key entry class and add it to the keystore.
     * Oracle's Java documentation describes a private key entry:
     * This type of entry holds a cryptographic PrivateKey, which is optionally stored in a protected format
     * to prevent unauthorized access. It is also accompanied by a certificate chain for the corresponding
     * public key.<p>
     * NOTE: The private key will be encrypted with THE SAME PASSWORD THAT DECRYPTS THE KEYSTORE.
     *
     * @param alias
     * @param chain
     * @param privateKey
     */
    public void addEntry(String alias, PrivateKey privateKey, X509Certificate[] chain) throws KeyStoreException {

        keystore.setKeyEntry(alias, privateKey, password, chain);

    }

    /**
     * Save the keystore to the original file and encrypt it with the original password.
     *
     * @throws CertificateException     a certificates included in the keystore data could not be stored
     * @throws NoSuchAlgorithmException the appropriate data integrity algorithm could not be found
     * @throws KeyStoreException        the keystore has not been initialized (loaded)
     * @throws IOException              here was an I/O problem with data
     */
    public void save() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {

        //Use the try-with-resources statement. If an exception is raised, rethrow the exception.
        try (FileOutputStream fd = new FileOutputStream(file)) {
            keystore.store(fd, password);
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Remove the key store entry at the given alias. If the alias does not exist, log that it does not exist.
     *
     * @param alias
     */
    public void removeEntry(String alias) {
        try {
            keystore.deleteEntry(alias);
        } catch (KeyStoreException e) {
            getLOGGER().info(String.format("Attempt to remove key named '%s' from keystore. No such such key ", alias), e);
        }
    }

    //Create a new instance of a KeyStore object.
    protected static KeyStore createSecurityObject() throws KeyStoreException {

        //Attempt to find the proper keystore type
        String type = System.getProperty("javax.net.ssl.keyStoreType");

        //If the keystore type is not set, log a warning and use the default keystore type.
        if (type == null) {
            type = KeyStore.getDefaultType();
            SecurityFileFacade.getLOGGER().warn("System property javax.net.ssl.keyStoreType not set. Using default keystore type " + type);
        }

        return KeyStore.getInstance(type);
    }


}
