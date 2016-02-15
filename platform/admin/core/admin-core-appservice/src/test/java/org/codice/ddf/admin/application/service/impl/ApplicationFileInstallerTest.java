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
package org.codice.ddf.admin.application.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;

import org.codice.ddf.admin.application.service.ApplicationServiceException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;

public class ApplicationFileInstallerTest {

    private static final String TEST_FILE_NAME = "/test-kar.zip";

    private static final String BAD_FILE_NAME = "/test-badfile";

    private static final String BAD_ZIP_NAME = "/test-badzip.zip";

    private static final String MAIN_FEATURE_NAME = "main-feature";

    private static final String MAIN_FEATURE_VERSION = "1.0.1";

    private static final String INVALID_ZIP_STRING =
            "Got an error when trying to read the application as a zip file.";

    private static final String IOEX_STRING = "Could not write out file.";

    private static final String INSTALL_IOEX =
            "Got an error when trying to read the incoming application.";

    private Logger logger = LoggerFactory.getLogger(ApplicationFileInstaller.class);

    private Appender mockAppender;

    private ch.qos.logback.classic.Logger root;

    @Before
    public void setUpLogger() {
        root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        mockAppender = mock(Appender.class);
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);
        root.setLevel(Level.ALL);
    }

    /**
     * Tests the {@link ApplicationFileInstaller#install(File)} method
     *
     * @Throws Exception
     */
    @Test
    public void testInstall() throws Exception {

        ApplicationFileInstaller testInstaller = new ApplicationFileInstaller();
        File testFile = new File(File.class.getResource(TEST_FILE_NAME)
                .getPath());

        assertTrue("Returned URI should have a unix-style path",
                testInstaller.install(testFile)
                        .getPath()
                        .matches("(/.+)+"));

        verify(mockAppender).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
                return ((LoggingEvent) argument).getFormattedMessage()
                        .contains(IOEX_STRING);
            }
        }));
    }

    /**
     * Tests the {@link ApplicationFileInstaller#install(File)} method
     * for the case where the file given is not a valid zip file
     *
     * @throws Exception
     */
    @Test(expected = ApplicationServiceException.class)
    public void testInstallInvalidZip() throws Exception {
        ApplicationFileInstaller testInstaller = new ApplicationFileInstaller();
        File testFile = new File(File.class.getResource(BAD_FILE_NAME)
                .getPath());

        testInstaller.install(testFile);

        verify(mockAppender).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
                return ((LoggingEvent) argument).getFormattedMessage()
                        .contains(INVALID_ZIP_STRING);
            }
        }));
    }

    /**
     * Tests the {@link ApplicationFileInstaller#install(File)} method
     * for the case where the file contains no features
     *
     * @Throws Exception
     */
    @Test(expected = ApplicationServiceException.class)
    public void testInstallInvalidFile() throws Exception {

        ApplicationFileInstaller testInstaller = new ApplicationFileInstaller();
        File testFile = new File(File.class.getResource(BAD_ZIP_NAME)
                .getPath());

        testInstaller.install(testFile);
    }

    /**
     * Tests the {@link ApplicationFileInstaller#install(File)} method
     * for the case where an IOException is thrown when the ZipFile is created
     *
     * @throws Exception
     */
    @Test(expected = ApplicationServiceException.class)
    public void testInstallZipFileIOEx() throws Exception {
        ApplicationFileInstaller testInstaller = new ApplicationFileInstaller();
        File testFile = mock(File.class);
        when(testFile.getPath()).thenReturn("TestPath");
        when(testFile.lastModified()).thenReturn((long) 1);

        testInstaller.install(testFile);

        verify(mockAppender).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
                return ((LoggingEvent) argument).getFormattedMessage()
                        .contains(INSTALL_IOEX);
            }
        }));
    }

    /**
     * Tests the {@link ApplicationFileInstaller#getAppDetails(File)} method
     *
     * @Throws Exception
     */
    @Test
    public void testGetAppDetails() throws Exception {
        ApplicationFileInstaller testInstaller = new ApplicationFileInstaller();
        File testFile = new File(File.class.getResource(TEST_FILE_NAME)
                .getPath());
        ZipFileApplicationDetails testFileDetails;

        testFileDetails = testInstaller.getAppDetails(testFile);

        assertNotNull(testFileDetails);
        assertEquals(MAIN_FEATURE_NAME, testFileDetails.getName());
        assertEquals(MAIN_FEATURE_VERSION, testFileDetails.getVersion());
    }

    /**
     * Tests the {@link ApplicationFileInstaller#getAppDetails(File)} method
     * for the case where a ZipException is thrown
     *
     * @Throws Exception
     */
    @Test(expected = ApplicationServiceException.class)
    public void testGetAppDetailsZipException() throws Exception {
        ApplicationFileInstaller testInstaller = new ApplicationFileInstaller();
        File testFile = new File(File.class.getResource(BAD_FILE_NAME)
                .getPath());

        testInstaller.getAppDetails(testFile);
    }

    /**
     * Tests the {@link ApplicationFileInstaller#getAppDetails(File)} method
     * for the case where an IOException is thrown when the ZipFile is created
     *
     * @throws Exception
     */
    @Test(expected = ApplicationServiceException.class)
    public void testGetAppDetailsIOException() throws Exception {
        ApplicationFileInstaller testInstaller = new ApplicationFileInstaller();
        File testFile = mock(File.class);
        when(testFile.getPath()).thenReturn("TestPath");
        when(testFile.lastModified()).thenReturn((long) 1);

        testInstaller.getAppDetails(testFile);
    }
}
