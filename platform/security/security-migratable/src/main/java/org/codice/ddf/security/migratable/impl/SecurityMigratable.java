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
package org.codice.ddf.security.migratable.impl;

import com.google.common.collect.ImmutableList;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import org.codice.ddf.configuration.migration.util.VersionUtils;
import org.codice.ddf.migration.ExportMigrationContext;
import org.codice.ddf.migration.ExportMigrationEntry;
import org.codice.ddf.migration.ImportMigrationContext;
import org.codice.ddf.migration.ImportMigrationEntry;
import org.codice.ddf.migration.Migratable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class handles the export process for all Security system files */
public class SecurityMigratable implements Migratable {

  private static final Logger LOGGER = LoggerFactory.getLogger(SecurityMigratable.class);

  private static final String WS_SECURITY = "ws-security";

  /**
   * Holds the current export version.
   *
   * <p>1.0 - initial version
   */
  private static final String CURRENT_VERSION = "1.0";

  private static final Path PDP_POLICIES_DIR = Paths.get("etc", "pdp");

  private static final Path CONFIGURATIONS_POLICY_FILE =
      Paths.get("security", "configurations.policy");

  private static final Path SECURITY_POLICIES_DIR = Paths.get("security");

  private static final List<Path> PROPERTIES_FILES =
      ImmutableList.of( //
          Paths.get("etc", SecurityMigratable.WS_SECURITY, "server", "encryption.properties"),
          Paths.get("etc", SecurityMigratable.WS_SECURITY, "server", "signature.properties"),
          Paths.get("etc", SecurityMigratable.WS_SECURITY, "issuer", "encryption.properties"),
          Paths.get("etc", SecurityMigratable.WS_SECURITY, "issuer", "signature.properties"));

  private static final String CRL_PROP_KEY = "org.apache.ws.security.crypto.merlin.x509crl.file";

  @Override
  public String getVersion() {
    return SecurityMigratable.CURRENT_VERSION;
  }

  @Override
  public String getId() {
    return "ddf.security";
  }

  @Override
  public String getTitle() {
    return "Security Migration";
  }

  @Override
  public String getDescription() {
    return "Exports Security system files";
  }

  @Override
  public String getOrganization() {
    return "Codice";
  }

  @Override
  public void doExport(ExportMigrationContext context) {
    SecurityMigratable.PROPERTIES_FILES
        .stream()
        .map(context::getEntry)
        .peek(
            me ->
                LOGGER.debug(
                    "Exporting CRL from property [{}] in file [{}]...",
                    SecurityMigratable.CRL_PROP_KEY,
                    me.getPath()))
        // do not automatically record an error if property is not defined (just skip that file)
        .map(
            me ->
                me.getPropertyReferencedEntry(
                    SecurityMigratable.CRL_PROP_KEY, (r, v) -> (v != null)))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .forEach(ExportMigrationEntry::store);
    LOGGER.debug("Exporting PDP files from [{}]...", SecurityMigratable.PDP_POLICIES_DIR);
    context.getEntry(SecurityMigratable.PDP_POLICIES_DIR).store();
    LOGGER.debug(
        "Exporting security policy files from [{}]...", SecurityMigratable.SECURITY_POLICIES_DIR);
    context.getEntry(SecurityMigratable.SECURITY_POLICIES_DIR).store();
  }

  @Override
  public void doImport(ImportMigrationContext context) {
    importPropertiesFiles(context);
    importPdpPolicies(context);
    LOGGER.debug(
        "Importing security policy Directory at [{}]...", SecurityMigratable.SECURITY_POLICIES_DIR);
    context.getEntry(SecurityMigratable.SECURITY_POLICIES_DIR).restore();
  }

  @Override
  public void doVersionUpgradeImport(ImportMigrationContext context) {
    if (!VersionUtils.isValidMigratableFloatVersion(context, getVersion(), getId())) {
      return;
    }

    importPropertiesFiles(context);
    importPdpPolicies(context);

    LOGGER.debug(
        "Importing configurations policy at [{}]...",
        SecurityMigratable.CONFIGURATIONS_POLICY_FILE);
    context.getEntry(SecurityMigratable.CONFIGURATIONS_POLICY_FILE).restore(false);
  }

  private void importPdpPolicies(ImportMigrationContext context) {
    LOGGER.debug("Importing PDP Directory at [{}]...", SecurityMigratable.PDP_POLICIES_DIR);
    context.getEntry(SecurityMigratable.PDP_POLICIES_DIR).restore();
  }

  private void importPropertiesFiles(ImportMigrationContext context) {
    SecurityMigratable.PROPERTIES_FILES
        .stream()
        .map(context::getEntry)
        .peek(
            me ->
                LOGGER.debug(
                    "Importing CRL from property [{}] in file [{}]...",
                    SecurityMigratable.CRL_PROP_KEY,
                    me.getPath()))
        .map(me -> me.getPropertyReferencedEntry(SecurityMigratable.CRL_PROP_KEY))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .forEach(ImportMigrationEntry::restore);
  }
}
