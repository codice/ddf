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
package org.codice.ddf.ui.searchui.query.solr;

import static org.codice.solr.factory.impl.EmbeddedSolrFactory.IMMEMORY_SOLRCONFIG_XML;
import static org.codice.solr.factory.impl.HttpSolrClientFactory.DEFAULT_SCHEMA_XML;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.source.solr.SolrCatalogProvider;
import ddf.catalog.source.solr.SolrFilterDelegateFactory;
import ddf.catalog.source.solr.SolrFilterDelegateFactoryImpl;
import java.util.ArrayList;
import java.util.List;
import org.codice.solr.factory.impl.ConfigurationFileProxy;
import org.codice.solr.factory.impl.ConfigurationStore;
import org.codice.solr.factory.impl.EmbeddedSolrFactory;

public class FilteringSolrIndex {

  private final SolrCatalogProvider provider;

  FilteringSolrIndex(SolrCatalogProvider provider) {
    this.provider = provider;
  }

  public FilteringSolrIndex(String queryId, FilterAdapter filterAdapter, QueryRequest request) {
    this(createInMemorySolrProvider(queryId, filterAdapter, request));
  }

  private static SolrCatalogProvider createInMemorySolrProvider(
      String queryId, FilterAdapter filterAdapter, QueryRequest request) {
    final ConfigurationStore configStore = new ConfigurationStore();

    configStore.setInMemory(true);
    configStore.setForceAutoCommit(true);
    ConfigurationFileProxy configurationFileProxy = new ConfigurationFileProxy(configStore);

    SolrFilterDelegateFactory solrFilterDelegateFactory = new SolrFilterDelegateFactoryImpl();

    return new SolrCatalogProvider(
        new EmbeddedSolrFactory()
            .newClient(
                queryId,
                IMMEMORY_SOLRCONFIG_XML,
                DEFAULT_SCHEMA_XML,
                configStore,
                configurationFileProxy),
        filterAdapter,
        solrFilterDelegateFactory,
        new FilteringDynamicSchemaResolver(filterAdapter, solrFilterDelegateFactory, request));
  }

  /**
   * Must be synchronized since force auto commit is enabled. If too many commits happen at the same
   * time, performance is impacted and might log “too many warming searchers” warnings.
   */
  public synchronized CreateResponse add(List<Result> results) throws IngestException {
    return provider.create(new CreateRequestImpl(getMetacards(results)));
  }

  public SourceResponse query(QueryRequest request) throws UnsupportedQueryException {
    return provider.query(request);
  }

  public void shutdown() {
    provider.shutdown();
  }

  private List<Metacard> getMetacards(List<Result> results) {
    List<Metacard> metacards = new ArrayList<>(results.size());

    for (Result result : results) {
      if (result != null && result.getMetacard() != null) {
        metacards.add(result.getMetacard());
      }
    }

    return metacards;
  }
}
