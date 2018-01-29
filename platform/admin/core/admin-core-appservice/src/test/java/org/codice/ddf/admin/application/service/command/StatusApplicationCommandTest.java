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
package org.codice.ddf.admin.application.service.command;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;
import org.apache.karaf.features.Feature;
import org.codice.ddf.admin.application.service.Application;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.codice.ddf.admin.application.service.ApplicationStatus;
import org.codice.ddf.admin.application.service.ApplicationStatus.ApplicationState;
import org.codice.ddf.admin.application.service.impl.ApplicationImpl;
import org.codice.ddf.admin.application.service.impl.ApplicationServiceImpl;
import org.codice.ddf.admin.application.service.impl.ApplicationStatusImpl;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class StatusApplicationCommandTest {

  private static final String TEST_APP_NAME = "TestApp";

  private static final String TEST_FEATURE_NAME = "TestFeature";

  private ApplicationService testAppService;

  private BundleContext bundleContext;

  private ServiceReference<ApplicationService> mockFeatureRef;

  private Application testApp;

  private Feature testFeature;

  private Set<Feature> featureSet;

  private ApplicationStatus testStatus;

  @Before
  public void setUp() throws Exception {
    testAppService = mock(ApplicationServiceImpl.class);
    bundleContext = mock(BundleContext.class);
    mockFeatureRef = (ServiceReference<ApplicationService>) mock(ServiceReference.class);
    testApp = mock(ApplicationImpl.class);
    testFeature = mock(Feature.class);
    featureSet = new HashSet<>();
    featureSet.add(testFeature);
    testStatus = mock(ApplicationStatusImpl.class);

    when(bundleContext.getServiceReference(ApplicationService.class)).thenReturn(mockFeatureRef);
    when(bundleContext.getService(mockFeatureRef)).thenReturn(testAppService);

    when(testStatus.getState()).thenReturn(ApplicationState.ACTIVE);
    when(testFeature.getName()).thenReturn(TEST_FEATURE_NAME);
    when(testApp.getName()).thenReturn(TEST_APP_NAME);
    when(testAppService.getApplication(TEST_APP_NAME)).thenReturn(testApp);
    when(testAppService.getApplicationStatus(testApp)).thenReturn(testStatus);

    when(testStatus.getErrorBundles()).thenReturn(new HashSet<Bundle>());
    when(testStatus.getErrorFeatures()).thenReturn(new HashSet<Feature>());
  }

  /**
   * Tests the {@link StatusApplicationCommand} class and all associated methods for the case where
   * there are no features not started and no bundles not started
   *
   * @throws Exception
   */
  @Test
  public void testStatusApplicationCommandNoError() throws Exception {
    StatusApplicationCommand statusApplicationCommand = new StatusApplicationCommand();
    statusApplicationCommand.appName = TEST_APP_NAME;

    when(testApp.getFeatures()).thenReturn(featureSet);
    statusApplicationCommand.doExecute(testAppService);
    verify(testAppService).getApplicationStatus(testApp);
  }

  /**
   * Tests the {@link StatusApplicationCommand} class and all associated methods for the case where
   * there are features that have not been started
   *
   * @throws Exception
   */
  @Test
  public void testStatusApplicationCommandErrorFeatures() throws Exception {
    when(testStatus.getErrorFeatures()).thenReturn(featureSet);

    StatusApplicationCommand statusApplicationCommand = new StatusApplicationCommand();
    statusApplicationCommand.appName = TEST_APP_NAME;

    when(testApp.getFeatures()).thenReturn(featureSet);
    statusApplicationCommand.doExecute(testAppService);
    verify(testAppService).getApplicationStatus(testApp);
  }

  /**
   * Tests the {@link StatusApplicationCommand} class and all associated methods for the case where
   * there are bundles that have not been started
   *
   * @throws Exception
   */
  @Test
  public void testStatusApplicationCommandErrorBundles() throws Exception {
    Set<Bundle> errorBundleSet = new HashSet<>();
    Bundle testBundle = mock(Bundle.class);
    errorBundleSet.add(testBundle);
    when(testBundle.getBundleId()).thenReturn((long) 1);
    when(testBundle.getSymbolicName()).thenReturn("TestBundle");
    when(testStatus.getErrorBundles()).thenReturn(errorBundleSet);

    StatusApplicationCommand statusApplicationCommand = new StatusApplicationCommand();
    statusApplicationCommand.appName = TEST_APP_NAME;

    when(testApp.getFeatures()).thenReturn(featureSet);
    statusApplicationCommand.doExecute(testAppService);
    verify(testAppService).getApplicationStatus(testApp);
  }

  @Test
  public void testNullApplication() throws Exception {
    ApplicationService mockApplicationService = mock(ApplicationService.class);
    when(mockApplicationService.getApplication(TEST_APP_NAME)).thenReturn(null);
    when(mockApplicationService.getApplicationStatus(null)).thenReturn(testStatus);

    StatusApplicationCommand statusApplicationCommand = new StatusApplicationCommand();
    statusApplicationCommand.appName = TEST_APP_NAME;
    statusApplicationCommand.doExecute(mockApplicationService);

    verify(mockApplicationService, never()).getApplicationStatus(null);
  }
}
