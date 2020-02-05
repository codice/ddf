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
package org.codice.ddf.catalog.ui.util;

import com.google.common.base.Stopwatch;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ddf.action.ActionRegistry;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Result;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.util.impl.QueryFunction;
import ddf.catalog.util.impl.ResultIterable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.codice.ddf.catalog.ui.query.cql.CqlQueryResponseImpl;
import org.codice.ddf.catalog.ui.query.cql.CqlRequestImpl;
import org.codice.ddf.catalog.ui.query.utility.CqlQueries;
import org.codice.ddf.catalog.ui.query.utility.CqlQueryResponse;
import org.codice.ddf.catalog.ui.query.utility.CqlRequest;
import org.codice.ddf.catalog.ui.transformer.TransformerDescriptors;
import org.codice.gsonsupport.GsonTypeAdapters;

public class CqlQueriesImpl implements CqlQueries {

  private final CatalogFramework catalogFramework;

  private final FilterBuilder filterBuilder;

  private TransformerDescriptors descriptors;

  private ActionRegistry actionRegistry;

  private FilterAdapter filterAdapter;

  private static final Gson GSON =
      new GsonBuilder()
          .disableHtmlEscaping()
          .serializeNulls()
          .registerTypeAdapterFactory(GsonTypeAdapters.LongDoubleTypeAdapter.FACTORY)
          .registerTypeAdapter(Date.class, new GsonTypeAdapters.DateLongFormatTypeAdapter())
          .create();

  public CqlQueriesImpl(
      CatalogFramework catalogFramework,
      FilterBuilder filterBuilder,
      FilterAdapter filterAdapter,
      ActionRegistry actionRegistry) {
    this.catalogFramework = catalogFramework;
    this.filterBuilder = filterBuilder;
    this.filterAdapter = filterAdapter;
    this.actionRegistry = actionRegistry;
  }

  @Override
  public CqlQueryResponse executeCqlQuery(CqlRequest cqlRequest)
      throws UnsupportedQueryException, SourceUnavailableException, FederationException {
    QueryRequest request = cqlRequest.createQueryRequest(catalogFramework.getId(), filterBuilder);
    Stopwatch stopwatch = Stopwatch.createStarted();

    List<QueryResponse> responses = Collections.synchronizedList(new ArrayList<>());

    List<Result> results;
    if (cqlRequest.getCount() == 0) {
      results = retrieveHitCount(request, responses);
    } else {
      results = retrieveResults(cqlRequest, request, responses);
    }

    QueryResponse aggregatedResponse =
        new QueryResponseImpl(
            request,
            results,
            true,
            responses
                .stream()
                .filter(Objects::nonNull)
                .map(QueryResponse::getHits)
                .findFirst()
                .orElse(-1L),
            responses
                .stream()
                .filter(Objects::nonNull)
                .map(QueryResponse::getProperties)
                .findFirst()
                .orElse(Collections.emptyMap()));

    responses
        .stream()
        .map(QueryResponse::getProcessingDetails)
        .forEach(aggregatedResponse.getProcessingDetails()::addAll);

    stopwatch.stop();

    return new CqlQueryResponseImpl(
        cqlRequest.getId(),
        request,
        aggregatedResponse,
        cqlRequest.getSourceResponseString(),
        stopwatch.elapsed(TimeUnit.MILLISECONDS),
        cqlRequest.isNormalize(),
        filterAdapter,
        actionRegistry,
        descriptors);
  }

  private List<Result> retrieveHitCount(QueryRequest request, List<QueryResponse> responses)
      throws UnsupportedQueryException, SourceUnavailableException, FederationException {
    QueryResponse queryResponse = catalogFramework.query(request);
    responses.add(queryResponse);
    return queryResponse.getResults();
  }

  private List<Result> retrieveResults(
      CqlRequest cqlRequest, QueryRequest request, List<QueryResponse> responses) {
    QueryFunction queryFunction =
        (queryRequest) -> {
          QueryResponse queryResponse = catalogFramework.query(queryRequest);
          responses.add(queryResponse);
          return queryResponse;
        };
    return ResultIterable.resultIterable(queryFunction, request, cqlRequest.getCount())
        .stream()
        .collect(Collectors.toList());
  }

  @SuppressWarnings("WeakerAccess" /* setter must be public for blueprint access */)
  public void setDescriptors(TransformerDescriptors descriptors) {
    this.descriptors = descriptors;
  }

  @Override
  public CqlRequest getCqlRequestFromJson(String jsonBody) {
    return GSON.fromJson(jsonBody, CqlRequestImpl.class);
  }
}
