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
package ddf.security.settings.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import ddf.security.SecurityConstants;
import ddf.security.encryption.EncryptionService;

public class SecuritySettingsServiceImplTest {

    private static final String KEYSTORE_PATH = SecuritySettingsServiceImplTest.class
            .getResource("/keystore.jks").getPath();

    private static final String KEYSTORE_PASSWORD = "changeit";


    @BeforeClass
    public static void setup() {
        System.setProperty(SecurityConstants.KEYSTORE_PATH, KEYSTORE_PATH);
        System.setProperty(SecurityConstants.KEYSTORE_PASSWORD, KEYSTORE_PASSWORD);
        System.setProperty(SecurityConstants.TRUSTSTORE_PATH, KEYSTORE_PATH);
        System.setProperty(SecurityConstants.TRUSTSTORE_PASSWORD, KEYSTORE_PASSWORD);
        System.setProperty(SecurityConstants.KEYSTORE_TYPE, "jks");
        System.setProperty(SecurityConstants.TRUSTSTORE_TYPE, "jks");
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

    private void getTLSParams(SecuritySettingsServiceImpl securitySettingsService) {
        securitySettingsService.init();
        TLSClientParameters clientParameters = securitySettingsService.getTLSParameters();
        assertNotNull(clientParameters);
        assertEquals(1, clientParameters.getKeyManagers().length);
        assertEquals(1, clientParameters.getTrustManagers().length);
    }

}
