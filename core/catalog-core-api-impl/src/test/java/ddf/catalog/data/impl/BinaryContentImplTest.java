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
package ddf.catalog.data.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.notification.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.impl.BinaryContentImpl;

public class BinaryContentImplTest {
    private static final String IO_FAILURE = "IO Failure";

    private static final Logger LOGGER = LoggerFactory.getLogger(BinaryContentImplTest.class);

    private File content;

    private MimeType mimeType;

    @Before
    public void setUp() {
        content = new File("src/test/resources/data/i4ce.png");
        MimetypesFileTypeMap mimeMapper = new MimetypesFileTypeMap();
        try {
            mimeType = new MimeType(mimeMapper.getContentType(content));
        } catch (MimeTypeParseException e) {
            LOGGER.error("Mime parser Failure", e);
            new Failure(null, e);
        }
    }

    @Test
    public void testBinaryContentImpl() {
        InputStream is;
        try {
            is = new FileInputStream(content);
            BinaryContentImpl bci = new BinaryContentImpl(is, mimeType);
            bci.setSize(content.length());
            assertEquals(is, bci.getInputStream());
            assertEquals(mimeType, bci.getMimeType());
            assertEquals(content.length(), bci.getSize());
            assertNotNull(bci.toString());
        } catch (IOException e) {
            LOGGER.error(IO_FAILURE, e);
            new Failure(null, e);
        }
    }

    @Test
    public void testBinaryContentImplNullInputStream() {
        InputStream is = null;
        BinaryContentImpl bci = new BinaryContentImpl(is, mimeType);
        bci.setSize(content.length());
        assertEquals(is, bci.getInputStream());
        assertEquals(mimeType, bci.getMimeType());
        assertEquals(content.length(), bci.getSize());
    }

    @Test
    public void testBinaryContentImplNullMimeType() {
        InputStream is;
        try {
            is = new FileInputStream(content);
            BinaryContentImpl bci = new BinaryContentImpl(is, null);
            assertEquals(null, bci.getMimeType());
        } catch (IOException e) {
            LOGGER.error(IO_FAILURE, e);
            new Failure(null, e);
        }
    }

    @Test
    public void testBinaryContentImplSizeNotSet() {
        InputStream is;
        try {
            is = new FileInputStream(content);
            BinaryContentImpl bci = new BinaryContentImpl(is, mimeType);
            assertEquals(BinaryContentImpl.UNKNOWN_SIZE, bci.getSize());
        } catch (IOException e) {
            LOGGER.error(IO_FAILURE, e);
            new Failure(null, e);
        }
    }

    @Test
    public void testBinaryContentImplNegativeSize() {
        InputStream is;
        try {
            is = new FileInputStream(content);
            BinaryContentImpl bci = new BinaryContentImpl(is, mimeType);
            bci.setSize(-20l);
            assertEquals(BinaryContentImpl.UNKNOWN_SIZE, bci.getSize());
        } catch (IOException e) {
            LOGGER.error(IO_FAILURE, e);
            new Failure(null, e);
        }
    }

    @Test
    public void testBinaryContentImplByteArrayValidity() {
        InputStream is;
        try {
            is = new FileInputStream(content);
            BinaryContentImpl bci = new BinaryContentImpl(is, mimeType);
            byte[] contents = bci.getByteArray();
            assertArrayEquals(contents, bci.getByteArray());
        } catch (IOException e) {
            LOGGER.error(IO_FAILURE, e);
            new Failure(null, e);
        }
    }

    @Test
    public void testBinaryContentImplByteArrayNotNull() {
        InputStream is;
        try {
            is = new FileInputStream(content);
            BinaryContentImpl bci = new BinaryContentImpl(is, mimeType);
            byte[] contents = bci.getByteArray();
            assertNotNull(contents);
        } catch (IOException e) {
            LOGGER.error(IO_FAILURE, e);
            new Failure(null, e);
        }
    }

    @Test
    public void testBinaryContentImplByteArrayX2NotNull() {
        InputStream is;
        try {
            is = new FileInputStream(content);
            BinaryContentImpl bci = new BinaryContentImpl(is, mimeType);
            assertNotNull(bci.getByteArray());
            assertNotNull(bci.getByteArray());
        } catch (IOException e) {
            LOGGER.error(IO_FAILURE, e);
            new Failure(null, e);
        }
    }

    @Test
    public void testBinaryContentImplByteArrayAndInputStreamNotNull() {
        InputStream is;
        try {
            is = new FileInputStream(content);
            BinaryContentImpl bci = new BinaryContentImpl(is, mimeType);
            byte[] contents = bci.getByteArray();
            assertNotNull(contents);
            InputStream newIs = bci.getInputStream();
            assertNotNull(newIs);

        } catch (IOException e) {
            LOGGER.error(IO_FAILURE, e);
            new Failure(null, e);
        }
    }

    @Test
    public void testByteArraysEqual() {

        byte[] bytes = "Hello World".getBytes();

        InputStream is;
        try {
            is = new ByteArrayInputStream(bytes);
            BinaryContentImpl bci = new BinaryContentImpl(is, mimeType);
            byte[] contents = bci.getByteArray();
            assertArrayEquals(bytes, contents);
            InputStream newIs = bci.getInputStream();
            byte[] inputStreamByteArray = IOUtils.toByteArray(newIs);
            assertArrayEquals(bytes, inputStreamByteArray);
        } catch (IOException e) {
            LOGGER.error(IO_FAILURE, e);
            new Failure(null, e);
        }

    }
}
