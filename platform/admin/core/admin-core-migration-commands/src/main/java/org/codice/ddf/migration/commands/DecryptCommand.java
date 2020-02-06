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
  name = "decrypt",
  description =
      "Decrypts an exported file. Decrypting an exported file is insecure and appropriate measures should be taken to secure the resulting decrypted file."
)
public class DecryptCommand extends MigrationCommand {

  public DecryptCommand(Security security) {
    super(security);
  }

  @VisibleForTesting
  DecryptCommand(
      ConfigurationMigrationService service,
      Security security,
      EventAdmin eventAdmin,
      Session session) {
    super(service, security, eventAdmin, session);
  }

  @Override
  protected Object executeWithSubject() {
    postSystemNotice("decrypt");
    configurationMigrationService.doDecrypt(exportDirectory, this::outputMessage);
    return null;
  }
}
