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
package org.codice.ddf.itests.common.config;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.codice.ddf.configuration.DictionaryMap;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UrlResourceReaderConfigurator {

  private static final Logger LOGGER = LoggerFactory.getLogger(UrlResourceReaderConfigurator.class);

  private static final String PID = "ddf.catalog.resource.impl.URLResourceReader";

  private static final String ROOT_RESOURCE_DIRECTORIES_PROPERTY_KEY = "rootResourceDirectories";

  private ConfigurationAdmin configAdmin;

  public UrlResourceReaderConfigurator(ConfigurationAdmin configAdmin) {
    this.configAdmin = configAdmin;
  }

  public void setUrlResourceReaderRootDirs(String... rootResourceDirs) throws IOException {
    Set<String> newRootResourceDirs = ImmutableSet.<String>builder().add(rootResourceDirs).build();

    Configuration configuration = configAdmin.getConfiguration(PID, null);

    if (configuration == null) {
      LOGGER.warn(
          "{} configuration was null, cannot update {}",
          PID,
          ROOT_RESOURCE_DIRECTORIES_PROPERTY_KEY);
      return;
    }

    if (configuration.getProperties() != null) {
      Set<String> currentRootResourceDirs =
          ImmutableSet.copyOf(
              (Collection<String>)
                  configuration.getProperties().get(ROOT_RESOURCE_DIRECTORIES_PROPERTY_KEY));

      LOGGER.debug(
          "{} {}, current value: {}, new value: {}",
          PID,
          ROOT_RESOURCE_DIRECTORIES_PROPERTY_KEY,
          currentRootResourceDirs,
          newRootResourceDirs);

      if (rootResourceDirsEqual(currentRootResourceDirs, newRootResourceDirs)) {
        LOGGER.debug(
            "{} {} unchanged, skipping update", PID, ROOT_RESOURCE_DIRECTORIES_PROPERTY_KEY);
        return;
      }
    }

    Dictionary<String, Object> properties = new DictionaryMap<>();
    properties.put(ROOT_RESOURCE_DIRECTORIES_PROPERTY_KEY, newRootResourceDirs);
    configuration.update(properties);

    for (int i = 0; i < 5; i++) {
      Configuration updatedConfig = configAdmin.getConfiguration(PID, null);

      if (updatedConfig
          .getProperties()
          .get(ROOT_RESOURCE_DIRECTORIES_PROPERTY_KEY)
          .equals(newRootResourceDirs)) {
        break;
      }

      boolean interrupted = false;

      try {
        TimeUnit.SECONDS.sleep(5);
      } catch (InterruptedException e) {
        interrupted = true;
      } finally {
        if (interrupted) {
          Thread.currentThread().interrupt();
        }
      }
    }

    LOGGER.debug("{} properties after update: {}", PID, configuration.getProperties());
  }

  private boolean rootResourceDirsEqual(
      Set<String> currentRootResourceDirs, Set<String> newRootResourceDirs) {
    return (currentRootResourceDirs.size() == newRootResourceDirs.size())
        && newRootResourceDirs.containsAll(currentRootResourceDirs);
  }
}
