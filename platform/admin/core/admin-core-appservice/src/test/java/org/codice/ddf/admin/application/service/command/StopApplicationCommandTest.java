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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.codice.ddf.admin.application.service.Application;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.codice.ddf.admin.application.service.ApplicationStatus;
import org.codice.ddf.admin.application.service.ApplicationStatus.ApplicationState;
import org.codice.ddf.admin.application.service.impl.ApplicationImpl;
import org.codice.ddf.admin.application.service.impl.ApplicationServiceImpl;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StopApplicationCommandTest {
    private Logger logger = LoggerFactory.getLogger(AddApplicationCommand.class);

    private static final String APP_NAME = "TestApp";

    /**
     * Tests the {@link StopApplicationCommand} class and its associated methods
     *
     * @throws Exception
     */
    @Test
    public void testStopApplicationCommandTest() throws Exception {
        ApplicationService testAppService = mock(ApplicationServiceImpl.class);

        Application testApp = mock(ApplicationImpl.class);
        ApplicationStatus testStatus = mock(ApplicationStatus.class);

        StopApplicationCommand stopApplicationCommand = new StopApplicationCommand();
        stopApplicationCommand.appName = APP_NAME;

        when(testStatus.getState()).thenReturn(ApplicationState.ACTIVE);
        when(testAppService.getApplicationStatus(testApp)).thenReturn(testStatus);
        when(testAppService.getApplication(APP_NAME)).thenReturn(testApp);

        stopApplicationCommand.doExecute(testAppService);
        verify(testAppService).stopApplication(APP_NAME);
    }

    /**
     * Tests the {@link StopApplicationCommand} class and its associated methods
     * for the case where the application parameter has already been stopped
     *
     * @throws Exception
     */
    @Test
    public void testStopApplicationCommandAlreadyStopped() throws Exception {
        ApplicationService testAppService = mock(ApplicationServiceImpl.class);

        Application testApp = mock(ApplicationImpl.class);
        ApplicationStatus testStatus = mock(ApplicationStatus.class);

        StopApplicationCommand stopApplicationCommand = new StopApplicationCommand();
        stopApplicationCommand.appName = APP_NAME;

        when(testStatus.getState()).thenReturn(ApplicationState.INACTIVE);
        when(testAppService.getApplicationStatus(testApp)).thenReturn(testStatus);
        when(testAppService.getApplication(APP_NAME)).thenReturn(testApp);

        // Should handle this condition gracefully without throwing an exception
        // If an exception is thrown, this test fails...
        stopApplicationCommand.doExecute(testAppService);
    }
}
