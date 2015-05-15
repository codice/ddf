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

public class EncryptionPropertiesFileValidatorTest {

    private static final String ISSUER_ENCRYPTION_PROPERTIES_FILE_WITH_DEFAULTS = "/issuerEncryptionWithDefaults.properties";

    private static final String FAKE_ENCRYPTION_PROPERTIES_FILE = "/fakeencryption.properties";

    private static final String ISSUER_ENCRYPTION_PROPERTIES_FILE_WITH_NON_DEFAULTS = "/issuerEncryptionNondefaults.properties";

    private static final String ISSUER_ENCRYPTION_PROPERTIES_FILE_WITH_DEFAULT_KEYSTORE_ALIAS = "/issuerEncryptionDefaultAlias.properties";

    private static final String ISSUER_ENCRYPTION_PROPERTIES_FILE_WITH_DEFAULT_KEYSTORE_PASSWORD = "/issuerEncryptionDefaultPassword.properties";
    
    private static final String SERVER_ENCRYPTION_PROPERTIES_FILE_WITH_DEFAULTS = "/serverEncryptionWithDefaults.properties";
    
    private static final String SERVER_ENCRYPTION_PROPERTIES_FILE_WITH_NON_DEFAULTS = "/serverEncryptionWithNonDefaults.properties";

    private static final String DEFAULT_KEYSTORE_PASSWORD = "changeit";

    private static final String DEFAULT_KEYSTORE_ALIAS = "localhost";
    
    private static final String DEFAULT_KEYSTORE_PRIVATE_PASSWORD = "changeit";

    @Test
    public void testEncryptionPropertiesFileDoesNotExist() throws Exception {
        // Setup
        EncryptionPropertiesFileValidator propertiesFileValidator = new EncryptionPropertiesFileValidator();
        propertiesFileValidator.setPath(Paths.get(FAKE_ENCRYPTION_PROPERTIES_FILE));
        propertiesFileValidator.setDefaultPassword(DEFAULT_KEYSTORE_PASSWORD);
        propertiesFileValidator.setDefaultAlias(DEFAULT_KEYSTORE_ALIAS);

        // Perform Test
        List<Alert> alerts = propertiesFileValidator.validate();

        // Verify
        assertThat(alerts.size(), is(1));
        assertThat(
                alerts.get(0).getMessage(),
                is(String.format(EncryptionPropertiesFileValidator.GENERIC_INSECURE_DEFAULTS_MSG,
                        FAKE_ENCRYPTION_PROPERTIES_FILE)
                        + FAKE_ENCRYPTION_PROPERTIES_FILE
                        + " (No such file or directory)"));
    }

    @Test
    public void testIssuerEncryptionPropertiesFileHasDefaultKeystoreAliasAndDefaultKeystorePassword()
        throws Exception {
        // Setup
        EncryptionPropertiesFileValidator propertiesFileValidator = new EncryptionPropertiesFileValidator();
        Path path = Paths.get(getClass().getResource(ISSUER_ENCRYPTION_PROPERTIES_FILE_WITH_DEFAULTS)
                .toURI());
        propertiesFileValidator.setPath(path);
        propertiesFileValidator.setDefaultPassword(DEFAULT_KEYSTORE_PASSWORD);
        propertiesFileValidator.setDefaultAlias(DEFAULT_KEYSTORE_ALIAS);

        // Perform Test
        List<Alert> alerts = propertiesFileValidator.validate();

        // Verify
        List<String> actualAlertMessages = getActualAlertMessages(alerts);
        String[] expectedAlertMessages = new String[] {
            String.format(EncryptionPropertiesFileValidator.DEFAULT_KEYSTORE_ALIAS_USED_MSG,
                    EncryptionPropertiesFileValidator.KEYSTORE_ALIAS_PROPERTY, path,
                    DEFAULT_KEYSTORE_ALIAS),
            String.format(EncryptionPropertiesFileValidator.DEFAULT_KEYSTORE_PASSWORD_USED_MSG,
                    EncryptionPropertiesFileValidator.KEYSTORE_PASSWORD_PROPERTY, path,
                    DEFAULT_KEYSTORE_PASSWORD)};
        assertThat(alerts.size(), is(2));
        assertThat(actualAlertMessages, hasItems(expectedAlertMessages));
    }

    @Test
    public void testIssuerEncryptionPropertiesFileHasDefaultKeystoreAliasAndNondefaultKeystorePassword()
        throws Exception {
        // Setup
        EncryptionPropertiesFileValidator propertiesFileValidator = new EncryptionPropertiesFileValidator();
        Path path = Paths.get(getClass().getResource(
                ISSUER_ENCRYPTION_PROPERTIES_FILE_WITH_DEFAULT_KEYSTORE_ALIAS).toURI());
        propertiesFileValidator.setPath(path);
        propertiesFileValidator.setDefaultPassword(DEFAULT_KEYSTORE_PASSWORD);
        propertiesFileValidator.setDefaultAlias(DEFAULT_KEYSTORE_ALIAS);

        // Perform Test
        List<Alert> alerts = propertiesFileValidator.validate();

        // Verify
        assertThat(alerts.size(), is(1));
        assertThat(alerts.get(0).getMessage(), is(String.format(
                EncryptionPropertiesFileValidator.DEFAULT_KEYSTORE_ALIAS_USED_MSG,
                EncryptionPropertiesFileValidator.KEYSTORE_ALIAS_PROPERTY, path,
                DEFAULT_KEYSTORE_ALIAS)));
    }

