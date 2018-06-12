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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.with;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.Dictionary;
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
    Collection<String> newRootResourceDirs =
        ImmutableSet.<String>builder().add(rootResourceDirs).build();
    Configuration configuration = configAdmin.getConfiguration(PID, null);
    Collection<String> currentRootResourceDirs = getCurrentRootResourceDirs(configuration);

    if (currentRootResourceDirs != null) {

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

    updateUrlResourceReaderRootDirs(configuration, newRootResourceDirs);
  }

  @SuppressWarnings("unchecked")
  private Collection<String> getCurrentRootResourceDirs(Configuration configuration) {
    Dictionary<String, Object> properties = configuration.getProperties();

    if (properties == null) {
      return null;
    }

    Object currentRootResourceDirs = properties.get(ROOT_RESOURCE_DIRECTORIES_PROPERTY_KEY);

    if (!(currentRootResourceDirs instanceof Collection)) {
      return null;
    }

    return (Collection<String>) currentRootResourceDirs;
  }

  private void updateUrlResourceReaderRootDirs(
      Configuration configuration, Collection<String> newRootResourceDirs) {
    Dictionary<String, Object> properties = new DictionaryMap<>();
    properties.put(ROOT_RESOURCE_DIRECTORIES_PROPERTY_KEY, newRootResourceDirs);

    try {
      configuration.update(properties);
    } catch (IOException e) {
      throw new UncheckedIOException(
          String.format(
              "Unexpected failure updating [%s %s] configuration!",
              PID, ROOT_RESOURCE_DIRECTORIES_PROPERTY_KEY),
          e);
    }

    with()
        .pollInterval(1, SECONDS)
        .await()
        .atMost(30, SECONDS)
        .until(() -> propertyIsUpdated(configuration, newRootResourceDirs));

    LOGGER.debug("{} properties after update: {}", PID, configuration.getProperties());
  }

  private boolean propertyIsUpdated(
      Configuration configuration, Collection<String> expectedRootResourceDirs) {

    return rootResourceDirsEqual(
        getCurrentRootResourceDirs(configuration), expectedRootResourceDirs);
  }

  private boolean rootResourceDirsEqual(
      Collection<String> currentRootResourceDirs, Collection<String> newRootResourceDirs) {
    return (currentRootResourceDirs != null)
        && (currentRootResourceDirs.size() == newRootResourceDirs.size())
        && newRootResourceDirs.containsAll(currentRootResourceDirs);
  }
}
