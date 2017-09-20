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
 * Interface for the Application Service MBean. Allows exposing the application service out via a
 * JMX MBean interface.
 */
public interface ApplicationServiceBeanMBean {

  /**
   * Creates an application hierarchy tree that shows relationships between applications.
   *
   * @return A list of the root applications expressed as maps.
   */
  List<Map<String, Object>> getApplicationTree();

  /**
   * Creates an application list that has two attributes that describes relationships between
   * applications (parent and children dependencies).
   *
   * @return A list of the root applications expressed as maps.
   */
  List<Map<String, Object>> getApplications();

  /**
   * Starts an application with the given name.
   *
   * @param appName Name of the application to start.
   * @return true if the application was successfully started, false if not.
   */
  boolean startApplication(String appName);

  /**
   * Stops an application with the given name.
   *
   * @param appName Name of the application to stop.
   * @return true if the application was successfully stopped, false if not.
   */
  boolean stopApplication(String appName);

  /**
   * Adds a list of applications that are specified by their URL.
   *
   * @param applicationURLList
   */
  void addApplications(List<Map<String, Object>> applicationURLList);

  /**
   * Removes an application that is specified by its URL.
   *
   * @param applicationURLList
   */
  void removeApplication(String applicationURL);

  /**
   * Gets all installation profiles on the system.
   *
   * @return installation profile objects.
   */
  List<Map<String, Object>> getInstallationProfiles();

  /**
   * TODO:
   *
   * @param applicationID
   * @return
   */
  List<Map<String, Object>> getServices(String applicationID);

  /**
   * Returns all Features in DDF
   *
   * @return
   */
  List<Map<String, Object>> getAllFeatures();

  /**
   * Returns all Features in DDF by Application
   *
   * @return
   */
  List<Map<String, Object>> findApplicationFeatures(String applicationName);

  /**
   * Returns the the json for the plugins based on the app name.
   *
   * @param appName - what we want the plugins for.
   * @return a mapping that will be converted to json.
   */
  List<Map<String, Object>> getPluginsForApplication(String appName);
}
