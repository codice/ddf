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
import java.util.List;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;
import org.codice.ddf.resourcemanagement.query.plugin.QueryMonitorPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(
  scope = "catalog",
  name = "removesearchaftercomplete",
  description =
      "Sets flag to determine whether to delete active searches from the map after the searches complete"
)
@Service
public class RemoveSearchAfterCompleteCommand implements Action {

  PrintStream console = System.out;

  @Service
  public static class TrueAndFalseCompleter implements Completer {

    /**
     * @param session the beginning string typed by the user
     * @param commandLine the position of the cursor
     * @param candidates the list of completions proposed to the user
     */
    @Override
    public int complete(Session session, CommandLine commandLine, List<String> candidates) {
      StringsCompleter delegate = new StringsCompleter();
      delegate.getStrings().add("true");
      delegate.getStrings().add("false");
      return delegate.complete(session, commandLine, candidates);
    }
  }

  @Argument(
    name = "removesearch",
    description =
        "The boolean value indicating whether to remove active searches from the search map after the search has completed.",
    index = 0,
    multiValued = false,
    required = true
  )
  @Completion(TrueAndFalseCompleter.class)
  private String removeSearchAfterComplete = "";

  private static final Logger LOGGER =
      LoggerFactory.getLogger(RemoveSearchAfterCompleteCommand.class);

  @Reference private QueryMonitorPlugin queryMonitor;

  public RemoveSearchAfterCompleteCommand(QueryMonitorPlugin q) {
    queryMonitor = q;
  }

  public RemoveSearchAfterCompleteCommand() {}

  /**
   * Method to set the boolean in a {@link QueryMonitorPlugin} which determines whether {@link
   * ActiveSearch}'s should be removed from the {@link ActiveSearch} {@link java.util.Map} when the
   * search completes. A value of true has the effect that searches will be removed while a value of
   * false has the effect that {@link ActiveSearch}'s will be left inside of the {@link
   * ActiveSearch} {@link java.util.Map} after query completion.
   *
   * @param b {@link Boolean} a value of true has the effect that searches will be removed while a
   *     value of false has the effect that {@link ActiveSearch}'s will be left inside of the {@link
   *     ActiveSearch} {@link java.util.Map} after query completion.
   * @return {@link Boolean} representing whether or not setting the variable was successful
   */
  public boolean setRemoveSearchAfterComplete(boolean b) {
    if (queryMonitor == null) {
      LOGGER.debug(
          "QueryMonitor not yet instantiated. Cannot set RemoveSearchAfterComplete({}).", b);
      return false;
    }
    queryMonitor.setRemoveSearchAfterComplete(b);
    return true;
  }

  /**
   * Method to implement {@link Action}. Called when the catalog:removesearchaftercomplete command
   * is called by the shell. Sets the value of the boolean which determines whether or not {@link
   * ActiveSearch}'s are removed from the {@link ActiveSearch} {@link java.util.Map} after search
   * completion.
   *
   * @return always returns {@code null}
   */
  @Override
  public Object execute() throws Exception {
    if (queryMonitor == null) {
      LOGGER.debug("QueryMonitor not yet instantiated.");
      return null;
    }
    if (!"".equals(removeSearchAfterComplete)) {
      if (Boolean.valueOf(removeSearchAfterComplete)) {
        queryMonitor.setRemoveSearchAfterComplete(true);
      } else if ("false".equalsIgnoreCase(removeSearchAfterComplete)) {
        queryMonitor.setRemoveSearchAfterComplete(false);
      } else {
        console.println(
            "Incorrect argument! Argument to removesearchaftercomplete command must have a value of either 'true' or 'false'.");
        LOGGER.debug(
            "Incorrect argument! Argument to removesearchaftercomplete command must have a value of either 'true' or 'false'.");
      }
    }
    return null;
  }
}
