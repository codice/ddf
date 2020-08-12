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
package ddf.catalog.solr.offlinegazetteer;

import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.COLLECTION_NAME;

import java.util.concurrent.TimeUnit;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.codice.solr.client.solrj.SolrClient;
import org.codice.solr.factory.SolrClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Command(
  scope = "offline-solr-gazetteer",
  name = "removeall",
  description = "Deletes all items in the solr gazetteer collection"
)
public class RemoveAllOfflineSolrGazetteerCommand implements Action {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(RemoveAllOfflineSolrGazetteerCommand.class);

  @Reference protected Session session;

  @Reference private SolrClientFactory clientFactory;

  @Option(
    name = "--force",
    aliases = {"-f"},
    description = "Force the removal without a confirmation message."
  )
  boolean force = false;

  @Override
  public Object execute() throws Exception {
    if (!force) {
      String answer =
          session
              .readLine(
                  "Are you sure you want to remove all gazetteer entries inside of the solr gazetteer collection?(y/n)",
                  ' ')
              .toLowerCase();
      if (!("y".equals(answer) || "yes".equals(answer))) {
        session.getConsole().println("Aborting.");
        return null;
      }
    }

    SolrClient solrClient = clientFactory.newClient(COLLECTION_NAME);

    Boolean response =
        Failsafe.with(
                new RetryPolicy()
                    .retryWhen(false)
                    .withMaxDuration(5, TimeUnit.SECONDS)
                    .withBackoff(25, 1_000, TimeUnit.MILLISECONDS))
            .get(() -> solrClient.isAvailable());
    if (response == null || !response) {
      LOGGER.error("Could not contact solr to remove all");
      session.getConsole().println("Could not contact solr to remove all, exiting.");
      return null;
    }
    try {
      solrClient.deleteByQuery("*:*");
    } catch (Exception e) {
      LOGGER.info("Error while executing", e);
      session.getConsole().println("Error while submitting remove all, exiting.");
      throw e;
    }
    session.getConsole().println("Removeall submitted successfully.");
    return null;
  }
}
