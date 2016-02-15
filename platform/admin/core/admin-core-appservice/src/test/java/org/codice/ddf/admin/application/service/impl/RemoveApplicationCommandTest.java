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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.codice.ddf.admin.application.service.ApplicationService;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoveApplicationCommandTest {
    private Logger logger = LoggerFactory.getLogger(RemoveApplicationCommand.class);

    private static final String APP_NAME = "TestApp";

    /**
     * Tests the {@link RemoveApplicationCommand} class
     *
     * @throws Exception
     */
    @Test
    public void testRemoveApplicationCommand() throws Exception {
        ApplicationService testAppService = mock(ApplicationServiceImpl.class);
        BundleContext bundleContext = mock(BundleContext.class);
        ServiceReference<ApplicationService> mockFeatureRef;
        mockFeatureRef = (ServiceReference<ApplicationService>) mock(ServiceReference.class);

        RemoveApplicationCommand removeApplicationCommand = new RemoveApplicationCommand();
        removeApplicationCommand.appName = APP_NAME;
        removeApplicationCommand.setBundleContext(bundleContext);

        when(bundleContext.getServiceReference(ApplicationService.class)).thenReturn(mockFeatureRef);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(testAppService);

        removeApplicationCommand.doExecute();
        verify(testAppService).removeApplication(APP_NAME);
    }
}
