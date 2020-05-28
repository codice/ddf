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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;

public class EncryptionPropertiesFileValidatorTest {

  private static final String ISSUER_ENCRYPTION_PROPERTIES_FILE_WITH_DEFAULTS =
      "/issuerEncryptionWithDefaults.properties";

  private static final String FAKE_ENCRYPTION_PROPERTIES_FILE = "/fakeencryption.properties";

  private static final String ISSUER_ENCRYPTION_PROPERTIES_FILE_WITH_NON_DEFAULTS =
      "/issuerEncryptionNondefaults.properties";

  private static final String ISSUER_ENCRYPTION_PROPERTIES_FILE_WITH_DEFAULT_KEYSTORE_ALIAS =
      "/issuerEncryptionDefaultAlias.properties";

  private static final String ISSUER_ENCRYPTION_PROPERTIES_FILE_WITH_DEFAULT_KEYSTORE_PASSWORD =
      "/issuerEncryptionDefaultPassword.properties";

  private static final String SERVER_ENCRYPTION_PROPERTIES_FILE_WITH_DEFAULTS =
      "/serverEncryptionWithDefaults.properties";

  private static final String SERVER_ENCRYPTION_PROPERTIES_FILE_WITH_NON_DEFAULTS =
      "/serverEncryptionWithNonDefaults.properties";

  private static final String DEFAULT_KEYSTORE_PASSWORD = "changeit";

  private static final String DEFAULT_KEYSTORE_ALIAS = "localhost";

  private static final String DEFAULT_KEYSTORE_PRIVATE_PASSWORD = "changeit";

  private static final String NO_DEFAULT_PASSWORD =
      "No default password provided to the validator.";

  private static final String NO_DEFAULT_ALIAS =
      "No default keystore alias provided to the validator";

  private static final String NO_KEYSTORE_ALIAS = "Could not find keystore alias";

  private static final String NO_PASSWORD = "Could not find password";

  @Test
  public void testEncryptionPropertiesFileDoesNotExist() throws Exception {
    // Setup
    EncryptionPropertiesFileValidator propertiesFileValidator =
        new EncryptionPropertiesFileValidator();

    Path fakeEncrypPropsFilePath = Paths.get(FAKE_ENCRYPTION_PROPERTIES_FILE);

    propertiesFileValidator.setPath(fakeEncrypPropsFilePath);
    propertiesFileValidator.setDefaultPassword(DEFAULT_KEYSTORE_PASSWORD);
    propertiesFileValidator.setDefaultAlias(DEFAULT_KEYSTORE_ALIAS);

    // Perform Test
    List<Alert> alerts = propertiesFileValidator.validate();

    // Verify
    assertThat(alerts.size(), is(1));
    assertThat(
        alerts.get(0).getMessage(),
        is(
            String.format(
                EncryptionPropertiesFileValidator.GENERIC_INSECURE_DEFAULTS_MSG,
                fakeEncrypPropsFilePath)));
  }

  @Test
  public void testIssuerEncryptionPropertiesFileHasDefaultKeystoreAliasAndDefaultKeystorePassword()
      throws Exception {
    // Setup
    EncryptionPropertiesFileValidator propertiesFileValidator =
        new EncryptionPropertiesFileValidator();
    Path path =
        Paths.get(getClass().getResource(ISSUER_ENCRYPTION_PROPERTIES_FILE_WITH_DEFAULTS).toURI());
    propertiesFileValidator.setPath(path);
    propertiesFileValidator.setDefaultPassword(DEFAULT_KEYSTORE_PASSWORD);
    propertiesFileValidator.setDefaultAlias(DEFAULT_KEYSTORE_ALIAS);

    // Perform Test
    List<Alert> alerts = propertiesFileValidator.validate();

    // Verify
    List<String> actualAlertMessages = getActualAlertMessages(alerts);
    String[] expectedAlertMessages =
        new String[] {
          String.format(
              EncryptionPropertiesFileValidator.DEFAULT_KEYSTORE_ALIAS_USED_MSG,
              EncryptionPropertiesFileValidator.KEYSTORE_ALIAS_PROPERTY,
              path,
              DEFAULT_KEYSTORE_ALIAS),
          String.format(
              EncryptionPropertiesFileValidator.DEFAULT_KEYSTORE_PASSWORD_USED_MSG,
              EncryptionPropertiesFileValidator.KEYSTORE_PASSWORD_PROPERTY,
              path,
              DEFAULT_KEYSTORE_PASSWORD)
        };
    assertThat(alerts.size(), is(2));
    assertThat(actualAlertMessages, hasItems(expectedAlertMessages));
  }

