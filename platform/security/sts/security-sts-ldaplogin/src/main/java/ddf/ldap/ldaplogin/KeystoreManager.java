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
package ddf.ldap.ldaplogin;

import java.io.File;

import org.apache.karaf.jaas.config.KeystoreInstance;
import org.apache.karaf.jaas.config.impl.ResourceKeystoreInstance;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.encryption.EncryptionService;

/**
 * Registers keystores based on the platform configuration.
 */
public class KeystoreManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeystoreManager.class);

    private static final String HOME_LOCATION = System.getProperty("ddf.home");

    private ServiceRegistration<KeystoreInstance> keystoreRegistration = null;

    private ServiceRegistration<KeystoreInstance> truststoreRegistration = null;

    private String keyAlias;

    private EncryptionService encryptService;

    /**
     * Creates a new instance of the KeystoreManager class.
     *
     * @param encryptService Service that can encrypt and decrypt passwords.
     */
    public KeystoreManager(EncryptionService encryptService, String alias) {
        this.encryptService = encryptService;
        this.keyAlias = alias;
        configure();
    }

    /**
     * Sets the alias that should be used in the keystore for encrypted
     * communication.
     *
     * @param keyAlias alias located inside of the keystore.
     */
    public void setKeyAlias(String keyAlias) {
        LOGGER.debug("Updating the keyAlias from {} to {}", this.keyAlias, keyAlias);
        this.keyAlias = keyAlias;
    }

    private void configure() {
        String truststoreLoc = System.getProperty("javax.net.ssl.trustStore");
        String truststorePass = System.getProperty("javax.net.ssl.trustStorePassword");
        String keystoreLoc = System.getProperty("javax.net.ssl.keyStore");
        String keystorePass = System.getProperty("javax.net.ssl.keyStorePassword");
        if (encryptService != null) {
            keystorePass = encryptService.decryptValue(keystorePass);
            truststorePass = encryptService.decryptValue(truststorePass);
        }
        keystoreRegistration = registerKeystore("ks", keystoreLoc, keystorePass,
                keystoreRegistration);

        truststoreRegistration = registerKeystore("ts", truststoreLoc, truststorePass,
                truststoreRegistration);
    }

    /**
     * Registers a keystore instance to the OSGi registry.
     *
     * @param name        Name of the keystore to use.
     * @param location    relative file location
     * @param password    Password of the keystore and private key
     * @param keystoreReg Previous registration instance
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

            BundleContext context = getContext();

            if (context != null) {
                return context.registerService(KeystoreInstance.class, keystore, null);
            }
            return null;
        } catch (Exception e) {
            LOGGER.warn("Encountered an error while trying to register the keystore at " + location
                            + ". Could not add to registry. Communication with LDAP may not work over SSL.",
                    e);
            return null;
        }
    }

    protected BundleContext getContext() {
        Bundle cxfBundle = FrameworkUtil.getBundle(KeystoreManager.class);
        if (cxfBundle != null) {
            return cxfBundle.getBundleContext();
        }
        return null;
    }
}
