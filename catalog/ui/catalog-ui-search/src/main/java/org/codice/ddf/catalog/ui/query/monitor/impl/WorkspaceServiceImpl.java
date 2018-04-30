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
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.codice.ddf.catalog.ui.metacard.workspace.QueryMetacardImpl;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl;
import org.codice.ddf.catalog.ui.metacard.workspace.transformer.impl.WorkspaceTransformerImpl;
import org.codice.ddf.catalog.ui.query.monitor.api.FilterService;
import org.codice.ddf.catalog.ui.query.monitor.api.SecurityService;
import org.codice.ddf.catalog.ui.query.monitor.api.SubscriptionsPersistentStore;
import org.codice.ddf.catalog.ui.query.monitor.api.WorkspaceService;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentStore;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkspaceServiceImpl implements WorkspaceService {

  private static final Logger LOGGER = LoggerFactory.getLogger(WorkspaceServiceImpl.class);

  private static final String ID_FIELD = "id_txt";

  private static final int MAX_SUBSCRIPTIONS = 100;

  private final CatalogFramework catalogFramework;

  private final FilterBuilder filterBuilder;

  private final WorkspaceTransformerImpl workspaceTransformer;

  private final FilterService filterService;

  private final SecurityService securityService;

  private final PersistentStore persistentStore;

  /**
   * @param catalogFramework must be non-null
   * @param filterBuilder must be non-null
   * @param workspaceTransformer must be non-null
   * @param filterService must be non-null
   * @param securityService must be non-null
   * @param persistentStore must be non-null
   */
  public WorkspaceServiceImpl(
      CatalogFramework catalogFramework,
      FilterBuilder filterBuilder,
      WorkspaceTransformerImpl workspaceTransformer,
      FilterService filterService,
      SecurityService securityService,
      PersistentStore persistentStore) {
    notNull(catalogFramework, "catalogFramework must be non-null");
    notNull(filterBuilder, "filterBuilder must be non-null");
    notNull(workspaceTransformer, "workspaceTransformer must be non-null");
    notNull(filterService, "filterService must be non-null");
    notNull(securityService, "securityService must be non-null");
    notNull(persistentStore, "persistentStore must be non-null");

    this.catalogFramework = catalogFramework;
    this.filterBuilder = filterBuilder;
    this.workspaceTransformer = workspaceTransformer;
    this.filterService = filterService;
    this.securityService = securityService;
    this.persistentStore = persistentStore;
  }

  @Override
  public String toString() {
    return "WorkspaceServiceImpl{"
        + "securityService="
        + securityService
        + ", catalogFramework="
        + catalogFramework
        + ", filterBuilder="
        + filterBuilder
        + ", workspaceTransformer="
        + workspaceTransformer
        + ", filterService="
        + filterService
        + '}';
  }

  @Override
  public List<WorkspaceMetacardImpl> getWorkspaceMetacards() {

    final QueryRequest queryRequest = createQueryRequestForSubscribedToWorkspaces();

    if (queryRequest != null) {
      try {
        return createWorkspaceMetacards(catalogFramework.query(queryRequest));
      } catch (UnsupportedQueryException | FederationException | SourceUnavailableException e) {
        LOGGER.warn("Error querying for workspaces", e);
      }
    }

    return Collections.emptyList();
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

    final List<Filter> filters = createFilters(workspaceIds);

    if (!filters.isEmpty()) {

      final QueryRequest queryRequest = createQueryRequest(filters);

      try {
        return createWorkspaceMetacards(catalogFramework.query(queryRequest));
      } catch (UnsupportedQueryException | FederationException | SourceUnavailableException e) {
        LOGGER.warn("Error querying for workspaces: queryRequest={}", queryRequest, e);
      }
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
      ids = subscriptions.stream().map(s -> s.get(ID_FIELD).toString()).collect(Collectors.toSet());
    } catch (PersistenceException e) {
      LOGGER.warn("Failed to get subscriptions for workspaces: {}", e.getMessage());
      LOGGER.debug("Failed to get subscriptions for workspaces: {}", e.getMessage(), e);
    }
    if (ids.size() == 0) {
      return null;
    }

    return createQueryRequest(createFilters(ids));
  }

  /**
   * Create a query request for a list of filters where the filters are OR'ed together.
   *
   * @param filters filter list
   * @return query request
   */
  private QueryRequest createQueryRequest(List<Filter> filters) {
    return createQueryRequest(filterBuilder.anyOf(filters));
  }

  /**
   * Create a query request for a filter.
   *
   * @param filter the filter
   * @return query request
   */
  private QueryRequest createQueryRequest(Filter filter) {
    QueryImpl query = new QueryImpl(filter);
    query.setPageSize(MAX_SUBSCRIPTIONS);
    return new QueryRequestImpl(query, createProperties());
  }

  /**
   * Create the filters for getting workspace metacards based on workspace ids.
   *
   * @param workspaceIds set of workspace ids
   * @return list of filters
   */
  private List<Filter> createFilters(Set<String> workspaceIds) {
    return workspaceIds
        .stream()
        .map(
            id ->
                filterBuilder.allOf(
                    filterService.buildMetacardIdFilter(id),
                    filterService.buildWorkspaceTagFilter()))
        .collect(Collectors.toList());
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
