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
package org.codice.ddf.platform.migratable.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.felix.utils.properties.Properties;
import org.codice.ddf.configuration.migration.util.AccessUtils;
import org.codice.ddf.configuration.migration.util.VersionUtils;
import org.codice.ddf.migration.ExportMigrationContext;
import org.codice.ddf.migration.ExportMigrationEntry;
import org.codice.ddf.migration.ImportMigrationContext;
import org.codice.ddf.migration.ImportMigrationEntry;
import org.codice.ddf.migration.Migratable;
import org.codice.ddf.migration.MigrationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class handles the export process for all Platform system files. */
public class PlatformMigratable implements Migratable {

  private static final Logger LOGGER = LoggerFactory.getLogger(PlatformMigratable.class);

  /**
   * Holds the current export version.
   *
   * <p>1.0 - initial version
   */
  private static final String CURRENT_VERSION = "1.0";

  private static final String CUSTOM_SYSTEM_PROPERTIES_ERROR =
      "Failed to upgrade custom.system.properties; %s";

  private static final String KEYSTORE_SYSTEM_PROP = "javax.net.ssl.keyStore";

  private static final String TRUSTSTORE_SYSTEM_PROP = "javax.net.ssl.trustStore";

  private static final Path ETC_DIR = Paths.get("etc");

  private static final Path BIN_DIR = Paths.get("bin");

  private static final Path CUSTOM_SYSTEM_PROPERTIES_PATH =
      Paths.get("etc", "custom.system.properties");

  private static final Set<String> CUSTOM_SYSTEM_PROPERTIES_TO_PRESERVE =
      ImmutableSet.of("org.codice.ddf.system.version", "solr.password");

  private static final List<Path> UPGRADEABLE_SYSTEM_PATHS =
      ImmutableList.of( //
          Paths.get("etc", "users.properties"), Paths.get("etc", "users.attributes"));

  /*
   Files below are included under export and import operations within the same product version.
  */
  private static final List<Path> REQUIRED_SYSTEM_PATHS =
      ImmutableList.of( //
          Paths.get("etc", "ws-security"),
          Paths.get("etc", "system.properties"),
          Paths.get("etc", "custom.system.properties"),
          Paths.get("etc", "startup.properties"),
          Paths.get("etc", "custom.properties"),
          Paths.get("etc", "config.properties"));

  private static final List<Path> OPTIONAL_SYSTEM_PATHS =
      ImmutableList.of( //
          Paths.get("etc", "users.properties"),
          Paths.get("etc", "users.attributes"),
          Paths.get("etc", "pdp", "ddf-metacard-attribute-ruleset.cfg"),
          Paths.get("etc", "pdp", "ddf-user-attribute-ruleset.cfg"),
          Paths.get("etc", "org.codice.ddf.admin.applicationlist.properties"),
          Paths.get("etc", "fipsToIso.properties"),
          Paths.get("etc", "log4j2.xml"),
          Paths.get("etc", "certs", "meta"),
          Paths.get("etc", "certs", "1"),
          Paths.get("bin", "karaf"),
          Paths.get("bin", "karaf.bat"));

  private static final PathMatcher SERVICE_WRAPPER_CONF_FILTER =
      FileSystems.getDefault().getPathMatcher("glob:**/*-wrapper.conf");

  @Override
  public String getVersion() {
    return PlatformMigratable.CURRENT_VERSION;
  }

  @Override
  public String getId() {
    return "ddf.platform";
  }

  @Override
  public String getTitle() {
    return "Platform Migration";
  }

  @Override
  public String getDescription() {
    return "Exports Platform system files";
  }

  @Override
  public String getOrganization() {
    return "Codice";
  }

