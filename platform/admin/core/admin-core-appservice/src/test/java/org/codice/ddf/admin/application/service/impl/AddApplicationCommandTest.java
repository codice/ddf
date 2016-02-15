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

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;

import org.codice.ddf.admin.application.service.ApplicationService;
import org.codice.ddf.admin.application.service.ApplicationServiceException;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;

public class AddApplicationCommandTest {

    private static final String CMD_ERROR_STRING = "Error while performing command.";

    private static final String BUNDLE_CONTEXT_STRING =
            "Bundle Context was closed, service reference already removed.";

    private Logger logger = LoggerFactory.getLogger(AddApplicationCommand.class);

    /**
     * Tests the {@link AddApplicationCommand} class and its contained methods
     *
     * @Throws Exception
     */
    @Test
    public void testAddApplicationCommand() throws Exception {
        ApplicationService testAppService = mock(ApplicationServiceImpl.class);
        BundleContext bundleContext = mock(BundleContext.class);
        ServiceReference<ApplicationService> mockFeatureRef;
        mockFeatureRef = (ServiceReference<ApplicationService>) mock(ServiceReference.class);

        AddApplicationCommand addApplicationCommand = new AddApplicationCommand();
        addApplicationCommand.appName = "TestApp";
        addApplicationCommand.setBundleContext(bundleContext);

        when(bundleContext.getServiceReference(ApplicationService.class)).thenReturn(mockFeatureRef);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(testAppService);

        addApplicationCommand.doExecute();
        verify(testAppService).addApplication(any(URI.class));
    }

    /**
     * Tests the {@link AddApplicationCommand} class and its contained methods
     * for the case where it is given an invalid URI as a parameter
     *
     * @throws Exception
     */
    @Test
    public void testAddApplicationCommandInvalidURIParam() throws Exception {
        ApplicationService testAppService = mock(ApplicationServiceImpl.class);
        BundleContext bundleContext = mock(BundleContext.class);
        ServiceReference<ApplicationService> mockFeatureRef;
        mockFeatureRef = (ServiceReference<ApplicationService>) mock(ServiceReference.class);

        AddApplicationCommand addApplicationCommand = new AddApplicationCommand();
        addApplicationCommand.appName = ">BadURI<";
        addApplicationCommand.setBundleContext(bundleContext);

        when(bundleContext.getServiceReference(ApplicationService.class)).thenReturn(mockFeatureRef);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(testAppService);

        //Should have a graceful recovery, if an exception is thrown, this test fails.
        addApplicationCommand.doExecute();
    }

    /**
     * Tests the {@link AddApplicationCommand} class and its contained methods
     * for the case where the ApplicationService throws an ApplicationServiceException
     *
     * @throws Exception
     */
    @Test
    public void testAddApplicationCommandASE() throws Exception {
        ch.qos.logback.classic.Logger root =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        final Appender mockAppender = mock(Appender.class);
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);
        root.setLevel(Level.ALL);

        ApplicationService testAppService = mock(ApplicationServiceImpl.class);
        BundleContext bundleContext = mock(BundleContext.class);
        ServiceReference<ApplicationService> mockFeatureRef;
        mockFeatureRef = (ServiceReference<ApplicationService>) mock(ServiceReference.class);

        AddApplicationCommand addApplicationCommand = new AddApplicationCommand();
        addApplicationCommand.appName = "TestApp";
        addApplicationCommand.setBundleContext(bundleContext);

        when(bundleContext.getServiceReference(ApplicationService.class)).thenReturn(mockFeatureRef);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(testAppService);

        doThrow(new ApplicationServiceException()).when(testAppService)
                .addApplication(any(URI.class));

        addApplicationCommand.doExecute();

        verify(mockAppender).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
                return ((LoggingEvent) argument).getFormattedMessage()
                        .contains(CMD_ERROR_STRING);
            }
        }));
    }

    /**
     * Tests the {@link AddApplicationCommand} class and its contained methods
     * for the case where the bundleContext throws an IllegalStateException
     *
     * @throws Exception
     */
    @Test
    public void testAddApplicationCommandISE() throws Exception {
        ch.qos.logback.classic.Logger root =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        final Appender mockAppender = mock(Appender.class);
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);
        root.setLevel(Level.ALL);

        ApplicationService testAppService = mock(ApplicationServiceImpl.class);
        BundleContext bundleContext = mock(BundleContext.class);
        ServiceReference<ApplicationService> mockFeatureRef;
        mockFeatureRef = (ServiceReference<ApplicationService>) mock(ServiceReference.class);

        AddApplicationCommand addApplicationCommand = new AddApplicationCommand();
        addApplicationCommand.appName = "TestApp";
        addApplicationCommand.setBundleContext(bundleContext);

        when(bundleContext.getServiceReference(ApplicationService.class)).thenReturn(mockFeatureRef);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(testAppService);

        doThrow(new IllegalStateException()).when(bundleContext)
                .ungetService(any(ServiceReference.class));

        addApplicationCommand.doExecute();

        verify(mockAppender).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
                return ((LoggingEvent) argument).getFormattedMessage()
                        .contains(BUNDLE_CONTEXT_STRING);
            }
        }));
    }
}
