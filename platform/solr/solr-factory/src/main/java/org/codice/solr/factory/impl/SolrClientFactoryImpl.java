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
package org.codice.solr.factory.impl;

import java.util.concurrent.Future;

import org.apache.solr.client.solrj.SolrClient;
import org.codice.solr.factory.SolrClientFactory;

public class SolrClientFactoryImpl implements SolrClientFactory {

    public Future<SolrClient> newClient(String core) {
        String clientType = System.getProperty("solr.client", "HttpSolrClient");
        SolrClientFactory factory;

        if ("EmbeddedSolrServer".equals(clientType)) {
            factory = new EmbeddedSolrFactory();
        } else if ("CloudSolrClient".equals(clientType)) {
            factory = new SolrCloudClientFactory();
        } else { // Use HttpSolrClient by default
            factory = new HttpSolrClientFactory();
        }

        return factory.newClient(core);
    }

}
