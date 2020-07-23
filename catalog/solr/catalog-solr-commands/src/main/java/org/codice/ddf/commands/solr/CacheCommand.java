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
package org.codice.ddf.commands.solr;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.codice.solr.client.solrj.SolrClient;
import org.codice.solr.factory.SolrClientFactory;

@Service
@Command(
    scope = SolrCommands.NAMESPACE,
    name = "cache",
    description = "Support the remote result cache.")
public class CacheCommand extends SolrCommands {

  @Option(name = "--clear", description = "Clear the remote result cache.", required = true)
  @VisibleForTesting
  protected boolean clear = false;

  @Option(
      name = "-f",
      aliases = {"--force"},
      description = "Force the operation without further user confirmation.")
  @VisibleForTesting
  protected boolean force = false;

  private SolrClient client = null;

  @Reference private SolrClientFactory clientFactory;

  @Reference protected Session session;

  @Override
  public Object execute() throws Exception {

    if (isAccidentalRemoval()) {
      return null;
    }

    if (client == null) {
      client = clientFactory.newClient("metacard_cache");
    }

    if (!isSolrClientAvailable(client)) {
      printErrorMessage("The Solr client is not available.");
      return null;
    }

    int status = client.deleteByQuery("metacard_cache", "*:*").getStatus();

    if (status != 0) {
      printErrorMessage("Unable to clear cache at this time. Review Solr logs.");
    } else {
      printSuccessMessage(
          "Successfully cleared cache. It could take a minute for all contents to be removed.");
    }

    return true;
  }

  private boolean isAccidentalRemoval() {
    if (!clear) {
      return true;
    }

    if (force) {
      return false;
    }

    final String response;
    try {
      response =
          session.readLine(
              "WARNING: This will permanently remove all cached remote query results. Do you want to proceed? (yes/no): ",
              null);
    } catch (IOException e) {
      console.println("Please add \"--force\" to command instead.");
      return true;
    }

    if (response.equalsIgnoreCase("yes")) {
      return false;
    } else if (response.equalsIgnoreCase("no")) {
      console.println("Cache was not cleared.");
      return true;
    } else {
      console.println("\"" + response + "\" is invalid. ");
      return true;
    }
  }

  @VisibleForTesting
  protected void setClient(SolrClient client) {
    this.client = client;
  }
}
