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

public class SignaturePropertiesFileValidatorTest {

    private static final String SIGNATURE_PROPERTIES_FILE_WITH_DEFAULTS = "/signatureWithDefaults.properties";

    private static final String FAKE_SIGNATURE_PROPERTIES_FILE = "/fakesignature.properties";

    private static final String SIGNATURE_PROPERTIES_FILE_WITH_NON_DEFAULTS = "/signatureNondefaults.properties";

    private static final String DEFAULT_KEYSTORE_PASSWORD = "changeit";

    private static final String DEFAULT_KEYSTORE_ALIAS = "localhost";

    private static final String DEFAULT_KEYSTORE_PRIVATE_PASSWORD = "changeit";

    @Test
    public void testSignaturePropertiesFileDoesNotExist() throws Exception {
        // Setup
        SignaturePropertiesFileValidator propertiesFileValidator = new SignaturePropertiesFileValidator();
        propertiesFileValidator.setPath(Paths.get(FAKE_SIGNATURE_PROPERTIES_FILE));
        propertiesFileValidator.setDefaultPassword(DEFAULT_KEYSTORE_PASSWORD);
        propertiesFileValidator.setDefaultAlias(DEFAULT_KEYSTORE_ALIAS);

        // Perform Test
        List<Alert> alerts = propertiesFileValidator.validate();

        // Verify
        assertThat(alerts.size(), is(1));
        assertThat(
                alerts.get(0).getMessage(),
                is(String.format(SignaturePropertiesFileValidator.GENERIC_INSECURE_DEFAULTS_MSG,
                        FAKE_SIGNATURE_PROPERTIES_FILE)
                        + FAKE_SIGNATURE_PROPERTIES_FILE
                        + " (No such file or directory)"));
    }

    @Test
    public void testSignaturePropertiesFileHasDefaultKeystoreAliasAndDefaultKeystorePasswordAndDefaultKeystorePrivatePassword()
        throws Exception {
        // Setup
        SignaturePropertiesFileValidator propertiesFileValidator = new SignaturePropertiesFileValidator();
        Path path = Paths.get(getClass().getResource(SIGNATURE_PROPERTIES_FILE_WITH_DEFAULTS)
                .toURI());
        propertiesFileValidator.setPath(path);
        propertiesFileValidator.setDefaultPassword(DEFAULT_KEYSTORE_PASSWORD);
        propertiesFileValidator.setDefaultAlias(DEFAULT_KEYSTORE_ALIAS);
        propertiesFileValidator.setDefaultPrivateKeyPassword(DEFAULT_KEYSTORE_PRIVATE_PASSWORD);

        // Perform Test
        List<Alert> alerts = propertiesFileValidator.validate();

        // Verify
        assertThat(alerts.size(), is(3));

        List<String> actualAlertMessages = getActualAlertMessages(alerts);
        String[] expectedAlertMessages = new String[] {
            String.format(SignaturePropertiesFileValidator.DEFAULT_KEYSTORE_ALIAS_USED_MSG,
                    SignaturePropertiesFileValidator.KEYSTORE_ALIAS_PROPERTY, path,
                    DEFAULT_KEYSTORE_ALIAS),
            String.format(SignaturePropertiesFileValidator.DEFAULT_KEYSTORE_PASSWORD_USED_MSG,
                    SignaturePropertiesFileValidator.KEYSTORE_PASSWORD_PROPERTY, path,
                    DEFAULT_KEYSTORE_PASSWORD),
            String.format(
                    SignaturePropertiesFileValidator.DEFAULT_KEYSTORE_PRIVATE_PASSWORD_USED_MSG,
                    SignaturePropertiesFileValidator.PRIVATE_KEY_PASSWORD_PROPERTY, path,
                    DEFAULT_KEYSTORE_PRIVATE_PASSWORD)};
        assertThat(actualAlertMessages, hasItems(expectedAlertMessages));
    }

    @Test
    public void testSignaturePropertiesFileHasNondefaults() throws Exception {
        // Setup
        SignaturePropertiesFileValidator propertiesFileValidator = new SignaturePropertiesFileValidator();
        propertiesFileValidator.setPath(Paths.get(getClass().getResource(
                SIGNATURE_PROPERTIES_FILE_WITH_NON_DEFAULTS).toURI()));
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
