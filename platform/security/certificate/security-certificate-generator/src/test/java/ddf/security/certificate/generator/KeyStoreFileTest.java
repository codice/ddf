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

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class KeyStoreFileTest {

    public static final String BOGUS_FILENAME = "not_keystore.jks";
    public static final String KEYSTORE_FILENAME = "keystore-password_changeit.jks";
    public static final String ALIAS = "ddf demo root ca";
    public static final char[] PASSWORD = "changeit".toCharArray();
    public static final char[] BOGUS_PASSWORD = "ThisIsNotThePassword".toCharArray();

    private String getPathTo(String path) {
        return getClass().getClassLoader().getResource(path).getPath();
    }

    //Test constructor. Null path to keyStore file.
    @Test(expected = IllegalArgumentException.class)
    public void constructorNullPath() throws Exception {
        KeyStoreFile.newInstance(null, null);
    }

    //Test constructor. Invalid path to keyStore file.
    @Test(expected = FileNotFoundException.class)
    public void constructorInvalidPath() throws Exception {
        KeyStoreFile.newInstance("", null);
    }

    //Test Constructor. Path is a directory, not a file.
    @Test(expected = FileNotFoundException.class)
    public void constructorPathIsDirectory() throws Exception {
        String anyDirectory = getPathTo("");
        KeyStoreFile.newInstance(anyDirectory, null);
    }

    //Test Constructor. No read permissions on file.
    //I tested this one manually and it threw the correct exception with the correct message.
    //Not sure how to test it automated testing, and probably not worth the effort anyway.

    //Test Constructor. File is not keyStore.
    @Test(expected = IOException.class)
    public void constructorFileNotKeyStore() throws Exception {
        KeyStoreFile.newInstance(getPathTo(BOGUS_FILENAME), null);
    }

    //Test Constructor. Password is null.
    @Test(expected = IOException.class)
    public void constructorNullPassword() throws Exception {
        KeyStoreFile.newInstance(getPathTo(KEYSTORE_FILENAME), null);
    }

    //Test Constructor. Password is wrong.
    @Test(expected = IOException.class)
    public void constructorWrongPassword() throws Exception {
        KeyStoreFile.newInstance(getPathTo(KEYSTORE_FILENAME), BOGUS_PASSWORD);
    }

    //Test Constructor. Valid file, valid password.
    @Test
    public void testConstructor() throws Exception {

        KeyStoreFile keyStore = KeyStoreFile.newInstance(getPathTo(KEYSTORE_FILENAME), PASSWORD);
        assertNotNull(keyStore.aliases());
        assertNotNull(keyStore.getCertificate(ALIAS));
    }

    @Test
    public void removeEntryFromKeystore() throws Exception {
        KeyStoreFile keyStore = KeyStoreFile.newInstance(getPathTo(KEYSTORE_FILENAME), PASSWORD);
        keyStore.removeEntry(ALIAS);
        assertThat(keyStore.aliases(), not(hasItem(ALIAS)));
    }
}
