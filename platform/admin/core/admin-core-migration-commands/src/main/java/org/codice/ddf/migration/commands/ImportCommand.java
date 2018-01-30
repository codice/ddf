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
package org.codice.ddf.migration.commands;

import com.google.common.annotations.VisibleForTesting;
import java.util.Collections;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.codice.ddf.configuration.migration.ConfigurationMigrationService;
import org.codice.ddf.security.common.Security;
import org.osgi.service.event.EventAdmin;

/**
 * Command class used to import the system configuration exported via the {@link ExportCommand}
 * command.
 */
@Service
@Command(
  scope = MigrationCommand.NAMESPACE,
  name = "import",
  description =
      "Restores the system profile and configuration to the one recorded by a previously executed "
          + MigrationCommand.NAMESPACE
          + ":export command."
)
public class ImportCommand extends MigrationCommand {

  @Option(
    name = "--profile",
    required = false,
    multiValued = false,
    description = "Enables the installed profile from the original system to be restored."
  )
  private boolean profile = false;

  public ImportCommand() {}

  @VisibleForTesting
  ImportCommand(
      ConfigurationMigrationService service,
      Security security,
      EventAdmin eventAdmin,
      Session session,
      boolean profile) {
    super(service, security, eventAdmin, session);
    this.profile = profile;
  }

  @Override
  protected Object executeWithSubject() {
    postSystemNotice("import");
    configurationMigrationService.doImport(
        exportDirectory,
        (profile ? Collections.singleton("ddf.profile") : Collections.emptySet()),
        this::outputMessage);
    return null;
  }
}
