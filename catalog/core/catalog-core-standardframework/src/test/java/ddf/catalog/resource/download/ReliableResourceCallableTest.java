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
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.cxf.common.i18n.Exception;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.common.io.CountingOutputStream;

public class ReliableResourceCallableTest {

    private static final int END_OF_FILE = -1;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    public File testFile;

    private AtomicLong bytesRead;

    private Object lock;

    private InputStream input = null;

    private ByteArrayOutputStream streamArray = new ByteArrayOutputStream();

    private CountingOutputStream countingFbos = new CountingOutputStream(streamArray);

    private FileInputStream cacheFis;

    private int chunkSize;

    private ReliableResourceStatus reliableResourceStatus;

    @Before
    public void setup() throws IOException {

        bytesRead = new AtomicLong(0);
        lock = mock(Object.class);
        input = mock(InputStream.class);
        testFile = testFolder.newFile("test.txt");
        chunkSize = 0;
        reliableResourceStatus = mock(ReliableResourceStatus.class);
        cacheFis = new FileInputStream(testFile);

    }

    /**
     * Checks that the value returned by getBytesRead changes to a new value after setBytesRead
     * has been passed a new value.
     */
    @Test
    public void setBytesReadChangesValue() throws Exception {

        ReliableResourceCallable testReliableResourceCallable = new ReliableResourceCallable(input,
                countingFbos,
                chunkSize,
                lock);

        testReliableResourceCallable.setBytesRead(0L);
        assertThat(testReliableResourceCallable.getBytesRead(), is(0L));
        testReliableResourceCallable.setBytesRead(15L);
        assertThat(testReliableResourceCallable.getBytesRead(), is(15L));
    }

    /**
     * Checks that when passed a true value, the set interrupt download recreates the reliable
     * resource status with the new interrupted status and the number of bytes in bytesRead.
     * Determines if the bytesRead are correct by comparing with what is returned by the
     * reliable resource status's getBytesRead method.
     */
    @Test
    public void setInterruptDownloadToTrue() throws Exception {

        ReliableResourceCallable testReliableResourceCallable = new ReliableResourceCallable(input,
                countingFbos,
                chunkSize,
                lock);

        testReliableResourceCallable.setInterruptDownload(true);

        assertThat(testReliableResourceCallable.getReliableResourceStatus()
                .getDownloadStatus(), is(DownloadStatus.RESOURCE_DOWNLOAD_INTERRUPTED));
        assertThat(testReliableResourceCallable.getReliableResourceStatus()
                .getBytesRead(), is(0L));
    }

    /**
     * Checks that when passed a true value, the set cancel download recreates the reliable
     * resource status with the new cancelled status and the number of bytes in bytesRead.
     * Determines if the bytesRead are correct by comparing with what is returned by the
     * reliable resource status's getBytesRead method.
     */
    @Test
    public void setCancelDownloadToTrue() throws Exception {

        ReliableResourceCallable testReliableResourceCallable = new ReliableResourceCallable(input,
                countingFbos,
                chunkSize,
                lock);

        testReliableResourceCallable.setCancelDownload(true);

        assertThat(testReliableResourceCallable.getReliableResourceStatus()
                .getDownloadStatus(), is(DownloadStatus.RESOURCE_DOWNLOAD_CANCELED));
        assertThat(testReliableResourceCallable.getReliableResourceStatus()
                .getBytesRead(), is(0L));

    }

    /**
     * Confirms that when the input is just an end of file, the download status and message
     * are set to indicate the download is complete.
     */
    @Test
    public void callDownloadSuccess() throws Exception, IOException {
        ReliableResourceStatus returnedStatus = null;

        ReliableResourceCallable testReliableResourceCallable = new ReliableResourceCallable(input,
                countingFbos,
                testFile,
                chunkSize,
                lock);

        when(input.read(any(byte[].class))).thenReturn(END_OF_FILE);
        returnedStatus = testReliableResourceCallable.call();

        assertThat(returnedStatus.getDownloadStatus(),
                is(DownloadStatus.RESOURCE_DOWNLOAD_COMPLETE));

        assertThat(returnedStatus.toString(),
                is("bytesRead = 0,  downloadStatus = RESOURCE_DOWNLOAD_COMPLETE,  message = Download completed successfully"));

    }