    @Test
    public void testIssuerEncryptionPropertiesFileHasDefaultKeystorePasswordAndNondefaultKeystoreAlias()
        throws Exception {
        // Setup
        EncryptionPropertiesFileValidator propertiesFileValidator = new EncryptionPropertiesFileValidator();
        Path path = Paths.get(getClass().getResource(
                ISSUER_ENCRYPTION_PROPERTIES_FILE_WITH_DEFAULT_KEYSTORE_PASSWORD).toURI());
        propertiesFileValidator.setPath(path);
        propertiesFileValidator.setDefaultPassword(DEFAULT_KEYSTORE_PASSWORD);
        propertiesFileValidator.setDefaultAlias(DEFAULT_KEYSTORE_ALIAS);

        // Perform Test
        List<Alert> alerts = propertiesFileValidator.validate();

        // Verify
        assertThat(alerts.size(), is(1));
        assertThat(alerts.get(0).getMessage(), is(String.format(
                EncryptionPropertiesFileValidator.DEFAULT_KEYSTORE_PASSWORD_USED_MSG,
                EncryptionPropertiesFileValidator.KEYSTORE_PASSWORD_PROPERTY, path,
                DEFAULT_KEYSTORE_PASSWORD)));
    }

    @Test
    public void testIssuerEncryptionPropertiesFileHasNondefaults() throws Exception {
        // Setup
        EncryptionPropertiesFileValidator propertiesFileValidator = new EncryptionPropertiesFileValidator();
        propertiesFileValidator.setPath(Paths.get(getClass().getResource(
                ISSUER_ENCRYPTION_PROPERTIES_FILE_WITH_NON_DEFAULTS).toURI()));
        propertiesFileValidator.setDefaultPassword(DEFAULT_KEYSTORE_PASSWORD);
        propertiesFileValidator.setDefaultAlias(DEFAULT_KEYSTORE_ALIAS);

        // Perform Test
        List<Alert> alerts = propertiesFileValidator.validate();

        // Verify
        assertThat(alerts.size(), is(0));
    }
    
    @Test
    public void testServerEncryptionPropertiesFileHasDefaults() throws Exception {
        // Setup
        EncryptionPropertiesFileValidator propertiesFileValidator = new EncryptionPropertiesFileValidator();
        Path path = Paths.get(getClass().getResource(
                SERVER_ENCRYPTION_PROPERTIES_FILE_WITH_DEFAULTS).toURI());
        propertiesFileValidator.setPath(path);
        propertiesFileValidator.setDefaultPassword(DEFAULT_KEYSTORE_PASSWORD);
        propertiesFileValidator.setDefaultAlias(DEFAULT_KEYSTORE_ALIAS);
        propertiesFileValidator.setDefaultPrivateKeyPassword(DEFAULT_KEYSTORE_PRIVATE_PASSWORD);

        // Perform Test
        List<Alert> alerts = propertiesFileValidator.validate();

        // Verify
        List<String> actualAlertMessages = getActualAlertMessages(alerts);
        String[] expectedAlertMessages = new String[] {
            String.format(EncryptionPropertiesFileValidator.DEFAULT_KEYSTORE_ALIAS_USED_MSG,
                    EncryptionPropertiesFileValidator.KEYSTORE_ALIAS_PROPERTY, path,
                    DEFAULT_KEYSTORE_ALIAS),
            String.format(EncryptionPropertiesFileValidator.DEFAULT_KEYSTORE_PASSWORD_USED_MSG,
                    EncryptionPropertiesFileValidator.KEYSTORE_PASSWORD_PROPERTY, path,
                    DEFAULT_KEYSTORE_PASSWORD),
            String.format(
                    EncryptionPropertiesFileValidator.DEFAULT_KEYSTORE_PRIVATE_PASSWORD_USED_MSG,
                    EncryptionPropertiesFileValidator.PRIVATE_KEY_PASSWORD_PROPERTY, path,
                    DEFAULT_KEYSTORE_PRIVATE_PASSWORD)};
        assertThat(alerts.size(), is(3));
        assertThat(actualAlertMessages, hasItems(expectedAlertMessages));
    }
    
    @Test
    public void testServerEncryptionPropertiesFileHasNondefaults() throws Exception {
        // Setup
        EncryptionPropertiesFileValidator propertiesFileValidator = new EncryptionPropertiesFileValidator();
        propertiesFileValidator.setPath(Paths.get(getClass().getResource(
                SERVER_ENCRYPTION_PROPERTIES_FILE_WITH_NON_DEFAULTS).toURI()));
        propertiesFileValidator.setDefaultPassword(DEFAULT_KEYSTORE_PASSWORD);
        propertiesFileValidator.setDefaultAlias(DEFAULT_KEYSTORE_ALIAS);
        propertiesFileValidator.setDefaultPrivateKeyPassword(DEFAULT_KEYSTORE_PRIVATE_PASSWORD);

        // Perform Test
        List<Alert> alerts = propertiesFileValidator.validate();

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
