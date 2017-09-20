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
package ddf.catalog.cache.solr.impl;

import java.io.IOException;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;

/**
 * Implementation of the {@link SolrClientAdaptor.State} interface that delegates all its operations
 * to an initialized {@link SolrClient}.
 */
class InitializedSolrClientAdaptor implements SolrClientAdaptor.State {
  private final SolrClient client;

  InitializedSolrClientAdaptor(SolrClient client) {
    this.client = client;
  }

  @Override
  public void commit() throws SolrServerException, IOException {
    client.commit();
  }

  @Override
  public void close() throws IOException {
    client.close();
  }

  @Override
  public UpdateResponse deleteByQuery(String query) throws SolrServerException, IOException {
    return client.deleteByQuery(query);
  }
}
