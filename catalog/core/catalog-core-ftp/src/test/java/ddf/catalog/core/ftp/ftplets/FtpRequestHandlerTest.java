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
package ddf.catalog.core.ftp.ftplets;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;

import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.ftplet.FtpletResult;
import org.apache.ftpserver.impl.IODataConnectionFactory;
import org.junit.Before;
import org.junit.Test;

import com.google.common.io.FileBackedOutputStream;

import ddf.catalog.CatalogFramework;
import ddf.catalog.content.operation.CreateStorageRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.mime.MimeTypeMapper;
import ddf.mime.MimeTypeResolutionException;
import ddf.security.Subject;

public class FtpRequestHandlerTest {

    private static final String FILE_NAME = "SomeFile.xml";

    private static final String SUBJECT = "subject";

    private FtpRequestHandler ftplet;

    private FtpletResult result;

    private FtpSession session;

    private FtpRequest request;

    private static CatalogFramework catalogFramework;

    private static MimeTypeMapper mimeTypeMapper;

    @Before
    public void setUp() {
        session = mock(FtpSession.class, RETURNS_DEEP_STUBS);
        request = mock(FtpRequest.class);

        catalogFramework = mock(CatalogFramework.class);
        mimeTypeMapper = mock(MimeTypeMapper.class);

        ftplet = new FtpRequestHandler(catalogFramework, mimeTypeMapper);
    }

    @Test(expected = FtpException.class)
    public void testOnUploadStartNullFtpFile() throws FtpException, IOException {
        Subject subject = mock(Subject.class);

        when(request.getArgument()).thenReturn(FILE_NAME);
        when(session.getAttribute(SUBJECT)).thenReturn(subject);
        when(session.getFileSystemView()
                .getFile(FILE_NAME)).thenReturn(null);

        ftplet.onUploadStart(session, request);
    }

    @Test(expected = FtpException.class)
    public void testOnUploadStartNoClientAddress() throws FtpException, IOException {
        Subject subject = mock(Subject.class);
        IODataConnectionFactory dataConnectionFactory = mock(IODataConnectionFactory.class);

        when(session.getAttribute(SUBJECT)).thenReturn(subject);
        when(session.getDataConnection()).thenReturn(dataConnectionFactory);
        when(dataConnectionFactory.getInetAddress()).thenReturn(null);

        ftplet.onUploadStart(session, request);
    }

    @Test(expected = FtpException.class)
    public void testOnUploadStartNoFileWritePermission() throws FtpException, IOException {
        Subject subject = mock(Subject.class);

        when(request.getArgument()).thenReturn(FILE_NAME);
        when(session.getAttribute(SUBJECT)).thenReturn(subject);
        when(session.getFileSystemView()
                .getFile(FILE_NAME)
                .isWritable()).thenReturn(false);

        ftplet.onUploadStart(session, request);
    }

    @Test(expected = IOException.class)
    public void testOnUploadStartFailFileTransfer() throws Exception {
        Subject subject = mock(Subject.class);

        when(request.getArgument()).thenReturn(FILE_NAME);
        when(session.getAttribute(SUBJECT)).thenReturn(subject);
        when(session.getFileSystemView()
                .getFile(FILE_NAME)
                .isWritable()).thenReturn(true);
        when(session.getDataConnection()
                .openConnection()
                .transferFromClient(eq(session),
                        any(FileBackedOutputStream.class))).thenThrow(new IOException());

        ftplet.onUploadStart(session, request);
    }

    @Test(expected = FtpException.class)
    public void testCreateResponseNull()
            throws FtpException, IOException, MimeTypeResolutionException,
            SourceUnavailableException, IngestException {
        Subject subject = mock(Subject.class);
        FtpFile ftpFile = mock(FtpFile.class);

        when(session.getAttribute(SUBJECT)).thenReturn(subject);
        when(request.getArgument()).thenReturn(FILE_NAME);
        when(session.getFileSystemView()
                .getFile(FILE_NAME)).thenReturn(ftpFile);
        when(ftpFile.isWritable()).thenReturn(true);
        when(ftpFile.getAbsolutePath()).thenReturn(FILE_NAME);
        when(catalogFramework.create(any(CreateStorageRequest.class))).thenReturn(null);

        ftplet.onUploadStart(session, request);
    }

    @Test(expected = FtpException.class)
    public void testCreateStorageRequestFail()
            throws FtpException, IOException, MimeTypeResolutionException,
            SourceUnavailableException, IngestException {
        Subject subject = mock(Subject.class);
        FtpFile ftpFile = mock(FtpFile.class);

        when(session.getAttribute(SUBJECT)).thenReturn(subject);
        when(request.getArgument()).thenReturn(FILE_NAME);
        when(session.getFileSystemView()
                .getFile(FILE_NAME)).thenReturn(ftpFile);
        when(ftpFile.isWritable()).thenReturn(true);
        when(ftpFile.getAbsolutePath()).thenReturn(FILE_NAME);
        when(catalogFramework.create(any(CreateStorageRequest.class))).thenThrow(new IngestException());

        ftplet.onUploadStart(session, request);
    }

    @Test
    public void testGetMimeTypeXml() throws MimeTypeResolutionException {
        String mimeType;

        when(mimeTypeMapper.guessMimeType(any(InputStream.class),
                eq("xml"))).thenReturn("text/xml");

        mimeType = ftplet.getMimeType("xml", new FileBackedOutputStream(10000));
        assertEquals("text/xml", mimeType);
    }

    @Test
    public void testGetMimeTypeOther() throws MimeTypeResolutionException {
        String mimeType;

        when(mimeTypeMapper.getMimeTypeForFileExtension("txt")).thenReturn("text/plain");

        mimeType = ftplet.getMimeType("txt", new FileBackedOutputStream(10000));
        assertEquals("text/plain", mimeType);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMimeTypeMapperNull() {
        String mimeType;
        catalogFramework = mock(CatalogFramework.class);
        mimeTypeMapper = null;

        FtpRequestHandler ftplett = new FtpRequestHandler(catalogFramework, mimeTypeMapper);

        mimeType = ftplett.getMimeType("xml", new FileBackedOutputStream(10000));
        assertEquals("", mimeType);
    }

    @Test
    public void testMimeTypeResolutionFailure() throws MimeTypeResolutionException {
        String mimeType;

        when(mimeTypeMapper.guessMimeType(any(InputStream.class),
                eq("xml"))).thenThrow(new MimeTypeResolutionException());

        mimeType = ftplet.getMimeType("xml", new FileBackedOutputStream(10000));
        assertEquals("", mimeType);
    }

    @Test
    public void testFileIngestSuccess()
            throws FtpException, IOException, SourceUnavailableException, IngestException {
        Subject subject = mock(Subject.class);
        FtpFile ftpFile = mock(FtpFile.class);
        CreateResponse createResponse = mock(CreateResponse.class);

        when(session.getAttribute(SUBJECT)).thenReturn(subject);
        when(request.getArgument()).thenReturn(FILE_NAME);
        when(session.getFileSystemView()
                .getFile(FILE_NAME)).thenReturn(ftpFile);
        when(ftpFile.isWritable()).thenReturn(true);
        when(ftpFile.getAbsolutePath()).thenReturn(FILE_NAME);
        when(catalogFramework.create(any(CreateStorageRequest.class))).thenReturn(createResponse);

        ftplet.onUploadStart(session, request);

        result = ftplet.onUploadStart(session, request);
        assertEquals(FtpletResult.SKIP, result);
    }
}
