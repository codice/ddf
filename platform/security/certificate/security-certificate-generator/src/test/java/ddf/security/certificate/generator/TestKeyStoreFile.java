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
package ddf.security.certificate.generator;

import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;

import static org.junit.Assert.assertNotNull;

public class TestKeyStoreFile {

    private String getPathTo(String path) {
        return getClass().getClassLoader().getResource(path).getPath();
    }

    //Test constructor. Null path to keyStore file.
    @Test(expected = FileNotFoundException.class)
    public void testConstructorNullPath() throws Exception {
        KeyStoreFile.getInstance(null, null);
    }

    //Test constructor. Invalid path to keyStore file.
    @Test(expected = FileNotFoundException.class)
    public void testConstructorInvalidPath() throws Exception {
        KeyStoreFile.getInstance("", null);
    }

    //Test Constructor. Path is a directory, not a file.
    @Test(expected = FileNotFoundException.class)
    public void testConstructorPathIsDirectory() throws Exception {
        String anyDirectory = getPathTo("");
        KeyStoreFile.getInstance(anyDirectory, null);
    }

    //Test Constructor. No read permissions on file.
    //I tested this one manually and it threw the correct exception with the correct message.
    //Not sure how to test it automated testing, and probably not worth the effort anyway.

    //Test Constructor. File is not keyStore.
    @Test(expected = IOException.class)
    public void testConstructorFileNotKeyStore() throws Exception {
        KeyStoreFile.getInstance(getPathTo("not_keystore.jks"), null);
    }

    //Test Constructor. Password is null.
    @Test(expected = IOException.class)
    public void testConstructorNullPassword() throws Exception {
        KeyStoreFile.getInstance(getPathTo("keystore-password_changeit.jks"), null);
    }

    //Test Constructor. Password is wrong.
    @Test(expected = IOException.class)
    public void testConstructorWrongPassword() throws Exception {
        KeyStoreFile.getInstance(getPathTo("keystore-password_changeit.jks"), "ThisIsNotThePassword".toCharArray());
    }

    //Test Constructor. Valid file, valid password.
    @Test
    public void testConstructor() throws Exception {
        //SYSTEM PROPERTIES NOT AVAILABLE UNTIL KARAF'S BOOT PROCESS IS COMPLETE
        //System.getProperty("javax.net.ssl.keyStorePassword")

        KeyStoreFile keyStore = KeyStoreFile.getInstance(getPathTo("keystore-password_changeit.jks"), "changeit".toCharArray());
        assertNotNull(keyStore.aliases());
        assertNotNull(keyStore.getCertificate("ddf demo root ca"));
    }
}
