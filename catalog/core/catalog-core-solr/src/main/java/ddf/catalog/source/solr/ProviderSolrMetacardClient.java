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
package ddf.catalog.source.solr;

import ddf.catalog.data.MetacardCreationException;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.filter.FilterAdapter;
import org.apache.solr.common.SolrDocument;
import org.codice.solr.client.solrj.SolrClient;

public class ProviderSolrMetacardClient extends SolrMetacardClientImpl {

  String sourcdId;

  public ProviderSolrMetacardClient(
      SolrClient client,
      FilterAdapter catalogFilterAdapter,
      SolrFilterDelegateFactory solrFilterDelegateFactory,
      DynamicSchemaResolver dynamicSchemaResolver,
      String id) {
    super(client, catalogFilterAdapter, solrFilterDelegateFactory, dynamicSchemaResolver);
    this.sourcdId = id;
  }

  @Override
  public MetacardImpl createMetacard(SolrDocument doc) throws MetacardCreationException {
    MetacardImpl metacard = super.createMetacard(doc);
    metacard.setSourceId(sourcdId);
    return metacard;
  }
}