  @Override
  public void doExport(ExportMigrationContext context) {
    LOGGER.debug("Exporting required system files & directories...");
    PlatformMigratable.REQUIRED_SYSTEM_PATHS
        .stream()
        .map(context::getEntry)
        .forEach(ExportMigrationEntry::store);
    LOGGER.debug("Exporting optional system files & directories...");
    PlatformMigratable.OPTIONAL_SYSTEM_PATHS
        .stream()
        .map(context::getEntry)
        .forEach(me -> me.store(false));
    LOGGER.debug("Exporting keystore and truststore...");
    context
        .getSystemPropertyReferencedEntry(PlatformMigratable.KEYSTORE_SYSTEM_PROP)
        .ifPresent(ExportMigrationEntry::store);
    context
        .getSystemPropertyReferencedEntry(PlatformMigratable.TRUSTSTORE_SYSTEM_PROP)
        .ifPresent(ExportMigrationEntry::store);
    LOGGER.debug("Exporting service wrapper config file");
    context
        .entries(PlatformMigratable.ETC_DIR, PlatformMigratable.SERVICE_WRAPPER_CONF_FILTER)
        .forEach(me -> me.store(false));
    context
        .entries(PlatformMigratable.BIN_DIR, PlatformMigratable.SERVICE_WRAPPER_CONF_FILTER)
        .forEach(me -> me.store(false));
  }

  @Override
  public void doImport(ImportMigrationContext context) {
    LOGGER.debug("Importing required system files & directories...");
    PlatformMigratable.REQUIRED_SYSTEM_PATHS
        .stream()
        .map(context::getEntry)
        .forEach(ImportMigrationEntry::restore);
    LOGGER.debug("Importing optional system files & directories...");
    PlatformMigratable.OPTIONAL_SYSTEM_PATHS
        .stream()
        .map(context::getEntry)
        .forEach(me -> me.restore(false));
    importKeystores(context);
    importServiceWrapperFiles(context);
  }

  @Override
  public void doVersionUpgradeImport(ImportMigrationContext context) {
    if (!VersionUtils.isValidMigratableFloatVersion(context, getVersion(), getId())) {
      return;
    }

    LOGGER.debug("Upgrading system properties...");
    try {
      AccessUtils.doPrivileged(() -> upgradeCustomSystemProperties(context));
    } catch (IOException e) {
      context.getReport().record(new MigrationException(CUSTOM_SYSTEM_PROPERTIES_ERROR, e));
    }

    LOGGER.debug("Importing remaining system files...");
    PlatformMigratable.UPGRADEABLE_SYSTEM_PATHS
        .stream()
        .map(context::getEntry)
        .forEach(me -> me.restore(false));
    importKeystores(context);
    importServiceWrapperFiles(context);
  }

  @VisibleForTesting
  void upgradeCustomSystemProperties(ImportMigrationContext context) throws IOException {
    Properties importedProps = new Properties();
    Properties currentProps;

    File fileOnSystem =
        Paths.get(System.getProperty("ddf.home"))
            .toRealPath(LinkOption.NOFOLLOW_LINKS)
            .resolve(Paths.get("etc", "custom.system.properties"))
            .toFile();
    currentProps = new Properties(fileOnSystem);

    importedProps.load(
        context
            .getEntry(PlatformMigratable.CUSTOM_SYSTEM_PROPERTIES_PATH)
            .getInputStream()
            .orElseThrow(IOException::new));

    currentProps
        .entrySet()
        .stream()
        .filter(e -> propertyShouldNotBeOverwritten(e, importedProps))
        .forEach(e -> importedProps.put(e.getKey(), e.getValue()));

    importedProps.save(fileOnSystem);
  }

  private boolean propertyShouldNotBeOverwritten(
      Entry<String, String> entry, Properties importedProps) {
    return !importedProps.containsKey(entry.getKey())
        || CUSTOM_SYSTEM_PROPERTIES_TO_PRESERVE.contains(entry.getKey());
  }

  private void importKeystores(ImportMigrationContext context) {
    LOGGER.debug("Importing keystore and truststore...");
    context
        .getSystemPropertyReferencedEntry(PlatformMigratable.KEYSTORE_SYSTEM_PROP)
        .ifPresent(ImportMigrationEntry::restore);
    context
        .getSystemPropertyReferencedEntry(PlatformMigratable.TRUSTSTORE_SYSTEM_PROP)
        .ifPresent(ImportMigrationEntry::restore);
  }

  private void importServiceWrapperFiles(ImportMigrationContext context) {
    LOGGER.debug("Importing service wrapper config files");
    context
        .entries(PlatformMigratable.ETC_DIR, PlatformMigratable.SERVICE_WRAPPER_CONF_FILTER)
        .forEach(me -> me.restore(false));
    context
        .entries(PlatformMigratable.BIN_DIR, PlatformMigratable.SERVICE_WRAPPER_CONF_FILTER)
        .forEach(me -> me.restore(false));
  }
}
