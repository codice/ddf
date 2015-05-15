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

    static final String GENERIC_INSECURE_DEFAULTS_MSG = "Unable to determine if keystore [%s] is using insecure defaults. ";

    static final String DEFAULT_KEY_PASSWORD_USED_MSG = "The key for alias [%s] in [%s] is using the default password of [%s].";

    static final String DEFAULT_KEYSTORE_PASSWORD_USED_MSG = "The keystore password for [%s] is the default password of [%s].";

    static final String INVALID_BLACKLIST_KEYSTORE_PASSWORD_MSG = "Unable to determine if keystore [%s] contains insecure default certificates. Error retrieving certificates from Blacklist keystore [%s]. %s.";

    static final String BLACKLIST_KEYSTORE_DOES_NOT_EXIST_MSG = GENERIC_INSECURE_DEFAULTS_MSG
            + "Cannot read Blacklist keystore [%s].";

    static final String CERT_CHAIN_CONTAINS_BLACKLISTED_CERT_MSG = "The certificate chain for alias [%s] in [%s] contains a blacklisted certificate with alias [%s].";

    static final String CERT_IS_BLACKLISTED_MSG = "The certificate for alias [%s] in [%s] is a blacklisted certificate with alias [%s].";

    static final String KEYSTORE_DOES_NOT_EXIST_MSG = GENERIC_INSECURE_DEFAULTS_MSG
            + "Cannot read keystore.";

    public KeystoreValidator() {
        alerts = new ArrayList<>();
    }

    public void setKeystorePath(Path path) {
        this.keystorePath = path;
    }

    public void setKeystorePassword(String password) {
        this.keystorePassword = password;
    }

    public void setDefaultKeystorePassword(String password) {
        this.defaultKeystorePassword = password;
    }

    public void setBlacklistKeystorePath(Path path) {
        this.blacklistKeystorePath = path;
    }

    public void setBlacklistKeystorePassword(String password) {
        this.blacklistKeystorePassword = password;
    }

    public void setDefaultKeyPassword(String password) {
        this.defaultKeyPassword = password;
    }

    public List<Alert> validate() {
        alerts = new ArrayList<>();

        if (isInitialized()) {
            List<Certificate> blacklistedCertificates = getBlackListedCertificates();
            KeyStore keystore = loadKeystore();
            if (keystore != null) {
                validateKeyPasswords(keystore);
                if (!blacklistedCertificates.isEmpty()) {
                    List<Certificate[]> keystoreCertificateChains = getKeystoreCertificatesChains(keystore);
                    validateKeystoreCertificates(keystore, keystoreCertificateChains,
                            blacklistedCertificates);
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
            alerts.add(new Alert(Level.WARN, String.format(GENERIC_INSECURE_DEFAULTS_MSG,
                    keystorePath) + "No Blacklist keystore path provided."));
            return false;
        }

        if (StringUtils.isBlank(keystorePassword)) {
            errors++;
            alerts.add(new Alert(Level.WARN, String.format(GENERIC_INSECURE_DEFAULTS_MSG,
                    keystorePath.toString()) + "No keystore password provided."));
        }

        if (StringUtils.isBlank(blacklistKeystorePassword)) {
            errors++;
            alerts.add(new Alert(Level.WARN, String.format(GENERIC_INSECURE_DEFAULTS_MSG,
                    keystorePath)
                    + "Password for Blacklist keystore ["
                    + blacklistKeystorePath.toString() + "] was not provided."));
        }

        return errors == 0;
    }

    private KeyStore loadKeystore() {
        KeyStore keystore = null;

        try {
            keystore = KeyStore.getInstance("JKS");
        } catch (KeyStoreException e) {
            LOGGER.warn(String.format(GENERIC_INSECURE_DEFAULTS_MSG, keystorePath.toString()), e);
            alerts.add(new Alert(Level.WARN, String.format(GENERIC_INSECURE_DEFAULTS_MSG,
                    keystorePath) + e.getMessage() + "."));
            return null;
        }

        if (!new File(keystorePath.toString()).canRead()) {
            alerts.add(new Alert(Level.WARN, String.format(KEYSTORE_DOES_NOT_EXIST_MSG,
                    keystorePath)));
            return null;
        }

        try (FileInputStream fis = new FileInputStream(keystorePath.toString())) {

            if (StringUtils.isNotBlank(keystorePassword)) {
                keystore.load(fis, keystorePassword.toCharArray());

                if (StringUtils.equals(keystorePassword, defaultKeystorePassword)) {
                    alerts.add(new Alert(Level.WARN, String.format(
                            DEFAULT_KEYSTORE_PASSWORD_USED_MSG, keystorePath.toString(),
                            defaultKeystorePassword)));
                }
            }
        } catch (NoSuchAlgorithmException | CertificateException | IOException e) {
            keystore = null;
            LOGGER.warn(String.format(GENERIC_INSECURE_DEFAULTS_MSG, keystorePath), e);
            alerts.add(new Alert(Level.WARN, String.format(GENERIC_INSECURE_DEFAULTS_MSG,
                    keystorePath) + e.getMessage() + "."));
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
                        // See if we can access the key using the default key password. If we
                        // cannot, we
                        // know that we are using a non-default password.
                        Key key = keystore.getKey(alias, defaultKeyPassword.toCharArray());
                        if (key != null) {
                            alerts.add(new Alert(Level.WARN, String.format(
                                    DEFAULT_KEY_PASSWORD_USED_MSG, alias, keystorePath,
                                    defaultKeyPassword)));
                        }
                    } else {
                        alerts.add(new Alert(Level.WARN, String.format(
                                GENERIC_INSECURE_DEFAULTS_MSG, keystorePath)
                                + "No key password provided."));
                    }
                }
            }
        } catch (UnrecoverableKeyException e) {
            // Key is not using default key password.
        } catch (KeyStoreException | NoSuchAlgorithmException e) {
            LOGGER.warn(String.format(GENERIC_INSECURE_DEFAULTS_MSG, keystorePath), e);
            alerts.add(new Alert(Level.WARN, String.format(GENERIC_INSECURE_DEFAULTS_MSG,
                    keystorePath) + e.getMessage() + "."));
        }
    }

    private List<Certificate> getBlackListedCertificates() {
        List<Certificate> blacklistedCertificates = new ArrayList<>();

        if (!new File(blacklistKeystorePath.toString()).canRead()) {
            alerts.add(new Alert(Level.WARN, String.format(BLACKLIST_KEYSTORE_DOES_NOT_EXIST_MSG,
                    keystorePath, blacklistKeystorePath)));
            return blacklistedCertificates;
        }

        try (FileInputStream fis = new FileInputStream(blacklistKeystorePath.toString())) {
            blacklistKeystore = KeyStore.getInstance("JKS");
            blacklistKeystore.load(fis, blacklistKeystorePassword.toCharArray());

            Enumeration<String> aliases = blacklistKeystore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = (String) aliases.nextElement();
                Certificate certificate = blacklistKeystore.getCertificate(alias);
                blacklistedCertificates.add(certificate);
            }
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
            String msg = String.format(INVALID_BLACKLIST_KEYSTORE_PASSWORD_MSG, keystorePath,
                    blacklistKeystorePath, e.getMessage());
            LOGGER.warn(msg, e);
            alerts.add(new Alert(Level.WARN, msg));
        }

        return blacklistedCertificates;
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
            LOGGER.warn(String.format(GENERIC_INSECURE_DEFAULTS_MSG, keystorePath), e);
        }

        return keystoreCertificateChains;
    }

    private void validateKeystoreCertificates(KeyStore keystore,
            List<Certificate[]> keystoreCertificateChains, List<Certificate> blackistedCertificates) {
        for (Certificate[] certificateChain : keystoreCertificateChains) {
            // validate each certificate chain against the blacklist
            validateCertificateChain(certificateChain, blackistedCertificates, keystore);
        }
    }

    private void validateCertificateChain(Certificate[] certificateChain,
            List<Certificate> blacklistedCertificates, KeyStore keystore) {
        Certificate headCertificate = certificateChain[0];
        for (Certificate certificate : certificateChain) {
            // validate each certificate in the certificate chain against the blacklist
            validateAgainstBlacklist(headCertificate, certificate, blacklistedCertificates, keystore,
                    certificateChain.length);
        }
    }

    private void validateAgainstBlacklist(Certificate headCertificate, Certificate certificate,
            List<Certificate> blacklistedCertificates, KeyStore keystore, int certChainLength) {
        for (Certificate blackListedCertificate : blacklistedCertificates) {
            try {
                if (areCertificatesEqual(certificate, blackListedCertificate)) {
                    String msg = null;
                    if (certChainLength > 1) {
                        msg = String.format(CERT_CHAIN_CONTAINS_BLACKLISTED_CERT_MSG,
                                keystore.getCertificateAlias(headCertificate), keystorePath,
                                blacklistKeystore.getCertificateAlias(blackListedCertificate));
                    } else {
                        msg = String.format(CERT_IS_BLACKLISTED_MSG,
                                keystore.getCertificateAlias(headCertificate), keystorePath,
                                blacklistKeystore.getCertificateAlias(blackListedCertificate));
                    }
                    alerts.add(new Alert(Level.WARN, msg));

                }
            } catch (CertificateEncodingException | KeyStoreException e) {
                LOGGER.warn(String.format(GENERIC_INSECURE_DEFAULTS_MSG, keystorePath), e);
                alerts.add(new Alert(Level.WARN, String.format(GENERIC_INSECURE_DEFAULTS_MSG,
                        keystorePath) + e.getMessage()));
            }
        }
    }

    private boolean areCertificatesEqual(Certificate certificate, Certificate blacklistedCertificate)
        throws CertificateEncodingException {
        return Arrays.equals(certificate.getEncoded(), blacklistedCertificate.getEncoded());
    }
}
