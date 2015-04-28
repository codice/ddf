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

public class SignaturePropertiesFileValidatorTest {

    private static final String SIGNATURE_PROPERTIES_FILE_WITH_DEFAULTS = "/signatureWithDefaults.properties";

    private static final String FAKE_SIGNATURE_PROPERTIES_FILE = "/fakesignature.properties";

    private static final String SIGNATURE_PROPERTIES_FILE_WITH_NON_DEFAULTS = "/signatureNondefaults.properties";

    private static final String KEYSTORE_PASSWORD_PROPERTY = "org.apache.ws.security.crypto.merlin.keystore.password";

    private static final String DEFAULT_KEYSTORE_PASSWORD = "changeit";

    private static final String KEYSTORE_ALIAS_PROPERTY = "org.apache.ws.security.crypto.merlin.keystore.alias";

    private static final String DEFAULT_KEYSTORE_ALIAS = "localhost";

    private static final String DEFAULT_KEYSTORE_PRIVATE_PASSWORD_PROPERTY = "org.apache.ws.security.crypto.merlin.keystore.private.password";

    private static final String DEFAUTL_KEYSTORE_PRIVATE_PASSWORD = "changeit";

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
        assertThat(alerts.get(0).getMessage(), is("Unable to determine if ["
                + FAKE_SIGNATURE_PROPERTIES_FILE + "] is using insecure defaults. "
                + FAKE_SIGNATURE_PROPERTIES_FILE + " (No such file or directory)"));
    }

    @Test
    public void testSignaturePropertiesFileHasDefaultKeystoreAliasAndDefaultKeystorePasswordAndDefaultKeystorePrivatePassword()
        throws Exception {
        // Setup
        SignaturePropertiesFileValidator propertiesFileValidator = new SignaturePropertiesFileValidator();
        propertiesFileValidator.setPath(Paths.get(getClass().getResource(
                SIGNATURE_PROPERTIES_FILE_WITH_DEFAULTS).toURI()));
        propertiesFileValidator.setDefaultPassword(DEFAULT_KEYSTORE_PASSWORD);
        propertiesFileValidator.setDefaultAlias(DEFAULT_KEYSTORE_ALIAS);
        propertiesFileValidator.setDefaultPrivateKeyPassword(DEFAUTL_KEYSTORE_PRIVATE_PASSWORD);

        // Perform Test
        List<Alert> alerts = propertiesFileValidator.validate();

        // Verify
        assertThat(alerts.size(), is(3));

        List<String> messages = new ArrayList<>(3);
        for (Alert alert : alerts) {
            messages.add(alert.getMessage());
        }
        Collections.sort(messages);

        assertThat(messages.get(0),
                containsString("The property [" + KEYSTORE_ALIAS_PROPERTY + "]"));
        assertThat(messages.get(0), containsString(SIGNATURE_PROPERTIES_FILE_WITH_DEFAULTS));
        assertThat(messages.get(0), containsString("is set to the default keystore alias of ["
                + DEFAULT_KEYSTORE_ALIAS + "]."));

        assertThat(messages.get(1), containsString("The property [" + KEYSTORE_PASSWORD_PROPERTY
                + "]"));
        assertThat(messages.get(1), containsString(SIGNATURE_PROPERTIES_FILE_WITH_DEFAULTS));
        assertThat(messages.get(1), containsString("is set to the default keystore password of ["
                + DEFAULT_KEYSTORE_PASSWORD + "]."));

        assertThat(messages.get(2), containsString("The property ["
                + DEFAULT_KEYSTORE_PRIVATE_PASSWORD_PROPERTY + "]"));
        assertThat(messages.get(2), containsString(SIGNATURE_PROPERTIES_FILE_WITH_DEFAULTS));
        assertThat(messages.get(2),
                containsString("is set to the default keystore private password of ["
                        + DEFAUTL_KEYSTORE_PRIVATE_PASSWORD + "]."));
    }

    @Test
    public void testSignaturePropertiesFileHasNondefaults() throws Exception {
        // Setup
        SignaturePropertiesFileValidator propertiesFileValidator = new SignaturePropertiesFileValidator();
        propertiesFileValidator.setPath(Paths.get(getClass().getResource(
                SIGNATURE_PROPERTIES_FILE_WITH_NON_DEFAULTS).toURI()));
        propertiesFileValidator.setDefaultPassword(DEFAULT_KEYSTORE_PASSWORD);
        propertiesFileValidator.setDefaultAlias(DEFAULT_KEYSTORE_ALIAS);
        propertiesFileValidator.setDefaultPrivateKeyPassword(DEFAUTL_KEYSTORE_PRIVATE_PASSWORD);

        // Perform Test
        List<Alert> alerts = propertiesFileValidator.validate();

        // Verify
        assertThat(alerts.size(), is(0));
    }
}
