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

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.apache.karaf.bundle.core.BundleService;
import org.apache.karaf.bundle.core.internal.BundleServiceImpl;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.internal.service.FeaturesServiceImpl;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.codice.ddf.admin.application.service.impl.ApplicationServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.resolver.ResolutionException;

public class AbstractProfileCommandTest {

  private ApplicationService applicationService;
  private FeaturesService featuresService;
  private BundleService bundleService;

  private AbstractProfileCommand abstractProfileCommand;

  @Before
  public void setUp() {
    this.applicationService = mock(ApplicationServiceImpl.class);
    this.featuresService = mock(FeaturesServiceImpl.class);
    this.bundleService = mock(BundleServiceImpl.class);

    this.abstractProfileCommand = getCommand(applicationService, featuresService, bundleService);
  }

  @Test(expected = ResolutionException.class)
  public void testFeatureServiceFailure() throws Exception {
    doThrow(ResolutionException.class).when(featuresService).installFeature(anyString());
    abstractProfileCommand.execute();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBundleServiceFailure() throws Exception {
    doThrow(IllegalArgumentException.class).when(bundleService).getBundle(anyString());
    abstractProfileCommand.execute();
  }

  @Test
  public void testWorkingServices() throws Exception {
    abstractProfileCommand.execute();
    verify(applicationService).getApplication(eq("test-app"));
    verify(featuresService).installFeature(eq("test-feature"));
    verify(bundleService).getBundle("test-bundle");
  }

  private AbstractProfileCommand getCommand(
      ApplicationService applicationService,
      FeaturesService featuresService,
      BundleService bundleService) {
    AbstractProfileCommand command =
        new AbstractProfileCommand() {

          @Override
          protected void doExecute(
              ApplicationService applicationService,
              FeaturesService featuresService,
              BundleService bundleService)
              throws Exception {

            applicationService.getApplication("test-app");
            featuresService.installFeature("test-feature");
            bundleService.getBundle("test-bundle");
          }
        };

    command.applicationService = applicationService;
    command.featuresService = featuresService;
    command.bundleService = bundleService;

    return command;
  }
}
