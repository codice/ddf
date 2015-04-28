/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package org.codice.ddf.admin.insecure.defaults.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.admin.insecure.defaults.service.Alert.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeystoreValidator implements Validator {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeystoreValidator.class);

    private String keystorePassword;

    private Path keystorePath;

    private String defaultKeystorePassword;

    private String blacklistKeystorePassword;

    private Path blacklistKeystorePath;

    private KeyStore blacklistKeystore;

    private String defaultKeyPassword;

    List<Alert> alerts;

    public KeystoreValidator() {
        alerts = new ArrayList<>();
    }

    public void setKeystorePath(Path path) {
        this.keystorePath = path;
    }

    public void setKeystorePassword(String passwd) {
        this.keystorePassword = passwd;
    }

    public void setDefaultKeystorePassword(String passwd) {
        this.defaultKeystorePassword = passwd;
    }

    public void setBlacklistKeystorePath(Path path) {
        this.blacklistKeystorePath = path;
    }

    public void setBlacklistKeystorePassword(String passwd) {
        this.blacklistKeystorePassword = passwd;
    }

    public void setDefaultKeyPassword(String passwd) {
        this.defaultKeyPassword = passwd;
    }

    public List<Alert> validate() {
        alerts = new ArrayList<>();
        
        if (isInitialized()) {
            List<Certificate> blackListedCertificates = getBlackListedCertificates();
            KeyStore keystore = loadKeystore();
            if (keystore != null) {
                validateKeyPasswords(keystore);
                if (!blackListedCertificates.isEmpty()) {
                    List<Certificate[]> keystoreCertificateChains = getKeystoreCertificatesChains(keystore);
                    validateKeystoreCertificates(keystore, keystoreCertificateChains,
                            blackListedCertificates);
                }
            }
        }

        for (Alert alert : alerts) {
            LOGGER.debug("Alert: {}, {}", alert.getLevel(), alert.getMessage());
        }

        return alerts;
    }

    private boolean isInitialized() {
        int errors = 0;

        if (keystorePath == null
                || (keystorePath != null && StringUtils.isBlank(keystorePath.toString()))) {
            alerts.add(new Alert(Level.WARN,
                    "Unable to determine if keystore is using insecure defaults. No keystore path provided."));
            return false;
        }

        if (blacklistKeystorePath == null
                || (blacklistKeystorePath != null && StringUtils.isBlank(blacklistKeystorePath
                        .toString()))) {
            alerts.add(new Alert(Level.WARN, "Unable to determine if keystore ["
                    + keystorePath.toString()
                    + "] is using insecure defaults. No Blacklist keystore path provided."));
            return false;
        }

        if (StringUtils.isBlank(keystorePassword)) {
            errors++;
            alerts.add(new Alert(Level.WARN, "Unable to determine if keystore ["
                    + keystorePath.toString()
                    + "] is using insecure defaults. No keystore password provided."));
        }

        if (StringUtils.isBlank(blacklistKeystorePassword)) {
            errors++;
            alerts.add(new Alert(Level.WARN, "Unable to determine if keystore ["
                    + keystorePath.toString()
                    + "] is using insecure defaults. Password for Blacklist keystore ["
                    + blacklistKeystorePath.toString() + "] was not provided."));
        }

        return errors == 0;
    }

    private KeyStore loadKeystore() {
        String msg = "Unable to determine if keystore [" + keystorePath.toString()
                + "] is using insecure defaults. ";

        KeyStore keystore = null;

        try {
            keystore = KeyStore.getInstance("JKS");
        } catch (KeyStoreException e) {
            LOGGER.error(msg, e);
            alerts.add(new Alert(Level.ERROR, msg + e.getMessage() + "."));
            return null;
        }

        if (!new File(keystorePath.toString()).canRead()) {
            alerts.add(new Alert(Level.WARN, "Unable to determine if keystore ["
                    + keystorePath.toString()
                    + "] is using insecure defaults. Cannot read keystore."));
            return null;
        }
        
        try (FileInputStream fis = new FileInputStream(keystorePath.toString())) {

            if (StringUtils.isNotBlank(keystorePassword)) {
                keystore.load(fis, keystorePassword.toCharArray());

                if (StringUtils.equals(keystorePassword, defaultKeystorePassword)) {
                    alerts.add(new Alert(Level.WARN, "The keystore password for ["
                            + keystorePath.toString() + "] is the default password of ["
                            + defaultKeystorePassword + "]."));
                }
            }
        } catch (NoSuchAlgorithmException | CertificateException | IOException e) {
            keystore = null;
            LOGGER.error(msg, e);
            alerts.add(new Alert(Level.WARN, msg + e.getMessage() + "."));
        }

        return keystore;
    }

    private void validateKeyPasswords(KeyStore keystore) {
        try {
            Enumeration<String> aliases = keystore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = (String) aliases.nextElement();
                if (keystore.entryInstanceOf(alias, KeyStore.PrivateKeyEntry.class)
                        || keystore.entryInstanceOf(alias, KeyStore.SecretKeyEntry.class)) {
                    if (StringUtils.isNotBlank(defaultKeyPassword)) {
                        // See if we can access the key using the default key password.  If we cannot, we
                        // know that we are using a non-default password.
                        Key key = keystore.getKey(alias, defaultKeyPassword.toCharArray());
                        if (key != null) {
                            alerts.add(new Alert(Level.WARN, "The key for alias [" + alias
                                    + "] in [" + keystorePath
                                    + "] is using the default password of [" + defaultKeyPassword
                                    + "]."));
                        }
                    } else {
                        String msg = "Unable to determine if keystore [" + keystorePath.toString()
                                + "] is using insecure defaults. No key password provided.";
                        alerts.add(new Alert(Level.WARN, msg));
                    }
                }
            }
        } catch (UnrecoverableKeyException e) {
            // Key is not using default key password.
        } catch (KeyStoreException | NoSuchAlgorithmException e) {
            String msg = "Unable to determine if keystore [" + keystorePath.toString()
                    + "] is using insecure defaults. Error validating key password in keystore ["
                    + keystorePath.toString() + "]. ";
            LOGGER.warn(msg, e);
            alerts.add(new Alert(Level.WARN, msg + e.getMessage() + "."));
        }
    }

    private List<Certificate> getBlackListedCertificates() {
        List<Certificate> blackListedCertificates = new ArrayList<>();

        if (!new File(blacklistKeystorePath.toString()).canRead()) {
            alerts.add(new Alert(Level.WARN, "Unable to determine if keystore ["
                    + keystorePath.toString()
                    + "] is using insecure defaults. Cannot read Blacklist keystore ["
                    + blacklistKeystorePath.toString() + "]."));
            return blackListedCertificates;
        }
        
        try (FileInputStream fis = new FileInputStream(blacklistKeystorePath.toString())) {
            blacklistKeystore = KeyStore.getInstance("JKS");
            blacklistKeystore.load(fis, blacklistKeystorePassword.toCharArray());

            Enumeration<String> aliases = blacklistKeystore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = (String) aliases.nextElement();
                Certificate certificate = blacklistKeystore.getCertificate(alias);
                blackListedCertificates.add(certificate);
            }
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
            String msg = "Unable to determine if keystore ["
                    + keystorePath.toString()
                    + "] contains insecure default certificates. Error retrieving certificates from Blacklist keystore ["
                    + blacklistKeystorePath.toString() + "]. ";
            LOGGER.warn(msg, e);
            alerts.add(new Alert(Level.WARN, msg + e.getMessage() + "."));
        }

        return blackListedCertificates;
    }

    private List<Certificate[]> getKeystoreCertificatesChains(KeyStore keystore) {
        List<Certificate[]> keystoreCertificateChains = new ArrayList<>();

        try {
            Enumeration<String> aliases = keystore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = (String) aliases.nextElement();
                Certificate[] certificateChain = keystore.getCertificateChain(alias);
                if (certificateChain != null) {
                    keystoreCertificateChains.add(certificateChain);
                } else {
                    Certificate certificate = keystore.getCertificate(alias);
                    keystoreCertificateChains.add(new Certificate[] {certificate});
                }
            }
        } catch (KeyStoreException e) {
            String msg = "Unable to determine if keystore [" + keystorePath.toString()
                    + "] is using insecure defaults. ";
            LOGGER.warn(msg, e);
        }

        return keystoreCertificateChains;
    }

    private void validateKeystoreCertificates(KeyStore keystore,
            List<Certificate[]> keystoreCertificateChains, List<Certificate> blackListedCertificates) {
        for (Certificate[] certificateChain : keystoreCertificateChains) {
            for (Certificate certificate : certificateChain) {
                for (Certificate blackListedCertificate : blackListedCertificates) {
                    try {
                        if (Arrays.equals(certificate.getEncoded(),
                                blackListedCertificate.getEncoded())) {
                            String msg = null;
                            if (certificateChain.length > 1) {
                                msg = "The certificate chain for alias ["
                                        + keystore.getCertificateAlias(certificate)
                                        + "] in ["
                                        + keystorePath.toString()
                                        + "] contains a blacklisted certificate with alias ["
                                        + blacklistKeystore
                                                .getCertificateAlias(blackListedCertificate) + "].";
                            } else {
                                msg = "The certificate for alias ["
                                        + keystore.getCertificateAlias(certificate)
                                        + "] in ["
                                        + keystorePath.toString()
                                        + "] is a blacklisted certificate with alias ["
                                        + blacklistKeystore
                                                .getCertificateAlias(blackListedCertificate) + "].";
                            }
                            alerts.add(new Alert(Level.WARN, msg));

                        }
                    } catch (CertificateEncodingException | KeyStoreException e) {
                        String msg = "Unable to determine if keystore [" + keystorePath.toString()
                                + "] is using insecure defaults. ";
                        LOGGER.warn(msg, e);
                        alerts.add(new Alert(Level.WARN, msg + e.getMessage()));
                    }
                }
            }
        }
    }
}
