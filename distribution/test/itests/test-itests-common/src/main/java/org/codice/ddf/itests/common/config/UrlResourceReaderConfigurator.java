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

  private ConfigurationAdmin configAdmin;

  public UrlResourceReaderConfigurator(ConfigurationAdmin configAdmin) {
    this.configAdmin = configAdmin;
  }

  public void setUrlResourceReaderRootDirs(String... rootResourceDirs) throws IOException {
    Configuration configuration = configAdmin.getConfiguration(PID, null);
    Dictionary<String, Object> properties = new DictionaryMap<>();
    Set<String> rootResourceDirectories =
        ImmutableSet.<String>builder().add(rootResourceDirs).build();
    properties.put("rootResourceDirectories", rootResourceDirectories);
    configuration.update(properties);
    for (int i = 0; i < 5; i++) {
      Configuration updatedConfig = configAdmin.getConfiguration(PID, null);
      if (updatedConfig
          .getProperties()
          .get("rootResourceDirectories")
          .equals(rootResourceDirectories)) {
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
    LOGGER.info("URLResourceReader props after update: {}", configuration.getProperties());
  }
}
