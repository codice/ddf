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
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.ProcessingDetailsImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.source.SourceCache;
import ddf.catalog.source.SourceMonitor;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.source.solr.BaseSolrCatalogProvider;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Source used internally by the {@link CachingFederationStrategy} to encapsulate interaction with
 * the cache.
 */
public class SolrCacheSource implements SourceCache {
  private static final Logger LOGGER = LoggerFactory.getLogger(SolrCacheSource.class);

  private static final String DESCRIBABLE_PROPERTIES_FILE = "/describable.properties";

  private static Properties describableProperties = new Properties();

  private final SolrCache cache;

  static {
    try (InputStream propertiesStream =
        BaseSolrCatalogProvider.class.getResourceAsStream(DESCRIBABLE_PROPERTIES_FILE)) {
      describableProperties.load(propertiesStream);
    } catch (IOException e) {
      LOGGER.info("IO exception loading describable properties", e);
    }
  }

  SolrCacheSource(SolrCache cache) {
    this.cache = cache;
  }

  @Override
  public boolean isAvailable() {
    return true;
  }

  @Override
  public boolean isAvailable(SourceMonitor callback) {
    return true;
  }

  @Override
  public SourceResponse query(QueryRequest request) throws UnsupportedQueryException {
    final QueryResponseImpl queryResponse = new QueryResponseImpl(request);
    try {
      SourceResponse result = cache.query(request);
      queryResponse.setHits(result.getHits());
      queryResponse.setProperties(result.getProperties());
      queryResponse.addResults(result.getResults(), true);
    } catch (UnsupportedQueryException e) {
      queryResponse.getProcessingDetails().add(new ProcessingDetailsImpl(getId(), e));
      queryResponse.closeResultQueue();
    }
    return queryResponse;
  }

  @Override
  public Set<ContentType> getContentTypes() {
    return cache.getContentTypes();
  }

  @Override
  public String getVersion() {
    return describableProperties.getProperty("version");
  }

  @Override
  public String getId() {
    return getClass().getName();
  }

  @Override
  public String getTitle() {
    return describableProperties.getProperty("name");
  }

  @Override
  public String getDescription() {
    return describableProperties.getProperty("description");
  }

  @Override
  public String getOrganization() {
    return describableProperties.getProperty("organization");
  }
}
