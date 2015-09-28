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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;

import org.apache.karaf.jaas.config.KeystoreInstance;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Matchers;
import org.osgi.framework.BundleContext;

import ddf.security.encryption.EncryptionService;

/**
 * Tests out KeystoreManager functionality.
 */
public class KeystoreManagerTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private BundleContext context;

    private EncryptionService encryptService;

    private File keystore;

    private File truststore;

    /**
     * Sets up a new context and encryptservice before each test.
     *
     * @throws IOException
     */
    @Before
    public void setUp() throws IOException {
        context = mock(BundleContext.class);
        encryptService = mock(EncryptionService.class);
        keystore = folder.newFile("keystore.jks");
        truststore = folder.newFile("truststore.jks");
    }

    /**
     * Verifies the keystore instances are properly registered as services.
     */
    @Test
    public void testKeystore() {

        System.setProperty("javax.net.ssl.keyStore", "/test/keystore.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "password");
        System.setProperty("javax.net.ssl.trustStore", "/test/truststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "password");

        KeystoreManager manager = new KeystoreManager(encryptService) {
            protected BundleContext getContext() {
                return context;
            }
        };

        verify(context, times(2))
                .registerService(eq(KeystoreInstance.class), any(KeystoreInstance.class),
                        Matchers.<Dictionary<String, Object>>any());
    }

    @Test
    public void testKeystoreNullEncrypt() {

        System.setProperty("javax.net.ssl.keyStore", "/test/keystore.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "password");
        System.setProperty("javax.net.ssl.trustStore", "/test/truststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "password");

        KeystoreManager manager = new KeystoreManager(null) {
            protected BundleContext getContext() {
                return context;
            }
        };

        verify(context, times(2))
                .registerService(eq(KeystoreInstance.class), any(KeystoreInstance.class),
                        Matchers.<Dictionary<String, Object>>any());
    }

}
