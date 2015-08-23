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
import java.security.Security;

/**
 * Common superclass that loosely groups the classes that wrap security-related files. The common behavior is
 * handling errors related to reading files.
 */
public abstract class SecurityFileFacade {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    protected String BC = BouncyCastleProvider.PROVIDER_NAME;

    //Helper method
    protected static char[] formatPassword(char[] password) {
        return password == null ? new char[0] : password;
    }

    //Helper method
    protected static File createFileObject(String filePath) throws FileNotFoundException {

        File file;

        //Check for null here or risk NPE
        if (filePath == null) {
            throw new FileNotFoundException("File path to security file is null");
        }

        // Create a file object from the given file path
        file = new File(filePath);

        // Verify the file exists.
        if (!file.exists()) {
            throw new FileNotFoundException("Cannot find security file at " + file.getAbsolutePath());
        }

        // Verify the file can be read. E.g. possible permission problem or the given path points to a directory.
        if (!file.canRead()) {
            String msg = String.format("Cannot read security file (possible file permission problem) %s",
                    file.getAbsolutePath());
            throw new FileNotFoundException(msg);
        }

        return file;
    }
}
