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
package org.codice.ddf.catalog.solr.cache.impl;

import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.ProcessingDetailsImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.SourceMonitor;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.source.solr.SolrCatalogProviderImpl;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrCacheSource implements FederatedSource {

  private static final Logger LOGGER = LoggerFactory.getLogger(SolrCacheSource.class);

  private static final String DESCRIBABLE_PROPERTIES_FILE = "/describable.properties";

  private static final Properties DESCRIBABLE_PROPERTIES = new Properties();

  private final SolrCache cache;

  private final CacheQueryFactory cacheQueryFactory;

  public SolrCacheSource(SolrCache cache, CacheQueryFactory cacheQueryFactory) {

    Validate.notNull(cache, "Valid SolrCache required.");
    Validate.notNull(cacheQueryFactory, "Valid CacheQueryFactory required.");

    this.cache = cache;
    this.cacheQueryFactory = cacheQueryFactory;
  }

  static {
    try (InputStream propertiesStream =
        SolrCatalogProviderImpl.class.getResourceAsStream(DESCRIBABLE_PROPERTIES_FILE)) {
      DESCRIBABLE_PROPERTIES.load(propertiesStream);
    } catch (IOException e) {
      LOGGER.info("IO exception loading describable properties", e);
    }
  }

  @Override
  public ResourceResponse retrieveResource(URI uri, Map<String, Serializable> arguments)
      throws IOException, ResourceNotFoundException, ResourceNotSupportedException {
    throw new ResourceNotSupportedException(
        "Resource cannot be retrieved directly from the cache.");
  }

  @Override
  public Set<String> getSupportedSchemes() {
    return new HashSet<>();
  }

  @Override
  public Set<String> getOptions(Metacard metacard) {
    return new HashSet<>();
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
    if (request.isEnterprise()) {
      LOGGER.debug("Ignoring enterprise query to the cache.");
      return new QueryResponseImpl(request, new ArrayList<>(), true, 0);
    }
    final QueryResponseImpl queryResponse = new QueryResponseImpl(request);
    try {
      SourceResponse result =
          cache.query(cacheQueryFactory.getQueryRequestWithSourcesFilter(request));
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
    return DESCRIBABLE_PROPERTIES.getProperty("version");
  }

  @Override
  public String getId() {
    return "cache";
  }

  @Override
  public String getTitle() {
    return DESCRIBABLE_PROPERTIES.getProperty("name");
  }

  @Override
  public String getDescription() {
    return DESCRIBABLE_PROPERTIES.getProperty("description");
  }

  @Override
  public String getOrganization() {
    return DESCRIBABLE_PROPERTIES.getProperty("organization");
  }
}