  @Test
  public void
      testIssuerEncryptionPropertiesFileHasDefaultKeystoreAliasAndNondefaultKeystorePassword()
          throws Exception {
    // Setup
    EncryptionPropertiesFileValidator propertiesFileValidator =
        new EncryptionPropertiesFileValidator();
    Path path =
        Paths.get(
            getClass()
                .getResource(ISSUER_ENCRYPTION_PROPERTIES_FILE_WITH_DEFAULT_KEYSTORE_ALIAS)
                .toURI());
    propertiesFileValidator.setPath(path);
    propertiesFileValidator.setDefaultPassword(DEFAULT_KEYSTORE_PASSWORD);
    propertiesFileValidator.setDefaultAlias(DEFAULT_KEYSTORE_ALIAS);

    // Perform Test
    List<Alert> alerts = propertiesFileValidator.validate();

    // Verify
    assertThat(alerts.size(), is(1));
    assertThat(
        alerts.get(0).getMessage(),
        is(
            String.format(
                EncryptionPropertiesFileValidator.DEFAULT_KEYSTORE_ALIAS_USED_MSG,
                EncryptionPropertiesFileValidator.KEYSTORE_ALIAS_PROPERTY,
                path,
                DEFAULT_KEYSTORE_ALIAS)));
  }

  @Test
  public void
      testIssuerEncryptionPropertiesFileHasDefaultKeystorePasswordAndNondefaultKeystoreAlias()
          throws Exception {
    // Setup
    EncryptionPropertiesFileValidator propertiesFileValidator =
        new EncryptionPropertiesFileValidator();
    Path path =
        Paths.get(
            getClass()
                .getResource(ISSUER_ENCRYPTION_PROPERTIES_FILE_WITH_DEFAULT_KEYSTORE_PASSWORD)
                .toURI());
    propertiesFileValidator.setPath(path);
    propertiesFileValidator.setDefaultPassword(DEFAULT_KEYSTORE_PASSWORD);
    propertiesFileValidator.setDefaultAlias(DEFAULT_KEYSTORE_ALIAS);

    // Perform Test
    List<Alert> alerts = propertiesFileValidator.validate();

    // Verify
    assertThat(alerts.size(), is(1));
    assertThat(
        alerts.get(0).getMessage(),
        is(
            String.format(
                EncryptionPropertiesFileValidator.DEFAULT_KEYSTORE_PASSWORD_USED_MSG,
                EncryptionPropertiesFileValidator.KEYSTORE_PASSWORD_PROPERTY,
                path,
                DEFAULT_KEYSTORE_PASSWORD)));
  }

  @Test
  public void testIssuerEncryptionPropertiesFileHasNondefaults() throws Exception {
    // Setup
    EncryptionPropertiesFileValidator propertiesFileValidator =
        new EncryptionPropertiesFileValidator();
    propertiesFileValidator.setPath(
        Paths.get(
            getClass().getResource(ISSUER_ENCRYPTION_PROPERTIES_FILE_WITH_NON_DEFAULTS).toURI()));
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
    EncryptionPropertiesFileValidator propertiesFileValidator =
        new EncryptionPropertiesFileValidator();
    Path path =
        Paths.get(getClass().getResource(SERVER_ENCRYPTION_PROPERTIES_FILE_WITH_DEFAULTS).toURI());
    propertiesFileValidator.setPath(path);
    propertiesFileValidator.setDefaultPassword(DEFAULT_KEYSTORE_PASSWORD);
    propertiesFileValidator.setDefaultAlias(DEFAULT_KEYSTORE_ALIAS);
    propertiesFileValidator.setDefaultPrivateKeyPassword(DEFAULT_KEYSTORE_PRIVATE_PASSWORD);

    // Perform Test
    List<Alert> alerts = propertiesFileValidator.validate();

    // Verify
    List<String> actualAlertMessages = getActualAlertMessages(alerts);
    String[] expectedAlertMessages =
        new String[] {
          String.format(
              EncryptionPropertiesFileValidator.DEFAULT_KEYSTORE_ALIAS_USED_MSG,
              EncryptionPropertiesFileValidator.KEYSTORE_ALIAS_PROPERTY,
              path,
              DEFAULT_KEYSTORE_ALIAS),
          String.format(
              EncryptionPropertiesFileValidator.DEFAULT_KEYSTORE_PASSWORD_USED_MSG,
              EncryptionPropertiesFileValidator.KEYSTORE_PASSWORD_PROPERTY,
              path,
              DEFAULT_KEYSTORE_PASSWORD),
          String.format(
              EncryptionPropertiesFileValidator.DEFAULT_KEYSTORE_PRIVATE_PASSWORD_USED_MSG,
              EncryptionPropertiesFileValidator.PRIVATE_KEY_PASSWORD_PROPERTY,
              path,
              DEFAULT_KEYSTORE_PRIVATE_PASSWORD)
        };
    assertThat(alerts.size(), is(3));
    assertThat(actualAlertMessages, hasItems(expectedAlertMessages));
  }

  @Test
  public void testServerEncryptionPropertiesFileHasNondefaults() throws Exception {
    // Setup
    EncryptionPropertiesFileValidator propertiesFileValidator =
        new EncryptionPropertiesFileValidator();
    propertiesFileValidator.setPath(
        Paths.get(
            getClass().getResource(SERVER_ENCRYPTION_PROPERTIES_FILE_WITH_NON_DEFAULTS).toURI()));
    propertiesFileValidator.setDefaultPassword(DEFAULT_KEYSTORE_PASSWORD);
    propertiesFileValidator.setDefaultAlias(DEFAULT_KEYSTORE_ALIAS);
    propertiesFileValidator.setDefaultPrivateKeyPassword(DEFAULT_KEYSTORE_PRIVATE_PASSWORD);

    // Perform Test
    List<Alert> alerts = propertiesFileValidator.validate();

    // Verify
    assertThat(alerts.size(), is(0));
  }

