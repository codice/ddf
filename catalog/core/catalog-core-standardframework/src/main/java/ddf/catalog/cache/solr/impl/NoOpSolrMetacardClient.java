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

import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardCreationException;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.source.solr.SolrMetacardClient;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** No-op implementation of the {@link SolrMetacardClient} interface. */
public class NoOpSolrMetacardClient implements SolrMetacardClient {
  private static final NoOpSolrMetacardClient INSTANCE = new NoOpSolrMetacardClient();

  private static final Logger LOGGER = LoggerFactory.getLogger(NoOpSolrMetacardClient.class);

  public static NoOpSolrMetacardClient getInstance() {
    return INSTANCE;
  }

  private NoOpSolrMetacardClient() {
    // Singleton
  }

  @Override
  public SourceResponse query(QueryRequest request) throws UnsupportedQueryException {
    LOGGER.debug("Query was not executed. SolrMetacardClient has not been initialized.");
    SourceResponseImpl sourceResponseImpl =
        new SourceResponseImpl(request, Collections.emptyList());
    sourceResponseImpl.setHits(0);
    return sourceResponseImpl;
  }

  @Override
  public List<Metacard> query(String queryString) throws UnsupportedQueryException {
    LOGGER.debug("Query was not executed. SolrMetacardClient has not been initialized.");
    return Collections.emptyList();
  }

  @Override
  public Set<ContentType> getContentTypes() {
    LOGGER.debug("No content types were found. SolrMetacardClient has not been initialized.");
    return Collections.emptySet();
  }

  @Override
  public List<SolrInputDocument> add(List<Metacard> metacards, boolean forceAutoCommit)
      throws IOException, SolrServerException, MetacardCreationException {
    LOGGER.debug("Metacards not added to Solr. SolrMetacardClient has not been initialized.");
    return Collections.emptyList();
  }

  @Override
  public void deleteByIds(
      String fieldName, List<? extends Serializable> identifiers, boolean forceCommit)
      throws IOException, SolrServerException {
    // No-op
    LOGGER.debug("Delete was not executed. SolrMetacardClient has not been initialized.");
  }

  @Override
  public void deleteByQuery(String query) throws IOException, SolrServerException {
    // No-op
    LOGGER.debug("Delete by query was not executed. SolrMetacardClient has not been initialized.");
  }
}
