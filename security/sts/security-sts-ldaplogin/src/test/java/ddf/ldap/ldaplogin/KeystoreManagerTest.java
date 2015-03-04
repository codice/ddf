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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.apache.karaf.jaas.config.KeystoreInstance;
import org.codice.ddf.configuration.ConfigurationManager;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Matchers;
import org.osgi.framework.BundleContext;

import ddf.security.encryption.EncryptionService;

/**
 * Tests out KeystoreManager functionality.
 * 
 */
public class KeystoreManagerTest {

    private BundleContext context;

    private EncryptionService encryptService;
    
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    
    private File keystore;
    private File truststore;

    /**
     * Sets up a new context and encryptservice before each test.
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
    public void testKeystoreChanged() {
        KeystoreManager manager = new KeystoreManager(encryptService, context);
        Map<String, String> updateMap = new HashMap<String, String>();
        updateMap.put(ConfigurationManager.KEY_STORE, "/test/keystore.jks");
        updateMap.put(ConfigurationManager.KEY_STORE_PASSWORD, "password");
        updateMap.put(ConfigurationManager.TRUST_STORE, "/test/truststore.jks");
        updateMap.put(ConfigurationManager.TRUST_STORE_PASSWORD, "password");

        manager.configurationUpdateCallback(updateMap);
        
        // register keystore and truststore with INVALID file
        verify(context, never()).registerService(eq(KeystoreInstance.class),
                any(KeystoreInstance.class), Matchers.<Dictionary<String, Object>> any());

        // only keystore changed to be valid
        updateMap.put(ConfigurationManager.KEY_STORE, keystore.getAbsolutePath());
        manager.configurationUpdateCallback(updateMap);
        verify(context, times(1)).registerService(eq(KeystoreInstance.class),
                any(KeystoreInstance.class), Matchers.<Dictionary<String, Object>> any());

        // only truststore changed to be valid
        updateMap.put(ConfigurationManager.TRUST_STORE, truststore.getAbsolutePath());
        manager.configurationUpdateCallback(updateMap);
        verify(context, times(2)).registerService(eq(KeystoreInstance.class),
                any(KeystoreInstance.class), Matchers.<Dictionary<String, Object>> any());

    }

}
