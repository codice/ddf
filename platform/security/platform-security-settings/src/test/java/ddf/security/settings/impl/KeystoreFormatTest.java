/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */

package ddf.security.settings.impl;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ddf.security.SecurityConstants;
import ddf.security.settings.SecuritySettingsService;

public class KeystoreFormatTest {

    private static Properties properties;

    @Before
    public void setUp() {
        properties = System.getProperties();
    }

    @Test
    public void jksKeyStoreMustWork() throws KeyStoreException {
        pointToKeystoreFile("keystore.jks", "JKS");
        accessKeyStore();
    }

    @Test
    public void pkcs12MustWork() throws KeyStoreException {
        pointToKeystoreFile("keystore.p12", "PKCS12");
        KeyStore keyStore = makeSecurityService().getKeystore();
        accessKeyStore();
    }

    @After
    public void tearDown() {
        System.setProperties(properties);
    }

    private String getPath(String filename) {
        return SecuritySettingsServiceImplTest.class.getResource("/" + filename).getPath();
    }

    private void pointToKeystoreFile(String filename, String keystoreType) {
        String password = "changeit";
        System.setProperty(SecurityConstants.KEYSTORE_PATH, getPath(filename));
        System.setProperty(SecurityConstants.KEYSTORE_PASSWORD, password);
        System.setProperty(SecurityConstants.TRUSTSTORE_PATH, getPath(filename));
        System.setProperty(SecurityConstants.TRUSTSTORE_PASSWORD, password);
        System.setProperty(SecurityConstants.KEYSTORE_TYPE, keystoreType);
        System.setProperty(SecurityConstants.TRUSTSTORE_TYPE, keystoreType);
    }

    private SecuritySettingsService makeSecurityService() {
        SecuritySettingsServiceImpl sss = new SecuritySettingsServiceImpl(null);
        sss.init();
        return sss;
    }

    private void accessKeyStore() throws KeyStoreException {
        makeSecurityService().getKeystore().aliases();
    }
}
