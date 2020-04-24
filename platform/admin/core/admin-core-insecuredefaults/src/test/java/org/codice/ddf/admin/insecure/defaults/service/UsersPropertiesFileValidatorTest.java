/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.admin.insecure.defaults.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import ddf.security.audit.SecurityLogger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class UsersPropertiesFileValidatorTest {

  private static final String USERS_PROPERTIES_FILE_WITH_DEFAULTS = "/usersWithDefaults.properties";

  private static final String USERS_PROPERTIES_FILE_WITH_NON_DEFAULTS =
      "/usersWithNondefaults.properties";

  private static final String USERS_PROPERTIES_FILE_WITH_NON_DEFAULTS_WITH_ADMIN_USER =
      "/usersWithNondefaultsWithAdminUser.properties";

  private static final String FAKE_USERS_PROPERTIES_FILE = "/fakeUsers.properties";

  private static final String DEFAULT_ADMIN_USER = "admin";

  private static final String DEFAULT_ADMIN_USER_PASSWORD = "admin";

  private static final String DEFAULT_CERTIFICATE_USER = "localhost";

  private static final String DEFAULT_CERTIFICATE_USER_PASSWORD = "localhost";

  @Test
  public void testUsersPropertiesFileDoesNotExist() throws Exception {
    // Setup
    UsersPropertiesFileValidator propertiesFileValidator = new UsersPropertiesFileValidator();
    propertiesFileValidator.setSecurityLogger(mock(SecurityLogger.class));
    propertiesFileValidator.setPath(Paths.get(FAKE_USERS_PROPERTIES_FILE));
    propertiesFileValidator.setDefaultAdminUser(DEFAULT_ADMIN_USER);
    propertiesFileValidator.setDefaultAdminUserPassword(DEFAULT_ADMIN_USER_PASSWORD);
    propertiesFileValidator.setDefaultCertificateUser(DEFAULT_CERTIFICATE_USER);
    propertiesFileValidator.setDefaultCertificateUserPassword(DEFAULT_CERTIFICATE_USER_PASSWORD);

    // Perform Test
    List<Alert> alerts = propertiesFileValidator.validate();

    // Verify
    // It is acceptable for there to be no users.properties file
    assertThat(alerts.size(), is(0));
  }

  @Test
  public void testUsersPropertiesFileHasDefaults() throws Exception {
    // Setup
    UsersPropertiesFileValidator propertiesFileValidator = new UsersPropertiesFileValidator();
    Path path = Paths.get(getClass().getResource(USERS_PROPERTIES_FILE_WITH_DEFAULTS).toURI());
    propertiesFileValidator.setSecurityLogger(mock(SecurityLogger.class));
    propertiesFileValidator.setPath(path);
    propertiesFileValidator.setDefaultAdminUser(DEFAULT_ADMIN_USER);
    propertiesFileValidator.setDefaultAdminUserPassword(DEFAULT_ADMIN_USER_PASSWORD);
    propertiesFileValidator.setDefaultCertificateUser(DEFAULT_CERTIFICATE_USER);
    propertiesFileValidator.setDefaultCertificateUserPassword(DEFAULT_CERTIFICATE_USER_PASSWORD);

    // Perform Test
    List<Alert> alerts = propertiesFileValidator.validate();

    // Verify
    List<String> actualAlertMessages = getActualAlertMessages(alerts);
    String[] expectedAlertMessages =
        new String[] {
          String.format(
              UsersPropertiesFileValidator.DEFAULT_CERT_USER_USED_MSG,
              DEFAULT_CERTIFICATE_USER,
              path),
          String.format(
              UsersPropertiesFileValidator.DEFAULT_CERT_USER_IS_USING_DEFAULT_PASSWORD_MSG,
              DEFAULT_CERTIFICATE_USER,
              path,
              DEFAULT_CERTIFICATE_USER_PASSWORD),
          String.format(
              UsersPropertiesFileValidator.DEFAULT_ADMIN_USER_IS_USING_DEFAULT_PASSWORD_MSG,
              DEFAULT_ADMIN_USER,
              path,
              DEFAULT_ADMIN_USER_PASSWORD)
        };
    assertThat(alerts.size(), is(4));
    assertThat(actualAlertMessages, hasItems(expectedAlertMessages));
  }

  @Test
  public void testUsersPropertiesFileHasNondefaults() throws Exception {
    // Setup
    UsersPropertiesFileValidator propertiesFileValidator = new UsersPropertiesFileValidator();
    propertiesFileValidator.setSecurityLogger(mock(SecurityLogger.class));
    propertiesFileValidator.setPath(
        Paths.get(getClass().getResource(USERS_PROPERTIES_FILE_WITH_NON_DEFAULTS).toURI()));
    propertiesFileValidator.setDefaultAdminUser(DEFAULT_ADMIN_USER);
    propertiesFileValidator.setDefaultAdminUserPassword(DEFAULT_ADMIN_USER_PASSWORD);
    propertiesFileValidator.setDefaultCertificateUser(DEFAULT_CERTIFICATE_USER);
    propertiesFileValidator.setDefaultCertificateUserPassword(DEFAULT_CERTIFICATE_USER_PASSWORD);

    // Perform Test
    List<Alert> alerts = propertiesFileValidator.validate();

    // Verify
    assertThat(alerts.size(), is(1));
  }

  @Test
  public void testUsersPropertiesFileHasNondefaultsWithAdminUser() throws Exception {
    // Setup
    UsersPropertiesFileValidator propertiesFileValidator = new UsersPropertiesFileValidator();
    propertiesFileValidator.setSecurityLogger(mock(SecurityLogger.class));
    propertiesFileValidator.setPath(
        Paths.get(
            getClass()
                .getResource(USERS_PROPERTIES_FILE_WITH_NON_DEFAULTS_WITH_ADMIN_USER)
                .toURI()));
    propertiesFileValidator.setDefaultAdminUser(DEFAULT_ADMIN_USER);
    propertiesFileValidator.setDefaultAdminUserPassword(DEFAULT_ADMIN_USER_PASSWORD);
    propertiesFileValidator.setDefaultCertificateUser(DEFAULT_CERTIFICATE_USER);
    propertiesFileValidator.setDefaultCertificateUserPassword(DEFAULT_CERTIFICATE_USER_PASSWORD);

    // Perform Test
    List<Alert> alerts = propertiesFileValidator.validate();

    // Verify
    assertThat(alerts.size(), is(1));
  }

  private List<String> getActualAlertMessages(List<Alert> alerts) {
    List<String> messages = new ArrayList<>(alerts.size());
    for (Alert alert : alerts) {
      messages.add(alert.getMessage());
    }
    return messages;
  }
}
