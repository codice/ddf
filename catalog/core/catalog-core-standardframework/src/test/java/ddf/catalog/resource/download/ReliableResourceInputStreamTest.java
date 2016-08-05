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
package ddf.catalog.resource.download;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.WriterAppender;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.CountingOutputStream;
import com.google.common.io.FileBackedOutputStream;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import ddf.catalog.operation.ResourceResponse;

public class ReliableResourceInputStreamTest {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(
            ReliableResourceInputStreamTest.class);

    private static final int THRESHOLD = 1024; // 1 KB

    ResourceResponse resourceResponse;

    private FileBackedOutputStream fbos;

    private CountingOutputStream countingFbos;

    private DownloadManagerState downloadState;

    private ReliableResourceCallable reliableResourceCallable;

    private Future<ReliableResourceStatus> downloadFuture;

    private String downloadIdentifier;

    @Before
    public void setup() {
        fbos = new FileBackedOutputStream(THRESHOLD);
        countingFbos = new CountingOutputStream(fbos);
        downloadState = mock(DownloadManagerState.class);
        when(downloadState.getDownloadState()).thenReturn(DownloadManagerState.DownloadState.COMPLETED);
        reliableResourceCallable = mock(ReliableResourceCallable.class);
        downloadFuture = mock(Future.class);
        downloadIdentifier = UUID.randomUUID()
                .toString();
        resourceResponse = mock(ResourceResponse.class);

    }

    /**
     * Verifies if no bytes written yet to FileBackedOutputStream, then when
     * ReliableResourceInputStream.read() is called it does not block and immediately returns zero.
     */
    @Test
    public void testReadWhenNoFbosBytesWritten() throws Exception {
        ReliableResourceInputStream is = new ReliableResourceInputStream(fbos,
                countingFbos,
                downloadState,
                downloadIdentifier,
                resourceResponse);
        is.setCallableAndItsFuture(reliableResourceCallable, downloadFuture);
        assertThat(is.read(), is(0));
        is.close();
        assertThat(is.isClosed(), is(true));
    }

    /**
     * Verifies if multiple bytes written to FileBackedOutputStream, then when
     * ReliableResourceInputStream.read() is called it returns the first byte.
     *
     * @throws Exception
     */
    @Test
    public void testReadWhenFbosBytesWritten() throws Exception {
        ReliableResourceInputStream is = new ReliableResourceInputStream(fbos,
                countingFbos,
                downloadState,
                downloadIdentifier,
                resourceResponse);
        is.setCallableAndItsFuture(reliableResourceCallable, downloadFuture);
        byte[] bytes = new String("Hello World").getBytes();
        countingFbos.write(bytes, 0, bytes.length);
        int c = is.read();
        Character ch = new Character((char) c);
        assertThat(ch, is('H'));
        assertThat(is.getBytesRead(), is(1L));
        is.close();
    }

    /**
     * Verifies if multiple bytes written to FileBackedOutputStream, then when multiple
     * ReliableResourceInputStream.read() calls made it returns each byte written.
     *
     * @throws Exception
     */
    @Test
    public void testMultipleReadsWhenFbosBytesWritten() throws Exception {
        ReliableResourceInputStream is = new ReliableResourceInputStream(fbos,
                countingFbos,
                downloadState,
                downloadIdentifier,
                resourceResponse);
        is.setCallableAndItsFuture(reliableResourceCallable, downloadFuture);
        byte[] bytes = new String("Hello World").getBytes();
        countingFbos.write(bytes, 0, bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            int c = is.read();
            Character ch = new Character((char) c);
            assertThat(ch, is(new Character((char) bytes[i])));
        }
        assertThat(is.getBytesRead(), is(new Long(bytes.length)));
        is.close();
    }

    @Test
    public void testReadByteBufferFbosBytesWritten() throws Exception {
        ReliableResourceInputStream is = new ReliableResourceInputStream(fbos,
                countingFbos,
                downloadState,
                downloadIdentifier,
                resourceResponse);
        is.setCallableAndItsFuture(reliableResourceCallable, downloadFuture);
        byte[] bytes = new String("Hello World").getBytes();
        countingFbos.write(bytes, 0, bytes.length);
        byte[] buffer = new byte[50];
        int numBytesRead = is.read(buffer, 0, buffer.length);
        assertThat(numBytesRead, is(bytes.length));
        assertThat(is.getBytesRead(), is(new Long(bytes.length)));
        is.close();
    }

