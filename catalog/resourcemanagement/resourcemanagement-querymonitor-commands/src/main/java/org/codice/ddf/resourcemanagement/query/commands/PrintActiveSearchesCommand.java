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
package org.codice.ddf.resourcemanagement.query.commands;

import java.io.PrintStream;
import java.util.Map;
import java.util.UUID;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.codice.ddf.resourcemanagement.query.plugin.ActiveSearch;
import org.codice.ddf.resourcemanagement.query.plugin.QueryMonitorPlugin;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(
    scope = "catalog",
    name = "printactivesearches",
    description = "Prints a summary of all ActiveSearches currently in the QueryMonitor")
@Service
public class PrintActiveSearchesCommand implements Action {

  private static final Logger LOGGER = LoggerFactory.getLogger(PrintActiveSearchesCommand.class);

  @Reference private QueryMonitorPlugin queryMonitor = null;

  private static final String FORMAT_STRING = ActiveSearch.FORMAT_STRING;

  PrintStream console;

  PrintStream getPrintStream() {
    return System.out;
  }

  public PrintActiveSearchesCommand(QueryMonitorPlugin q) {
    queryMonitor = q;
    console = getPrintStream();
  }

  public PrintActiveSearchesCommand() {
    console = getPrintStream();
  }

  /**
   * Method to implement {@link Action}. Called when the catalog:printacticesearches command is
   * called by the shell. Prints out a formatted table representing a {@link QueryMonitorPlugin}'s
   * {@link ActiveSearch} {@link Map}.
   *
   * @return always returns {@code null}
   */
  @Override
  public Object execute() throws Exception {

    try {
      printActiveSearchesToConsole();
    } catch (Exception e) {
      LOGGER.debug("Exception encountered in doExecute of PrintActiveSearchesCommand.java.", e);
    }
    return null;
  }

  /**
   * Helper method called by the execute method when the catalog:printacticesearches command is
   * called by the shell. Prints the {@link String} message using the specified {@link
   * org.fusesource.jansi.Ansi.Color}
   */
  void printColor(Ansi.Color color, String message) {
    String colorString;
    if (color == null || color.equals(Ansi.Color.DEFAULT)) {
      colorString = Ansi.ansi().reset().toString();
    } else {
      colorString = Ansi.ansi().fg(color).toString();
    }
    console.print(colorString);
    console.print(message);
    console.println(Ansi.ansi().reset().toString());
  }

  /**
   * Called by the execute method when the catalog:printacticesearches command is called by the
   * shell. Loops through the {@link ActiveSearch} {@link Map} to print a formatted table
   * representation of all {@link ActiveSearch}'s in the {@link ActiveSearch} {@link Map}
   */
  void printActiveSearchesToConsole() {

    if (queryMonitor == null) {
      LOGGER.debug("QueryMonitorImpl not yet instantiated. Cannot printActiveSearchesToConsole().");
      return;
    }
    Map<UUID, ActiveSearch> activeSearchMap = queryMonitor.getActiveSearches();
    console.println();
    String colorString = Ansi.ansi().fg(Ansi.Color.CYAN).toString();
    console.print(colorString);
    console.print(
        String.format(
            FORMAT_STRING, "User Info", "Source", "Search Criteria", "Start Time", "Unique ID"));
    console.println(Ansi.ansi().reset().toString());
    if (activeSearchMap == null) {
      LOGGER.debug(
          "Retrieving ActiveSearch information from queryMonitor returned null. Cannot printActiveSearchesToConsole().");
      return;
    }
    for (ActiveSearch as : activeSearchMap.values()) {
      console.print(as.toFormattedString());
    }
  }
}
