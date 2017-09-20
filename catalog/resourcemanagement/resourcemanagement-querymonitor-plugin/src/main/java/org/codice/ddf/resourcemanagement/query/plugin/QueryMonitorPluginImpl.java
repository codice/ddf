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
package org.codice.ddf.resourcemanagement.query.plugin;

import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.ProcessingDetailsImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.source.Source;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryMonitorPluginImpl implements QueryMonitorPlugin {

  private boolean removeSearchAfterComplete = true;

  private ConcurrentHashMap<UUID, ActiveSearch> activeSearches = new ConcurrentHashMap<>();

  private static final Logger LOGGER = LoggerFactory.getLogger(QueryMonitorPluginImpl.class);

  public static final String SEARCH_ID = "SEARCH_ID";

  /** {@inheritDoc} */
  @Override
  public Map<UUID, ActiveSearch> getActiveSearches() {
    return activeSearches;
  }

  /** {@inheritDoc} */
  public void setRemoveSearchAfterComplete(boolean removeSearchAfterComplete) {
    this.removeSearchAfterComplete = removeSearchAfterComplete;
  }

  /** {@inheritDoc} */
  @Override
  public boolean addActiveSearch(ActiveSearch activeSearch) {
    if (activeSearch == null) {
      LOGGER.debug("Cannot add null ActiveSearch to map.");
      return false;
    }
    activeSearches.put(activeSearch.getUniqueID(), activeSearch);
    return true;
  }

  /** {@inheritDoc} */
  @Override
  public boolean removeActiveSearch(UUID uniqueID) {
    if (uniqueID == null) {
      LOGGER.debug("Can't remove active search with null ID.");
      return false;
    }
    return (activeSearches.remove(uniqueID) != null);
  }

  /**
   * Method that is implemented for {@link PreFederatedQueryPlugin}. Uses the given {@link Source}
   * and {@link QueryRequest} information to create a new {@link ActiveSearch} to add to the {@link
   * ActiveSearch} {@link Map}.
   *
   * @param source {@link Source} that corresponds to source the search is querying
   * @param input {@link QueryRequest} that corresponds to request generated when a user queried the
   *     source
   * @return {@link QueryRequest} that was given as a parameter with updated property information
   *     corresponding to the {@link ActiveSearch}'s {@link UUID}
   */
  @Override
  public QueryRequest process(Source source, QueryRequest input)
      throws PluginExecutionException, StopProcessingException {
    if (source == null) {
      LOGGER.debug("Source given was null.");
    }
    if (input == null) {
      LOGGER.debug("QueryRequest in process was null. Cannot add active search to map.");
    } else {
      ActiveSearch tempAS = new ActiveSearch(source, input);
      UUID uniqueID = tempAS.getUniqueID();
      input.getProperties().put(SEARCH_ID, uniqueID);
      addActiveSearch(tempAS);
    }

    return input;
  }

  /**
   * Method that is implemented for {@link PostFederatedQueryPlugin}. Uses the given {@link
   * QueryResponse} information to remove the {@link ActiveSearch} from the {@link ActiveSearch}
   * {@link Map}.
   *
   * @param input {@link QueryResponse} that corresponds to response from the source that was
   *     queried by the user's original {@link QueryRequest}
   * @return {@link QueryResponse} that was given as a parameter
   */
  @Override
  public QueryResponse process(QueryResponse input)
      throws PluginExecutionException, StopProcessingException {

    if (!removeSearchAfterComplete) {
      LOGGER.debug(
          "Not removing active search from map due to catalog:removeSearchAfterComplete false. To enable removing searches as searches finish, use command catalog:removesearchaftercomplete true.");
      return input;
    }
    if (input == null) {
      LOGGER.debug(
          "Cannot remove ActiveSearch from the ActiveSearch Map. QueryResponse received in QueryMonitorPluginImpl was null.");
      return null;
    }
    if (!removeActiveSearch((UUID) input.getRequest().getPropertyValue(SEARCH_ID))) {
      QueryResponseImpl queryResponse =
          new QueryResponseImpl(input.getRequest(), new ArrayList<>(), 0);
      queryResponse.closeResultQueue();
      Set<ProcessingDetails> processingDetails =
          Collections.singleton(
              new ProcessingDetailsImpl(
                  QueryMonitorPlugin.class.getCanonicalName(),
                  new StopProcessingException("Query was cancelled by administrator")));
      queryResponse.setProcessingDetails(processingDetails);
      return queryResponse;
    }

    return input;
  }
}