    @Test
    public void testReadByteBufferBlocksUntilNewFbosBytesWritten() throws Exception {
        final ReliableResourceInputStream is = new ReliableResourceInputStream(fbos,
                countingFbos,
                downloadState,
                downloadIdentifier,
                resourceResponse);
        is.setCallableAndItsFuture(reliableResourceCallable, downloadFuture);
        byte[] bytes = new String("Hello World").getBytes();
        countingFbos.write(bytes, 0, bytes.length);
        final byte[] buffer = new byte[50];
        int numBytesRead = is.read(buffer, 0, buffer.length);

        // Read again and ReliableResourceInputStream should block until more bytes written to
        // FileBackedOutputStream (do this in separate thread so unit test can write more bytes
        // to FileBackedOutputStream)

        Callable<Integer> callable = new Callable<Integer>() {
            @Override
            public Integer call() {
                int numBytesRead2 = 0;
                try {
                    numBytesRead2 = is.read(buffer, 0, buffer.length);
                } catch (IOException e) {
                    LOGGER.info("Failed to read bytes second time", e);
                }
                return numBytesRead2;
            }
        };

        ListeningExecutorService executor =
                MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
        ListenableFuture<Integer> future = executor.submit(callable);

        // Write second string to FileBackedOutputStream - ReliableResourceInputStream's running
        // read(byte[], off, len) method is in loop waiting for new bytes to be written and should
        // detect this, read the new bytes and put them in the buffer
        String secondString = "Hello a second time";
        byte[] bytes2 = secondString.getBytes();
        countingFbos.write(bytes2, 0, bytes2.length);
        Integer bytesReadCount = future.get();
        assertThat(bytesReadCount, is(bytes2.length));
        assertThat(new String(buffer), containsString(secondString));

        is.close();
    }

    @Test(expected = NullPointerException.class)
    public void testReadByteBufferWithNullBuffer() throws Exception {
        ReliableResourceInputStream is = new ReliableResourceInputStream(fbos,
                countingFbos,
                downloadState,
                downloadIdentifier,
                resourceResponse);
        is.setCallableAndItsFuture(reliableResourceCallable, downloadFuture);
        is.read(null, 0, 50);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadByteBufferWithInvalidOffset() throws Exception {
        ReliableResourceInputStream is = new ReliableResourceInputStream(fbos,
                countingFbos,
                downloadState,
                downloadIdentifier,
                resourceResponse);
        is.setCallableAndItsFuture(reliableResourceCallable, downloadFuture);
        byte[] buffer = new byte[50];
        is.read(buffer, -1, 50);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadByteBufferWithInvalidLength() throws Exception {
        ReliableResourceInputStream is = new ReliableResourceInputStream(fbos,
                countingFbos,
                downloadState,
                downloadIdentifier,
                resourceResponse);
        is.setCallableAndItsFuture(reliableResourceCallable, downloadFuture);
        byte[] buffer = new byte[50];
        is.read(buffer, 0, buffer.length + 1);
    }

    @Test
    public void testReadByteBufferWithZeroLength() throws Exception {
        ReliableResourceInputStream is = new ReliableResourceInputStream(fbos,
                countingFbos,
                downloadState,
                downloadIdentifier,
                resourceResponse);
        is.setCallableAndItsFuture(reliableResourceCallable, downloadFuture);
        byte[] buffer = new byte[50];
        int numBytesRead = is.read(buffer, 0, 0);
        assertThat(numBytesRead, is(0));
    }

    @Test
    public void testInputStreamReadRetry() throws Exception {
        LOGGER.info("Testing testInputStreamReadTwice()");
        ReliableResourceInputStream is = new ReliableResourceInputStream(fbos,
                countingFbos,
                downloadState,
                downloadIdentifier,
                resourceResponse);
        is.setCallableAndItsFuture(reliableResourceCallable, downloadFuture);

        org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(is.getClass());
        logger.setLevel(Level.TRACE);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Layout layout = new SimpleLayout();
        Appender appender = new WriterAppender(layout, out);
        logger.addAppender(appender);

        //downloadState.setDownloadState(DownloadManagerState.DownloadState.IN_PROGRESS);
        try {
            // Write zero bytes to FileBackedOutputStream
            byte[] bytes = new String("").getBytes();
            countingFbos.write(bytes, 0, bytes.length);

            // Attempt to read from FileBackedOutputStream
            final byte[] buffer = new byte[50];
            int numBytesRead = is.read(buffer, 0, 50);

            // Verify bytes read is -1
            assertThat(numBytesRead, is(-1));

            // Verify read inputstream performed twice
            String logMsg = out.toString();
            assertThat(logMsg, is(notNullValue()));
            assertThat(logMsg, containsString("First time reading inputstream"));
            //assertThat(logMsg, containsString("Retry reading inputstream"));

        } finally {
            logger.removeAppender(appender);
        }
    }

}
