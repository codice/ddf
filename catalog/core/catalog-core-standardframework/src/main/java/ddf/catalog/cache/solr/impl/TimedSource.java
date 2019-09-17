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
import ddf.catalog.source.Source;
import ddf.catalog.source.SourceMonitor;
import ddf.catalog.source.UnsupportedQueryException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimedSource implements Source {

  private static final Logger LOGGER = LoggerFactory.getLogger(TimedSource.class);

  /**
   * Prefix that defines the API of the property key to get source latency. Consider backwards
   * compatibility and impact to plugins and endpoints that might use this property before changing
   * this constant.
   */
  private static final String METRICS_SOURCE_ELAPSED_PREFIX_API = "metrics.source.elapsed.";

  private final Source source;

  public TimedSource(Source originalSource) {
    source = originalSource;
  }

  @Override
  public boolean isAvailable() {
    return source.isAvailable();
  }

  @Override
  public boolean isAvailable(SourceMonitor callback) {
    return source.isAvailable(callback);
  }

  @Override
  public SourceResponse query(QueryRequest request) throws UnsupportedQueryException {
    long startTime = System.currentTimeMillis();
    SourceResponse result = source.query(request);
    long endTime = System.currentTimeMillis();

    int elapsedTime = Math.toIntExact(endTime - startTime);
    String sourceLatencyMetricKey = METRICS_SOURCE_ELAPSED_PREFIX_API + source.getId();

    result.getProperties().put(sourceLatencyMetricKey, elapsedTime);
    LOGGER.trace("Query latency for source [{}] was {}ms.", source.getId(), elapsedTime);

    return result;
  }

  @Override
  public Set<ContentType> getContentTypes() {
    return source.getContentTypes();
  }

  @Override
  public String getVersion() {
    return source.getVersion();
  }

  @Override
  public String getId() {
    return source.getId();
  }

  @Override
  public String getTitle() {
    return source.getTitle();
  }

  @Override
  public String getDescription() {
    return source.getDescription();
  }

  @Override
  public String getOrganization() {
    return source.getOrganization();
  }
}
