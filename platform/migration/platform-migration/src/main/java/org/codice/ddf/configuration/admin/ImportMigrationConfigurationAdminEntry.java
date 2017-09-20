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
package org.codice.ddf.configuration.admin;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Objects;
import javax.annotation.Nullable;
import org.apache.commons.lang.Validate;
import org.codice.ddf.migration.ImportMigrationEntry;
import org.codice.ddf.migration.MigrationException;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class extends on the {@link ImportMigrationEntry} interface to represent an exported
 * configuration object.
 */
public class ImportMigrationConfigurationAdminEntry {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(ImportMigrationConfigurationAdminEntry.class);

  private final ImportMigrationEntry entry;

  private final ConfigurationAdmin configurationAdmin;

  private final Dictionary<String, Object> properties;

  @Nullable private final String factoryPid;

  private final String pid;

  @Nullable private final Configuration memoryConfiguration;

  private boolean restored = false;

  public ImportMigrationConfigurationAdminEntry(
      ConfigurationAdmin configurationAdmin,
      ImportMigrationEntry entry,
      @Nullable String factoryPid,
      String pid,
      Dictionary<String, Object> properties,
      @Nullable Configuration memoryConfiguration) {
    Validate.notNull(entry, "invalid null entry");
    Validate.notNull(configurationAdmin, "invalid null configuration admin");
    Validate.notNull(properties, "invalid null properties");
    this.entry = entry;
    this.configurationAdmin = configurationAdmin;
    this.properties = properties;
    this.factoryPid = factoryPid;
    this.pid = pid;
    this.memoryConfiguration = memoryConfiguration;
  }

  public boolean restore() {
    if (!restored) {
      final Configuration cfg;

      if (memoryConfiguration != null) {
        if (propertiesMatch()) {
          LOGGER.debug(
              "Importing configuration for [{}] from [{}]; no update required",
              memoryConfiguration.getPid(),
              entry.getPath());
          this.restored = true;
          return true;
        }
        cfg = memoryConfiguration;
        LOGGER.debug(
            "Importing configuration for [{}] from [{}]; updating existing configuration...",
            memoryConfiguration.getPid(),
            entry.getPath());
      } else {
        logCreatingConfig();
        try {
          cfg = createConfiguration();
        } catch (IOException e) {
          reportCreatingConfigError(e);
          return false;
        }
        LOGGER.debug(
            "Importing configuration for [{}] from [{}]; initializing configuration...",
            cfg.getPid(),
            entry.getPath());
      }
      try {
        cfg.update(properties);
        this.restored = true;
      } catch (IOException e) {
        entry
            .getReport()
            .record(
                new MigrationException(
                    "Import error: failed to update configuration [%s] with pid [%s]; %s.",
                    entry.getPath(), getPid(), e));
      }
    }
    return restored;
  }

  public String getFactoryPid() {
    return factoryPid;
  }

  public String getPid() {
    return pid;
  }

  public boolean isManagedServiceFactory() {
    return factoryPid != null;
  }

  public boolean isManagedService() {
    return factoryPid == null;
  }

  private Configuration createConfiguration() throws IOException {
    // Question: should we use the bundle location that was exported???
    // If we do, should we perform additional checks to make sure we're not loading a malicious
    // bundle?
    // This might be unnecessary if we are comfortable with the encryption of the zip file as our
    // only countermeasure.
    if (isManagedServiceFactory()) {
      return configurationAdmin.createFactoryConfiguration(factoryPid, null);
    }
    return configurationAdmin.getConfiguration(pid);
  }

  private boolean propertiesMatch() {
    final Dictionary<String, Object> props = memoryConfiguration.getProperties();

    if (props == null) {
      return false;
    }
    // remove factory pid and pid from the dictionary as we do not want to match these
    props.remove(ConfigurationAdmin.SERVICE_FACTORYPID);
    props.remove(Constants.SERVICE_PID);
    if (properties.size() != props.size()) {
      return false;
    }
    // @formatter:off - to shut up checkstyle!!!!!!!
    for (final Enumeration<String> e = properties.keys(); e.hasMoreElements(); ) {
      // @formatter:on
      final String key = e.nextElement();

      if (!Objects.deepEquals(properties.get(key), props.get(key))) {
        return false;
      }
    }
    return true;
  }

  private void logCreatingConfig() {
    if (LOGGER.isDebugEnabled()) {
      if (isManagedServiceFactory()) {
        LOGGER.debug(
            "Importing configuration for [{}-?] from [{}]; creating new factory configuration...",
            factoryPid,
            entry.getPath());
      } else {
        LOGGER.debug(
            "Importing configuration for [{}] from [{}]; creating new configuration...",
            pid,
            entry.getPath());
      }
    }
  }

  private void reportCreatingConfigError(IOException e) {
    if (isManagedServiceFactory()) {
      entry
          .getReport()
          .record(
              new MigrationException(
                  "Import error: failed to create factory configuration [%s] with factory pid [%s]; %s.",
                  entry.getPath(), factoryPid, e));
    } else {
      entry
          .getReport()
          .record(
              new MigrationException(
                  "Import error: failed to create configuration [%s] with pid [%s]; %s.",
                  entry.getPath(), pid, e));
    }
  }
}
