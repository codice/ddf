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
import java.util.List;
import java.util.Properties;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.admin.insecure.defaults.service.Alert.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UsersPropertiesFileValidator extends PropertiesFileValidator {

  static final String DEFAULT_CERT_USER_USED_MSG =
      "The default certificate user of [%s] was found in [%s].";

  static final String DEFAULT_CERT_USER_IS_USING_DEFAULT_PASSWORD_MSG =
      "The default certificate user of [%s] was found in [%s] with default password of [%s].";

  static final String DEFAULT_ADMIN_USER_IS_USING_DEFAULT_PASSWORD_MSG =
      "The default admin user of [%s] was found in [%s] with default password of [%s].";

  static final String CANNOT_PARSE_PASSWORD_MSG =
      "Unable to determine if [%s] is using insecure defaults. Cannot parse password from [%s].";

  static final String USERS_PROPERTIES_FILE_EXISTS_MSG =
      "The users.properties file is present at [%s].";

  private static final Logger LOGGER = LoggerFactory.getLogger(UsersPropertiesFileValidator.class);

  private String defaultAdminUser;

  private String defaultAdminUserPassword;

  private String defaultCertificateUser;

  private String defaultCertificateUserPassword;

  private SecurityLogger securityLogger;

  public void setDefaultAdminUser(String user) {
    this.defaultAdminUser = user;
  }

  public void setDefaultAdminUserPassword(String password) {
    this.defaultAdminUserPassword = password;
  }

  public void setDefaultCertificateUser(String user) {
    this.defaultCertificateUser = user;
  }

  public void setDefaultCertificateUserPassword(String password) {
    this.defaultCertificateUserPassword = password;
  }

  public void setSecurityLogger(SecurityLogger securityLogger) {
    this.securityLogger = securityLogger;
  }

  @Override
  public List<Alert> validate() {
    resetAlerts();
    Properties properties = readFile(false);

    if (properties != null && properties.size() > 0) {
      // the existence of the properties file is an insecure default
      securityLogger.audit("System is running with the users.properties file.");
      alerts.add(
          new Alert(Level.WARN, String.format(USERS_PROPERTIES_FILE_EXISTS_MSG, path.toString())));
      validateAdminUser(properties);
      validateCertificateUser(properties);
    }

    for (Alert alert : alerts) {
      LOGGER.debug("Alert: {}, {}", alert.getLevel(), alert.getMessage());
    }

    return alerts;
  }

  private void validateCertificateUser(Properties properties) {
    String value = properties.getProperty(defaultCertificateUser);

    if (value != null) {
      alerts.add(
          new Alert(
              Level.WARN,
              String.format(DEFAULT_CERT_USER_USED_MSG, defaultCertificateUser, path.toString())));

      String password = getPassword(value);

      if (StringUtils.equals(password, defaultCertificateUserPassword)) {
        alerts.add(
            new Alert(
                Level.WARN,
                String.format(
                    DEFAULT_CERT_USER_IS_USING_DEFAULT_PASSWORD_MSG,
                    defaultCertificateUser,
                    path,
                    defaultCertificateUserPassword)));
      }
    }
  }

  private void validateAdminUser(Properties properties) {
    String user = properties.getProperty(defaultAdminUser);
    String password = null;

    if (StringUtils.isNotBlank(user)) {
      password = getPassword(user);

      if (StringUtils.equals(password, defaultAdminUserPassword)) {
        alerts.add(
            new Alert(
                Level.WARN,
                String.format(
                    DEFAULT_ADMIN_USER_IS_USING_DEFAULT_PASSWORD_MSG,
                    defaultAdminUser,
                    path,
                    defaultAdminUserPassword)));
      }
    }
  }

  private String getPassword(String value) {
    String[] parts = StringUtils.split(value, ",");

    String password = null;

    if (parts != null && parts.length >= 1) {
      password = parts[0];
    } else {
      alerts.add(new Alert(Level.WARN, String.format(CANNOT_PARSE_PASSWORD_MSG, path, value)));
    }

    return password;
  }
}
