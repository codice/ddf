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
package ddf.security.settings.impl;

import ddf.security.encryption.EncryptionService;
import ddf.security.settings.SecuritySettingsService;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.security.KeyStore;
import java.security.KeyStoreException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SecuritySettingsServiceImplTest {

    private static final String KEYSTORE_PATH = SecuritySettingsServiceImplTest.class.getResource(
            "/keystore.jks").getPath();
    private static final String KEYSTORE_PASSWORD = "changeit";
    private static final String KEYSTORE_ALIAS = "server";

    @BeforeClass
    public static void setup() {
        System.setProperty(SecuritySettingsService.SSL_KEYSTORE_JAVA_PROPERTY, KEYSTORE_PATH);
        System.setProperty(SecuritySettingsService.SSL_KEYSTORE_PASSWORD_JAVA_PROPERTY, KEYSTORE_PASSWORD);
        System.setProperty(SecuritySettingsService.SSL_TRUSTSTORE_JAVA_PROPERTY, KEYSTORE_PATH);
        System.setProperty(SecuritySettingsService.SSL_TRUSTSTORE_PASSWORD_JAVA_PROPERTY, KEYSTORE_PASSWORD);
    }

    @Test
    public void testGetTLSParamsNoEncrypt() {
        SecuritySettingsServiceImpl securitySettingsService = new SecuritySettingsServiceImpl(null);
        getTLSParams(securitySettingsService);
    }

    @Test
    public void testGetTLSParamsWithEncrypt() {
        EncryptionService encryptionService = mock(EncryptionService.class);
        when(encryptionService.decryptValue(any(String.class))).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                return args[0].toString();
            }
        });
        SecuritySettingsServiceImpl securitySettingsService = new SecuritySettingsServiceImpl(
                encryptionService);
        getTLSParams(securitySettingsService);
    }

    @Test
    public void testGetKeystores() throws KeyStoreException {
        SecuritySettingsServiceImpl securitySettingsService = new SecuritySettingsServiceImpl(null);
        securitySettingsService.init();
        KeyStore keyStore = securitySettingsService.getKeystore();
        assertNotNull(keyStore);
        assertTrue(keyStore.containsAlias(KEYSTORE_ALIAS));
        KeyStore trustStore = securitySettingsService.getTruststore();
        assertNotNull(trustStore);
        assertTrue(keyStore.containsAlias(KEYSTORE_ALIAS));
    }

    private void getTLSParams(SecuritySettingsServiceImpl securitySettingsService) {
        securitySettingsService.init();
        TLSClientParameters clientParameters = securitySettingsService.getTLSParameters();
        assertNotNull(clientParameters);
        assertEquals(1, clientParameters.getKeyManagers().length);
        assertEquals(1, clientParameters.getTrustManagers().length);
    }

}
