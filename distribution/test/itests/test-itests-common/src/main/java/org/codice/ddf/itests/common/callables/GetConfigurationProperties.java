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
package org.codice.ddf.itests.common.callables;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link Callable} that retrieves the properties of a {@link Configuration} object. The search is
 * done by looking for the {@link Configuration} object that has a property set to a specific value.
 * If multiple objects match the criteria, an {@link IllegalArgumentException} will be thrown. For
 * that reason, care should be taken to use a property name whose value is known to be unique.
 */
public class GetConfigurationProperties implements Callable<Dictionary<String, Object>> {
  private static final Logger LOGGER = LoggerFactory.getLogger(GetConfigurationProperties.class);

  private ConfigurationAdmin configAdmin;

  private String propertyName;

  private String propertyValue;

  /**
   * Constructor.
   *
   * @param configAdmin reference to the container's {@link ConfigurationAdmin}
   * @param propertyName name of the property to use for the search
   * @param propertyValue property value to search for
   */
  public GetConfigurationProperties(
      ConfigurationAdmin configAdmin, String propertyName, String propertyValue) {
    this.configAdmin = configAdmin;
    this.propertyName = propertyName;
    this.propertyValue = propertyValue;
  }

  /**
   * Retrieves the {@link Configuration} object's properties.
   *
   * @return {@link Configuration} object's properties. {@code null} if the {@link Configuration}
   *     object does not exist or has no properties.
   * @throws IllegalArgumentException thrown if multiple {@link Configuration} objects match the
   *     search criteria, i.e., have the same property name/value pair
   * @throws Exception thrown for any other reasons
   */
  @Override
  public Dictionary<String, Object> call() throws Exception {

    String query = String.format("(%s=%s)", propertyName, propertyValue);
    Configuration[] configurations = configAdmin.listConfigurations(query);

    if (configurations == null) {
      return null;
    }

    // multiple configurations are returned because there are multiple persistence managers
    Set<Configuration> configurationSet = new HashSet<>(Arrays.asList(configurations));

    if (configurationSet.isEmpty()) {
      return null;
    }

    if (configurationSet.size() > 1) {
      LOGGER.error("Multiple Configuration objects returned for query {}", query);
      throw new IllegalArgumentException("Property name/value pair isn't unique");
    }

    Configuration configuration = configurations[0];

    return configuration.getProperties();
  }
}
