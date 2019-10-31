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

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.codice.ddf.migration.ExportMigrationContext;
import org.codice.ddf.migration.ExportMigrationEntry;
import org.codice.ddf.migration.ImportMigrationContext;
import org.codice.ddf.migration.Migratable;
import org.codice.ddf.migration.MigrationContext;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.platform.io.internal.PersistenceStrategy;
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

  private static final String ATTRIBUTE_MAP = "attributeMap";

  private static final String DDF_HOSTNAME_PROP_KEY = "org.codice.ddf.system.hostname";

  private static final String METACARD_VALIDITY_FILTER_PLUGIN_PID =
      "ddf.catalog.metacard.validation.MetacardValidityFilterPlugin";

  @VisibleForTesting
  static final List<String> ACCEPTED_ENTRY_PIDS =
      Arrays.asList(
          "ddf.catalog.resource.impl.URLResourceReader",
          "org.codice.ddf.security.policy.context.impl.PolicyManager",
          "ddf.security.sts.guestclaims",
          "ddf.security.sts",
          "ddf.security.http.impl.HttpSessionFactory",
          "org.codice.ddf.security.filter.login.Session",
          "org.codice.ddf.registry.federationadmin.service.impl.RefreshRegistryEntries",
          "org.codice.ddf.registry.policy.RegistryPolicyPlugin",
          "Registry_Configuration_Event_Handler",
          "org.codice.ddf.registry.api.impl.RegistryStoreCleanupHandler",
          "ddf.catalog.federation.impl.CachingFederationStrategy",
          METACARD_VALIDITY_FILTER_PLUGIN_PID,
          "ddf.catalog.metacard.validation.MetacardValidityMarkerPlugin",
          "ddf.catalog.CatalogFrameworkImpl");

  private static final List<String> ACCEPTED_ENTRY_FACTORY_PIDS =
      Arrays.asList(
          "org.codice.ddf.catalog.content.monitor.ContentDirectoryMonitor", "Csw_Registry_Store");

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationAdminMigratable.class);

  private final ConfigurationAdmin configurationAdmin;

  private final List<PersistenceStrategy> strategies;

  private final String defaultFileExtension;

  public ConfigurationAdminMigratable(
      ConfigurationAdmin configurationAdmin,
      List<PersistenceStrategy> strategies,
      String defaultFileExtension) {
    Validate.notNull(configurationAdmin, "invalid null config admin");
    Validate.notNull(defaultFileExtension, "invalid null default file extension");
    this.configurationAdmin = configurationAdmin;
    this.strategies = strategies;
    this.defaultFileExtension = defaultFileExtension;
  }

  @SuppressWarnings(
      "PMD.DefaultPackage" /* designed to be called from ExportMigrationConfigurationAdminContext and ImportMigrationConfigurationAdminContext in this package */)
  static boolean isManagedServiceFactory(Configuration cfg) {
    return cfg.getFactoryPid() != null;
  }

  @SuppressWarnings(
      "PMD.DefaultPackage" /* designed to be called from ExportMigrationConfigurationAdminContext and ImportMigrationConfigurationAdminContext in this package */)
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
    return "Exports Configuration objects and files";
  }

  @Override
  public String getOrganization() {
    return "Codice";
  }

  @Override
  public void doExport(ExportMigrationContext context) {
    final ExportMigrationConfigurationAdminContext adminContext =
        new ExportMigrationConfigurationAdminContext(
            context, defaultFileExtension, this, getConfigurations(context));

    adminContext.fileEntries().forEach(ExportMigrationEntry::store);
    adminContext.memoryEntries().forEach(ExportMigrationConfigurationAdminEntry::store);
  }

  @Override
  public void doImport(ImportMigrationContext context) {
    final ImportMigrationConfigurationAdminContext adminContext =
        new ImportMigrationConfigurationAdminContext(
            context, this, configurationAdmin, getConfigurations(context), true);

    adminContext.memoryEntries().forEach(ImportMigrationConfigurationAdminEntry::restore);
    context
        .getReport()
        .doAfterCompletion(adminContext::deleteUnexportedConfigurationsAfterCompletion);
  }

  @Override
  public void doVersionUpgradeImport(ImportMigrationContext context) {
    final ImportMigrationConfigurationAdminContext adminContext =
        new ImportMigrationConfigurationAdminContext(
            context, this, configurationAdmin, getConfigurations(context), false);

    adminContext
        .memoryEntries()
        .filter(this::isAcceptedConfigurationAdminEntry)
        .map(this::updateNecessaryConfigurationAdminEntries)
        .forEach(ImportMigrationConfigurationAdminEntry::restore);
  }

  private boolean isAcceptedConfigurationAdminEntry(ImportMigrationConfigurationAdminEntry entry) {
    return (ACCEPTED_ENTRY_PIDS.contains(entry.getPid())
        || ACCEPTED_ENTRY_FACTORY_PIDS.contains(entry.getFactoryPid())
        || isASource(entry.getFactoryPid()));
  }

  private boolean isASource(String factoryPid) {
    // this is generic in order to get downstream project sources and is being handled similarly to
    // how the CSW Registry uses the SourceConfigurationHandler class
    return (StringUtils.containsIgnoreCase(factoryPid, "source")
        || StringUtils.containsIgnoreCase(factoryPid, "service"));
  }

  // entries must be updated to the latest versions of themselves in order to restore correctly
  private ImportMigrationConfigurationAdminEntry updateNecessaryConfigurationAdminEntries(
      ImportMigrationConfigurationAdminEntry entry) {
    // this pid was updated in versions 2.16.1 and 2.17.0
    // see https://github.com/codice/ddf/issues/5131
    if (entry.getPid().equals("org.codice.ddf.security.filter.login.Session")) {
      entry.setPid("ddf.security.http.impl.HttpSessionFactory");
    }
    // this configuration was updated in version 2.13.6
    // see https://github.com/codice/ddf/pull/4388
    if (entry.getPid().equals(METACARD_VALIDITY_FILTER_PLUGIN_PID)) {
      entry = updateMetacardValidityFilterPluginConfiguration(entry);
    }
    return entry;
  }

  private ImportMigrationConfigurationAdminEntry updateMetacardValidityFilterPluginConfiguration(
      ImportMigrationConfigurationAdminEntry entry) {
    String[] attributeMap = (String[]) entry.getProperty(ATTRIBUTE_MAP);

    for (int i = 0; i < attributeMap.length; i++) {
      String val = attributeMap[i];
      String hostname = System.getProperty(DDF_HOSTNAME_PROP_KEY);
      attributeMap[i] = val.replaceAll("(?<!-)data-manager", hostname + "-data-manager");
    }
    entry.setProperty(ATTRIBUTE_MAP, String.join(",", attributeMap));
    return entry;
  }

  @SuppressWarnings(
      "PMD.DefaultPackage" /* designed to be called from ExportMigrationConfigurationAdminContext and ImportMigrationConfigurationAdminContext in this package */)
  @Nullable
  PersistenceStrategy getPersister(String extension) {
    // we do not anticipate many persistence strategies (< 5 and 2 at the moment) so a simple linear
    // search should suffice
    return strategies
        .stream()
        .filter(s -> s.getExtension().equals(extension))
        .findFirst()
        .orElse(null);
  }

  @SuppressWarnings(
      "PMD.DefaultPackage" /* designed to be called from ExportMigrationConfigurationAdminContext and ImportMigrationConfigurationAdminContext in this package */)
  boolean isConfigFile(Path path) {
    return getPersister(FilenameUtils.getExtension(path.toString())) != null;
  }

  private Configuration[] getConfigurations(MigrationContext context) {
    try {
      final Configuration[] configurations = configurationAdmin.listConfigurations(null);

      if (configurations != null) {
        return configurations;
      }
    } catch (IOException | InvalidSyntaxException e) {
      // InvalidSyntaxException should never happen since the filter is null
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
