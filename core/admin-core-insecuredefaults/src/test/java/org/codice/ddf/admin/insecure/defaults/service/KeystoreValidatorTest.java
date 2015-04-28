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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
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

    @Test
    public void testInvalidPasswordForBlacklistKeystore() throws Exception {

        // Setup
        KeystoreValidator keystoreValidator = new KeystoreValidator();
        keystoreValidator.setBlacklistKeystorePath(Paths.get(getClass().getResource(BLACK_LIST)
                .toURI()));
        keystoreValidator.setBlacklistKeystorePassword(INVALID_BLACKLIST_PASSWORD);
        keystoreValidator.setKeystorePath(Paths.get(getClass().getResource(INSECURE_KEYSTORE)
                .toURI()));
        keystoreValidator.setKeystorePassword(INSECURE_KEYSTORE_PASSWORD);
        keystoreValidator.setDefaultKeystorePassword(DEFAULT_KEYSTORE_PASSWORD);
        keystoreValidator.setDefaultKeyPassword(DEFAULT_KEY_PASSWORD);

        // Perform Test
        List<Alert> alerts = keystoreValidator.validate();

        // Verify
        assertThat(alerts.size(), is(3));

        List<String> messages = new ArrayList<>(3);
        for (Alert alert : alerts) {
            messages.add(alert.getMessage());
        }
        Collections.sort(messages);

        assertThat(messages.get(0), containsString("The key for alias [localhost]"));
        assertThat(messages.get(0), containsString(INSECURE_KEYSTORE));
        assertThat(messages.get(0), containsString("default password of [changeit]."));
        assertThat(messages.get(1), containsString("The keystore password for"));
        assertThat(messages.get(1), containsString(INSECURE_KEYSTORE));
        assertThat(messages.get(1), containsString("default password of ["
                + DEFAULT_KEYSTORE_PASSWORD + "]"));
        assertThat(messages.get(2), containsString("Unable to determine if keystore"));
        assertThat(messages.get(2), containsString(INSECURE_KEYSTORE));
        assertThat(
                messages.get(2),
                containsString("contains insecure default certificates. Error retrieving certificates from Blacklist keystore"));
        assertThat(messages.get(2), containsString(BLACK_LIST));
        assertThat(messages.get(2),
                containsString("Keystore was tampered with, or password was incorrect"));
    }

    @Test
    public void testBlacklistKeystoreDoesNotExist() throws Exception {

        // Setup
        KeystoreValidator keystoreValidator = new KeystoreValidator();
        keystoreValidator.setBlacklistKeystorePath(Paths.get(FAKE_BLACK_LIST));
        keystoreValidator.setBlacklistKeystorePassword(DEFAULT_BLACKLIST_PASSWORD);
        keystoreValidator.setKeystorePath(Paths.get(getClass().getResource(INSECURE_KEYSTORE)
                .toURI()));
        keystoreValidator.setKeystorePassword(INSECURE_KEYSTORE_PASSWORD);
        keystoreValidator.setDefaultKeystorePassword(DEFAULT_KEYSTORE_PASSWORD);
        keystoreValidator.setDefaultKeyPassword(DEFAULT_KEY_PASSWORD);

        // Perform Test
        List<Alert> alerts = keystoreValidator.validate();

        // Verify
        assertThat(alerts.size(), is(3));

        List<String> messages = new ArrayList<>(3);
        for (Alert alert : alerts) {
            messages.add(alert.getMessage());
        }
        Collections.sort(messages);

        assertThat(messages.get(0), containsString("The key for alias [localhost]"));
        assertThat(messages.get(0), containsString(INSECURE_KEYSTORE));
        assertThat(messages.get(0), containsString("default password of [changeit]."));
        assertThat(messages.get(1), containsString("The keystore password for"));
        assertThat(messages.get(1), containsString(INSECURE_KEYSTORE));
        assertThat(messages.get(1), containsString("default password of ["
                + DEFAULT_KEYSTORE_PASSWORD + "]"));
        assertThat(messages.get(2), containsString("Unable to determine if keystore"));
        assertThat(messages.get(2), containsString(INSECURE_KEYSTORE));
        assertThat(messages.get(2),
                containsString("is using insecure defaults. Cannot read Blacklist keystore"));
        assertThat(messages.get(2), containsString(FAKE_BLACK_LIST));
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
        assertThat(alerts.get(0).getMessage(), containsString("Unable to determine if keystore"));
        assertThat(alerts.get(0).getMessage(), containsString(FAKE_KEYSTORE));
        assertThat(alerts.get(0).getMessage(),
                containsString("is using insecure defaults. Cannot read keystore."));
        assertThat(alerts.get(0).getMessage(), containsString("Cannot read keystore."));
    }

    @Test
    public void testInvalidPasswordForKeystore() throws Exception {

        // Setup
        KeystoreValidator keystoreValidator = new KeystoreValidator();
        keystoreValidator.setBlacklistKeystorePath(Paths.get(getClass().getResource(BLACK_LIST)
                .toURI()));
        keystoreValidator.setBlacklistKeystorePassword(DEFAULT_BLACKLIST_PASSWORD);
        keystoreValidator.setKeystorePath(Paths.get(getClass().getResource(INSECURE_KEYSTORE)
                .toURI()));
        keystoreValidator.setKeystorePassword(INVALID_KEYSTORE_PASSWORD);
        keystoreValidator.setDefaultKeystorePassword(DEFAULT_KEYSTORE_PASSWORD);
        keystoreValidator.setDefaultKeyPassword(DEFAULT_KEY_PASSWORD);

        // Perform Test
        List<Alert> alerts = keystoreValidator.validate();

        // Verify
        assertThat(alerts.size(), is(1));
        assertThat(alerts.get(0).getMessage(), containsString("Unable to determine if keystore"));
        assertThat(alerts.get(0).getMessage(), containsString(INSECURE_KEYSTORE));
        assertThat(alerts.get(0).getMessage(), containsString("is using insecure defaults."));
        assertThat(alerts.get(0).getMessage(),
                containsString("Keystore was tampered with, or password was incorrect."));
    }

    @Test
    public void testKeystoreContainsInsecureDefaults() throws Exception {

        // Setup
        KeystoreValidator keystoreValidator = new KeystoreValidator();
        keystoreValidator.setBlacklistKeystorePath(Paths.get(getClass().getResource(BLACK_LIST)
                .toURI()));
        keystoreValidator.setBlacklistKeystorePassword(DEFAULT_BLACKLIST_PASSWORD);
        keystoreValidator.setKeystorePath(Paths.get(getClass().getResource(INSECURE_KEYSTORE)
                .toURI()));
        keystoreValidator.setKeystorePassword(INSECURE_KEYSTORE_PASSWORD);
        keystoreValidator.setDefaultKeystorePassword(DEFAULT_KEYSTORE_PASSWORD);
        keystoreValidator.setDefaultKeyPassword(DEFAULT_KEY_PASSWORD);

        // Perform Test
        List<Alert> alerts = keystoreValidator.validate();

        // Verify
        assertThat(alerts.size(), is(5));

        List<String> messages = new ArrayList<>(5);
        for (Alert alert : alerts) {
            messages.add(alert.getMessage());
        }
        Collections.sort(messages);

        assertThat(messages.get(0),
                containsString("The certificate chain for alias [ddf demo root ca]"));
        assertThat(messages.get(0), containsString(INSECURE_KEYSTORE));
        assertThat(messages.get(0),
                containsString("contains a blacklisted certificate with alias [ddf demo root ca]"));
        assertThat(messages.get(1), containsString("The certificate chain for alias [localhost]"));
        assertThat(messages.get(1), containsString(INSECURE_KEYSTORE));
        assertThat(messages.get(1),
                containsString("contains a blacklisted certificate with alias [localhost]"));
        assertThat(messages.get(2), containsString("The certificate for alias [ddf demo root ca]"));
        assertThat(messages.get(2), containsString(INSECURE_KEYSTORE));
        assertThat(messages.get(2),
                containsString("is a blacklisted certificate with alias [ddf demo root ca]"));
        assertThat(messages.get(3), containsString("The key for alias [localhost]"));
        assertThat(messages.get(3), containsString(INSECURE_KEYSTORE));
        assertThat(messages.get(3), containsString("default password of [changeit]"));
        assertThat(messages.get(4), containsString("The keystore password"));
        assertThat(messages.get(4), containsString(INSECURE_KEYSTORE));
        assertThat(messages.get(4), containsString("default password of ["
                + DEFAULT_KEYSTORE_PASSWORD + "]"));
    }

    @Test
    public void testTruststoreContainsInsecureDefaults() throws Exception {

        // Setup
        KeystoreValidator truststoreValidator = new KeystoreValidator();
        truststoreValidator.setBlacklistKeystorePath(Paths.get(getClass().getResource(BLACK_LIST)
                .toURI()));
        truststoreValidator.setBlacklistKeystorePassword(DEFAULT_BLACKLIST_PASSWORD);
        truststoreValidator.setKeystorePath(Paths.get(getClass().getResource(INSECURE_TRUSTSTORE)
                .toURI()));
        truststoreValidator.setKeystorePassword(INSECURE_TRUSTSTORE_PASSWORD);
        truststoreValidator.setDefaultKeystorePassword(DEFAULT_TRUSTSTORE_PASSWORD);

        // Perform Test
        List<Alert> alerts = truststoreValidator.validate();

        // Verify
        assertThat(alerts.size(), is(2));

        List<String> messages = new ArrayList<>(2);
        for (Alert alert : alerts) {
            messages.add(alert.getMessage());
        }
        Collections.sort(messages);

        assertThat(messages.get(0), containsString("The certificate for alias [ddf demo root ca]"));
        assertThat(messages.get(0), containsString(INSECURE_TRUSTSTORE));
        assertThat(messages.get(0),
                containsString("blacklisted certificate with alias [ddf demo root ca]"));
        assertThat(messages.get(1), containsString("The keystore password"));
        assertThat(messages.get(1), containsString(INSECURE_TRUSTSTORE));
        assertThat(messages.get(1), containsString("default password of ["
                + DEFAULT_TRUSTSTORE_PASSWORD + "]"));

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
}
