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
package ddf.security.settings.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.configuration.security.FiltersType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.SecurityConstants;
import ddf.security.encryption.EncryptionService;
import ddf.security.settings.SecuritySettingsService;

public class SecuritySettingsServiceImpl implements SecuritySettingsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecuritySettingsServiceImpl.class);

    private static final String NO_ENCRYPT_SERVICE = "Could not get encryption service to decrypt password. Sending password AS-IS (assuming cleartext).";

    private EncryptionService encryptionService;

    private String keystorePath;

    private String keystorePassword;

    private String truststorePath;

    private String truststorePassword;

    private KeyStore keyStore;

    private KeyStore trustStore;

    private List<String> getSslAllowedAlgorithms() {
        return stringToArray(System.getProperty("org.apache.cxf.configuration.jsse.sslAllowedAlgorithms"));
    }

    private List<String> getSslDisallowedAlgorithms() {
        return stringToArray(System.getProperty("org.apache.cxf.configuration.jsse.sslDisallowedAlgorithms"));
    }

    static List<String> stringToArray(String commaDelimitedString) {
        return Arrays.asList(commaDelimitedString.split("\\s*,\\s"));

    }

    public SecuritySettingsServiceImpl(EncryptionService encryptService) {
        this.encryptionService = encryptService;
    }

    public void init() {
        String setTrustStorePath = System.getProperty(SecurityConstants.TRUSTSTORE_PATH);
        if (setTrustStorePath != null) {
            truststorePath = setTrustStorePath;
        }

        String setTrustStorePassword = System.getProperty(SecurityConstants.TRUSTSTORE_PASSWORD);
        if (setTrustStorePassword != null) {
            if (encryptionService == null) {
                LOGGER.debug("TRUSTSTORE: {}", NO_ENCRYPT_SERVICE);
                truststorePassword = setTrustStorePassword;
            } else {
                setTrustStorePassword = encryptionService.decryptValue(setTrustStorePassword);
                truststorePassword = setTrustStorePassword;
            }
        }

        trustStore = getTruststore();

        String setKeyStorePath = System.getProperty(SecurityConstants.KEYSTORE_PATH);
        if (setKeyStorePath != null) {
            keystorePath = setKeyStorePath;
        }

        String setKeyStorePassword = System.getProperty(SecurityConstants.KEYSTORE_PASSWORD);
        if (setKeyStorePassword != null) {
            if (encryptionService == null) {
                LOGGER.debug("KEYSTORE: {}", NO_ENCRYPT_SERVICE);
                keystorePassword = setKeyStorePassword;
            } else {
                setKeyStorePassword = encryptionService.decryptValue(setKeyStorePassword);
                keystorePassword = setKeyStorePassword;
            }
        }

        keyStore = getKeystore();
    }

    private KeyStore createKeyStore(String path, String password, String type) {
        KeyStore keyStore = null;
        File keyStoreFile = new File(path);
        if (keyStoreFile.exists() && StringUtils.isNotBlank(password)) {
            FileInputStream fis = null;
            try {
                keyStore = KeyStore.getInstance(type);
                fis = new FileInputStream(keyStoreFile);
                LOGGER.debug("Loading trustStore");
                keyStore.load(fis, password.toCharArray());
            } catch (KeyStoreException | CertificateException e) {
                LOGGER.warn("Issue while trying to load ");
            } catch (IOException e) {
                LOGGER.warn("Unable to load keystore file from path" + path, e);
            } catch (NoSuchAlgorithmException nsae) {
                LOGGER.warn("JVM implementation does not come with default keystore type", nsae);
            } finally {
                IOUtils.closeQuietly(fis);
            }
        }
        return keyStore;
    }

    @Override
    public TLSClientParameters getTLSParameters() {
        TLSClientParameters tlsParams = new TLSClientParameters();
        try {
            TrustManagerFactory trustFactory = TrustManagerFactory
                    .getInstance(System.getProperty("javax.net.ssl.trustManagerAlgorithm"));
            trustFactory.init(trustStore);
            TrustManager[] tm = trustFactory.getTrustManagers();
            tlsParams.setTrustManagers(tm);

            KeyManagerFactory keyFactory = KeyManagerFactory
                    .getInstance(System.getProperty("javax.net.ssl.keyManagerAlgorithm"));
            keyFactory.init(keyStore, keystorePassword.toCharArray());
            KeyManager[] km = keyFactory.getKeyManagers();
            tlsParams.setKeyManagers(km);
        } catch (NoSuchAlgorithmException | KeyStoreException | UnrecoverableKeyException e) {
            LOGGER.warn(
                    "Could not fully load keystore/truststore into TLSParameters. Parameters may not be fully functional.",
                    e);
        }

        FiltersType filter = new FiltersType();
        filter.getInclude().addAll(getSslAllowedAlgorithms());
        filter.getExclude().addAll(getSslDisallowedAlgorithms());
        filter.getInclude().addAll(SecurityConstants.SSL_ALLOWED_ALGORITHMS);
        filter.getExclude().addAll(SecurityConstants.SSL_DISALLOWED_ALGORITHMS);
        tlsParams.setCipherSuitesFilter(filter);

        return tlsParams;
    }

    @Override
    public KeyStore getKeystore() {
        return createKeyStore(keystorePath, keystorePassword,
                System.getProperty(SecurityConstants.KEYSTORE_TYPE));
    }

    @Override
    public KeyStore getTruststore() {
        return createKeyStore(truststorePath, truststorePassword,
                System.getProperty(SecurityConstants.TRUSTSTORE_TYPE));
    }
}
