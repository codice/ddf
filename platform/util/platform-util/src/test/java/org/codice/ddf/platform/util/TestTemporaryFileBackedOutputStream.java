/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.platform.util;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.io.ByteSource;

public class TestTemporaryFileBackedOutputStream {

    private static final int TEST_BYTE = 1;

    private static final byte[] TEST_BYTE_ARRAY = new byte[] {TEST_BYTE};

    private TemporaryFileBackedOutputStream temporaryFileBackedOutputStream;

    @Before
    public void setup() {
        temporaryFileBackedOutputStream = new TemporaryFileBackedOutputStream(1);
    }

    @After
    public void teardown() throws IOException {
        temporaryFileBackedOutputStream.close();
    }

    /**
     * Make sure asByteSource() throws an IOException after close() is called.
     *
     * @throws IOException
     */
    @Test(expected = IOException.class)
    public void testAsByteSourceAfterClose() throws IOException {

        temporaryFileBackedOutputStream.close();

        temporaryFileBackedOutputStream.asByteSource();

    }

    @Test
    public void testWriteByte() throws IOException {

        temporaryFileBackedOutputStream.write(TEST_BYTE);

        ByteSource byteSource = temporaryFileBackedOutputStream.asByteSource();

        assertThat(byteSource.read(), is(TEST_BYTE_ARRAY));

    }

    @Test
    public void testWriteByteArray() throws IOException {

        temporaryFileBackedOutputStream.write(TEST_BYTE_ARRAY);

        ByteSource byteSource = temporaryFileBackedOutputStream.asByteSource();

        assertThat(byteSource.read(), is(TEST_BYTE_ARRAY));

    }

    @Test
    public void testWriteByteArrayOffLen() throws IOException {

        temporaryFileBackedOutputStream.write(TEST_BYTE_ARRAY, 0, 1);

        ByteSource byteSource = temporaryFileBackedOutputStream.asByteSource();

        assertThat(byteSource.read(), is(TEST_BYTE_ARRAY));

    }

    @Test(expected = NullPointerException.class)
    public void testWriteNullArray() throws IOException {
        temporaryFileBackedOutputStream.write(null, 0, 0);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteNegativeOffset() throws IOException {
        temporaryFileBackedOutputStream.write(TEST_BYTE_ARRAY, -1, 0);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteNegativeLength() throws IOException {
        temporaryFileBackedOutputStream.write(TEST_BYTE_ARRAY, 0, -1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteBadOffsetLength() throws IOException {
        temporaryFileBackedOutputStream.write(TEST_BYTE_ARRAY, 1, 1);
    }

    /**
     * Make sure that flush doesn't throw an exception.
     */
    @Test
    public void testFlush() throws IOException {
        temporaryFileBackedOutputStream.write(TEST_BYTE_ARRAY);
        temporaryFileBackedOutputStream.flush();
    }

    /**
     * Make sure that flush doesn't throw an exception after close is called
     */
    @Test
    public void testFlushAfterClose() throws IOException {
        temporaryFileBackedOutputStream.write(TEST_BYTE_ARRAY);
        temporaryFileBackedOutputStream.close();
        temporaryFileBackedOutputStream.flush();
    }

}
