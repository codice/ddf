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
package ddf.security.encryption.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.Test;
import ddf.security.encryption.impl.EncryptionServiceImpl;

public class EncryptionServiceImplTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptionServiceImplTest.class);

    private static final String KEY = "secret";

    @Test
    public void testEncryptDecrypt() {
        final String unencryptedPassword = "protect";

        LOGGER.debug("Unencrypted Password: {}", unencryptedPassword);

        final EncryptionServiceImpl encryptionService = new EncryptionServiceImpl();
        encryptionService.setKey(KEY);
        final String encryptedPassword = encryptionService.encrypt(unencryptedPassword);
        LOGGER.debug("Encrypted Password: {}", encryptedPassword);

        final String decryptedPassword = encryptionService.decrypt(encryptedPassword);
        LOGGER.debug("Decrypted Password: {}", decryptedPassword);

        assertEquals(unencryptedPassword, decryptedPassword);
    }

    @Test
    public void testUnwrapDecrypt() {
        final String wrappedEncryptedValue = "ENC(OItcdA9Z79ZSyc8eczWfaw==)";
        final String expectedDecryptedValue = "test";

        LOGGER.debug("Original wrapped encrypted value is: {}", wrappedEncryptedValue);

        final EncryptionServiceImpl encryptionService = new EncryptionServiceImpl();
        encryptionService.setKey(KEY);

        final String decryptedValue = encryptionService.decryptValue(wrappedEncryptedValue);
        LOGGER.debug("Unwrapped decrypted value is: {}", decryptedValue);

        assertEquals(expectedDecryptedValue, decryptedValue);
    }

    @Test
    public void testUnwrapDecryptNull() {
        final String wrappedEncryptedValue = null;

        LOGGER.debug("Original wrapped encrypted value is: null");

        final EncryptionServiceImpl encryptionService = new EncryptionServiceImpl();
        encryptionService.setKey(KEY);

        final String decryptedValue = encryptionService.decryptValue(wrappedEncryptedValue);

        assertNull(decryptedValue);
    }

    @Test
    public void testUnwrapDecryptEmpty() {
        final String wrappedEncryptedValue = "";

        LOGGER.debug("Original wrapped encrypted value is: <blank>");

        final EncryptionServiceImpl encryptionService = new EncryptionServiceImpl();
        encryptionService.setKey(KEY);

        final String decryptedValue = encryptionService.decryptValue(wrappedEncryptedValue);

        assertNull(decryptedValue);
    }

    @Test
    public void testUnwrapDecryptPlainText() {
        final String wrappedEncryptedValue = "plaintext";

        LOGGER.debug("Original value is: {}", wrappedEncryptedValue);

        final EncryptionServiceImpl encryptionService = new EncryptionServiceImpl();
        encryptionService.setKey(KEY);

        final String decryptedValue = encryptionService.decryptValue(wrappedEncryptedValue);
        LOGGER.debug("Unwrapped decrypted value is: {}", decryptedValue);

        assertEquals(wrappedEncryptedValue, decryptedValue);
    }
}