  @Test
  public void testServerEncryptionPropertiesFileNullDefaultPassword() throws Exception {
    EncryptionPropertiesFileValidator propertiesFileValidator =
        new EncryptionPropertiesFileValidator();
    Path path =
        Paths.get(getClass().getResource(SERVER_ENCRYPTION_PROPERTIES_FILE_WITH_DEFAULTS).toURI());
    propertiesFileValidator.setPath(path);
    propertiesFileValidator.setDefaultPassword(null);
    propertiesFileValidator.setDefaultAlias(DEFAULT_KEYSTORE_ALIAS);
    propertiesFileValidator.setDefaultPrivateKeyPassword(DEFAULT_KEYSTORE_PRIVATE_PASSWORD);

    List<Alert> alerts = propertiesFileValidator.validate();

    assertThat(
        "Should return a warning about the default password.",
        alerts.get(0).getMessage(),
        containsString(NO_DEFAULT_PASSWORD));
  }

  @Test
  public void testServerEncryptionPropertiesFileNullDefaultAlias() throws Exception {
    EncryptionPropertiesFileValidator propertiesFileValidator =
        new EncryptionPropertiesFileValidator();
    Path path =
        Paths.get(getClass().getResource(SERVER_ENCRYPTION_PROPERTIES_FILE_WITH_DEFAULTS).toURI());
    propertiesFileValidator.setPath(path);
    propertiesFileValidator.setDefaultPassword(DEFAULT_KEYSTORE_PASSWORD);
    propertiesFileValidator.setDefaultAlias(null);
    propertiesFileValidator.setDefaultPrivateKeyPassword(DEFAULT_KEYSTORE_PRIVATE_PASSWORD);

    List<Alert> alerts = propertiesFileValidator.validate();

    assertThat(
        "Should return a warning about the default alias.",
        alerts.get(2).getMessage(),
        containsString(NO_DEFAULT_ALIAS));
  }

  @Test
  public void testServerEncryptionPropertiesFileNullAlias() throws Exception {
    final Properties testProp = mock(Properties.class);
    EncryptionPropertiesFileValidator propertiesFileValidator =
        new EncryptionPropertiesFileValidator() {
          @Override
          public List<Alert> validate() {
            validateAlias(testProp);
            return alerts;
          }
        };
    Path path =
        Paths.get(getClass().getResource(SERVER_ENCRYPTION_PROPERTIES_FILE_WITH_DEFAULTS).toURI());
    propertiesFileValidator.setPath(path);
    propertiesFileValidator.setDefaultPassword(DEFAULT_KEYSTORE_PASSWORD);
    propertiesFileValidator.setDefaultAlias(DEFAULT_KEYSTORE_ALIAS);
    propertiesFileValidator.setDefaultPrivateKeyPassword(DEFAULT_KEYSTORE_PRIVATE_PASSWORD);

    when(testProp.getProperty(anyString())).thenReturn(StringUtils.EMPTY);

    List<Alert> alerts = propertiesFileValidator.validate();

    assertThat(
        "Should return a warning about the alias.",
        alerts.get(0).getMessage(),
        containsString(NO_KEYSTORE_ALIAS));
  }

  @Test
  public void testServerEncryptionPropertiesFileNullPassword() throws Exception {
    final Properties testProp = mock(Properties.class);
    EncryptionPropertiesFileValidator propertiesFileValidator =
        new EncryptionPropertiesFileValidator() {
          @Override
          public List<Alert> validate() {
            validateKeystorePassword(testProp);
            return alerts;
          }
        };
    Path path =
        Paths.get(getClass().getResource(SERVER_ENCRYPTION_PROPERTIES_FILE_WITH_DEFAULTS).toURI());
    propertiesFileValidator.setPath(path);
    propertiesFileValidator.setDefaultPassword(DEFAULT_KEYSTORE_PASSWORD);
    propertiesFileValidator.setDefaultAlias(DEFAULT_KEYSTORE_ALIAS);
    propertiesFileValidator.setDefaultPrivateKeyPassword(DEFAULT_KEYSTORE_PRIVATE_PASSWORD);

    when(testProp.getProperty(anyString())).thenReturn(StringUtils.EMPTY);

    List<Alert> alerts = propertiesFileValidator.validate();

    assertThat(
        "Should return a warning about the password.",
        alerts.get(0).getMessage(),
        containsString(NO_PASSWORD));
  }

  private List<String> getActualAlertMessages(List<Alert> alerts) {
    List<String> messages = new ArrayList<>(alerts.size());
    for (Alert alert : alerts) {
      messages.add(alert.getMessage());
    }
    return messages;
  }
}
