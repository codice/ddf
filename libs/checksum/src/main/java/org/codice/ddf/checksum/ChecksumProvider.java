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
package org.codice.ddf.checksum;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

public interface ChecksumProvider {

    /**
     * Calculates checksum from {@link InputStream}
     *
     * @param {@link InputStream} to process
     * @return calculated checksum value
     */
    String calculateChecksum(InputStream inputStream) throws IOException, NoSuchAlgorithmException;

    /**
     * Returns identifier for check sum algorithm
     *
     * @return checksum algorithm identifier
     */
    String getChecksumAlgorithm();
}
