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

import ddf.security.audit.SecurityLogger;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.codice.ddf.configuration.AbsolutePathResolver;
import org.codice.ddf.system.alerts.NoticePriority;
import org.codice.ddf.system.alerts.SystemNotice;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InsecureDefaultsCollector implements Runnable {

  private static final Logger LOGGER = LoggerFactory.getLogger(InsecureDefaultsCollector.class);

  private static final String BLACK_LIST =
      new AbsolutePathResolver("etc/keystores/blacklisted.jks").getPath();

  private static final String BLACK_LIST_PASSWORD = "changeit";

  private static final String DEFAULT_KEYSTORE_PASSWORD = "changeit";

  private static final String DEFAULT_KEYSTORE_ALIAS = "localhost";

  private static final String DEFAULT_KEY_PASSWORD = "changeit";

  private static final String DEFAULT_TRUSTSTORE_PASSWORD = "changeit";

  private static final String ISSUER_ENCRYPTION_PROPERTIES_FILE =
      new AbsolutePathResolver("etc/ws-security/issuer/encryption.properties").getPath();

  private static final String ISSUER_SIGNATURE_PROPERTIES_FILE =
      new AbsolutePathResolver("etc/ws-security/issuer/signature.properties").getPath();

  private static final String SERVER_ENCRYPTION_PROPERTIES_FILE =
      new AbsolutePathResolver("etc/ws-security/server/encryption.properties").getPath();

  private static final String SERVER_SIGNATURE_PROPERTIES_FILE =
      new AbsolutePathResolver("etc/ws-security/server/signature.properties").getPath();

  private static final String USERS_PROPERTIES_FILE =
      new AbsolutePathResolver("etc/users.properties").getPath();

  private static final String DEFAULT_ADMIN_USER = "admin";

  private static final String DEFAULT_ADMIN_USER_PASSWORD = "admin";

  private static final String DEFAULT_CERTIFICATE_USER = "localhost";

  private static final String DEFAULT_CERTIFICATE_USER_PASSWORD = "localhost";

  private static final String PAX_WEB_CFG_FILE =
      new AbsolutePathResolver("etc/org.ops4j.pax.web.cfg").getPath();

  private static final String KEYSTORE_SYSTEM_PROPERTY = "javax.net.ssl.keyStore";

  private static final String KEYSTORE_PASSWORD_SYSTEM_PROPERTY = "javax.net.ssl.keyStorePassword";

  private static final String TRUSTSTORE_SYSTEM_PROPERTY = "javax.net.ssl.trustStore";

  private static final String TRUSTSTORE_PASSWORD_SYSTEM_PROPERTY =
      "javax.net.ssl.trustStorePassword";

  private List<Validator> validators = new ArrayList<>();

  private final EventAdmin eventAdmin;

  private SecurityLogger securityLogger;

  public InsecureDefaultsCollector(EventAdmin eventAdmin, SecurityLogger securityLogger) {
    this.eventAdmin = eventAdmin;
    this.securityLogger = securityLogger;
    addValidators();
  }

  @Override
  public void run() {
    List<Alert> alerts = new ArrayList<>();

    LOGGER.debug("Found {} validator(s)", validators.size());

    for (Validator validator : validators) {
      LOGGER.debug("{} is starting validation process.", validator.getClass().getSimpleName());
      alerts.addAll(validator.validate());
      LOGGER.debug("{} finished the validation process.", validator.getClass().getSimpleName());
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("There are {} total alert(s).", alerts.size());
      for (Alert alert : alerts) {
        LOGGER.debug("Alert: {}; {}", alert.getLevel(), alert.getMessage());
      }
    }
    if (!alerts.isEmpty()) {
      Set<String> alertMessages =
          alerts.stream().map(Alert::getMessage).collect(Collectors.toSet());
      SystemNotice notice =
          new SystemNotice(
              this.getClass().getName(),
              NoticePriority.IMPORTANT,
              "Insecure Defaults Found",
              alertMessages);
      eventAdmin.postEvent(
          new Event(
              SystemNotice.SYSTEM_NOTICE_BASE_TOPIC + "insecureDefaults", notice.getProperties()));
    }
  }

  void addValidators() {
    addKeystoreValidator();
    addTruststoreValidator();
    addIssuerEncryptionPropertiesFileValidator();
    addIssuerSignaturePropertiesFileValidator();
    addServerEncryptionPropertiesFileValidator();
    addServerSignaturePropertiesFileValidator();
    addUsersPropertiesFileValidator();
    addPaxWebCfgFileValidator();
    addPlatformGlobalConfigurationValidator();
  }

  void setValidators(List<Validator> validatorsList) {
    validators = validatorsList;
  }

  private void addKeystoreValidator() {
    KeystoreValidator keystoreValidator = new KeystoreValidator();
    keystoreValidator.setBlacklistKeystorePath(Paths.get(BLACK_LIST));
    keystoreValidator.setBlacklistKeystorePassword(BLACK_LIST_PASSWORD);
    keystoreValidator.setKeystorePath(Paths.get(getKeystorePath()));
    keystoreValidator.setKeystorePassword(getKeystorePassword());
    keystoreValidator.setDefaultKeystorePassword(DEFAULT_KEYSTORE_PASSWORD);
    keystoreValidator.setDefaultKeyPassword(DEFAULT_KEY_PASSWORD);
    validators.add(keystoreValidator);
  }

  private void addTruststoreValidator() {
    KeystoreValidator truststoreValidator = new KeystoreValidator();
    truststoreValidator.setBlacklistKeystorePath(Paths.get(BLACK_LIST));
    truststoreValidator.setBlacklistKeystorePassword(BLACK_LIST_PASSWORD);
    truststoreValidator.setKeystorePath(Paths.get(getTruststorePath()));
    truststoreValidator.setKeystorePassword(getTruststorePassword());
    truststoreValidator.setDefaultKeystorePassword(DEFAULT_TRUSTSTORE_PASSWORD);
    validators.add(truststoreValidator);
  }

  private void addIssuerEncryptionPropertiesFileValidator() {
    EncryptionPropertiesFileValidator encryptionPropertiesFileValidator =
        new EncryptionPropertiesFileValidator();
    encryptionPropertiesFileValidator.setPath(Paths.get(ISSUER_ENCRYPTION_PROPERTIES_FILE));
    encryptionPropertiesFileValidator.setDefaultPassword(DEFAULT_KEYSTORE_PASSWORD);
    encryptionPropertiesFileValidator.setDefaultAlias(DEFAULT_KEYSTORE_ALIAS);
    validators.add(encryptionPropertiesFileValidator);
  }

  private void addIssuerSignaturePropertiesFileValidator() {
    SignaturePropertiesFileValidator issuerSignatureEncryptionPropertiesFileValidator =
        new SignaturePropertiesFileValidator();
    issuerSignatureEncryptionPropertiesFileValidator.setPath(
        Paths.get(ISSUER_SIGNATURE_PROPERTIES_FILE));
    issuerSignatureEncryptionPropertiesFileValidator.setDefaultPassword(DEFAULT_KEYSTORE_PASSWORD);
    issuerSignatureEncryptionPropertiesFileValidator.setDefaultAlias(DEFAULT_KEYSTORE_ALIAS);
    issuerSignatureEncryptionPropertiesFileValidator.setDefaultPrivateKeyPassword(
        DEFAULT_KEY_PASSWORD);
    validators.add(issuerSignatureEncryptionPropertiesFileValidator);
  }

  private void addServerEncryptionPropertiesFileValidator() {
    EncryptionPropertiesFileValidator encryptionPropertiesFileValidator =
        new EncryptionPropertiesFileValidator();
    encryptionPropertiesFileValidator.setPath(Paths.get(SERVER_ENCRYPTION_PROPERTIES_FILE));
    encryptionPropertiesFileValidator.setDefaultPassword(DEFAULT_KEYSTORE_PASSWORD);
    encryptionPropertiesFileValidator.setDefaultAlias(DEFAULT_KEYSTORE_ALIAS);
    encryptionPropertiesFileValidator.setDefaultPrivateKeyPassword(DEFAULT_KEY_PASSWORD);
    validators.add(encryptionPropertiesFileValidator);
  }

  private void addServerSignaturePropertiesFileValidator() {
    SignaturePropertiesFileValidator issuerSignatureEncryptionPropertiesFileValidator =
        new SignaturePropertiesFileValidator();
    issuerSignatureEncryptionPropertiesFileValidator.setPath(
        Paths.get(SERVER_SIGNATURE_PROPERTIES_FILE));
    issuerSignatureEncryptionPropertiesFileValidator.setDefaultPassword(DEFAULT_KEYSTORE_PASSWORD);
    issuerSignatureEncryptionPropertiesFileValidator.setDefaultAlias(DEFAULT_KEYSTORE_ALIAS);
    issuerSignatureEncryptionPropertiesFileValidator.setDefaultPrivateKeyPassword(
        DEFAULT_KEY_PASSWORD);
    validators.add(issuerSignatureEncryptionPropertiesFileValidator);
  }

  private void addUsersPropertiesFileValidator() {
    UsersPropertiesFileValidator userPropertiesFileValidator = new UsersPropertiesFileValidator();
    userPropertiesFileValidator.setSecurityLogger(securityLogger);
    userPropertiesFileValidator.setPath(Paths.get(USERS_PROPERTIES_FILE));
    userPropertiesFileValidator.setDefaultAdminUser(DEFAULT_ADMIN_USER);
    userPropertiesFileValidator.setDefaultAdminUserPassword(DEFAULT_ADMIN_USER_PASSWORD);
    userPropertiesFileValidator.setDefaultCertificateUser(DEFAULT_CERTIFICATE_USER);
    userPropertiesFileValidator.setDefaultCertificateUserPassword(
        DEFAULT_CERTIFICATE_USER_PASSWORD);
    validators.add(userPropertiesFileValidator);
  }

  private void addPaxWebCfgFileValidator() {
    PaxWebCfgFileValidator paxWebCfgFileValidator = new PaxWebCfgFileValidator();
    paxWebCfgFileValidator.setPath(Paths.get(PAX_WEB_CFG_FILE));
    validators.add(paxWebCfgFileValidator);
  }

  private void addPlatformGlobalConfigurationValidator() {
    PlatformGlobalConfigurationValidator platformGlobalConfigurationValidator =
        new PlatformGlobalConfigurationValidator();
    validators.add(platformGlobalConfigurationValidator);
  }

  private String getKeystorePath() {
    return new AbsolutePathResolver(System.getProperty(KEYSTORE_SYSTEM_PROPERTY)).getPath();
  }

  private String getKeystorePassword() {
    String keystorePassword = System.getProperty(KEYSTORE_PASSWORD_SYSTEM_PROPERTY);
    return keystorePassword;
  }

  private String getTruststorePath() {
    return new AbsolutePathResolver(System.getProperty(TRUSTSTORE_SYSTEM_PROPERTY)).getPath();
  }

  private String getTruststorePassword() {
    return System.getProperty(TRUSTSTORE_PASSWORD_SYSTEM_PROPERTY);
  }

  /** Package-private getter for validators list. Needed for unit tests. */
  List<Validator> getValidators() {
    return validators;
  }
}
