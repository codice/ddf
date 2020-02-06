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
package ddf.catalog.operation.impl;

import ddf.catalog.data.Result;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceProcessingDetails;
import ddf.catalog.operation.SourceResponse;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The SourceResponseImpl represents a default implementation of the {@link SourceResponse}. */
public class SourceResponseImpl extends ResponseImpl<QueryRequest> implements SourceResponse {

  protected long hits;

  protected Set<SourceProcessingDetails> sourceProcessingDetails;

  ResponseImpl<QueryRequest> queryResponse;

  private List<Result> results;

  /**
   * Instantiates a new SourceResponseImpl with the original query request and results from the
   * query being executed.
   *
   * @param request the original request
   * @param results the results associated with the query
   */
  public SourceResponseImpl(QueryRequest request, List<Result> results) {
    this(request, null, results, results != null ? results.size() : 0, null);
  }

  /**
   * Instantiates a new SourceResponseImpl. Use when the total amount of hits is known.
   *
   * @param request the original request
   * @param results the hits associated with the query
   * @param totalHits the total results associated with the query.
   */
  public SourceResponseImpl(QueryRequest request, List<Result> results, Long totalHits) {
    this(request, null, results, totalHits != null ? totalHits : 0, null);
  }

  /**
   * Instantiates a new SourceResponseImpl with properties.
   *
   * @param request the original request
   * @param properties the properties associated with the operation
   * @param results the results associated with the query
   */
  public SourceResponseImpl(
      QueryRequest request, Map<String, Serializable> properties, List<Result> results) {
    this(request, properties, results, results != null ? results.size() : 0, null);
  }

  /**
   * Instantiates a new SourceResponseImpl with properties and the total number of hits
   *
   * @param request the original request
   * @param properties the properties associated with the operation
   * @param results the results associated with the query
   * @param totalHits the total hits
   */
  public SourceResponseImpl(
      QueryRequest request,
      Map<String, Serializable> properties,
      List<Result> results,
      long totalHits) {
    this(request, properties, results, totalHits, null);
  }

  /**
   * Instantiates a new SourceResponseImpl with properties, the total number of hits, and details.
   *
   * @param request the original request
   * @param properties the properties associated with the operation
   * @param results the results associated with the query
   * @param totalHits the number of distinct results from the query
   * @param sourceProcessingDetails the set of details which contain applicable warnings about how
   *     the source processed the query
   */
  public SourceResponseImpl(
      QueryRequest request,
      Map<String, Serializable> properties,
      List<Result> results,
      long totalHits,
      Set<SourceProcessingDetails> sourceProcessingDetails) {
    super(request, properties);
    queryResponse = new ResponseImpl<>(request, properties);
    this.results = results;
    this.hits = totalHits;
    this.sourceProcessingDetails =
        sourceProcessingDetails == null ? new HashSet<>() : sourceProcessingDetails;
  }

  /*
   * (non-Javadoc)
   *
   * @see ddf.catalog.operation.OperationImpl#containsPropertyName(java.lang.String)
   */
  public boolean containsPropertyName(String name) {
    return queryResponse.containsPropertyName(name);
  }

  /*
   * (non-Javadoc)
   *
   * @see ddf.catalog.operation.SourceResponse#getHits()
   */
  @Override
  public long getHits() {
    return hits;
  }

  /**
   * Sets the hits.
   *
   * @param hits the new hits
   */
  public void setHits(long hits) {
    this.hits = hits;
  }

  /*
   * (non-Javadoc)
   *
   * @see ddf.catalog.operation.SourceResponse#getResults()
   */
  @Override
  public List<Result> getResults() {
    return results;
  }

  /*
   * (non-Javadoc)
   *
   * @see ddf.catalog.operation.SourceResponse#getProcessingDetails()
   */
  @Override
  public Set<SourceProcessingDetails> getProcessingDetails() {
    return sourceProcessingDetails;
  }

  /**
   * Sets the warnings associated with the {@link Source}.
   *
   * @param warnings the new warnings associated with the {@link Source}.
   */
  public void setWarnings(List<String> warnings) {
    if (warnings != null && !warnings.isEmpty()) {
      sourceProcessingDetails = new HashSet<SourceProcessingDetails>();
      sourceProcessingDetails.add(new SourceProcessingDetailsImpl(warnings));
    }
  }
}
