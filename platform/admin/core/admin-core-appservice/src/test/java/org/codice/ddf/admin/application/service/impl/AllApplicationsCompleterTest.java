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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.codice.ddf.admin.application.service.Application;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;

public class AllApplicationsCompleterTest {

    private static final String NO_APP_SERV = "No application service";

    /**
     * Tests the {@link AllApplicationsCompleter#complete(String, int, List)} method,
     * and the {@link AllApplicationsCompleter#AllApplicationsCompleter(ApplicationService)} constructor
     */
    @Test
    public void testAllApplicationsCompleter() {
        Application testApp = mock(ApplicationImpl.class);
        ApplicationService testAppService = mock(ApplicationServiceImpl.class);
        Set<Application> testAppSet = new HashSet<>();
        testAppSet.add(testApp);
        when(testAppService.getApplications()).thenReturn(testAppSet);
        when(testApp.getName()).thenReturn("TestApp");

        AllApplicationsCompleter applicationsCompleter =
                new AllApplicationsCompleter(testAppService);

        assertThat("If the return value is -1, the expected match was not found.",
                applicationsCompleter.complete("Tes", 2, new ArrayList()),
                is(not(-1)));
    }

    /**
     * Tests the {@link AllApplicationsCompleter#complete(String, int, List)} method
     * for the case where the ApplicationService given to it is null
     */
    @Test
    public void testAllApplicationsCompleterNullAppService() {
        ch.qos.logback.classic.Logger root =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        final Appender mockAppender = mock(Appender.class);
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);
        root.setLevel(Level.ALL);

        ApplicationService testAppService = null;

        AllApplicationsCompleter allApplicationsCompleter = new AllApplicationsCompleter(
                testAppService);

        allApplicationsCompleter.complete("Tes", 2, new ArrayList());

        verify(mockAppender).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
                return ((LoggingEvent) argument).getFormattedMessage()
                        .contains(NO_APP_SERV);
            }
        }));
    }
}
