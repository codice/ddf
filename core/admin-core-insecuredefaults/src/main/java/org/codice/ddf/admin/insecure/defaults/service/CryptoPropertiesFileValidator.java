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

    private String defaultPassword;

    private String defaultAlias;
    
    protected String defaultPrivateKeyPassword;

    static final String KEYSTORE_PASSWORD_PROPERTY = "org.apache.ws.security.crypto.merlin.keystore.password";

    static final String KEYSTORE_ALIAS_PROPERTY = "org.apache.ws.security.crypto.merlin.keystore.alias";
    
    static final String PRIVATE_KEY_PASSWORD_PROPERTY = "org.apache.ws.security.crypto.merlin.keystore.private.password";

    static final String DEFAULT_KEYSTORE_PRIVATE_PASSWORD_USED_MSG = "The property [%s] in [%s] is set to the default keystore private password of [%s].";

    static final String DEFAULT_KEYSTORE_ALIAS_USED_MSG = "The property [%s] in [%s] is set to the default keystore alias of [%s].";

    static final String DEFAULT_KEYSTORE_PASSWORD_USED_MSG = "The property [%s] in [%s] is set to the default keystore password of [%s].";

    static final String NO_DEFAULT_PASSWORD_PROVIDED_TO_VALIDATOR_MSG = "Unable to determine if [%s] is using a default keystore password. No default password provided to the validator.";

    static final String COULD_NOT_FIND_PASSWORD_IN_PROPS_FILE_MSG = "Unable to determine if [%s] is using a default keystore password. Could not find password in [%s].";

    static final String COULD_NOT_FIND_ALIAS_IN_PROPS_FILE_MSG = "Unable to determine if [%s] is using a default keystore alias. Could not find keystore alias in [%s]";

    static final String NO_DEFAULT_ALIAS_PROVIDED_TO_VALIDATOR_MSG = "Unable to determine if [%s] is using a default keystore alias. No default keystore alias provided to the validator.";

    public void setDefaultPassword(String password) {
        this.defaultPassword = password;
    }

    public void setDefaultAlias(String alias) {
        this.defaultAlias = alias;
    }
    
    public void setDefaultPrivateKeyPassword(String password) {
        this.defaultPrivateKeyPassword = password;
    }

    public abstract List<Alert> validate();

    protected void validateKeystorePassword(Properties properties) {
        String password = properties.getProperty(KEYSTORE_PASSWORD_PROPERTY);

        if (StringUtils.isBlank(defaultPassword)) {
            alerts.add(new Alert(Level.WARN, String.format(
                    NO_DEFAULT_PASSWORD_PROVIDED_TO_VALIDATOR_MSG, path)));

        }

        if (StringUtils.isBlank(password)) {
            alerts.add(new Alert(Level.WARN, String.format(
                    COULD_NOT_FIND_PASSWORD_IN_PROPS_FILE_MSG, path.toString(), path)));
        }

        if (StringUtils.equals(password, defaultPassword)) {
            alerts.add(new Alert(Level.WARN, String.format(DEFAULT_KEYSTORE_PASSWORD_USED_MSG,
                    KEYSTORE_PASSWORD_PROPERTY, path, defaultPassword)));
        }
    }

    protected void validateAlias(Properties properties) {
        String alias = properties.getProperty(KEYSTORE_ALIAS_PROPERTY);

        if (StringUtils.isBlank(defaultAlias)) {
            alerts.add(new Alert(Level.WARN, String.format(
                    NO_DEFAULT_ALIAS_PROVIDED_TO_VALIDATOR_MSG, path)));
        }

        if (StringUtils.isBlank(alias)) {
            alerts.add(new Alert(Level.WARN, String.format(COULD_NOT_FIND_ALIAS_IN_PROPS_FILE_MSG,
                    path.toString(), path)));
        }

        if (StringUtils.equals(alias, defaultAlias)) {
            alerts.add(new Alert(Level.WARN, String.format(DEFAULT_KEYSTORE_ALIAS_USED_MSG,
                    KEYSTORE_ALIAS_PROPERTY, path, defaultAlias)));
        }
    }
    
    protected abstract void validatePrivateKeyPassword(Properties properties);
}
