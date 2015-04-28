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

public abstract class CryptoPropertiesFileValidator extends PropertiesFileValidator {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(CryptoPropertiesFileValidator.class);

    private static final String KEYSTORE_PASSWORD_PROPERTY = "org.apache.ws.security.crypto.merlin.keystore.password";

    private static final String KEYSTORE_ALIAS_PROPERTY = "org.apache.ws.security.crypto.merlin.keystore.alias";

    private String defaultPassword;

    private String defaultAlias;

    public void setDefaultPassword(String passwd) {
        this.defaultPassword = passwd;
    }

    public void setDefaultAlias(String alias) {
        this.defaultAlias = alias;
    }

    public abstract List<Alert> validate();

    protected void validateKeystorePassword(Properties properties) {
        String password = properties.getProperty(KEYSTORE_PASSWORD_PROPERTY);

        if (StringUtils.isBlank(defaultPassword)) {
            alerts.add(new Alert(
                    Level.WARN,
                    "Unable to determine if ["
                            + path.toString()
                            + "] is using a default keystore password. No default password provided to the validator."));
        }

        if (StringUtils.isBlank(password)) {
            alerts.add(new Alert(Level.WARN, "Unable to determine if [" + path.toString()
                    + "] is using a default password. Could not find password in ["
                    + path.toString() + "]."));
        }

        if (StringUtils.equals(password, defaultPassword)) {
            alerts.add(new Alert(Level.WARN, "The property [" + KEYSTORE_PASSWORD_PROPERTY
                    + "] in [" + path.toString() + "] is set to the default keystore password of ["
                    + defaultPassword + "]."));
        }
    }

    protected void validateAlias(Properties properties) {
        String alias = properties.getProperty(KEYSTORE_ALIAS_PROPERTY);

        if (StringUtils.isBlank(defaultAlias)) {
            alerts.add(new Alert(
                    Level.WARN,
                    "Unable to determine if ["
                            + path.toString()
                            + "] is using a default keystore alias. No default keystore alias provided to the validator."));
        }

        if (StringUtils.isBlank(alias)) {
            alerts.add(new Alert(Level.WARN, "Unable to determine if [" + path.toString()
                    + "] is using a default keystore alias. Could not find keystore alias in ["
                    + path.toString() + "]."));
        }

        if (StringUtils.equals(alias, defaultAlias)) {
            alerts.add(new Alert(Level.WARN, "The property [" + KEYSTORE_ALIAS_PROPERTY + "] in ["
                    + path.toString() + "] is set to the default keystore alias of ["
                    + defaultAlias + "]."));
        }
    }
}
