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
package org.codice.ddf.admin.application.service;

import java.util.List;
import java.util.Set;
import org.apache.karaf.features.Feature;
import org.codice.ddf.admin.application.rest.model.FeatureDetails;

/**
 * @deprecated going away in a future release. Service that keeps track and obtains status for
 *     applications running in the system.
 */
@Deprecated
public interface ApplicationService {

  /**
   * Retrieve a set of applications are currently installed in the system.
   *
   * @return Set of applications.
   */
  @Deprecated
  Set<Application> getApplications();

  /**
   * Returns the application that has the given name
   *
   * @param applicationName Name of the application to retrieve. Is case-insensitive.
   * @return The application that matches the name or null if no application matched.
   */
  @Deprecated
  Application getApplication(String applicationName);

  /**
   * Gets the application Profile features on the system.
   *
   * @return the installation profiles.
   */
  @Deprecated
  List<Feature> getInstallationProfiles();

  /**
   * Returns List of FeatureDtos with repository and status information
   *
   * @return
   */
  @Deprecated
  List<FeatureDetails> getAllFeatures();
}
