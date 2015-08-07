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
package org.codice.ddf.admin.application.rest;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;

public class ApplicationUploadEndpointTest {
    private static final String FILENAME_CONTENT_DISPOSITION_PARAMETER_NAME = "filename";

    private static final String TEST_FILE_LOCATION = "target/ApplicationUploadEndpointTest/";

    private static final String TEST_FILE_NAME = "/test-kar.jar";

    private static final String BAD_FILE_NAME = "/test-badfile";

    private static final String WRONG_FILE_TYPE = "Wrong file type.";

    private static final String IOEX_STRING = "IOException";

    private static final String FILE_NOT_FOUND = "No file attachment found";

    private static final String USING_DEFAULT = "Filename not found, using default.";

    private Logger logger = LoggerFactory.getLogger(ApplicationUploadEndpoint.class);

    private ApplicationService testAppService;

    private MultipartBody testMultipartBody;

    private UriInfo testUriInfo;

    private List<Attachment> attachmentList;

    private Attachment testAttach1;

    private ContentDisposition testDisp;

    private DataHandler testDataHandler;

    private File testFile;

    private InputStream testIS;

    @Before
    public void setUp() throws Exception {
        testAppService = mock(ApplicationService.class);
        testMultipartBody = mock(MultipartBody.class);
        testUriInfo = mock(UriInfo.class);
        attachmentList = new ArrayList<>();
        testAttach1 = mock(Attachment.class);
        testDisp = mock(ContentDisposition.class);
        testDataHandler = mock(DataHandler.class);
        attachmentList.add(testAttach1);
        testFile = new File(File.class.getResource(TEST_FILE_NAME).getPath());
        testIS = new FileInputStream(testFile);

        when(testAttach1.getDataHandler()).thenReturn(testDataHandler);
        when(testAttach1.getContentDisposition()).thenReturn(testDisp);
        when(testMultipartBody.getAllAttachments()).thenReturn(attachmentList);
        when(testDataHandler.getInputStream()).thenReturn(testIS);
        when(testDisp.getParameter(FILENAME_CONTENT_DISPOSITION_PARAMETER_NAME))
                .thenReturn(TEST_FILE_NAME);
    }

    /**
     * Tests the {@link ApplicationUploadEndpoint#update(MultipartBody, UriInfo)} method
     *
     * @throws Exception
     */
    @Test
    public void testApplicationUploadEndpointUpdate() throws Exception {
        ApplicationUploadEndpoint applicationUploadEndpoint = new ApplicationUploadEndpoint(
                testAppService);
        applicationUploadEndpoint.setDefaultFileLocation(TEST_FILE_LOCATION);

        Response response = applicationUploadEndpoint.update(testMultipartBody, testUriInfo);
        Response testResponse = Response.ok("{\"status\":\"success\"}").type("application/json")
                .build();
        assertThat("Returned status is success", response.getStatus(),
                is(testResponse.getStatus()));
    }

    /**
     * Tests the {@link ApplicationUploadEndpoint#update(MultipartBody, UriInfo)} method
     * for the case where the application exists and has already been started
     *
     * @throws Exception
     */
    @Test
    public void testApplicationUploadEndpointUpdateAppStarted() throws Exception {
        Application testApp = mock(Application.class);
        when(testAppService.getApplication(anyString())).thenReturn(testApp);
        when(testAppService.isApplicationStarted(testApp)).thenReturn(true);

        ApplicationUploadEndpoint applicationUploadEndpoint = new ApplicationUploadEndpoint(
                testAppService);
        applicationUploadEndpoint.setDefaultFileLocation(TEST_FILE_LOCATION);
        Response response = applicationUploadEndpoint.update(testMultipartBody, testUriInfo);

        Response testResponse = Response.ok("{\"status\":\"success\"}").type("application/json")
                .build();
        assertThat("Returned status is success", response.getStatus(),
                is(testResponse.getStatus()));
        verify(testAppService).removeApplication(testApp);
        verify(testAppService).startApplication(anyString());
    }

    /**
     * Tests the {@link ApplicationUploadEndpoint#update(MultipartBody, UriInfo)} method
     * for the case where the file can not be found
     *
     * @throws Exception
     */
    @Test
    public void testApplicationUploadEndpointUpdateFileNotFound() throws Exception {
        when(testMultipartBody.getAllAttachments()).thenReturn(new ArrayList<Attachment>());

        ApplicationUploadEndpoint applicationUploadEndpoint = new ApplicationUploadEndpoint(
                testAppService);
        applicationUploadEndpoint.setDefaultFileLocation(TEST_FILE_LOCATION);

        Response response = applicationUploadEndpoint.update(testMultipartBody, testUriInfo);
        Response expectedResponse = Response.serverError().build();
        assertThat("Response should indicate server error.", response.getStatus(),
                is(expectedResponse.getStatus()));

    }

    /**
     * Tests the {@link ApplicationUploadEndpoint#create(MultipartBody, UriInfo)} method
     *
     * @throws Exception
     */
    @Test
    public void testApplicationUploadEndpointCreate() throws Exception {
        ApplicationUploadEndpoint applicationUploadEndpoint = new ApplicationUploadEndpoint(
                testAppService);
        applicationUploadEndpoint.setDefaultFileLocation(TEST_FILE_LOCATION);
        Response response = applicationUploadEndpoint.create(testMultipartBody, testUriInfo);

        Response expectedResponse = Response.ok().build();
        assertThat("No errors reported by response", response.getStatus(),
                is(expectedResponse.getStatus()));
        verify(testAppService).addApplication(Mockito.any(URI.class));
    }

