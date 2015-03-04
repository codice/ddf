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
package ddf.ldap.ldaplogin;

import ddf.security.encryption.EncryptionService;
import org.apache.commons.lang.StringUtils;
import org.apache.karaf.jaas.config.KeystoreInstance;
import org.apache.karaf.jaas.config.impl.ResourceKeystoreInstance;
import org.codice.ddf.configuration.ConfigurationManager;
import org.codice.ddf.configuration.ConfigurationWatcher;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

/**
 * Registers keystores based on the platform configuration.
 * 
 */
public class KeystoreManager implements ConfigurationWatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeystoreManager.class);

    private ServiceRegistration<KeystoreInstance> keystoreRegistration = null;

    private ServiceRegistration<KeystoreInstance> truststoreRegistration = null;

    private String keystoreLoc, keystorePass;

    private String truststoreLoc, truststorePass;

    private String keyAlias;

    private static final String HOME_LOCATION = System.getProperty("ddf.home");

    private BundleContext context;

    private EncryptionService encryptService;

    /**
     * Creates a new instance of the KeystoreManager class.
     * 
     * @param encryptService
     *            Service that can encrypt and decrypt passwords.
     * @param context
     *            Bundlecontext to use for service registration.
     */
    public KeystoreManager(EncryptionService encryptService, BundleContext context) {
        this.encryptService = encryptService;
        this.context = context;
    }

    /**
     * Sets the alias that should be used in the keystore for encrypted
     * communication.
     * 
     * @param keyAlias
     *            alias located inside of the keystore.
     */
    public void setKeyAlias(String keyAlias) {
        LOGGER.debug("Updating the keyAlias from {} to {}", this.keyAlias, keyAlias);
        this.keyAlias = keyAlias;
    }

    @Override
    public void configurationUpdateCallback(Map<String, String> props) {
        LOGGER.debug("Got a new configuration.");
        String keystoreLocation = props.get(ConfigurationManager.KEY_STORE);
        String keystorePassword = encryptService.decryptValue(props
                .get(ConfigurationManager.KEY_STORE_PASSWORD));

        String truststoreLocation = props.get(ConfigurationManager.TRUST_STORE);
        String truststorePassword = encryptService.decryptValue(props
                .get(ConfigurationManager.TRUST_STORE_PASSWORD));

        if (StringUtils.isNotBlank(keystoreLocation)
                && (!StringUtils.equals(this.keystoreLoc, keystoreLocation) || !StringUtils.equals(
                        this.keystorePass, keystorePassword))) {
            if (new File(keystoreLocation).exists()) {
                LOGGER.debug("Detected a change in the values for the keystore, registering new keystore instance.");
                keystoreRegistration = registerKeystore("ks", keystoreLocation, keystorePassword,
                        keystoreRegistration);
                this.keystoreLoc = keystoreLocation;
                this.keystorePass = keystorePassword;
            } else {
                LOGGER.debug("Keystore file does not exist at location {}, not updating keystore values.");
            }
        }
        if (StringUtils.isNotBlank(truststoreLocation)
                && (!StringUtils.equals(this.truststoreLoc, truststoreLocation) || !StringUtils
                        .equals(this.truststorePass, truststorePassword))) {
            if (new File(truststoreLocation).exists()) {
                LOGGER.debug("Detected a change in the values for the truststore, registering new keystore instance.");
                truststoreRegistration = registerKeystore("ts", truststoreLocation,
                        truststorePassword, truststoreRegistration);
                this.truststoreLoc = truststoreLocation;
                this.truststorePass = truststorePassword;
            } else {
                LOGGER.debug("Truststore file does not exist at location {}, not updating truststore values.");
            }
        }

    }

    /**
     * Registers a keystore instance to the OSGi registry.
     * 
     * @param name
     *            Name of the keystore to use.
     * @param location
     *            relative file location
     * @param password
     *            Password of the keystore and private key
     * @param keystoreReg
     *            Previous registration instance
     * @return
     */
    private ServiceRegistration<KeystoreInstance> registerKeystore(String name, String location,
            String password, ServiceRegistration<KeystoreInstance> keystoreReg) {
        location = HOME_LOCATION + File.separator + location;
        if (keystoreReg != null) {
            try {
                keystoreReg.unregister();
            } catch (IllegalStateException ise) {
                LOGGER.debug("Previous keystore instance was already unregistered.");
            }
        }
        try {
            ResourceKeystoreInstance keystore = new ResourceKeystoreInstance();
            keystore.setName(name);
            keystore.setKeyPasswords(keyAlias + "=" + password);
            keystore.setKeystorePassword(password);
            keystore.setPath(new File(location).toURI().toURL());

            return context.registerService(KeystoreInstance.class, keystore, null);
        } catch (Exception e) {
            LOGGER.warn(
                    "Encountered an error while trying to register the keystore at "
                            + location
                            + ". Could not add to registry. Communication with LDAP may not work over SSL.",
                    e);
            return null;
        }
    }
}
