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
package org.codice.ddf.catalog.ui.query.monitor.impl;

import static org.apache.commons.lang3.Validate.notNull;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Result;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.util.impl.CatalogQueryException;
import ddf.catalog.util.impl.QueryFunction;
import ddf.catalog.util.impl.ResultIterable;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import javax.annotation.concurrent.ThreadSafe;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.codice.ddf.catalog.ui.metacard.workspace.QueryMetacardImpl;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl;
import org.codice.ddf.catalog.ui.metacard.workspace.transformer.impl.WorkspaceTransformerImpl;
import org.codice.ddf.catalog.ui.query.monitor.api.SecurityService;
import org.codice.ddf.catalog.ui.query.monitor.api.SubscriptionsPersistentStore;
import org.codice.ddf.catalog.ui.query.monitor.api.WorkspaceQueryBuilder;
import org.codice.ddf.catalog.ui.query.monitor.api.WorkspaceService;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentStore;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
public class WorkspaceServiceImpl implements WorkspaceService {

  private static final Logger LOGGER = LoggerFactory.getLogger(WorkspaceServiceImpl.class);

  private static final String ID_FIELD = "id_txt";

  private final CatalogFramework catalogFramework;

  private final WorkspaceTransformerImpl workspaceTransformer;

  private final SecurityService securityService;

  private final PersistentStore persistentStore;

  private final WorkspaceQueryBuilder workspaceQueryBuilder;

  /** Use {@code volatile} to make the class thread-safe. */
  private volatile int maxSubscriptions;

  /**
   * @param catalogFramework must be non-null
   * @param workspaceTransformer must be non-null
   * @param securityService must be non-null
   * @param persistentStore must be non-null
   */
  @SuppressWarnings("WeakerAccess" /* Needed by blueprint. */)
  public WorkspaceServiceImpl(
      CatalogFramework catalogFramework,
      WorkspaceTransformerImpl workspaceTransformer,
      WorkspaceQueryBuilder workspaceQueryBuilder,
      SecurityService securityService,
      PersistentStore persistentStore,
      int maxSubscriptions) {
    notNull(catalogFramework, "catalogFramework must be non-null");
    notNull(workspaceTransformer, "workspaceTransformer must be non-null");
    notNull(securityService, "securityService must be non-null");
    notNull(persistentStore, "persistentStore must be non-null");
    notNull(workspaceQueryBuilder, "workspaceQueryBuilder must be non-null");

    this.catalogFramework = catalogFramework;
    this.workspaceTransformer = workspaceTransformer;
    this.securityService = securityService;
    this.persistentStore = persistentStore;
    this.maxSubscriptions = maxSubscriptions;
    this.workspaceQueryBuilder = workspaceQueryBuilder;
  }

  @SuppressWarnings("unused" /* Needed by metatype. */)
  public void setMaxSubscriptions(int maxSubscriptions) {
    this.maxSubscriptions = maxSubscriptions;
  }

  @Override
  public String toString() {
    return String.format(
        "WorkspaceServiceImpl{securityService=%s, catalogFramework=%s, workspaceTransformer=%s, maxSubscriptions=%d}",
        securityService, catalogFramework, workspaceTransformer, maxSubscriptions);
  }

  @Override
  public List<WorkspaceMetacardImpl> getWorkspaceMetacards() {

    final QueryRequest queryRequest = createQueryRequestForSubscribedToWorkspaces();

    if (queryRequest != null) {
      try {
        return createWorkspaceMetacards(query(queryRequest));
      } catch (CatalogQueryException e) {
        LOGGER.warn("Error querying for workspaces", e);
      }
    }

    return Collections.emptyList();
  }

