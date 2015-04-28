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

public class UsersPropertiesFileValidatorTest {

    private static final String USERS_PROPERTIES_FILE_WITH_DEFAULTS = "/usersWithDefaults.properties";

    private static final String USERS_PROPERTIES_FILE_WITH_NON_DEFAULTS = "/usersWithNondefaults.properties";

    private static final String USERS_PROPERTIES_FILE_WITH_NON_DEFAULTS_WITH_ADMIN_USER = "/usersWithNondefaultsWithAdminUser.properties";

    private static final String FAKE_USERS_PROPERTIES_FILE = "/fakeUsers.properties";

    private static final String DEFAULT_ADMIN_USER = "admin";

    private static final String DEFAULT_ADMIN_USER_PASSWORD = "admin";

    private static final String DEFAULT_CERTIFICATE_USER = "localhost";

    private static final String DEFAULT_CERTIFICATE_USER_PASSWORD = "localhost";

    @Test
    public void testUsersPropertiesFileDoesNotExist() throws Exception {
        // Setup
        UsersPropertiesFileValidator propertiesFileValidator = new UsersPropertiesFileValidator();
        propertiesFileValidator.setPath(Paths.get(FAKE_USERS_PROPERTIES_FILE));
        propertiesFileValidator.setDefaultAdminUser(DEFAULT_ADMIN_USER);
        propertiesFileValidator.setDefaultAdminUserPassword(DEFAULT_ADMIN_USER_PASSWORD);
        propertiesFileValidator.setDefaultCertificateUser(DEFAULT_CERTIFICATE_USER);
        propertiesFileValidator
                .setDefaultCertificateUserPassword(DEFAULT_CERTIFICATE_USER_PASSWORD);

        // Perform Test
        List<Alert> alerts = propertiesFileValidator.validate();

        // Verify
        assertThat(alerts.size(), is(1));
        assertThat(alerts.get(0).getMessage(), is("Unable to determine if ["
                + FAKE_USERS_PROPERTIES_FILE + "] is using insecure defaults. "
                + FAKE_USERS_PROPERTIES_FILE + " (No such file or directory)"));
    }

    @Test
    public void testUsersPropertiesFileHasDefaults() throws Exception {
        // Setup
        UsersPropertiesFileValidator propertiesFileValidator = new UsersPropertiesFileValidator();
        propertiesFileValidator.setPath(Paths.get(getClass().getResource(
                USERS_PROPERTIES_FILE_WITH_DEFAULTS).toURI()));
        propertiesFileValidator.setDefaultAdminUser(DEFAULT_ADMIN_USER);
        propertiesFileValidator.setDefaultAdminUserPassword(DEFAULT_ADMIN_USER_PASSWORD);
        propertiesFileValidator.setDefaultCertificateUser(DEFAULT_CERTIFICATE_USER);
        propertiesFileValidator
                .setDefaultCertificateUserPassword(DEFAULT_CERTIFICATE_USER_PASSWORD);

        // Perform Test
        List<Alert> alerts = propertiesFileValidator.validate();

        // Verify
        assertThat(alerts.size(), is(3));

        List<String> messages = new ArrayList<>();
        for (Alert alert : alerts) {
            messages.add(alert.getMessage());
        }
        Collections.sort(messages);

        assertThat(messages.get(0),
                containsString("The default admin user of [admin] was found in"));
        assertThat(messages.get(0), containsString(USERS_PROPERTIES_FILE_WITH_DEFAULTS));
        assertThat(messages.get(0), containsString("with default password of [admin]"));

        assertThat(messages.get(1),
                containsString("The default certificate user of [localhost] was found in"));
        assertThat(messages.get(1), containsString(USERS_PROPERTIES_FILE_WITH_DEFAULTS));
        assertThat(messages.get(1), containsString("with default password of [localhost]"));

        assertThat(messages.get(2),
                containsString("The default certificate user of [localhost] was found in"));
        assertThat(messages.get(2), containsString(USERS_PROPERTIES_FILE_WITH_DEFAULTS));
    }

    @Test
    public void testUsersPropertiesFileHasNondefaults() throws Exception {
        // Setup
        UsersPropertiesFileValidator propertiesFileValidator = new UsersPropertiesFileValidator();
        propertiesFileValidator.setPath(Paths.get(getClass().getResource(
                USERS_PROPERTIES_FILE_WITH_NON_DEFAULTS).toURI()));
        propertiesFileValidator.setDefaultAdminUser(DEFAULT_ADMIN_USER);
        propertiesFileValidator.setDefaultAdminUserPassword(DEFAULT_ADMIN_USER_PASSWORD);
        propertiesFileValidator.setDefaultCertificateUser(DEFAULT_CERTIFICATE_USER);
        propertiesFileValidator
                .setDefaultCertificateUserPassword(DEFAULT_CERTIFICATE_USER_PASSWORD);

        // Perform Test
        List<Alert> alerts = propertiesFileValidator.validate();

        // Verify
        assertThat(alerts.size(), is(0));
    }

    @Test
    public void testUsersPropertiesFileHasNondefaultsWithAdminUser() throws Exception {
        // Setup
        UsersPropertiesFileValidator propertiesFileValidator = new UsersPropertiesFileValidator();
        propertiesFileValidator.setPath(Paths.get(getClass().getResource(
                USERS_PROPERTIES_FILE_WITH_NON_DEFAULTS_WITH_ADMIN_USER).toURI()));
        propertiesFileValidator.setDefaultAdminUser(DEFAULT_ADMIN_USER);
        propertiesFileValidator.setDefaultAdminUserPassword(DEFAULT_ADMIN_USER_PASSWORD);
        propertiesFileValidator.setDefaultCertificateUser(DEFAULT_CERTIFICATE_USER);
        propertiesFileValidator
                .setDefaultCertificateUserPassword(DEFAULT_CERTIFICATE_USER_PASSWORD);

        // Perform Test
        List<Alert> alerts = propertiesFileValidator.validate();

        // Verify
        assertThat(alerts.size(), is(0));
    }
}
