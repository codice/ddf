/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.certificate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JKSManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(JKSManager.class);

    /*---------------------------------------------------------------------
    |  Method: saveCertToJKSKeyStore
    |
    |  Purpose:  Saves a certificate to an existing keystore file,
    |
    |  Pre-condition:  A valid keystore file
    |
    |  Post-condition: The keystore file will contain the certificate passed in a parameter (certficateChain)
    |
    |  Parameters:
    |        String  keyStorePath    -- path to the keyStore
    |        String  keyStorePassword   -- password for the keystore
    |        String  alias           -- alias for the new keystore entry
    |        String keyPassword      -- password to protect the key
    |        X509Certificate[] certChain -- the certificate chain for the corresponding public key
    |        PrivateKey privKey      -- privateKey to be associated with the alias
    |
    |  Returns:  None
    *-------------------------------------------------------------------*/

    public static void addCertToKeyStore(String keyStorePath, String keyStorePassword, String alias,
            String keyPassword, X509Certificate[] certChain, PrivateKey privKey) {
        try {
            KeyStore jks = KeyStore.getInstance("JKS");
            jks.load(new FileInputStream(keyStorePath), keyStorePassword.toCharArray());
            jks.setKeyEntry(alias, privKey, keyPassword.toCharArray(), certChain);
            jks.store(new FileOutputStream(keyStorePath), keyStorePassword.toCharArray());
        } catch (CertificateException | NoSuchAlgorithmException | IOException | KeyStoreException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * ---------------------------------------------------------------------
     * Method: deleteCertFromKeyStore
     * <p/>
     * Purpose:  Deletes the certificate with the given alias from the keystore.
     * <p/>
     * Pre-condition:  Keystore contains a certificate with the given alias
     * <p/>
     * Post-condition: The keystore file that is output will contain the certificate passed in as a parameter
     *
     * @param keyStorePath     path to the keyStore
     * @param keyStorePassword password for the keystore
     * @param alias            alias of the certificate to delete
     *                         -------------------------------------------------------------------
     */

    public static void removeCertFromKeyStore(String keyStorePath, String keyStorePassword,
            String alias) {
        try {
            KeyStore jks = KeyStore.getInstance("JKS");
            jks.load(new FileInputStream(keyStorePath), keyStorePassword.toCharArray());
            jks.deleteEntry(alias);
            jks.store(new FileOutputStream(keyStorePath), keyStorePassword.toCharArray());
        } catch (CertificateException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * ---------------------------------------------------------------------
     * Method: saveX509toNewKeystore
     * <p/>
     * Purpose:  Creates a new keystore file and saves an X509 certificate to it.
     * <p/>
     * Pre-condition:  None
     * <p/>
     * Post-condition: The keystore file that is output will contain the certificate passed in as a parameter
     *
     * @param x509       certificate to be saved to the new keystore
     * @param privateKey the key to be associated with the alias
     * @param fileName   the name of the file to save
     * @param alias      alias name
     * @param password   the password to protect the key
     *                   -------------------------------------------------------------------
     */

    public static void saveX509toNewKeystore(X509Certificate x509, PrivateKey privateKey,
            String fileName, String alias, String password) {
        try {
            privateKey.getEncoded();
            KeyStore keyStore = KeyStore.getInstance("jks");
            keyStore.load(null, null);
            keyStore.setKeyEntry(alias, privateKey, password.toCharArray(),
                    new java.security.cert.Certificate[] {x509});
            keyStore.store(new FileOutputStream(new File(fileName)), password.toCharArray());
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * ---------------------------------------------------------------------
     * Method: createNewKeyStore
     * <p/>
     * Purpose:  Creates a new keystore file with the given name
     * <p/>
     * Pre-condition:  None
     * <p/>
     * Post-condition: The keystore file is created
     *
     * @param keyStoreName the name of the keystore file that will be output
     * @param password     the password for the new keystore
     *                     -------------------------------------------------------------------
     */

    public static void createNewKeyStore(String keyStoreName, String password) {
        try {
            KeyStore jks = KeyStore.getInstance("jks");
            jks.load(null, null);
            jks.store(new FileOutputStream(new File(keyStoreName)), password.toCharArray());
        } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * ---------------------------------------------------------------------
     * Method: getAliases
     * <p/>
     * Purpose:  Creates a new keystore file with the given name
     * <p/>
     * Pre-condition:  None
     * <p/>
     * Post-condition: The keystore file is created
     *
     * @param keyStorePath the path to the keystore file
     * @param password     the password for the keystore
     * @return Enumeration<String> that represents all aliases found in the keystore file
     * -------------------------------------------------------------------
     */

    public static Enumeration<String> getAliases(String keyStorePath, String password) {
        Enumeration<String> aliases = null;
        try {
            KeyStore jks = KeyStore.getInstance("jks");
            jks.load(new FileInputStream(keyStorePath), password.toCharArray());
            aliases = jks.aliases();
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return aliases;
    }

    /**
     * ---------------------------------------------------------------------
     * Method: getKeyStore
     * <p/>
     * Purpose:  Given a filename and password, returns a KeyStore object for the given keystore
     * <p/>
     * Pre-condition:  None
     * <p/>
     * Post-condition: The keystore file is created
     *
     * @param keyStorePath the path to the keystore file
     * @param password     the password for the keystore
     * @return KeyStore object for the given keystore
     * -------------------------------------------------------------------
     */

    public static KeyStore getKeyStore(String keyStorePath, String password) {
        KeyStore jks = null;
        try {
            jks = KeyStore.getInstance("jks");
            jks.load(new FileInputStream(keyStorePath), password.toCharArray());
        } catch (IOException | NoSuchAlgorithmException | KeyStoreException | CertificateException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return jks;
    }

}
