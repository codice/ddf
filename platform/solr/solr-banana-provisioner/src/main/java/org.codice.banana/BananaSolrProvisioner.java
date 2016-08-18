/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
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

public class BananaSolrProvisioner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolrClientFactory.class);

    public BananaSolrProvisioner() {
        CompletableFuture.supplyAsync(BananaSolrProvisioner::bananaClientSupplier)
                .thenAccept(BananaSolrProvisioner::closeSolrClient);
    }

    private static SolrClient bananaClientSupplier() {
        SolrClient client = null;
        try {
            client =
                    SolrClientFactory.getHttpSolrClient(SolrClientFactory.getDefaultHttpsAddress(),
                            "banana")
                            .get();
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
