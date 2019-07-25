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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.codice.ddf.admin.core.api.ConfigurationStatus;
import org.codice.ddf.admin.core.api.Service;

/**
 * MBean interface for Admin Console back end.
 *
 * <p><b> This code is experimental. While this interface is functional and tested, it may change or
 * be removed in a future version of the library. </b>
 */
public interface AdminConsoleServiceMBean {

  String OBJECT_NAME = "org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0";

  /**
   * Lists all managed services and managed service factories with associated metatypes and
   * configurations
   *
   * @return the list of all Services
   */
  List<Service> listServices();

  /**
   * Returns a managed service or managed service factory with associated metatype and
   * configurations
   *
   * <p>*@param filter the string representation of the <code>org.osgi.framework.Filter</code>
   *
   * @return Service
   */
  Service getService(String filter);

  /**
   * Lists all modules that are available
   *
   * @return the list of all Modules
   */
  List<Map<String, Object>> listModules();

  /**
   * Create a new configuration instance for the supplied persistent id of the factory, answering
   * the PID of the created configuration
   *
   * @param factoryPid the persistent id of the factory
   * @return the PID of the created configuration
   * @throws java.io.IOException if the operation failed
   */
  String createFactoryConfiguration(String factoryPid) throws IOException;

  /**
   * Create a factory configuration for the supplied persistent id of the factory and the bundle
   * location bound to bind the created configuration to, answering the PID of the created
   * configuration
   *
   * @param factoryPid the persistent id of the factory
   * @param location the bundle location
   * @return the pid of the created configuation
   * @throws IOException if the operation failed
   */
  String createFactoryConfigurationForLocation(String factoryPid, String location)
      throws IOException;

  /**
   * Delete the configuration
   *
   * @param pid the persistent identifier of the configuration
   * @throws IOException if the operation fails
   */
  void delete(String pid) throws IOException;

  /**
   * Delete the configuration
   *
   * @param pid the persistent identifier of the configuration
   * @param location the bundle location
   * @throws IOException if the operation fails
   */
  void deleteForLocation(String pid, String location) throws IOException;

  /**
   * Delete the configurations matching the filter specification.
   *
   * @param filter the string representation of the <code>org.osgi.framework.Filter</code>
   * @throws IOException if the operation failed
   * @throws IllegalArgumentException if the filter is invalid
   */
  void deleteConfigurations(String filter) throws IOException;

  /**
   * Answer the bundle location the configuration is bound to
   *
   * @param pid the persistent identifier of the configuration
   * @return the bundle location
   * @throws IOException if the operation fails
   */
  String getBundleLocation(String pid) throws IOException;

  /**
   * Answer the factory PID if the configuration is a factory configuration, null otherwise.
   *
   * @param pid the persistent identifier of the configuration
   * @return the factory PID
   * @throws IOException if the operation fails
   */
  String getFactoryPid(String pid) throws IOException;

  /**
   * Answer the factory PID if the configuration is a factory configuration, null otherwise.
   *
   * @param pid the persistent identifier of the configuration
   * @param location the bundle location
   * @return the factory PID
   * @throws IOException if the operation fails
   */
  String getFactoryPidForLocation(String pid, String location) throws IOException;

  /**
   * Answer the contents of the configuration
   *
   * <p>
   *
   * @param pid the persistent identifier of the configuration
   * @return the table of contents
   * @throws IOException if the operation fails
   * @see org.osgi.jmx.JmxConstants#PROPERTIES_TYPE for the details of the TabularType
   */
  Map<String, Object> getProperties(String pid) throws IOException;

  /**
   * Answer the contents of the configuration
   *
   * <p>
   *
   * @param pid the persistent identifier of the configuration
   * @param location the bundle location
   * @return the table of contents
   * @throws IOException if the operation fails
   * @see org.osgi.jmx.JmxConstants#PROPERTIES_TYPE for the details of the TabularType
   */
  Map<String, Object> getPropertiesForLocation(String pid, String location) throws IOException;

  /**
   * Answer the list of PID/Location pairs of the configurations managed by this service
   *
   * @param filter the string representation of the <code>org.osgi.framework.Filter</code>
   * @return the list of configuration PID/Location pairs
   * @throws IOException if the operation failed
   * @throws IllegalArgumentException if the filter is invalid
   */
  String[][] getConfigurations(String filter) throws IOException;

  /**
   * Set the bundle location the configuration is bound to
   *
   * @param pid the persistent identifier of the configuration
   * @param location the bundle location
   * @throws IOException if the operation fails
   */
  void setBundleLocation(String pid, String location) throws IOException;

  /**
   * Update the configuration with the supplied properties For each property entry, the following
   * row is supplied
   *
   * <p>It is necessary to have a return value so that the ajax call to Jolokia waits for it to
   * finish and we get any errors returned
   *
   * <p>
   *
   * @param pid the persistent identifier of the configuration
   * @param configurationTable the table of properties
   * @throws IOException if the operation fails
   * @see org.osgi.jmx.JmxConstants#PROPERTIES_TYPE for the details of the TabularType
   */
  boolean update(String pid, Map<String, Object> configurationTable) throws IOException;

  /**
   * Interceptor method for special handling of guest claims profile data.
   *
   * @param pid the persistent identifier of the configuration
   * @param configurationTable the table of properties
   * @throws IOException if the operation fails {@link #update(String, Map)}.
   * @see #update(String, Map) for the generic version of this method; this method acts as an
   *     interceptor to handle the special case where processing specific to guest claims occurs
   *     before passing the configuration data to {@link #update(String, Map)} for persistence in
   *     OSGi's configuration admin.
   */
  boolean updateGuestClaimsProfile(String pid, Map<String, Object> configurationTable)
      throws IOException;

  /**
   * Update the configuration with the supplied properties For each property entry, the following
   * row is supplied
   *
   * <p>
   *
   * @param pid the persistent identifier of the configuration
   * @param location the bundle location
   * @param configurationTable the table of properties
   * @throws IOException if the operation fails
   * @see org.osgi.jmx.JmxConstants#PROPERTIES_TYPE for the details of the TabularType
   */
  void updateForLocation(String pid, String location, Map<String, Object> configurationTable)
      throws IOException;

  /**
   * Enables a previously disabled configuration
   *
   * @param servicePid that uniquely identifies the source to enable.
   * @return the status of the configuration
   * @throws IOException if the Source to be enabled cannot be located via the provided service PID.
   */
  ConfigurationStatus enableConfiguration(String servicePid) throws IOException;

  /**
   * Disables a configuration but preserves the configuration to be enabled at a later time.
   *
   * @param servicePid that uniquely identifies the source to disable.
   * @return the status of the configuration
   * @throws IOException if the Source to be disabled cannot be located via the provided service
   *     PID.
   */
  ConfigurationStatus disableConfiguration(String servicePid) throws IOException;

  Map<String, Object> getClaimsConfiguration(String filter);

  Map<String, Object>[] getSsoConfigurations();

  void setSsoConfigurations(Map<String, Object>[] config) throws IOException;
}
