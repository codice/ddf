/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.ftp.ftplets;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.CatalogFramework;
import ddf.catalog.content.operation.CreateStorageRequest;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.mime.MimeTypeMapper;
import ddf.mime.MimeTypeResolutionException;
import ddf.security.Subject;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.FtpReply;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.ftplet.FtpletResult;
import org.apache.ftpserver.impl.IODataConnectionFactory;
import org.codice.ddf.platform.util.TemporaryFileBackedOutputStream;
import org.codice.ddf.platform.util.uuidgenerator.UuidGenerator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class FtpRequestHandlerTest {

  private static final String FILE_NAME = "SomeFile.xml";

  private static final String SUBJECT = "subject";

  private static final String METACARD_ID = "00000000000000000000000";

  private FtpRequestHandler ftplet;

  private FtpSession session;

  private FtpRequest request;

  private CatalogFramework catalogFramework;

  private MimeTypeMapper mimeTypeMapper;

  private UuidGenerator uuidGenerator;

  @Before
  public void setUp() {
    session = mock(FtpSession.class, RETURNS_DEEP_STUBS);
    request = mock(FtpRequest.class);

    catalogFramework = mock(CatalogFramework.class);
    mimeTypeMapper = mock(MimeTypeMapper.class);
    uuidGenerator = mock(UuidGenerator.class);
    when(uuidGenerator.generateUuid()).thenReturn(UUID.randomUUID().toString());
    ftplet = new FtpRequestHandler(catalogFramework, mimeTypeMapper, uuidGenerator);
  }

  @Test(expected = FtpException.class)
  public void testOnUploadStartNullFtpFile() throws FtpException, IOException {
    Subject subject = mock(Subject.class);

    when(request.getArgument()).thenReturn(FILE_NAME);
    when(session.getAttribute(SUBJECT)).thenReturn(subject);
    when(session.getFileSystemView().getFile(FILE_NAME)).thenReturn(null);

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
    when(session.getFileSystemView().getFile(FILE_NAME).isWritable()).thenReturn(false);

    ftplet.onUploadStart(session, request);
  }

  @Test(expected = IOException.class)
  public void testOnUploadStartFailFileTransfer() throws Exception {
    Subject subject = mock(Subject.class);

    when(request.getArgument()).thenReturn(FILE_NAME);
    when(session.getAttribute(SUBJECT)).thenReturn(subject);
    when(session.getFileSystemView().getFile(FILE_NAME).isWritable()).thenReturn(true);
    when(session
            .getDataConnection()
            .openConnection()
            .transferFromClient(eq(session), any(TemporaryFileBackedOutputStream.class)))
        .thenThrow(new IOException());

    ftplet.onUploadStart(session, request);
  }

  @SuppressWarnings("unchecked")
  @Test(expected = FtpException.class)
  public void testCreateStorageRequestFail() throws Exception {
    Subject subject = mock(Subject.class);
    FtpFile ftpFile = mock(FtpFile.class);

    when(session.getAttribute(SUBJECT)).thenReturn(subject);
    when(request.getArgument()).thenReturn(FILE_NAME);
    when(session.getFileSystemView().getFile(FILE_NAME)).thenReturn(ftpFile);
    when(ftpFile.isWritable()).thenReturn(true);
    when(ftpFile.getAbsolutePath()).thenReturn(FILE_NAME);
    when(subject.execute(any(Callable.class)))
        .thenAnswer(invocationOnMock -> ((Callable) invocationOnMock.getArguments()[0]).call());
    when(catalogFramework.create((CreateStorageRequest) anyObject()))
        .thenThrow(new IngestException());

    ftplet.onUploadStart(session, request);
  }

  @Test
  public void testGetMimeTypeXml() throws MimeTypeResolutionException {
    String mimeType;

    when(mimeTypeMapper.guessMimeType(any(InputStream.class), eq("xml"))).thenReturn("text/xml");

    mimeType = ftplet.getMimeType("xml", new TemporaryFileBackedOutputStream());
    assertEquals("text/xml", mimeType);
  }

  @Test
  public void testGetMimeTypeOther() throws MimeTypeResolutionException {
    String mimeType;

    when(mimeTypeMapper.getMimeTypeForFileExtension("txt")).thenReturn("text/plain");

    mimeType = ftplet.getMimeType("txt", new TemporaryFileBackedOutputStream());
    assertEquals("text/plain", mimeType);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMimeTypeMapperNull() {
    String mimeType;
    catalogFramework = mock(CatalogFramework.class);

    FtpRequestHandler ftplett = new FtpRequestHandler(catalogFramework, null, uuidGenerator);

    mimeType = ftplett.getMimeType("xml", new TemporaryFileBackedOutputStream());
    assertEquals("", mimeType);
  }

  @Test
  public void testMimeTypeResolutionFailure() throws MimeTypeResolutionException {
    String mimeType;

    when(mimeTypeMapper.guessMimeType(any(InputStream.class), eq("xml")))
        .thenThrow(new MimeTypeResolutionException());

    mimeType = ftplet.getMimeType("xml", new TemporaryFileBackedOutputStream());
    assertEquals("", mimeType);
  }

  @SuppressWarnings("unchecked")
  private void setupIngest() throws FtpException, SourceUnavailableException, IngestException {
    Subject subject = mock(Subject.class);
    FtpFile ftpFile = mock(FtpFile.class);
    CreateResponse createResponse = mock(CreateResponse.class);

    Metacard metacard = mock(Metacard.class);
    when(metacard.getId()).thenReturn(METACARD_ID);

    when(createResponse.getCreatedMetacards()).thenReturn(Collections.singletonList(metacard));

    when(session.getAttribute(SUBJECT)).thenReturn(subject);
    when(request.getArgument()).thenReturn(FILE_NAME);
    when(session.getFileSystemView().getFile(FILE_NAME)).thenReturn(ftpFile);
    when(ftpFile.isWritable()).thenReturn(true);
    when(ftpFile.getAbsolutePath()).thenReturn(FILE_NAME);
    when(subject.execute(any(Callable.class)))
        .thenAnswer(invocationOnMock -> ((Callable) invocationOnMock.getArguments()[0]).call());
    when(catalogFramework.create(any(CreateStorageRequest.class))).thenReturn(createResponse);
  }

  @Test
  public void testFileIngestSuccess()
      throws FtpException, IOException, SourceUnavailableException, IngestException {
    setupIngest();
    FtpletResult result = ftplet.onUploadStart(session, request);
    assertEquals(FtpletResult.SKIP, result);
  }

  @Test
  public void testFileIngestUniqueSuccess()
      throws FtpException, IOException, SourceUnavailableException, IngestException {
    setupIngest();
    FtpletResult result = ftplet.onUploadUniqueStart(session, request);
    ArgumentCaptor<FtpReply> argumentCaptor = ArgumentCaptor.forClass(FtpReply.class);
    verify(session, atLeast(1)).write(argumentCaptor.capture());
    assertEquals(FtpletResult.SKIP, result);
    List<String> strReplies =
        argumentCaptor
            .getAllValues()
            .stream()
            .map(FtpReply::getMessage)
            .collect(Collectors.toList());
    assertThat(
        strReplies, hasItem(containsString("Storing data with unique name: " + METACARD_ID)));
  }

  private List<Integer> getReplyCodes() throws FtpException {
    ArgumentCaptor<FtpReply> argumentCaptor = ArgumentCaptor.forClass(FtpReply.class);
    verify(session, atLeast(1)).write(argumentCaptor.capture());
    return argumentCaptor
        .getAllValues()
        .stream()
        .map(FtpReply::getCode)
        .collect(Collectors.toList());
  }

  @Test
  public void testOnDeleteStart() throws FtpException, IOException {
    FtpletResult result = ftplet.onDeleteStart(session, request);
    assertEquals(FtpletResult.SKIP, result);
    assertThat(getReplyCodes(), hasItem(250));
  }

  @Test
  public void testOnDownloadStart() throws FtpException, IOException {
    FtpletResult result = ftplet.onDownloadStart(session, request);
    assertEquals(FtpletResult.SKIP, result);
    assertThat(getReplyCodes(), hasItem(250));
  }

  @Test
  public void testOnRmdirStart() throws FtpException, IOException {
    FtpletResult result = ftplet.onRmdirStart(session, request);
    assertEquals(FtpletResult.SKIP, result);
    assertThat(getReplyCodes(), hasItem(250));
  }

  @Test
  public void testOnMkdirStart() throws FtpException, IOException {
    FtpletResult result = ftplet.onMkdirStart(session, request);
    assertEquals(FtpletResult.SKIP, result);
    assertThat(getReplyCodes(), hasItem(257));
  }

  @Test
  public void testOnAppendStart() throws FtpException, IOException {
    FtpletResult result = ftplet.onAppendStart(session, request);
    assertEquals(FtpletResult.SKIP, result);
    assertThat(getReplyCodes(), hasItem(250));
  }

  @Test
  public void testOnRenameStart() throws FtpException, IOException {
    FtpFile ftpFile = mock(FtpFile.class);
    when(ftpFile.getName()).thenReturn("test.txt");
    when(session.getRenameFrom()).thenReturn(ftpFile);
    FtpletResult result = ftplet.onRenameStart(session, request);
    assertEquals(FtpletResult.SKIP, result);
    assertThat(getReplyCodes(), hasItem(250));
  }

  @Test
  public void testOnSite() throws FtpException, IOException {
    FtpletResult result = ftplet.onSite(session, request);
    assertEquals(FtpletResult.SKIP, result);
    assertThat(getReplyCodes(), hasItem(202));
  }
}
