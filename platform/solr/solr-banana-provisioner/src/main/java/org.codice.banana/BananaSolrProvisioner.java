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
package org.codice.banana;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.apache.solr.client.solrj.SolrClient;
import org.codice.solr.factory.SolrClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Class used to create the Solr core needed by Banana. */
public class BananaSolrProvisioner {

  private static final Logger LOGGER = LoggerFactory.getLogger(BananaSolrProvisioner.class);

  /**
   * Constructor. Uses the {@link SolrClientFactory} to create the appropriate Solr client and core
   * and then closes the client.
   *
   * @param solrClientFactory client factory to use
   */
  public BananaSolrProvisioner(SolrClientFactory solrClientFactory) {
    CompletableFuture.supplyAsync(() -> bananaClientSupplier(solrClientFactory))
        .thenAccept(BananaSolrProvisioner::closeSolrClient);
  }

  private static SolrClient bananaClientSupplier(SolrClientFactory solrClientFactory) {
    SolrClient client = null;
    try {
      client = solrClientFactory.newClient("banana").get();
    } catch (InterruptedException | ExecutionException e) {
      LOGGER.debug("Failed to provision Banana Solr core", e);
    }
    return client;
  }

  private static void closeSolrClient(SolrClient client) {
    if (client != null) {
      try {
        client.close();
      } catch (IOException e) {
        LOGGER.debug("Failed to close Banana Solr core", e);
      }
    }
  }
}
