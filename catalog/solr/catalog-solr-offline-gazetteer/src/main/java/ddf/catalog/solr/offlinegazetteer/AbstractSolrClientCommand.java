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

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.solr.client.solrj.SolrServerException;
import org.codice.ddf.commands.catalog.SubjectCommands;
import org.codice.solr.client.solrj.SolrClient;
import org.codice.solr.client.solrj.UnavailableSolrException;
import org.codice.solr.factory.SolrClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSolrClientCommand extends SubjectCommands {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSolrClientCommand.class);

  @Reference protected SolrClientFactory clientFactory;

  @Option(
      name = "--force",
      aliases = "-f",
      description = "Force the command without a confirmation message.")
  protected boolean force = false;

  @SuppressWarnings({"java:S2139" /* Logging and rethrowing failure exception intentionally */})
  @Override
  protected Object executeWithSubject() throws Exception {
    if (!force) {
      String answer =
          session.readLine("Are you sure you want to continue?(y/n)", ' ').toLowerCase();
      if (!("y".equals(answer) || "yes".equals(answer))) {
        console.println("Aborting.");
        return null;
      }
    }

    try (SolrClient solrClient = clientFactory.newClient(COLLECTION_NAME)) {
      Callable<Boolean> booleanCallable = solrClient::isAvailable;
      boolean response =
          Failsafe.with(
                  new RetryPolicy()
                      .retryWhen(false)
                      .withMaxDuration(5, TimeUnit.SECONDS)
                      .withBackoff(25, 1_000, TimeUnit.MILLISECONDS))
              .get(booleanCallable);
      if (!response) {
        LOGGER.error("Could not contact solr");
        printErrorMessage("Could not contact solr, exiting.");
        return null;
      }
      executeWithSolrClient(solrClient);
    } catch (SolrServerException | IOException e) {
      // Note that this will also catch failures closing the SolrClient
      LOGGER.info("Error while executing", e);
      printErrorMessage("Error while executing.");
      throw e;
    }

    printSuccessMessage("Success");
    return null;
  }

  /**
   * @throws IOException if there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   * @throws RuntimeException if there is another error
   * @throws InterruptedException if interrupted while executing
   */
  abstract void executeWithSolrClient(SolrClient solrClient)
      throws SolrServerException, IOException, InterruptedException;
}
