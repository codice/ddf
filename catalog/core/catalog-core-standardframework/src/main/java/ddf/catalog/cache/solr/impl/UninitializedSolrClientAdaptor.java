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
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.util.NamedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the {@link SolrClientAdaptor} interface not connected to any {@link
 * org.apache.solr.client.solrj.SolrClient}. All operations are no-op.
 */
class UninitializedSolrClientAdaptor implements SolrClientAdaptor.State {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(UninitializedSolrClientAdaptor.class);

  private static final UninitializedSolrClientAdaptor INSTANCE =
      new UninitializedSolrClientAdaptor();

  static UninitializedSolrClientAdaptor getInstance() {
    return INSTANCE;
  }

  private UninitializedSolrClientAdaptor() {
    // Singleton
  }

  @Override
  public void commit() throws SolrServerException, IOException {
    LOGGER.debug("Called commit on un-initialized SolrClientAdaptor. Ignoring.");
  }

  @Override
  public void close() throws IOException {
    LOGGER.debug("Called close on un-initialized SolrClientAdaptor. Ignoring.");
  }

  @Override
  public UpdateResponse deleteByQuery(String query) throws SolrServerException, IOException {
    LOGGER.debug("Called deleteByQuery on un-initialized SolrClientAdaptor. Ignoring.");
    UpdateResponse updateResponse = new UpdateResponse();
    updateResponse.setElapsedTime(0);
    updateResponse.setRequestUrl("");
    updateResponse.setResponse(new NamedList<>());
    return updateResponse;
  }
}
