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

import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.admin.insecure.defaults.service.Alert.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UsersPropertiesFileValidator extends PropertiesFileValidator {
    
    private static final Logger LOGGER = LoggerFactory
            .getLogger(UsersPropertiesFileValidator.class);
    
    private String defaultAdminUser;

    private String defaultAdminUserPassword;

    private String defaultCertificateUser;

    private String defaultCertificateUserPassword;

    public void setDefaultAdminUser(String user) {
        this.defaultAdminUser = user;
    }

    public void setDefaultAdminUserPassword(String passwd) {
        this.defaultAdminUserPassword = passwd;
    }

    public void setDefaultCertificateUser(String user) {
        this.defaultCertificateUser = user;
    }

    public void setDefaultCertificateUserPassword(String passwd) {
        this.defaultCertificateUserPassword = passwd;
    }

    @Override
    public List<Alert> validate() {
        resetAlerts();
        Properties properties = readFile();

        if (properties != null && properties.size() > 0) {
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
            alerts.add(new Alert(Level.WARN, "The default certificate user of ["
                    + defaultCertificateUser + "] was found in [" + path.toString() + "]."));

            String password = getPassword(value);
            
            if (StringUtils.equals(password, defaultCertificateUserPassword)) {
                alerts.add(new Alert(Level.WARN, "The default certificate user of ["
                        + defaultCertificateUser + "] was found in [" + path.toString()
                        + "] with default password of [" + defaultCertificateUserPassword + "]."));
            }
        }
    }

    private void validateAdminUser(Properties properties) {
        String user = properties.getProperty(defaultAdminUser);
        String password = null;

        if (StringUtils.isNotBlank(user)) {
            password = getPassword(user);

            if (StringUtils.equals(password, defaultAdminUserPassword)) {
                alerts.add(new Alert(Level.WARN, "The default admin user of [" + defaultAdminUser
                        + "] was found in [" + path.toString() + "] with default password of ["
                        + defaultAdminUserPassword + "]."));
            }
        }
    }
    
    private String getPassword(String value) {
        String[] parts = StringUtils.split(value, ",");

        String password = null;

        if (parts != null && parts.length >= 1) {
            password = parts[0];
        } else {
            alerts.add(new Alert(Level.WARN, "Unable to determine if [" + path.toString()
                    + "] is using insecure defaults. Cannot parse password from [" + value + "]."));
        }

        return password;
    }

}
