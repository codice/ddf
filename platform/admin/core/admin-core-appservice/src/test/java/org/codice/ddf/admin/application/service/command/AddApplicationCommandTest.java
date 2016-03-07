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
package org.codice.ddf.admin.application.service.command;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;

import org.codice.ddf.admin.application.service.ApplicationService;
import org.codice.ddf.admin.application.service.ApplicationServiceException;
import org.codice.ddf.admin.application.service.impl.ApplicationServiceImpl;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;

public class AddApplicationCommandTest {

    private static final String CMD_ERROR_STRING = "Error while performing command.";

    private static final String APPLICATION_SERVICE_NOT_FOUND =
            "ApplicationService not found";

    private Logger logger = LoggerFactory.getLogger(AddApplicationCommand.class);

    /**
     * Tests the {@link AddApplicationCommand} class and its contained methods
     *
     * @Throws Exception
     */
    @Test
    public void testAddApplicationCommand() throws Exception {
        ApplicationService testAppService = mock(ApplicationServiceImpl.class);

        AddApplicationCommand addApplicationCommand = new AddApplicationCommand();
        addApplicationCommand.appName = "TestApp";

        addApplicationCommand.doExecute(testAppService);
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

        AddApplicationCommand addApplicationCommand = new AddApplicationCommand();
        addApplicationCommand.appName = ">BadURI<";

        //Should have a graceful recovery, if an exception is thrown, this test fails.
        addApplicationCommand.doExecute(testAppService);
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

        AddApplicationCommand addApplicationCommand = new AddApplicationCommand();
        addApplicationCommand.appName = "TestApp";
        addApplicationCommand.setApplicationService(testAppService);

        doThrow(new ApplicationServiceException()).when(testAppService)
                .addApplication(any(URI.class));

        addApplicationCommand.execute();

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
     * for the case where the ApplicationService is null.
     *
     * @throws Exception
     */
    @Test (expected = IllegalStateException.class)
    public void testAddApplicationCommandISE() throws Exception {
        ch.qos.logback.classic.Logger root =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        final Appender mockAppender = mock(Appender.class);
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);
        root.setLevel(Level.ALL);

        AddApplicationCommand addApplicationCommand = new AddApplicationCommand();
        addApplicationCommand.appName = "TestApp";

        addApplicationCommand.execute();
    }
}