    /**
     * Tests the {@link ApplicationUploadEndpoint#create(MultipartBody, UriInfo)} method
     * for the case where an ApplicationServiceException is thrown
     *
     * @throws Exception
     */
    @Test
    public void testApplicationUploadEndpointCreateApplicationServiceException() throws Exception {
        ApplicationUploadEndpoint applicationUploadEndpoint = new ApplicationUploadEndpoint(
                testAppService);

        doThrow(new ApplicationServiceException()).when(testAppService)
                .addApplication(Mockito.any(URI.class));

        applicationUploadEndpoint.setDefaultFileLocation(TEST_FILE_LOCATION);

        Response response = applicationUploadEndpoint.create(testMultipartBody, testUriInfo);

        Response expectedResponse = Response.serverError().build();

        assertThat("Response should report server error.", response.getStatus(),
                is(expectedResponse.getStatus()));
    }

    /**
     * Tests the {@link ApplicationUploadEndpoint#create(MultipartBody, UriInfo)} method
     * for the case where the source file has an invalid type
     *
     * @throws Exception
     */
    @Test
    public void testApplicationUploadEndpointCreateInvalidType() throws Exception {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory
                .getLogger(Logger.ROOT_LOGGER_NAME);
        final Appender mockAppender = mock(Appender.class);
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);
        root.setLevel(Level.ALL);

        ApplicationUploadEndpoint applicationUploadEndpoint = new ApplicationUploadEndpoint(
                testAppService);

        testFile = new File(File.class.getResource(BAD_FILE_NAME).getPath());
        testIS = mock(InputStream.class);

        when(testIS.available()).thenReturn(1);
        when(testDataHandler.getInputStream()).thenReturn(testIS);
        when(testDisp.getParameter(FILENAME_CONTENT_DISPOSITION_PARAMETER_NAME))
                .thenReturn(BAD_FILE_NAME);

        applicationUploadEndpoint.setDefaultFileLocation(TEST_FILE_LOCATION);

        Response response = applicationUploadEndpoint.create(testMultipartBody, testUriInfo);

        Response expectedResponse = Response.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE_415).build();

        verify(mockAppender).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
                return ((LoggingEvent) argument).getFormattedMessage().contains(WRONG_FILE_TYPE);
            }
        }));
    }

    /**
     * Tests the {@link ApplicationUploadEndpoint#create(MultipartBody, UriInfo)} method
     * for the case where the source file causes an IOException when it is read
     *
     * @throws Exception
     */
    @Test
    public void testApplicationUploadEndpointCreateIOException() throws Exception {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory
                .getLogger(Logger.ROOT_LOGGER_NAME);
        final Appender mockAppender = mock(Appender.class);
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);

        ApplicationUploadEndpoint applicationUploadEndpoint = new ApplicationUploadEndpoint(
                testAppService);

        doThrow(new IOException()).when(testDataHandler).getInputStream();
        when(testDisp.getParameter(FILENAME_CONTENT_DISPOSITION_PARAMETER_NAME))
                .thenReturn(BAD_FILE_NAME);

        applicationUploadEndpoint.setDefaultFileLocation(TEST_FILE_LOCATION);

        applicationUploadEndpoint.create(testMultipartBody, testUriInfo);

        verify(mockAppender).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
                return ((LoggingEvent) argument).getFormattedMessage().contains(IOEX_STRING);
            }
        }));
    }

    /**
     * Tests the {@link ApplicationUploadEndpoint#create(MultipartBody, UriInfo)} method
     * for the case where the file cannot be found (inside of createFileAttachement(..))
     *
     * @throws Exception
     */
    @Test
    public void testApplicationUploadEndpointCreateFileNotFound() throws Exception {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory
                .getLogger(Logger.ROOT_LOGGER_NAME);
        final Appender mockAppender = mock(Appender.class);
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);
        root.setLevel(Level.ALL);

        ApplicationUploadEndpoint applicationUploadEndpoint = new ApplicationUploadEndpoint(
                testAppService);

        testFile = new File(File.class.getResource(TEST_FILE_NAME).getPath());
        testIS = null;

        when(testDataHandler.getInputStream()).thenReturn(testIS);
        when(testDisp.getParameter(FILENAME_CONTENT_DISPOSITION_PARAMETER_NAME))
                .thenReturn(TEST_FILE_NAME);

        applicationUploadEndpoint.setDefaultFileLocation(TEST_FILE_LOCATION);

        applicationUploadEndpoint.create(testMultipartBody, testUriInfo);

        verify(mockAppender).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
                return ((LoggingEvent) argument).getFormattedMessage().contains(FILE_NOT_FOUND);
            }
        }));
    }

    /**
     * Tests the {@link ApplicationUploadEndpoint#create(MultipartBody, UriInfo)} method
     * for the case where the filename is empty
     *
     * @throws Exception
     */
    @Test
    public void testApplicationUploadEndpointCreateEmptyFilename() throws Exception {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory
                .getLogger(Logger.ROOT_LOGGER_NAME);
        final Appender mockAppender = mock(Appender.class);
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);
        root.setLevel(Level.ALL);

        ApplicationUploadEndpoint applicationUploadEndpoint = new ApplicationUploadEndpoint(
                testAppService);

        testFile = new File(File.class.getResource(TEST_FILE_NAME).getPath());
        testIS = new FileInputStream(testFile);

        when(testDataHandler.getInputStream()).thenReturn(testIS);
        when(testDisp.getParameter(FILENAME_CONTENT_DISPOSITION_PARAMETER_NAME))
                .thenReturn(StringUtils.EMPTY);

        applicationUploadEndpoint.setDefaultFileLocation(TEST_FILE_LOCATION);

        applicationUploadEndpoint.create(testMultipartBody, testUriInfo);

        verify(mockAppender).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
                return ((LoggingEvent) argument).getFormattedMessage().contains(USING_DEFAULT);
            }
        }));
    }
}