    /**
     * Throw an IOException while reading the input and check that the download status
     * is changed to indicate the product input stream exception by checking the
     * downloadStatus's getDownloadStatus() and toString().
     */
    @Test
    public void callDownloadIOException() throws Exception, IOException {
        ReliableResourceStatus returnedStatus = null;

        ReliableResourceCallable testReliableResourceCallable = new ReliableResourceCallable(input,
                countingFbos,
                testFile,
                chunkSize,
                lock);

        when(input.read(any(byte[].class))).thenThrow(new IOException());
        returnedStatus = testReliableResourceCallable.call();

        assertThat(returnedStatus.getDownloadStatus(),
                is(DownloadStatus.PRODUCT_INPUT_STREAM_EXCEPTION));

        assertThat(returnedStatus.toString(),
                is("bytesRead = 0,  downloadStatus = PRODUCT_INPUT_STREAM_EXCEPTION,  message = IOException during read of product's InputStream"));

    }

    /**
     * Confirms that, when the cache file passed to the constructor is null, the input is read
     * the expected number of times, and the bytes streamed to the client
     * match what is expected.
     */
    @Test
    public void callDownloadWithBytesToReadWhenCacheFileDoesNotExist()
            throws Exception, IOException {

        chunkSize = 1;
        int n = 1;

        byte[] expectedTotalBuffer = new byte[2];
        Arrays.fill(expectedTotalBuffer, (byte) 1);

        byte[] expectedLoopByLoopBuffer = new byte[1];
        Arrays.fill(expectedLoopByLoopBuffer, (byte) 1);

        ReliableResourceStatus returnedStatus = null;

        ReliableResourceCallable testReliableResourceCallable = new ReliableResourceCallable(input,
                countingFbos,
                null,
                chunkSize,
                lock);

        when(input.read(any(byte[].class))).thenAnswer(new Answer() {

            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                byte[] buffer = (byte[]) (args[0]);
                Arrays.fill(buffer, (byte) 1);
                return n;
            }
        })
                .thenAnswer(new Answer() {

                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                        Object[] args = invocationOnMock.getArguments();
                        byte[] buffer = (byte[]) (args[0]);
                        Arrays.fill(buffer, (byte) 1);
                        return n;
                    }
                })
                .thenReturn(END_OF_FILE);

        returnedStatus = testReliableResourceCallable.call();

        verify(input, times(3)).read(expectedLoopByLoopBuffer);

        assertThat(streamArray.toByteArray(), is(expectedTotalBuffer));
    }

    /**
     * Confirms that, when the cache file passed to the constructor is null, the download status
     * is set to complete when done, and the toString method reflects the number of bytes read.
     */
    @Test
    public void callDownloadWithBytesToReadWhenCacheFileDoesNotExistMessage()
            throws Exception, IOException {

        chunkSize = 1;
        int n = 1;

        byte[] expectedTotalBuffer = new byte[2];
        Arrays.fill(expectedTotalBuffer, (byte) 1);

        byte[] expectedLoopByLoopBuffer = new byte[1];
        Arrays.fill(expectedLoopByLoopBuffer, (byte) 1);

        ReliableResourceStatus returnedStatus = null;

        ReliableResourceCallable testReliableResourceCallable = new ReliableResourceCallable(input,
                countingFbos,
                null,
                chunkSize,
                lock);

        when(input.read(any(byte[].class))).thenAnswer(new Answer() {

            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                byte[] buffer = (byte[]) (args[0]);
                Arrays.fill(buffer, (byte) 1);
                return n;
            }
        })
                .thenAnswer(new Answer() {

                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                        Object[] args = invocationOnMock.getArguments();
                        byte[] buffer = (byte[]) (args[0]);
                        Arrays.fill(buffer, (byte) 1);
                        return n;
                    }
                })
                .thenReturn(END_OF_FILE);

        returnedStatus = testReliableResourceCallable.call();

        assertThat(returnedStatus.getDownloadStatus(),
                is(DownloadStatus.RESOURCE_DOWNLOAD_COMPLETE));

        assertThat(returnedStatus.toString(),
                is("bytesRead = 2,  downloadStatus = RESOURCE_DOWNLOAD_COMPLETE,  message = Download completed successfully"));

    }

    /**
     * Confirms that when the callable's call method is executed and the constructor is given
     * a cache file, data that is read from the input is also added to the cache file. Also
     * confirms that the input is read the expected number of times, and that the download status
     * is set to complete when the download is over.
     */
    @Test
    public void callDownloadWithBytesToReadWhenCacheFileExists() throws Exception, IOException {

        chunkSize = 1;
        int n = 1;

        byte[] expectedTotalBuffer = new byte[2];
        Arrays.fill(expectedTotalBuffer, (byte) 2);

        byte[] expectedLoopByLoopBuffer = new byte[1];
        Arrays.fill(expectedLoopByLoopBuffer, (byte) 2);

        ReliableResourceStatus returnedStatus = null;

        ReliableResourceCallable testReliableResourceCallable = new ReliableResourceCallable(input,
                countingFbos,
                testFile,
                chunkSize,
                lock);

        when(input.read(any(byte[].class))).thenAnswer(new Answer() {

            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                byte[] buffer = (byte[]) (args[0]);
                Arrays.fill(buffer, (byte) 2);
                return n;
            }
        })
                .thenAnswer(new Answer() {

                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                        Object[] args = invocationOnMock.getArguments();
                        byte[] buffer = (byte[]) (args[0]);
                        Arrays.fill(buffer, (byte) 2);
                        return n;
                    }
                })
                .thenReturn(END_OF_FILE);

        returnedStatus = testReliableResourceCallable.call();

        verify(input, times(3)).read(expectedLoopByLoopBuffer);

        assertThat(returnedStatus.getDownloadStatus(),
                is(DownloadStatus.RESOURCE_DOWNLOAD_COMPLETE));

        assertThat(cacheFis.read(), is(2));
        assertThat(cacheFis.read(), is(2));
        assertThat(cacheFis.read(), is(END_OF_FILE));

    }

    /**
     * When the cache file passed to the constructor is not null, tests the input is read
     * the expected number of times and the bytes streamed to the client
     * match what is expected.
     */
    @Test
    public void callDownloadWritesToClientOutputStream() throws IOException {
        int n = 5;
        chunkSize = 5;
        byte[] expectedBuffer = new byte[5];
        Arrays.fill(expectedBuffer, (byte) 5);

        when(input.read(any(byte[].class))).thenAnswer(new Answer() {

            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                byte[] buffer = (byte[]) (args[0]);
                Arrays.fill(buffer, (byte) 5);
                return n;
            }
        })
                .thenReturn(END_OF_FILE);

        ReliableResourceCallable testReliableResourceCallable = new ReliableResourceCallable(input,
                countingFbos,
                testFile,
                chunkSize,
                lock);

        testReliableResourceCallable.call();

        assertThat(streamArray.toByteArray(), is(expectedBuffer));
    }

    /**
     * Confirms that setting the interrupt download flag to true means that the input is never
     * read, and the download status and message indicate interruption.
     */
    @Test
    public void interruptStopsRead() throws IOException {
        chunkSize = 1;
        int n = 1;
        byte[] expectedTotalBuffer = new byte[2];
        Arrays.fill(expectedTotalBuffer, (byte) 2);

        byte[] expectedLoopByLoopBuffer = new byte[1];
        Arrays.fill(expectedLoopByLoopBuffer, (byte) 2);

        ReliableResourceStatus returnedStatus = null;

        ReliableResourceCallable testReliableResourceCallable = new ReliableResourceCallable(input,
                countingFbos,
                testFile,
                chunkSize,
                lock);

        testReliableResourceCallable.setInterruptDownload(true);

        when(input.read(any(byte[].class))).thenAnswer(new Answer() {

            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                byte[] buffer = (byte[]) (args[0]);
                Arrays.fill(buffer, (byte) 2);
                return n;
            }
        })
                .thenAnswer(new Answer() {

                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                        Object[] args = invocationOnMock.getArguments();
                        byte[] buffer = (byte[]) (args[0]);
                        Arrays.fill(buffer, (byte) 2);
                        return n;
                    }
                })
                .thenReturn(END_OF_FILE);

        returnedStatus = testReliableResourceCallable.call();

        verify(input, times(0)).read(expectedLoopByLoopBuffer);

        assertThat(testReliableResourceCallable.getReliableResourceStatus()
                .getDownloadStatus(), is(DownloadStatus.RESOURCE_DOWNLOAD_INTERRUPTED));
        assertThat(testReliableResourceCallable.getReliableResourceStatus()
                .getBytesRead(), is(0L));
        assertThat(returnedStatus.toString(),
                is("bytesRead = 0,  downloadStatus = RESOURCE_DOWNLOAD_INTERRUPTED,  message = Download interrupted - returning 0 bytes read"));

    }

}
