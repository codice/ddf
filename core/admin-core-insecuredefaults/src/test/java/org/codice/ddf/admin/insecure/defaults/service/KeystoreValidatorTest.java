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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class KeystoreValidatorTest {

    private static final String DEFAULT_BLACKLIST_PASSWORD = "changeit";

    private static final String INVALID_BLACKLIST_PASSWORD = "invalid";

    private static final String BLACK_LIST = "/blacklisted.jks";

    private static final String FAKE_BLACK_LIST = "/fakeblacklisted.jks";

    private static final String INSECURE_KEYSTORE = "/insecureKeystore.jks";

    private static final String FAKE_KEYSTORE = "/fakeKeystore.jks";

    private static final String SECURE_KEYSTORE = "/secureKeystore.jks";

    private static final String SECURE_KEYSTORE_PASSWORD = "password";

    private static final String DEFAULT_KEYSTORE_PASSWORD = "changeit";

    private static final String INSECURE_KEYSTORE_PASSWORD = "changeit";

    private static final String INVALID_KEYSTORE_PASSWORD = "invalid";

    private static final String INSECURE_TRUSTSTORE = "/insecureTruststore.jks";

    private static final String SECURE_TRUSTSTORE = "/secureTruststore.jks";

    private static final String DEFAULT_TRUSTSTORE_PASSWORD = "changeit";

    private static final String INSECURE_TRUSTSTORE_PASSWORD = "changeit";

    private static final String SECURE_TRUSTSTORE_PASSWORD = "password";

    private static final String DEFAULT_KEY_PASSWORD = "changeit";

    private static final String DEFAULT_ALIAS = "localhost";

    private static final String DEFAULT_ROOT_CA = "ddf demo root ca";

    @Test
    public void testInvalidPasswordForBlacklistKeystore() throws Exception {

        // Setup
        KeystoreValidator keystoreValidator = new KeystoreValidator();
        Path blacklistPath = Paths.get(getClass().getResource(BLACK_LIST).toURI());
        keystoreValidator.setBlacklistKeystorePath(blacklistPath);
        keystoreValidator.setBlacklistKeystorePassword(INVALID_BLACKLIST_PASSWORD);
        Path keystorePath = Paths.get(getClass().getResource(INSECURE_KEYSTORE).toURI());
        keystoreValidator.setKeystorePath(keystorePath);
        keystoreValidator.setKeystorePassword(INSECURE_KEYSTORE_PASSWORD);
        keystoreValidator.setDefaultKeystorePassword(DEFAULT_KEYSTORE_PASSWORD);
        keystoreValidator.setDefaultKeyPassword(DEFAULT_KEY_PASSWORD);

        // Perform Test
        List<Alert> alerts = keystoreValidator.validate();

        // Verify
        List<String> actualAlertMessages = getActualAlertMessages(alerts);
        String[] expectedAlertMessages = new String[] {
            String.format(KeystoreValidator.DEFAULT_KEY_PASSWORD_USED_MSG, DEFAULT_ALIAS,
                    keystorePath, DEFAULT_KEY_PASSWORD),
            String.format(KeystoreValidator.DEFAULT_KEYSTORE_PASSWORD_USED_MSG, keystorePath,
                    DEFAULT_KEYSTORE_PASSWORD),
            String.format(KeystoreValidator.INVALID_BLACKLIST_KEYSTORE_PASSWORD_MSG, keystorePath,
                    blacklistPath, "Keystore was tampered with, or password was incorrect")};
        assertThat(alerts.size(), is(3));
        assertThat(actualAlertMessages, hasItems(expectedAlertMessages));
    }

    @Test
    public void testBlacklistKeystoreDoesNotExist() throws Exception {

        // Setup
        KeystoreValidator keystoreValidator = new KeystoreValidator();
        keystoreValidator.setBlacklistKeystorePath(Paths.get(FAKE_BLACK_LIST));
        keystoreValidator.setBlacklistKeystorePassword(DEFAULT_BLACKLIST_PASSWORD);
        Path path = Paths.get(getClass().getResource(INSECURE_KEYSTORE).toURI());
        keystoreValidator.setKeystorePath(path);
        keystoreValidator.setKeystorePassword(INSECURE_KEYSTORE_PASSWORD);
        keystoreValidator.setDefaultKeystorePassword(DEFAULT_KEYSTORE_PASSWORD);
        keystoreValidator.setDefaultKeyPassword(DEFAULT_KEY_PASSWORD);

        // Perform Test
        List<Alert> alerts = keystoreValidator.validate();

        // Verify
        assertThat(alerts.size(), is(3));
        List<String> actualMessages = getActualAlertMessages(alerts);
        String[] expectedAlertMessages = new String[] {
            String.format(KeystoreValidator.DEFAULT_KEY_PASSWORD_USED_MSG, DEFAULT_ALIAS, path,
                    DEFAULT_KEY_PASSWORD),
            String.format(KeystoreValidator.DEFAULT_KEYSTORE_PASSWORD_USED_MSG, path,
                    DEFAULT_KEYSTORE_PASSWORD),
            String.format(KeystoreValidator.BLACKLIST_KEYSTORE_DOES_NOT_EXIST_MSG, path,
                    FAKE_BLACK_LIST, FAKE_BLACK_LIST)};
        assertThat(actualMessages, hasItems(expectedAlertMessages));
    }

    @Test
    public void testKeystoreDoesNotExist() throws Exception {

        // Setup
        KeystoreValidator keystoreValidator = new KeystoreValidator();
        keystoreValidator.setBlacklistKeystorePath(Paths.get(getClass().getResource(BLACK_LIST)
                .toURI()));
        keystoreValidator.setBlacklistKeystorePassword(DEFAULT_BLACKLIST_PASSWORD);
        keystoreValidator.setKeystorePath(Paths.get(FAKE_KEYSTORE));
        keystoreValidator.setKeystorePassword(INSECURE_KEYSTORE_PASSWORD);
        keystoreValidator.setDefaultKeystorePassword(DEFAULT_KEYSTORE_PASSWORD);
        keystoreValidator.setDefaultKeyPassword(DEFAULT_KEY_PASSWORD);

        // Perform Test
        List<Alert> alerts = keystoreValidator.validate();

        // Verify
        assertThat(alerts.size(), is(1));
        assertThat(alerts.get(0).getMessage(),
                is(String.format(KeystoreValidator.KEYSTORE_DOES_NOT_EXIST_MSG, FAKE_KEYSTORE, "")));
    }

    @Test
    public void testInvalidPasswordForKeystore() throws Exception {

        // Setup
        KeystoreValidator keystoreValidator = new KeystoreValidator();
        keystoreValidator.setBlacklistKeystorePath(Paths.get(getClass().getResource(BLACK_LIST)
                .toURI()));
        keystoreValidator.setBlacklistKeystorePassword(DEFAULT_BLACKLIST_PASSWORD);
        Path path = Paths.get(getClass().getResource(INSECURE_KEYSTORE).toURI());
        keystoreValidator.setKeystorePath(path);
        keystoreValidator.setKeystorePassword(INVALID_KEYSTORE_PASSWORD);
        keystoreValidator.setDefaultKeystorePassword(DEFAULT_KEYSTORE_PASSWORD);
        keystoreValidator.setDefaultKeyPassword(DEFAULT_KEY_PASSWORD);

        // Perform Test
        List<Alert> alerts = keystoreValidator.validate();

        // Verify
        assertThat(alerts.size(), is(1));
        assertThat(alerts.get(0).getMessage(),
                is(String.format(KeystoreValidator.GENERIC_INSECURE_DEFAULTS_MSG, path)
                        + "Keystore was tampered with, or password was incorrect."));
    }

    @Test
    public void testKeystoreContainsInsecureDefaults() throws Exception {

        // Setup
        KeystoreValidator keystoreValidator = new KeystoreValidator();
        keystoreValidator.setBlacklistKeystorePath(Paths.get(getClass().getResource(BLACK_LIST)
                .toURI()));
        keystoreValidator.setBlacklistKeystorePassword(DEFAULT_BLACKLIST_PASSWORD);
        Path keystorePath = Paths.get(getClass().getResource(INSECURE_KEYSTORE).toURI());
        keystoreValidator.setKeystorePath(keystorePath);
        keystoreValidator.setKeystorePassword(INSECURE_KEYSTORE_PASSWORD);
        keystoreValidator.setDefaultKeystorePassword(DEFAULT_KEYSTORE_PASSWORD);
        keystoreValidator.setDefaultKeyPassword(DEFAULT_KEY_PASSWORD);

        // Perform Test
        List<Alert> alerts = keystoreValidator.validate();

        // Verify
        List<String> actualAlertMessages = getActualAlertMessages(alerts);
        String[] expectedAlertMessages = new String[] {
            String.format(KeystoreValidator.CERT_CHAIN_CONTAINS_BLACKLISTED_CERT_MSG,
                    DEFAULT_ALIAS, keystorePath, DEFAULT_ROOT_CA),
            String.format(KeystoreValidator.CERT_CHAIN_CONTAINS_BLACKLISTED_CERT_MSG,
                    DEFAULT_ALIAS, keystorePath, DEFAULT_ALIAS),
            String.format(KeystoreValidator.CERT_IS_BLACKLISTED_MSG, DEFAULT_ROOT_CA, keystorePath,
                    DEFAULT_ROOT_CA),
            String.format(KeystoreValidator.DEFAULT_KEY_PASSWORD_USED_MSG, DEFAULT_ALIAS,
                    keystorePath, DEFAULT_KEY_PASSWORD),
            String.format(KeystoreValidator.DEFAULT_KEYSTORE_PASSWORD_USED_MSG, keystorePath,
                    DEFAULT_KEYSTORE_PASSWORD)};
        assertThat(alerts.size(), is(5));
        assertThat(actualAlertMessages, hasItems(expectedAlertMessages));
    }

    @Test
    public void testTruststoreContainsInsecureDefaults() throws Exception {

        // Setup
        KeystoreValidator truststoreValidator = new KeystoreValidator();
        truststoreValidator.setBlacklistKeystorePath(Paths.get(getClass().getResource(BLACK_LIST)
                .toURI()));
        truststoreValidator.setBlacklistKeystorePassword(DEFAULT_BLACKLIST_PASSWORD);
        Path path = Paths.get(getClass().getResource(INSECURE_TRUSTSTORE).toURI());
        truststoreValidator.setKeystorePath(path);
        truststoreValidator.setKeystorePassword(INSECURE_TRUSTSTORE_PASSWORD);
        truststoreValidator.setDefaultKeystorePassword(DEFAULT_TRUSTSTORE_PASSWORD);

        // Perform Test
        List<Alert> alerts = truststoreValidator.validate();

        // Verify
        List<String> actualAlertMessages = getActualAlertMessages(alerts);
        String[] expectedAlertMessages = new String[] {
            String.format(KeystoreValidator.CERT_IS_BLACKLISTED_MSG, DEFAULT_ROOT_CA, path,
                    DEFAULT_ROOT_CA),
            String.format(KeystoreValidator.DEFAULT_KEYSTORE_PASSWORD_USED_MSG, path,
                    DEFAULT_KEYSTORE_PASSWORD)};
        assertThat(alerts.size(), is(2));
        assertThat(actualAlertMessages, hasItems(expectedAlertMessages));
    }

    @Test
    public void testKeystoreContainsNoInsecureDefaults() throws Exception {

        // Setup
        KeystoreValidator keystoreValidator = new KeystoreValidator();
        keystoreValidator.setBlacklistKeystorePath(Paths.get(getClass().getResource(BLACK_LIST)
                .toURI()));
        keystoreValidator.setBlacklistKeystorePassword(DEFAULT_BLACKLIST_PASSWORD);
        keystoreValidator.setKeystorePath(Paths
                .get(getClass().getResource(SECURE_KEYSTORE).toURI()));
        keystoreValidator.setKeystorePassword(SECURE_KEYSTORE_PASSWORD);
        keystoreValidator.setDefaultKeystorePassword(DEFAULT_KEYSTORE_PASSWORD);
        keystoreValidator.setDefaultKeyPassword(DEFAULT_KEY_PASSWORD);

        // Perform Test
        List<Alert> alerts = keystoreValidator.validate();

        // Verify
        assertThat(alerts.size(), is(0));
    }

    @Test
    public void testTruststoreContainsNoInsecureDefaults() throws Exception {

        // Setup
        KeystoreValidator truststoreValidator = new KeystoreValidator();
        truststoreValidator.setBlacklistKeystorePath(Paths.get(getClass().getResource(BLACK_LIST)
                .toURI()));
        truststoreValidator.setBlacklistKeystorePassword(DEFAULT_BLACKLIST_PASSWORD);
        truststoreValidator.setKeystorePath(Paths.get(getClass().getResource(SECURE_TRUSTSTORE)
                .toURI()));
        truststoreValidator.setKeystorePassword(SECURE_TRUSTSTORE_PASSWORD);
        truststoreValidator.setDefaultKeystorePassword(DEFAULT_TRUSTSTORE_PASSWORD);

        // Perform Test
        List<Alert> alerts = truststoreValidator.validate();

        // Verify
        assertThat(alerts.size(), is(0));
    }

    private List<String> getActualAlertMessages(List<Alert> alerts) {
        List<String> messages = new ArrayList<>(alerts.size());
        for (Alert alert : alerts) {
            messages.add(alert.getMessage());
        }
        return messages;
    }
}