  private QueryResponse query(QueryRequest queryRequest) {
    AtomicLong hitCount = new AtomicLong(0);

    QueryFunction queryFunction =
        qr -> {
          SourceResponse sourceResponse = catalogFramework.query(qr);
          hitCount.compareAndSet(0, sourceResponse.getHits());
          return sourceResponse;
        };

    ResultIterable results =
        ResultIterable.resultIterable(queryFunction, queryRequest, maxSubscriptions);

    List<Result> resultList = results.stream().collect(Collectors.toList());

    long totalHits = hitCount.get();

    totalHits = totalHits != 0 ? totalHits : resultList.size();

    return new QueryResponseImpl(queryRequest, resultList, totalHits);
  }

  /** Get the metacards from the query response and convert them to workspace metacards. */
  private List<WorkspaceMetacardImpl> createWorkspaceMetacards(QueryResponse response) {
    return response
        .getResults()
        .stream()
        .map(Result::getMetacard)
        .filter(WorkspaceMetacardImpl::isWorkspaceMetacard)
        .map(WorkspaceMetacardImpl::from)
        .collect(Collectors.toList());
  }

  private Map<String, Serializable> createProperties() {
    return securityService.addSystemSubject(new HashMap<>());
  }

  @Override
  public List<QueryMetacardImpl> getQueryMetacards(WorkspaceMetacardImpl workspaceMetacard) {
    return workspaceMetacard
        .getQueries()
        .stream()
        .map(workspaceTransformer::xmlToMetacard)
        .map(QueryMetacardImpl::from)
        .collect(Collectors.toList());
  }

  @Override
  public List<WorkspaceMetacardImpl> getWorkspaceMetacards(Set<String> workspaceIds) {

    if (workspaceIds.isEmpty()) {
      return Collections.emptyList();
    }

    final Filter filter = workspaceQueryBuilder.createFilter(workspaceIds);

    final QueryRequest queryRequest = createQueryRequest(filter);

    try {
      return createWorkspaceMetacards(query(queryRequest));
    } catch (CatalogQueryException e) {
      LOGGER.warn("Error querying for workspaces: queryRequest={}", queryRequest, e);
    }

    return Collections.emptyList();
  }

  /**
   * Create a query request to find all workspace metacards.
   *
   * @return query request
   */
  private QueryRequest createQueryRequestForSubscribedToWorkspaces() {
    Set<String> ids = new HashSet<>();
    try {
      List<Map<String, Object>> subscriptions =
          persistentStore.get(SubscriptionsPersistentStore.SUBSCRIPTIONS_TYPE);
      ids =
          subscriptions
              .stream()
              .map(subscription -> subscription.get(ID_FIELD))
              .filter(Objects::nonNull)
              .map(Object::toString)
              .collect(Collectors.toSet());
    } catch (PersistenceException e) {
      LOGGER.warn("Failed to get subscriptions for workspaces: {}", e.getMessage());
      LOGGER.debug("Failed to get subscriptions for workspaces: {}", e.getMessage(), e);
    }
    if (ids.isEmpty()) {
      return null;
    }

    return createQueryRequest(workspaceQueryBuilder.createFilter(ids));
  }

  /**
   * Create a query request for a filter.
   *
   * @param filter the filter
   * @return query request
   */
  private QueryRequest createQueryRequest(Filter filter) {
    QueryImpl query = new QueryImpl(filter);
    query.setPageSize(maxSubscriptions);
    return new QueryRequestImpl(query, createProperties());
  }

  @Override
  public Map<String, Pair<WorkspaceMetacardImpl, List<QueryMetacardImpl>>> getQueryMetacards() {
    Map<String, Pair<WorkspaceMetacardImpl, List<QueryMetacardImpl>>> queryMetacards =
        new HashMap<>();
    for (WorkspaceMetacardImpl workspaceMetacard : getWorkspaceMetacards()) {
      queryMetacards.put(
          workspaceMetacard.getId(),
          new ImmutablePair<>(workspaceMetacard, getQueryMetacards(workspaceMetacard)));
    }
    return queryMetacards;
  }

  @Override
  public WorkspaceMetacardImpl getWorkspaceMetacard(String workspaceId) {
    return getWorkspaceMetacards(Collections.singleton(workspaceId)).get(0);
  }
}
