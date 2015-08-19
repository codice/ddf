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

public class TestPemFile {

    private String getPathTo(String path) {
        return getClass().getClassLoader().getResource(path).getPath();
    }
    // The basic file I/O is tested in TestKeyStoreFile.
    // Test constructor. No test to ensure PEM objects are in the file. Would have to try and
    // Read an object, test if it was null, and if it is not, reset the stream.


    @Test
    public void testEncryptedPrivateKeyNullPassword() throws FileNotFoundException {

        PemFile.get(getPathTo("private-key-3des-encypted-password_changeit.pem"), null);
    }

    @Test
    public void testEncryptedPrivateKeyWrongPassword() throws IOException {

        PemFile pf = PemFile.get(getPathTo("private-key-3des-encypted-password_changeit.pem"), "ThisIsNotPassword".toCharArray());
        pf.getPrivateKey();
    }

    @Test
       public void testEncryptedPrivateKey() throws IOException {

        PemFile pf = PemFile.get(getPathTo("private-key-3des-encypted-password_changeit.pem"), "changeit".toCharArray());
        pf.getPrivateKey();
    }
}




