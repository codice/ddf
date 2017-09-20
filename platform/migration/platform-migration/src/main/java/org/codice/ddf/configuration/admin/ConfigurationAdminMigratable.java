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
import org.apache.commons.lang.Validate;
import org.codice.ddf.configuration.persistence.PersistenceStrategy;
import org.codice.ddf.configuration.persistence.felix.FelixCfgPersistenceStrategy;
import org.codice.ddf.configuration.persistence.felix.FelixConfigPersistenceStrategy;
import org.codice.ddf.migration.ExportMigrationContext;
import org.codice.ddf.migration.ImportMigrationContext;
import org.codice.ddf.migration.Migratable;
import org.codice.ddf.migration.MigrationContext;
import org.codice.ddf.migration.MigrationException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class used to migrate {@link ConfigurationAdmin} configurations. This includes importing
 * configuration files from a configuration directory and creating {@link Configuration} objects for
 * those and exporting {@link Configuration} objects to configuration files.
 */
public class ConfigurationAdminMigratable implements Migratable {
  /**
   * Holds the current export version.
   *
   * <p>1.0 - initial version
   */
  private static final String CURRENT_VERSION = "1.0";

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationAdminMigratable.class);

  private final ConfigurationAdmin configurationAdmin;

  private final PersistenceStrategy defaultStrategy;

  public ConfigurationAdminMigratable(
      ConfigurationAdmin configurationAdmin, String defaultFileExtension) {
    Validate.notNull(configurationAdmin, "invalid null config admin");
    Validate.notNull(defaultFileExtension, "invalid null default file extension");
    this.configurationAdmin = configurationAdmin;
    this.defaultStrategy = getPersister(defaultFileExtension);
    Validate.notNull(
        defaultStrategy, "unknown persistence strategy extension: " + defaultFileExtension);
  }

  // PMD.DefaultPackage - designed to be called from ExportMigrationConfigurationAdminContext and
  // ImportMigrationConfigurationAdminContext in this package
  @SuppressWarnings("PMD.DefaultPackage")
  static boolean isManagedServiceFactory(Configuration cfg) {
    return cfg.getFactoryPid() != null;
  }

  // PMD.DefaultPackage - designed to be called from ExportMigrationConfigurationAdminContext and
  // ImportMigrationConfigurationAdminContext in this package
  @SuppressWarnings("PMD.DefaultPackage")
  static boolean isManagedService(Configuration cfg) {
    return cfg.getFactoryPid() == null;
  }

  @Override
  public String getVersion() {
    return ConfigurationAdminMigratable.CURRENT_VERSION;
  }

  @Override
  public String getId() {
    return "config.admin";
  }

  @Override
  public String getTitle() {
    return "Configuration Admin Migratable";
  }

  @Override
  public String getDescription() {
    return "Exports Configuration Admin";
  }

  @Override
  public String getOrganization() {
    return "Codice";
  }

  @Override
  public void doExport(ExportMigrationContext context) {
    final ExportMigrationConfigurationAdminContext adminContext =
        new ExportMigrationConfigurationAdminContext(context, this, getConfigurations(context));

    adminContext.entries().forEach(ExportMigrationConfigurationAdminEntry::store);
  }

  @Override
  public void doImport(ImportMigrationContext context) {
    final ImportMigrationConfigurationAdminContext adminContext =
        new ImportMigrationConfigurationAdminContext(
            context, this, configurationAdmin, getConfigurations(context));

    adminContext.entries().forEach(ImportMigrationConfigurationAdminEntry::restore);
  }

  // PMD.DefaultPackage - designed to be called from ExportMigrationConfigurationAdminContext in
  // this package
  @SuppressWarnings("PMD.DefaultPackage")
  PersistenceStrategy getDefaultPersister() {
    return defaultStrategy;
  }

  // PMD.DefaultPackage - designed to be called from ExportMigrationConfigurationAdminContext and
  // ImportMigrationConfigurationAdminContext in this package
  @SuppressWarnings("PMD.DefaultPackage")
  PersistenceStrategy getPersister(String extension) {
    if ("cfg".equals(extension)) {
      return new FelixCfgPersistenceStrategy();
    } else if ("config".equals(extension)) {
      return new FelixConfigPersistenceStrategy();
    }
    return null;
  }

  private Configuration[] getConfigurations(MigrationContext context) {
    try {
      final Configuration[] configurations = configurationAdmin.listConfigurations(null);

      if (configurations != null) {
        return configurations;
      }
    } catch (IOException
        | InvalidSyntaxException
            e) { // InvalidSyntaxException should never happen since the filter is null
      String message =
          String.format(
              "There was an issue retrieving configurations from ConfigurationAdmin: %s",
              e.getMessage());

      LOGGER.info(message);
      context.getReport().record(new MigrationException(message, e));
    }
    return new Configuration[0];
  }
}
