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
package org.codice.ddf.admin.core.api.jmx;

import java.util.Map;

/**
 * OSGI service interface. Objects that provide this service are called by the SystemPropertiesAdmin
 * object just before it saves system property changes to disk. This lets objects in other modules a
 * chance to make their own changes to system properties during the UI installation process -- the
 * SystemPropertiesAdmin object is not invoked as part of a headless installation.
 *
 * <p>This service interface was created because the SystemPropertiesAdmin would overwrite changes
 * to the system properties made by other objects during the installation process. This happens
 * because the SystemPropertiesAdmin object reads the entire system properties into memory, makes
 * alterations, then saves all of them to disk. Any other object that changes system properties
 * risks having its changes overwritten on disk. Unfortunately, there is no reasonable way to
 * synchronize access to the system properties file across bundles and threads. Creating this
 * service interface and invoking its providers from the SystemPropertiesAdmin object was the most
 * reasonable way to give multiple objects a chance to update the system properties during
 * installation.
 */
public interface SystemPropertiesAdminInterceptor {

  /**
   * This method will read and potentially mutate the input object. It does not persist the changes
   * to a file or copy any changes to the System's Properties object. That is the responsibility of
   * the caller.
   *
   * @param properties is typically an instance of org.apache.felix.utils.properties.Properties
   *     which extends AbstractMap
   */
  void updateSystemProperties(Map<String, String> properties);
}
