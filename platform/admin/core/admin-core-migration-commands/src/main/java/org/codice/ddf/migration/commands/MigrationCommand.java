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
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.codice.ddf.configuration.migration.ConfigurationMigrationService;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationMessage;
import org.codice.ddf.migration.MigrationSuccessfulInformation;
import org.codice.ddf.migration.MigrationWarning;
import org.codice.ddf.security.common.Security;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Attribute;

/** Provides common methods and instance variables that migration commands can use. */
public abstract class MigrationCommand implements Action {
  public static final String ERROR_MESSAGE =
      "An error was encountered while executing this command; %s.";

  public static final String NAMESPACE = "migration";

  protected static final String EXPORTED = "exported";

  protected final Security security;

  @Reference protected ConfigurationMigrationService configurationMigrationService;

  protected Path defaultExportDirectory =
      Paths.get(System.getProperty("ddf.home"), MigrationCommand.EXPORTED);

  public MigrationCommand() {
    this.security = Security.getInstance();
  }

  @VisibleForTesting
  MigrationCommand(ConfigurationMigrationService service, Security security) {
    this.configurationMigrationService = service;
    this.security = security;
  }

  protected void outputErrorMessage(String message) {
    outputMessageWithColor(message, Ansi.Color.RED);
  }

  protected void outputMessage(MigrationMessage msg) {
    if (msg instanceof MigrationException) {
      outputErrorMessage(msg.getMessage());
    } else if (msg instanceof MigrationWarning) {
      outputMessageWithColor(msg.getMessage(), Ansi.Color.YELLOW);
    } else if (msg instanceof MigrationSuccessfulInformation) {
      outputMessageWithColor(msg.getMessage(), Ansi.Color.GREEN);
    } else {
      outputMessageWithColor(msg.getMessage(), Ansi.Color.WHITE);
    }
  }

  // squid:S106 - we purposely need to output to the output stream for the info to get to the admin
  // console
  @SuppressWarnings("squid:S106")
  @VisibleForTesting
  protected PrintStream getConsole() {
    return System.out;
  }

  private void outputMessageWithColor(String message, Ansi.Color color) {
    final String colorAsString = Ansi.ansi().a(Attribute.RESET).fg(color).toString();
    final PrintStream console = getConsole();

    console.print(colorAsString);
    console.print(message);
    console.println(Ansi.ansi().a(Attribute.RESET).toString());
  }
}
