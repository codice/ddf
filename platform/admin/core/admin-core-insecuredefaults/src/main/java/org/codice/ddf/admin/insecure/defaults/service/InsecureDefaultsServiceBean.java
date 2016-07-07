/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.admin.insecure.defaults.service;

import java.lang.management.ManagementFactory;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.codice.ddf.configuration.AbsolutePathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InsecureDefaultsServiceBean implements InsecureDefaultsServiceBeanMBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(InsecureDefaultsServiceBean.class);

    private static final String BLACK_LIST =
            new AbsolutePathResolver("etc/keystores/blacklisted.jks").getPath();

    private static final String BLACK_LIST_PASSWORD = "changeit";

    private static final String DEFAULT_KEYSTORE_PASSWORD = "changeit";

    private static final String DEFAULT_KEYSTORE_ALIAS = "localhost";

    private static final String DEFAULT_KEY_PASSWORD = "changeit";

    private static final String DEFAULT_TRUSTSTORE_PASSWORD = "changeit";

    private static final String ISSUER_ENCRYPTION_PROPERTIES_FILE = new AbsolutePathResolver(
            "etc/ws-security/issuer/encryption.properties").getPath();

    private static final String ISSUER_SIGNATURE_PROPERTIES_FILE = new AbsolutePathResolver(
            "etc/ws-security/issuer/signature.properties").getPath();

    private static final String SERVER_ENCRYPTION_PROPERTIES_FILE = new AbsolutePathResolver(
            "etc/ws-security/server/encryption.properties").getPath();

    private static final String SERVER_SIGNATURE_PROPERTIES_FILE = new AbsolutePathResolver(
            "etc/ws-security/server/signature.properties").getPath();

    private static final String USERS_PROPERTIES_FILE =
            new AbsolutePathResolver("etc/users.properties").getPath();

    private static final String DEFAULT_ADMIN_USER = "admin";

    private static final String DEFAULT_ADMIN_USER_PASSWORD = "admin";

    private static final String DEFAULT_CERTIFICATE_USER = "localhost";

    private static final String DEFAULT_CERTIFICATE_USER_PASSWORD = "localhost";

    private static final String PAX_WEB_CFG_FILE =
            new AbsolutePathResolver("etc/org.ops4j.pax.web.cfg").getPath();

    private static final String KEYSTORE_SYSTEM_PROPERTY = "javax.net.ssl.keyStore";

    private static final String KEYSTORE_PASSWORD_SYSTEM_PROPERTY =
            "javax.net.ssl.keyStorePassword";

    private static final String TRUSTSTORE_SYSTEM_PROPERTY = "javax.net.ssl.trustStore";

    private static final String TRUSTSTORE_PASSWORD_SYSTEM_PROPERTY =
            "javax.net.ssl.trustStorePassword";

    public static final String MBEAN_NAME =
            InsecureDefaultsServiceBean.class.getName() + ":service=insecure-defaults-service";

    private ObjectName objectName;

    private MBeanServer mBeanServer;

    private List<Validator> validators;

    public InsecureDefaultsServiceBean() {
        validators = new ArrayList<>();
        addValidators();

        try {
            objectName = new ObjectName(MBEAN_NAME);
            mBeanServer = ManagementFactory.getPlatformMBeanServer();
        } catch (MalformedObjectNameException e) {
            LOGGER.error("Unable to create Insecure Defaults Service MBean with name [{}].",
                    MBEAN_NAME,
                    e);
        }
    }

    @Override
    public List<Alert> validate() {
        List<Alert> alerts = new ArrayList<>();

        LOGGER.debug("Found {} validator(s)", validators.size());

        for (Validator validator : validators) {
            LOGGER.debug("{} is starting validation process.",
                    validator.getClass()
                            .getSimpleName());
            alerts.addAll(validator.validate());
            LOGGER.debug("{} finished the validation process.",
                    validator.getClass()
                            .getSimpleName());
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("There are {} total alert(s).", alerts.size());
            for (Alert alert : alerts) {
                LOGGER.debug("Alert: {}; {}", alert.getLevel(), alert.getMessage());
            }
        }

        return alerts;
    }

    public void init() {
        try {
            try {
                mBeanServer.registerMBean(this, objectName);
                LOGGER.info("Registered Insecure Defaults Service MBean under object name: {}",
                        objectName.toString());
            } catch (InstanceAlreadyExistsException e) {
                // Try to remove and re-register
                mBeanServer.unregisterMBean(objectName);
                mBeanServer.registerMBean(this, objectName);
                LOGGER.info("Re-registered Insecure Defaults Service MBean");
            }
        } catch (MBeanRegistrationException | InstanceNotFoundException |
                InstanceAlreadyExistsException | NotCompliantMBeanException e) {
            LOGGER.error("Could not register MBean [{}].", objectName.toString(), e);
        }
    }

    public void destroy() {
        try {
            if (objectName != null && mBeanServer != null) {
                mBeanServer.unregisterMBean(objectName);
                LOGGER.info("Unregistered Insecure Defaults Service MBean");
            }
        } catch (InstanceNotFoundException | MBeanRegistrationException e) {
            LOGGER.error("Exception unregistering MBean [{}].", objectName.toString(), e);
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
        issuerSignatureEncryptionPropertiesFileValidator.setPath(Paths.get(
                ISSUER_SIGNATURE_PROPERTIES_FILE));
        issuerSignatureEncryptionPropertiesFileValidator.setDefaultPassword(
                DEFAULT_KEYSTORE_PASSWORD);
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
        issuerSignatureEncryptionPropertiesFileValidator.setPath(Paths.get(
                SERVER_SIGNATURE_PROPERTIES_FILE));
        issuerSignatureEncryptionPropertiesFileValidator.setDefaultPassword(
                DEFAULT_KEYSTORE_PASSWORD);
        issuerSignatureEncryptionPropertiesFileValidator.setDefaultAlias(DEFAULT_KEYSTORE_ALIAS);
        issuerSignatureEncryptionPropertiesFileValidator.setDefaultPrivateKeyPassword(
                DEFAULT_KEY_PASSWORD);
        validators.add(issuerSignatureEncryptionPropertiesFileValidator);
    }

    private void addUsersPropertiesFileValidator() {
        UsersPropertiesFileValidator userPropertiesFileValidator =
                new UsersPropertiesFileValidator();
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
        return new AbsolutePathResolver(TRUSTSTORE_SYSTEM_PROPERTY).getPath();
    }

    private String getTruststorePassword() {
        String truststorePassword = System.getProperty(TRUSTSTORE_PASSWORD_SYSTEM_PROPERTY);
        return truststorePassword;
    }

    /**
     * Package-private getter for validators list. Needed for unit tests.
     */
    List<Validator> getValidators() {
        return validators;
    }
}
