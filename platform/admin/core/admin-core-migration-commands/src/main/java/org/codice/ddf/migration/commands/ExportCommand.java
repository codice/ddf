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
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.codice.ddf.configuration.migration.ConfigurationMigrationService;
import org.codice.ddf.security.Security;
import org.osgi.service.event.EventAdmin;

/** Command class used to export the system configuration and data. */
@Service
@Command(
  scope = MigrationCommand.NAMESPACE,
  name = "export",
  description = "Exports the system profile and configuration."
)
public class ExportCommand extends MigrationCommand {

  public ExportCommand(Security security) {
    super(security);
  }

  @VisibleForTesting
  ExportCommand(
      ConfigurationMigrationService service,
      Security security,
      EventAdmin eventAdmin,
      Session session) {
    super(service, security, eventAdmin, session);
  }

  @Override
  protected Object executeWithSubject() {
    postSystemNotice("export");
    configurationMigrationService.doExport(exportDirectory, this::outputMessage);
    return null;
  }
}
