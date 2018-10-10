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

import java.util.List;
import java.util.Map;

/**
 * @deprecated going away in a future release. Interface for the Application Service MBean. Allows
 *     exposing the application service out via a JMX MBean interface.
 */
@Deprecated
public interface ApplicationServiceBeanMBean {

  /**
   * Installs the specified feature
   *
   * @param feature name to install.
   */
  @Deprecated
  void installFeature(String feature);

  /**
   * Uninstalls the specified feature
   *
   * @param feature name to install.
   */
  @Deprecated
  void uninstallFeature(String feature);

  /**
   * Creates an application list that has two attributes that describes relationships between
   * applications (parent and children dependencies).
   *
   * @return A list of the root applications expressed as maps.
   */
  @Deprecated
  List<Map<String, Object>> getApplications();

  /**
   * Gets all installation profiles on the system.
   *
   * @return installation profile objects.
   */
  @Deprecated
  List<Map<String, Object>> getInstallationProfiles();

  /**
   * TODO:
   *
   * @param applicationID
   * @return
   */
  @Deprecated
  List<Map<String, Object>> getServices(String applicationID);

  /**
   * Returns all Features in DDF
   *
   * @return
   */
  @Deprecated
  List<Map<String, Object>> getAllFeatures();

  /**
   * Returns the the json for the plugins based on the app name.
   *
   * @param appName - what we want the plugins for.
   * @return a mapping that will be converted to json.
   */
  @Deprecated
  List<Map<String, Object>> getPluginsForApplication(String appName);

  /** Triggers a restart of the system. */
  @Deprecated
  void restart();
}
