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
package org.codice.ddf.checksum.impl;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.io.IOUtils;
import org.codice.ddf.checksum.AbstractChecksumProvider;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

public class MD5ChecksumProvider extends AbstractChecksumProvider {

    private static final String DIGEST_ALGORITHM = "MD5";

    private static final XLogger LOGGER = new XLogger(
            LoggerFactory.getLogger(MD5ChecksumProvider.class));

    @Override
    public String calculateChecksum(InputStream inputStream)
            throws IOException, NoSuchAlgorithmException {

        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }

        byte[] bytes = IOUtils.toByteArray(inputStream);
        MessageDigest messageDigest = MessageDigest.getInstance(DIGEST_ALGORITHM);
        byte[] digested = messageDigest.digest(bytes);
        return bytesToHex(digested);
    }

    @Override
    public String getChecksumAlgorithm() {
        return DIGEST_ALGORITHM;
    }

}