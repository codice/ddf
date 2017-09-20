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
package org.codice.ddf.admin.application.service.impl;

import java.util.Collections;
import java.util.Set;
import org.apache.karaf.features.Feature;
import org.codice.ddf.admin.application.service.Application;
import org.codice.ddf.admin.application.service.ApplicationStatus;
import org.osgi.framework.Bundle;

/**
 * Implementation of ApplicationStatus. Exposes information that was passed in through the
 * constructor.
 */
public class ApplicationStatusImpl implements ApplicationStatus {

  private Application application = null;

  private ApplicationState installState = null;

  private Set<Feature> errorFeatures = null;

  private Set<Bundle> errorBundles = null;

  /**
   * Creates a new instance of application status.
   *
   * @param application The application this status is for.
   * @param installState The state of the application.
   * @param errorFeatures Set of features that are in an error state.
   * @param errorBundles Set of bundles that are in an error state.
   */
  public ApplicationStatusImpl(
      Application application,
      ApplicationState installState,
      Set<Feature> errorFeatures,
      Set<Bundle> errorBundles) {
    this.application = application;
    this.installState = installState;
    this.errorFeatures = errorFeatures;
    this.errorBundles = errorBundles;
  }

  @Override
  public Application getApplication() {
    return application;
  }

  @Override
  public ApplicationState getState() {
    return installState;
  }

  @Override
  public Set<Feature> getErrorFeatures() {
    return Collections.unmodifiableSet(errorFeatures);
  }

  @Override
  public Set<Bundle> getErrorBundles() {
    return Collections.unmodifiableSet(errorBundles);
  }
}
