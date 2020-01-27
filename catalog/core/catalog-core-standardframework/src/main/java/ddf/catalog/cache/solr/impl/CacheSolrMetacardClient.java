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

import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardCreationException;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.source.solr.DynamicSchemaResolver;
import ddf.catalog.source.solr.SolrFilterDelegateFactory;
import ddf.catalog.source.solr.SolrMetacardClientImpl;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.codice.solr.client.solrj.SolrClient;

/** {@link SolrCache} specific implementation of {@link SolrMetacardClientImpl}. */
class CacheSolrMetacardClient extends SolrMetacardClientImpl {

  private static final List<String> ADDITIONAL_FIELDS =
      Arrays.asList(SolrCache.METACARD_SOURCE_NAME, SolrCache.METACARD_ID_NAME);

  public CacheSolrMetacardClient(
      SolrClient client,
      FilterAdapter catalogFilterAdapter,
      SolrFilterDelegateFactory solrFilterDelegateFactory,
      DynamicSchemaResolver dynamicSchemaResolver) {
    super(
        client,
        catalogFilterAdapter,
        solrFilterDelegateFactory,
        enhanceResolver(dynamicSchemaResolver));
  }

  private static DynamicSchemaResolver enhanceResolver(
      DynamicSchemaResolver dynamicSchemaResolver) {
    DynamicSchemaResolver.addAdditionalFields(dynamicSchemaResolver, ADDITIONAL_FIELDS);
    return dynamicSchemaResolver;
  }

  @Override
  public MetacardImpl createMetacard(SolrDocument doc) throws MetacardCreationException {
    MetacardImpl metacard = super.createMetacard(doc);

    if (metacard != null) {
      metacard.setSourceId(getMetacardSource(doc));
      metacard.setId(getMetacardId(doc));
    }

    return metacard;
  }

  public UpdateResponse delete(String query) throws IOException, SolrServerException {
    return getClient().deleteByQuery(query);
  }

  @Override
  protected SolrInputDocument getSolrInputDocument(Metacard metacard)
      throws MetacardCreationException {
    SolrInputDocument solrInputDocument = super.getSolrInputDocument(metacard);

    solrInputDocument.addField(SolrCache.CACHED_DATE, new Date());

    if (StringUtils.isNotBlank(metacard.getSourceId())) {
      solrInputDocument.addField(SolrCache.METACARD_SOURCE_NAME, metacard.getSourceId());
      solrInputDocument.setField(
          SolrCache.METACARD_UNIQUE_ID_NAME, metacard.getSourceId() + metacard.getId());
      solrInputDocument.addField(SolrCache.METACARD_ID_NAME, metacard.getId());
    }

    return solrInputDocument;
  }

  private String getMetacardId(SolrDocument doc) {
    return doc.getFirstValue(SolrCache.METACARD_ID_NAME).toString();
  }

  private String getMetacardSource(SolrDocument doc) {
    return doc.getFirstValue(SolrCache.METACARD_SOURCE_NAME).toString();
  }
}
