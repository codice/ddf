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
package ddf.catalog.solr.cloud;

import java.util.Optional;
import java.util.concurrent.Future;

import org.apache.solr.client.solrj.SolrClient;
import org.codice.solr.factory.impl.SolrCloudClientFactory;

import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.source.solr.RemoteSolrCatalogProvider;
import ddf.catalog.source.solr.SolrFilterDelegateFactory;

/**
 * Catalog Provider that interfaces with a Solr Cloud
 */
public class SolrCloudCatalogProvider extends RemoteSolrCatalogProvider {

    public SolrCloudCatalogProvider(FilterAdapter filterAdapter,
            SolrFilterDelegateFactory solrFilterDelegateFactory) {
        super(filterAdapter, solrFilterDelegateFactory);
    }

    @Override
    protected Future<SolrClient> createClient() {
        return SolrCloudClientFactory.getClient(Optional.ofNullable(url)
                .orElse(SolrCloudClientFactory.DEFAULT_ZOOKEEPER_HOST), SOLR_CATALOG_CORE_NAME);
    }

}
