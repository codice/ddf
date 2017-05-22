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
package ddf.security.command;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.encryption.impl.EncryptionServiceImpl;

public class EncryptCommandTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptCommandTest.class);

    private static File ddfHome;

    @Before
    public void setUp() throws Exception {
        ddfHome = Files.createTempDirectory("encrypt")
                .toFile();
        System.setProperty("ddf.home", ddfHome.toString());
        String path = new File(System.getProperty("ddf.home")
                .concat("/etc/certs")).getCanonicalPath();
        new File(path).mkdirs();
    }

    @After
    public void cleanUp() throws Exception {
        FileUtils.deleteDirectory(ddfHome);
    }

    @Test
    public void testExecuteNonNullPlainTextValue() {
        final String plainTextValue = "protect";

        final EncryptionServiceImpl encryptionService = new EncryptionServiceImpl();
        final EncryptCommand encryptCommand = new EncryptCommand();
        encryptCommand.setEncryptionService(encryptionService);
        setPlainTextValueField(encryptCommand, plainTextValue);
        LOGGER.debug("Plain text value: {}", plainTextValue);

        Object encryptedValue = null;

        try {
            encryptedValue = encryptCommand.execute();

        } catch (Exception e) {
            fail(e.getMessage());
        }

        LOGGER.debug("Encrypted Value: {}", encryptedValue);
        assertNull(encryptedValue);
    }

    @Test
    public void testExecuteNullPlainTextValue() {
        final String plainTextValue = null;

        final EncryptionServiceImpl encryptionService = new EncryptionServiceImpl();
        final EncryptCommand encryptCommand = new EncryptCommand();
        encryptCommand.setEncryptionService(encryptionService);
        setPlainTextValueField(encryptCommand, plainTextValue);
        LOGGER.debug("Plain text value: {}", plainTextValue);

        Object encryptedValue = null;

        try {
            encryptedValue = encryptCommand.execute();
        } catch (Exception e) {
            fail(e.getMessage());
        }

        LOGGER.debug("Encrypted Value: " + encryptedValue);
        assertNull(encryptedValue);
    }

    private void setPlainTextValueField(EncryptCommand encryptCommand, String value) {
        final String plainTextValueField = "plainTextValue";
        final Class<? extends EncryptCommand> clazz = encryptCommand.getClass();

        try {
            final Field field = clazz.getDeclaredField(plainTextValueField);
            field.setAccessible(true);
            field.set(encryptCommand, value);
        } catch (SecurityException e) {
            LOGGER.debug("Security exception setting field value", e);
            fail(e.getMessage());
        } catch (NoSuchFieldException e) {
            LOGGER.debug("No such field exception setting field value", e);
            fail(e.getMessage());
        } catch (IllegalArgumentException e) {
            LOGGER.debug("Illegal argument exception setting field value", e);
            fail(e.getMessage());
        } catch (IllegalAccessException e) {
            LOGGER.debug("Illegal exception exception setting field value", e);
            fail(e.getMessage());
        }

    }
}
