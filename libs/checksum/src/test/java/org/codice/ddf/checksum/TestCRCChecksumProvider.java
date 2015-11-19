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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.security.NoSuchAlgorithmException;

import org.codice.ddf.checksum.impl.CRC32ChecksumProvider;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

public class TestCRCChecksumProvider {

    private static final XLogger LOGGER = new XLogger(
            LoggerFactory.getLogger(TestCRCChecksumProvider.class));

    private ChecksumProvider checksumProvider;

    @Before
    public void intialize() {
        checksumProvider = new CRC32ChecksumProvider();
    }

    @Test
    public void testCalculateCheckSumString() throws IOException, NoSuchAlgorithmException {

        String testString = "Hello World";
        String checksumCompareHash = "c35ef163";

        InputStream stringInputStream = getInputStreamFromObject(testString);
        String checksumValue = checksumProvider.calculateChecksum(stringInputStream);

        //compare returned checksum to previous checksum
        //as they should be the same if the checksum is calculated
        //correctly
        Assert.assertThat(checksumValue, is(checksumCompareHash));
    }

    @Test
    public void testCalculateCheckSumObject() throws IOException, NoSuchAlgorithmException {

        SerializableTestObject obj = new SerializableTestObject();
        obj.setName("Test Name");
        obj.setDescription("Test Description");

        String checksumCompareHash = "44e13aef";

        InputStream stringInputStream = getInputStreamFromObject(obj);
        String checksumValue = checksumProvider.calculateChecksum(stringInputStream);

        //compare returned checksum to previous checksum
        //as they should be the same if the checksum is calculated
        //correctly
        Assert.assertThat(checksumValue, is(checksumCompareHash));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateCheckSumWithNullInputStream()
            throws IOException, NoSuchAlgorithmException {

        checksumProvider.calculateChecksum(null);
    }

    @Test
    public void testGetCheckSumAlgorithm() {

        String expectedAlgorithm = "CRC32";
        String checksumAlgorithm = checksumProvider.getChecksumAlgorithm();

        //we want to make sure that the checksum algrithm
        //returned does not change in the event their exists
        //checks against the 'CRC32'
        assertThat(expectedAlgorithm, is(checksumAlgorithm));
    }

    private InputStream getInputStreamFromObject(Object obj) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objOutputStream.writeObject(obj);
        objOutputStream.flush();
        objOutputStream.close();

        InputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        return inputStream;
    }
}