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

import java.util.HashSet;
import java.util.Set;

import org.apache.karaf.features.Feature;
import org.codice.ddf.admin.application.service.Application;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.codice.ddf.admin.application.service.ApplicationStatus;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListApplicationCommandTest {
    private Logger logger = LoggerFactory.getLogger(ListApplicationCommand.class);

    private ApplicationStatus testStatus;

    private Application testApp;

    private Set<Application> testAppSet;

    private ApplicationService testAppService;

    private BundleContext bundleContext;

    private ServiceReference<ApplicationService> mockFeatureRef;

    private Feature testFeature;

    private Set<Feature> featureSet;

    @Before
    public void setUp() throws Exception {
        testStatus = mock(ApplicationStatusImpl.class);
        testApp = mock(Application.class);
        testAppSet = new HashSet<>();
        testAppService = mock(ApplicationServiceImpl.class);
        bundleContext = mock(BundleContext.class);
        mockFeatureRef = (ServiceReference<ApplicationService>) mock(ServiceReference.class);
        testFeature = mock(Feature.class);
        testFeature = mock(Feature.class);
        featureSet = new HashSet<>();
        featureSet.add(testFeature);

        when(testAppService.getApplications()).thenReturn(testAppSet);
        testAppSet.add(testApp);
        when(testAppService.getApplicationStatus(testApp)).thenReturn(testStatus);
        when(testApp.getFeatures()).thenReturn(featureSet);

        when(bundleContext.getServiceReference(ApplicationService.class))
                .thenReturn(mockFeatureRef);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(testAppService);
    }

    /**
     * Tests the {@link ListApplicationCommand} class for active applications
     *
     * @throws Exception
     */
    @Test
    public void testListApplicationCommandActiveApp() throws Exception {
        when(testStatus.getState()).thenReturn(ApplicationStatus.ApplicationState.ACTIVE);

        ListApplicationCommand listApplicationCommand = new ListApplicationCommand();
        listApplicationCommand.setBundleContext(bundleContext);

        listApplicationCommand.doExecute();

        verify(testAppService).getApplications();
        verify(testAppService).getApplicationStatus(testApp);
    }

    /**
     * Tests the {@link ListApplicationCommand} class for inactive applications
     *
     * @throws Exception
     */
    @Test
    public void testListApplicationCommandInactiveApp() throws Exception {
        when(testStatus.getState()).thenReturn(ApplicationStatus.ApplicationState.INACTIVE);

        ListApplicationCommand listApplicationCommand = new ListApplicationCommand();
        listApplicationCommand.setBundleContext(bundleContext);

        listApplicationCommand.doExecute();

        verify(testAppService).getApplications();
        verify(testAppService).getApplicationStatus(testApp);
    }

    /**
     * Tests the {@link ListApplicationCommand} class for failed applications
     *
     * @throws Exception
     */
    @Test
    public void testListApplicationCommandFailedApp() throws Exception {
        when(testStatus.getState()).thenReturn(ApplicationStatus.ApplicationState.FAILED);

        ListApplicationCommand listApplicationCommand = new ListApplicationCommand();
        listApplicationCommand.setBundleContext(bundleContext);

        listApplicationCommand.doExecute();

        verify(testAppService).getApplications();
        verify(testAppService).getApplicationStatus(testApp);
    }

    /**
     * Tests the {@link ListApplicationCommand} class for unknown status applications
     *
     * @throws Exception
     */
    @Test
    public void testListApplicationCommandUnknownApp() throws Exception {
        when(testStatus.getState()).thenReturn(ApplicationStatus.ApplicationState.UNKNOWN);

        ListApplicationCommand listApplicationCommand = new ListApplicationCommand();
        listApplicationCommand.setBundleContext(bundleContext);

        listApplicationCommand.doExecute();

        verify(testAppService).getApplications();
        verify(testAppService).getApplicationStatus(testApp);
    }

}
