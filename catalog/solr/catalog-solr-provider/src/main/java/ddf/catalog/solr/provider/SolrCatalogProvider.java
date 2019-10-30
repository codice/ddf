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
package ddf.catalog.solr.provider;

import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.source.solr.DynamicSchemaResolver;
import ddf.catalog.source.solr.RemoteSolrCatalogProvider;
import ddf.catalog.source.solr.SolrFilterDelegateFactory;
import org.codice.solr.factory.SolrClientFactory;

/** Catalog Provider that interfaces with Solr */
public class SolrCatalogProvider extends RemoteSolrCatalogProvider {
  public SolrCatalogProvider(
      FilterAdapter filterAdapter,
      SolrClientFactory clientFactory,
      SolrFilterDelegateFactory solrFilterDelegateFactory,
      DynamicSchemaResolver dynamicSchemaResolver) {
    super(
        filterAdapter,
        clientFactory.newClient(SOLR_CATALOG_CORE_NAME),
        solrFilterDelegateFactory,
        dynamicSchemaResolver);
  }
}
