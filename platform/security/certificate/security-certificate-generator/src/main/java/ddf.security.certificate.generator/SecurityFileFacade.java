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

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.Security;

/**
 * Common superclass that loosely groups the classes that wrap security-related files. The common behavior is
 * handling errors related to reading files.
 */
public abstract class SecurityFileFacade {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * If input is a character array, return the character array. If input is null, return a zero length
     * character array
     *
     * @param password character array
     * @return character array
     */
    static char[] formatPassword(char[] password) {
        return password == null ? new char[0] : password;
    }

    protected static File createFileObject(String filePath) throws IOException {

        File file;

        if (filePath == null) {
            throw new IllegalArgumentException("File path to security file is null");
        }

        file = new File(filePath);

        if (!file.exists()) {
            throw new FileNotFoundException("Cannot find security file at " + file.getAbsolutePath());
        }

        if (!file.canRead()) {
            String msg = String.format("Cannot read security file (possible file permission problem)  or %s is a directory",
                    file.getAbsolutePath());
            throw new IOException(msg);
        }

        return file;
    }
}
