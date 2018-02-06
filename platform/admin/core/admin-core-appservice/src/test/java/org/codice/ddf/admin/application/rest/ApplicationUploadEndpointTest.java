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
package org.codice.ddf.admin.application.rest;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.activation.DataHandler;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.codice.ddf.admin.application.service.Application;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.codice.ddf.admin.application.service.ApplicationServiceException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationUploadEndpointTest {
  private static final String FILENAME_CONTENT_DISPOSITION_PARAMETER_NAME = "filename";

  private static final String TEST_FILE_LOCATION = "target/ApplicationUploadEndpointTest/";

  private static final String TEST_FILE_NAME = "/test-kar.jar";

  private static final String BAD_FILE_NAME = "/test-badfile";

  @Mock private ApplicationService mockAppService;

  @Mock private MultipartBody mockBody;

  @Mock private UriInfo mockUriInfo;

  @Mock private Attachment mockAttachment;

  @Mock private ContentDisposition mockDisposition;

  @Mock private DataHandler mockHandler;

  private File testFile;

  private InputStream testDataStream;

  @Before
  public void setUp() throws Exception {
    List<Attachment> attachmentList = new ArrayList<>();
    attachmentList.add(mockAttachment);

    testFile = new File(File.class.getResource(TEST_FILE_NAME).getPath());
    testDataStream = new FileInputStream(testFile);

    when(mockAttachment.getDataHandler()).thenReturn(mockHandler);
    when(mockAttachment.getContentDisposition()).thenReturn(mockDisposition);
    when(mockBody.getAllAttachments()).thenReturn(attachmentList);
    when(mockHandler.getInputStream()).thenReturn(testDataStream);
    when(mockDisposition.getParameter(FILENAME_CONTENT_DISPOSITION_PARAMETER_NAME))
        .thenReturn(TEST_FILE_NAME);

    ApplicationUploadEndpoint.setDefaultFileLocation(TEST_FILE_LOCATION);
  }

  /** Tests the {@link ApplicationUploadEndpoint#update(MultipartBody, UriInfo)} method */
  @Test
  public void testApplicationUploadEndpointUpdate() throws Exception {
    ApplicationUploadEndpoint applicationUploadEndpoint =
        new ApplicationUploadEndpoint(mockAppService);

    Response response = applicationUploadEndpoint.update(mockBody, mockUriInfo);
    Response testResponse =
        Response.ok("{\"status\":\"success\"}").type("application/json").build();
    assertThat("Returned status is success", response.getStatus(), is(testResponse.getStatus()));
  }

  /**
   * Tests the {@link ApplicationUploadEndpoint#update(MultipartBody, UriInfo)} method for the case
   * where the application exists and has already been started
   */
  @Test
  public void testApplicationUploadEndpointUpdateAppStarted() throws Exception {
    Application testApp = mock(Application.class);
    when(mockAppService.getApplication(anyString())).thenReturn(testApp);
    when(mockAppService.isApplicationStarted(testApp)).thenReturn(true);

    ApplicationUploadEndpoint applicationUploadEndpoint =
        new ApplicationUploadEndpoint(mockAppService);
    Response response = applicationUploadEndpoint.update(mockBody, mockUriInfo);

    Response testResponse =
        Response.ok("{\"status\":\"success\"}").type("application/json").build();
    assertThat("Returned status is success", response.getStatus(), is(testResponse.getStatus()));
    verify(mockAppService).removeApplication(testApp);
    verify(mockAppService).startApplication(anyString());
  }

  /**
   * Tests the {@link ApplicationUploadEndpoint#update(MultipartBody, UriInfo)} method for the case
   * where the file can not be found
   */
  @Test
  public void testApplicationUploadEndpointUpdateFileNotFound() throws Exception {
    when(mockBody.getAllAttachments()).thenReturn(new ArrayList<>());

    ApplicationUploadEndpoint applicationUploadEndpoint =
        new ApplicationUploadEndpoint(mockAppService);

    Response response = applicationUploadEndpoint.update(mockBody, mockUriInfo);
    Response expectedResponse = Response.serverError().build();

    assertThat(
        "Response should indicate server error.",
        response.getStatus(),
        is(expectedResponse.getStatus()));
  }

  /** Tests the {@link ApplicationUploadEndpoint#create(MultipartBody, UriInfo)} method */
  @Test
  public void testApplicationUploadEndpointCreate() throws Exception {
    ApplicationUploadEndpoint applicationUploadEndpoint =
        new ApplicationUploadEndpoint(mockAppService);

    Response response = applicationUploadEndpoint.create(mockBody, mockUriInfo);
    Response expectedResponse = Response.ok().build();

    assertThat(
        "No errors reported by response", response.getStatus(), is(expectedResponse.getStatus()));
    verify(mockAppService).addApplication(Mockito.any(URI.class));
  }

  /**
   * Tests the {@link ApplicationUploadEndpoint#create(MultipartBody, UriInfo)} method for the case
   * where an ApplicationServiceException is thrown
   */
  @Test
  public void testApplicationUploadEndpointCreateApplicationServiceException() throws Exception {
    ApplicationUploadEndpoint applicationUploadEndpoint =
        new ApplicationUploadEndpoint(mockAppService);

    doThrow(new ApplicationServiceException())
        .when(mockAppService)
        .addApplication(Mockito.any(URI.class));

    Response response = applicationUploadEndpoint.create(mockBody, mockUriInfo);

    Response expectedResponse = Response.serverError().build();

    assertThat(
        "Response should report server error.",
        response.getStatus(),
        is(expectedResponse.getStatus()));
  }

  /**
   * Tests the {@link ApplicationUploadEndpoint#create(MultipartBody, UriInfo)} method for the case
   * where the source file has an invalid type
   */
  @Test
  public void testApplicationUploadEndpointCreateInvalidType() throws Exception {
    ApplicationUploadEndpoint applicationUploadEndpoint =
        new ApplicationUploadEndpoint(mockAppService);

    testFile = new File(File.class.getResource(BAD_FILE_NAME).getPath());
    testDataStream = mock(InputStream.class);

    when(testDataStream.available()).thenReturn(1);
    when(mockHandler.getInputStream()).thenReturn(testDataStream);
    when(mockDisposition.getParameter(FILENAME_CONTENT_DISPOSITION_PARAMETER_NAME))
        .thenReturn(BAD_FILE_NAME);

    applicationUploadEndpoint.create(mockBody, mockUriInfo);
    verifyZeroInteractions(mockAppService);
  }

  /**
   * Tests the {@link ApplicationUploadEndpoint#create(MultipartBody, UriInfo)} method for the case
   * where the source file causes an IOException when it is read
   */
  @Test
  public void testApplicationUploadEndpointCreateIOException() throws Exception {
    ApplicationUploadEndpoint applicationUploadEndpoint =
        new ApplicationUploadEndpoint(mockAppService);

    doThrow(IOException.class).when(mockHandler).getInputStream();

    applicationUploadEndpoint.create(mockBody, mockUriInfo);
    verifyZeroInteractions(mockAppService);
  }

  /**
   * Tests the {@link ApplicationUploadEndpoint#create(MultipartBody, UriInfo)} method for the case
   * where the file cannot be found (inside of createFileAttachement(..))
   */
  @Test
  public void testApplicationUploadEndpointCreateFileNotFound() throws Exception {
    ApplicationUploadEndpoint applicationUploadEndpoint =
        new ApplicationUploadEndpoint(mockAppService);

    when(mockHandler.getInputStream()).thenReturn(null);

    applicationUploadEndpoint.create(mockBody, mockUriInfo);
    verifyZeroInteractions(mockAppService);
  }

  /**
   * Tests the {@link ApplicationUploadEndpoint#create(MultipartBody, UriInfo)} method for the case
   * where the filename is empty
   */
  @Test
  public void testApplicationUploadEndpointCreateEmptyFilename() throws Exception {
    ApplicationUploadEndpoint applicationUploadEndpoint =
        new ApplicationUploadEndpoint(mockAppService);

    testFile = new File(File.class.getResource(TEST_FILE_NAME).getPath());
    testDataStream = new FileInputStream(testFile);

    when(mockHandler.getInputStream()).thenReturn(testDataStream);
    when(mockDisposition.getParameter(FILENAME_CONTENT_DISPOSITION_PARAMETER_NAME))
        .thenReturn(StringUtils.EMPTY);

    applicationUploadEndpoint.create(mockBody, mockUriInfo);
    verify(mockAppService).addApplication(eq(new File(TEST_FILE_LOCATION, "file.jar").toURI()));
    verifyNoMoreInteractions(mockAppService);
  }
}
