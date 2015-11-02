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
package org.codice.ddf.security.certificate.keystore.editor;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Map;

import org.bouncycastle.cms.CMSException;

public interface KeystoreEditorMBean {

    List<Map<String, Object>> getKeystore();

    List<Map<String, Object>> getTruststore();

    void reInitializeKeystores();

    /**
     * Checks the given keystore for an alias matching the target parameter. If a matching alias
     * exists return true otherwise false
     * @param target - target alias to find
     * @param storePassword - store password for the data
     * @param data - Base64 encoded keystore data
     * @param type - Keystore type. Either PKCS12 or JKS
     * @param fileName - Name of the file the data came from
     * @return
     * @throws CertificateException
     * @throws UnrecoverableKeyException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws IOException
     * @throws NoSuchProviderException
     * @throws CMSException
     * @throws KeystoreEditor.KeystoreEditorException
     */
    boolean keystoreContainsEntry(String target, String storePassword, String data, String type,
            String fileName)
            throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException,
            KeyStoreException, IOException, NoSuchProviderException, CMSException,
            KeystoreEditor.KeystoreEditorException;

    /**
     * Adds all keystore entries in @prama data to the system keystore. If an entry with the same
     * name already exists it will be overwritten.
     * @param keyPassword - private key password
     * @param storePassword - store password for the data
     * @param data - Base64 encoded keystore data
     * @param type - Keystore type. Either PKCS12 or JKS
     * @param fileName - Name of the file the data came from
     * @throws CertificateException
     * @throws UnrecoverableKeyException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws IOException
     * @throws NoSuchProviderException
     * @throws CMSException
     * @throws KeystoreEditor.KeystoreEditorException
     */
    void addAllKeystoreEntries(String keyPassword, String storePassword, String data, String type,
            String fileName)
            throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException,
            KeyStoreException, IOException, NoSuchProviderException, CMSException,
            KeystoreEditor.KeystoreEditorException;

    void addPrivateKey(String alias, String keyPassword, String storePassword, String data,
            String type, String fileName)
            throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException,
            KeyStoreException, IOException, NoSuchProviderException, CMSException,
            KeystoreEditor.KeystoreEditorException;

    /**
     * Adds all keystore entries in @prama data to the system truststore. If an entry with the same
     * name already exists it will be overwritten.
     * @param keyPassword - private key password
     * @param storePassword - store password for the data
     * @param data - Base64 encoded keystore data
     * @param type - Keystore type. Either PKCS12 or JKS
     * @param fileName - Name of the file the data came from
     * @throws CertificateException
     * @throws UnrecoverableKeyException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws IOException
     * @throws NoSuchProviderException
     * @throws CMSException
     * @throws KeystoreEditor.KeystoreEditorException
     */
    void addAllTruststoreEntries(String keyPassword, String storePassword, String data, String type,
            String fileName)
            throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException,
            KeyStoreException, IOException, NoSuchProviderException, CMSException,
            KeystoreEditor.KeystoreEditorException;

    void addTrustedCertificate(String alias, String keyPassword, String storePassword, String data,
            String type, String fileName)
            throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException,
            KeyStoreException, IOException, NoSuchProviderException, CMSException,
            KeystoreEditor.KeystoreEditorException;

    void deletePrivateKey(String alias);

    void deleteTrustedCertificate(String alias);
}
