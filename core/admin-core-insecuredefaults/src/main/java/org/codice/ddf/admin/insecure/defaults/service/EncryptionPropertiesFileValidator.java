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

public class EncryptionPropertiesFileValidator extends CryptoPropertiesFileValidator {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(EncryptionPropertiesFileValidator.class);

    public List<Alert> validate() {
        resetAlerts();
        Properties properties = readFile();

        if (properties != null && properties.size() > 0) {
            validateKeystorePassword(properties);
            validatePrivateKeyPassword(properties);
            validateAlias(properties);
        }
        
        for (Alert alert : alerts) {
            LOGGER.debug("Alert: {}, {}", alert.getLevel(), alert.getMessage());
        }

        return alerts;
    }
    
    protected void validatePrivateKeyPassword(Properties properties) {

        String privateKeyPassword = properties.getProperty(PRIVATE_KEY_PASSWORD_PROPERTY);

        if (StringUtils.isNotBlank(privateKeyPassword)
                && StringUtils.isNotBlank(defaultPrivateKeyPassword)
                && StringUtils.equals(privateKeyPassword, defaultPrivateKeyPassword)) {
            alerts.add(new Alert(Level.WARN, String.format(
                    DEFAULT_KEYSTORE_PRIVATE_PASSWORD_USED_MSG, PRIVATE_KEY_PASSWORD_PROPERTY,
                    path, defaultPrivateKeyPassword)));
        }

    }
}
