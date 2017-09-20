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
package org.codice.ddf.registry.federationadmin.service.impl;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.impl.PropertyNameImpl;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.security.SecurityConstants;
import java.io.Serializable;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.codice.ddf.registry.api.internal.RegistryStore;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.common.metacard.RegistryUtility;
import org.codice.ddf.registry.federationadmin.service.internal.FederationAdminException;
import org.codice.ddf.registry.federationadmin.service.internal.FederationAdminService;
import org.codice.ddf.security.common.Security;
import org.geotools.filter.SortByImpl;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Though refreshRegistrySubscriptions in this class is a public method, it should never be called
 * by any class with external connections. This class is only meant to be accessed through a camel
 * route and should avoid elevating privileges for any other service or being exposed to any other
 * endpoint.
 */
public class RefreshRegistryEntries {
  private static final Logger LOGGER = LoggerFactory.getLogger(RefreshRegistryEntries.class);

  private static final int PAGE_SIZE = 1000;

  private static final int SHUTDOWN_TIMEOUT_SECONDS = 60;

  private List<RegistryStore> registryStores;

  private FederationAdminService federationAdminService;

  private FilterBuilder filterBuilder;

  private boolean enableDelete = true;

  private ScheduledExecutorService executor;

  private int taskWaitTimeSeconds = 30;

  private int refreshIntervalSeconds = 30;

  private Future scheduledTask;

  private Security security;

  public RefreshRegistryEntries() {
    this.security = Security.getInstance();
  }

  public RefreshRegistryEntries(Security security) {
    this.security = security;
  }

  public void init() {
    scheduledTask =
        executor.scheduleWithFixedDelay(
            () -> {
              try {
                refreshRegistryEntries();
              } catch (FederationAdminException e) {
                LOGGER.error("Problem refreshing registry entries.", e);
              }
            },
            10,
            refreshIntervalSeconds,
            TimeUnit.SECONDS);
  }

  /**
   * Do not call refreshRegistrySubscriptions directly! It is a public method, but is only meant to
   * be accessed through a camel route and should avoid elevating privileges for any other service
   * or being exposed to any other endpoint.
   *
   * @throws FederationAdminException
   */
  public void refreshRegistryEntries() throws FederationAdminException {

    if (!registriesAvailable()) {
      return;
    }

    RemoteRegistryResults remoteResults = getRemoteRegistryMetacardsMap();
    Map<String, Metacard> remoteRegistryMetacardsMap = remoteResults.getRemoteRegistryMetacards();
    Map<String, Metacard> registryMetacardsMap = getRegistryMetacardsMap();

    if (LOGGER.isDebugEnabled() && !remoteResults.getFailureList().isEmpty()) {
      LOGGER.debug(
          "Failed to query the following registry stores: {}",
          String.join(",", remoteResults.getFailureList()));
    }

    List<Metacard> remoteMetacardsToUpdate = new ArrayList<>();
    List<Metacard> remoteMetacardsToCreate = new ArrayList<>();
    List<Metacard> remoteMetacardsToDelete = new ArrayList<>();

    // Maps registry-id to a list of metacards that came from that registry
    Map<String, List<Metacard>> remoteRegistryToMetacardMap =
        getMetacardRegistryIdMap(remoteRegistryMetacardsMap.values());
    Map<String, List<Metacard>> localRegistryToMetacardMap =
        getMetacardRegistryIdMap(registryMetacardsMap.values());

    // Loop through all the registry-ids of the stores queried and find any local internal
    // representation metacards that need to be deleted
    for (String regId : remoteResults.getRegistryStoresQueried()) {
      LOGGER.trace("Checking for registry entries from {} that need to be removed", regId);
      // If we failed to query a store or we don't have any metacards from the store locally
      // skip to the next store since we can't determine if we need to delete anything
      if (!localRegistryToMetacardMap.containsKey(regId)
          || remoteResults.getFailureList().contains(regId)) {
        LOGGER.trace(
            "No local registry entries for {} or there was a failure querying the remote registry",
            regId);
        continue;
      }

      if (remoteRegistryToMetacardMap.containsKey(regId)) {
        // delete all metacards that are found locally that came from the given
        // registry that no longer exist on the remote registry
        remoteMetacardsToDelete.addAll(
            localRegistryToMetacardMap
                .get(regId)
                .stream()
                .filter(e -> shouldDelete(e, remoteRegistryToMetacardMap.get(regId)))
                .collect(Collectors.toList()));
      } else {
        // remote registry is returning no results so delete all local metacards from that registry
        LOGGER.trace(
            "Remote registry {} returned 0 registry entries. Scheduling all local entries from this registry for removal.",
            regId);
        remoteMetacardsToDelete.addAll(localRegistryToMetacardMap.get(regId));
      }
    }

    // Loop through all the remote entries returned and determine if they need to be
    // created or updated locally
    for (Map.Entry<String, Metacard> remoteEntry : remoteRegistryMetacardsMap.entrySet()) {
      if (registryMetacardsMap.containsKey(remoteEntry.getKey())) {
        Metacard existingMetacard = registryMetacardsMap.get(remoteEntry.getKey());
        // If it isn't a local node and it is newer that our current internal representation update
        // it
        if (!RegistryUtility.isLocalNode(existingMetacard)
            && remoteEntry.getValue().getModifiedDate().after(existingMetacard.getModifiedDate())) {
          remoteMetacardsToUpdate.add(remoteEntry.getValue());
          LOGGER.trace(
              "Scheduling update for {}:{}",
              remoteEntry.getValue().getTitle(),
              remoteEntry.getKey());
        } else {
          LOGGER.trace(
              "No update scheduled for {}:{} because a newer version exists locally.",
              remoteEntry.getValue().getTitle(),
              remoteEntry.getKey());
        }
      } else {
        // add any metacards we don't currently have
        remoteMetacardsToCreate.add(remoteEntry.getValue());
        LOGGER.trace(
            "Scheduling creation for {}:{}",
            remoteEntry.getValue().getTitle(),
            remoteEntry.getKey());
      }
    }

    if (CollectionUtils.isNotEmpty(remoteMetacardsToUpdate)) {
      LOGGER.debug(
          "Registry subscriptions found {} registry entries that need to be updated",
          remoteMetacardsToUpdate.size());
      logRegistryMetacards("Registry metacards to be updated", remoteMetacardsToUpdate);
      writeRemoteUpdates(remoteMetacardsToUpdate);
    }
    if (CollectionUtils.isNotEmpty(remoteMetacardsToCreate)) {
      LOGGER.debug(
          "Registry subscriptions found {} registry entries that need to be created",
          remoteMetacardsToCreate.size());
      logRegistryMetacards("Registry metacards to be created", remoteMetacardsToCreate);
      createRemoteEntries(remoteMetacardsToCreate);
    }
    if (enableDelete && !remoteMetacardsToDelete.isEmpty()) {
      LOGGER.debug(
          "Registry subscriptions found {} registry entries that need to be deleted",
          remoteMetacardsToDelete.size());
      logRegistryMetacards("Registry metacards to be deleted", remoteMetacardsToDelete);
      deleteRemoteEntries(remoteMetacardsToDelete);
    }
  }

  private boolean shouldDelete(Metacard local, List<Metacard> remoteMetacards) {
    String id =
        RegistryUtility.getStringAttribute(
            local, RegistryObjectMetacardType.REMOTE_METACARD_ID, "");
    boolean delete =
        !remoteMetacards.stream().filter(e -> e.getId().equals(id)).findFirst().isPresent();
    if (delete) {
      LOGGER.trace(
          "{}: {} is no longer available from {}. Scheduling it for deletion",
          local.getTitle(),
          RegistryUtility.getRegistryId(local),
          id);
    }
    return delete;
  }

  private boolean registriesAvailable() {
    return registryStores.stream().filter(RegistryStore::isAvailable).findFirst().isPresent();
  }

  private Map<String, List<Metacard>> getMetacardRegistryIdMap(Collection<Metacard> metacards) {
    Map<String, List<Metacard>> map = new HashMap<>();
    for (Metacard mcard : metacards) {
      String regId =
          RegistryUtility.getStringAttribute(
              mcard, RegistryObjectMetacardType.REMOTE_REGISTRY_ID, "");
      map.computeIfAbsent(regId, k -> new ArrayList<>());
      map.get(regId).add(mcard);
    }
    return map;
  }

  /**
   * @return a map of local registry metacards mapped by there remote metacard-id
   * @throws FederationAdminException
   */
  private Map<String, Metacard> getRegistryMetacardsMap() throws FederationAdminException {
    try {
      List<Metacard> registryMetacards =
          security.runAsAdminWithException(
              () -> federationAdminService.getInternalRegistryMetacards());

      return registryMetacards
          .stream()
          .collect(
              Collectors.toMap(
                  e ->
                      RegistryUtility.getStringAttribute(
                          e, RegistryObjectMetacardType.REMOTE_METACARD_ID, null),
                  Function.identity()));
    } catch (Exception e) {
      throw new FederationAdminException("Error querying for internal registry metacards ", e);
    }
  }

  /** Directly queries the stores without going through the catalog framework */
  private RemoteRegistryResults getRemoteRegistryMetacardsMap() throws FederationAdminException {
    Map<String, Metacard> remoteRegistryMetacards = new HashMap<>();
    List<String> failedQueries = new ArrayList<>();
    List<String> storesQueried = new ArrayList<>();
    List<String> localMetacardRegIds = getLocalRegistryIds();

    Map<String, Serializable> queryProps = new HashMap<>();
    queryProps.put(
        SecurityConstants.SECURITY_SUBJECT, security.runAsAdmin(() -> security.getSystemSubject()));

    LOGGER.debug("Querying {} remote registries", registryStores.size());
    // Create the remote query task to be run.
    List<Callable<RemoteResult>> tasks = new ArrayList<>();
    for (RegistryStore store : registryStores) {
      if (!store.isPullAllowed() || !store.isAvailable()) {
        LOGGER.debug(
            "Skipping store {} because pull is disabled or it is unavailable", store.getId());
        continue;
      }
      storesQueried.add(store.getRegistryId());
      tasks.add(
          () -> {
            SourceResponse response =
                store.query(new QueryRequestImpl(getBasicRegistryQuery(), queryProps));
            Map<String, Metacard> results =
                response
                    .getResults()
                    .stream()
                    .map(Result::getMetacard)
                    .filter(e -> !localMetacardRegIds.contains(RegistryUtility.getRegistryId(e)))
                    .collect(Collectors.toMap(Metacard::getId, Function.identity()));
            LOGGER.debug(
                "Retrieved {} registry entries from {} with {} local entries filtered.",
                results.size(),
                store.getId(),
                response.getResults().size());
            logRegistryMetacards("Filtered remote entries", results.values());
            return new RemoteResult(store.getRegistryId(), results);
          });
    }

    failedQueries.addAll(storesQueried);
    List<RemoteResult> results = executeTasks(tasks);
    results
        .stream()
        .forEach(
            result -> {
              failedQueries.remove(result.getRegistryId());
              remoteRegistryMetacards.putAll(result.getRemoteRegistryMetacards());
            });

    return new RemoteRegistryResults(remoteRegistryMetacards, failedQueries, storesQueried);
  }

  private List<RemoteResult> executeTasks(List<Callable<RemoteResult>> tasks) {
    List<RemoteResult> results = new ArrayList<>();
    try {
      List<Future<RemoteResult>> futures = executor.invokeAll(tasks);
      for (Future<RemoteResult> future : futures) {
        try {
          results.add(future.get(taskWaitTimeSeconds, TimeUnit.SECONDS));
        } catch (ExecutionException e) {
          LOGGER.warn(
              "Error executing query on a remote registry. Verify network connection to remote host is available. If connection is good, verify this instance has permission to access the registry.",
              e);
        } catch (TimeoutException e) {
          LOGGER.warn(
              "Timeout occurred when querying a remote registry. Verify network connection to remote host is available.");
        }
      }
    } catch (InterruptedException e) {
      LOGGER.debug("Remote registry queries interrupted", e);
    }
    return results;
  }

  private List<String> getLocalRegistryIds() throws FederationAdminException {
    try {
      List<Metacard> localMetacards =
          security.runAsAdminWithException(
              () -> federationAdminService.getLocalRegistryMetacards());
      return localMetacards
          .stream()
          .map(e -> RegistryUtility.getRegistryId(e))
          .collect(Collectors.toList());
    } catch (Exception e) {
      throw new FederationAdminException("Error querying for local metacards ", e);
    }
  }

  private void writeRemoteUpdates(List<Metacard> remoteMetacardsToUpdate)
      throws FederationAdminException {
    try {
      security.runAsAdminWithException(
          () -> {
            for (Metacard m : remoteMetacardsToUpdate) {
              federationAdminService.updateRegistryEntry(m);
            }
            return null;
          });
    } catch (PrivilegedActionException e) {
      String message = "Error writing remote updates.";
      LOGGER.debug("{} Metacard IDs: {}", message, remoteMetacardsToUpdate);
      throw new FederationAdminException(message, e);
    }
  }

  private void createRemoteEntries(List<Metacard> remoteMetacardsToCreate)
      throws FederationAdminException {
    try {
      security.runAsAdminWithException(
          () -> federationAdminService.addRegistryEntries(remoteMetacardsToCreate, null));
    } catch (PrivilegedActionException e) {
      throw new FederationAdminException("Error creating remote entries", e);
    }
  }

  private void deleteRemoteEntries(List<Metacard> remoteMetacardsToDelete)
      throws FederationAdminException {
    try {
      security.runAsAdminWithException(
          () -> {
            federationAdminService.deleteRegistryEntriesByMetacardIds(
                remoteMetacardsToDelete.stream().map(Metacard::getId).collect(Collectors.toList()));
            return null;
          });
    } catch (PrivilegedActionException e) {
      String message = "Error deleting remote entries.";
      LOGGER.debug("{} Metacard IDs: {}", message, remoteMetacardsToDelete);
      throw new FederationAdminException(message, e);
    }
  }

  private Query getBasicRegistryQuery() {
    List<Filter> filters = new ArrayList<>();
    filters.add(
        filterBuilder.attribute(Metacard.TAGS).is().equalTo().text(RegistryConstants.REGISTRY_TAG));

    PropertyName propertyName = new PropertyNameImpl(Metacard.MODIFIED);
    SortBy sortBy = new SortByImpl(propertyName, SortOrder.ASCENDING);
    QueryImpl query = new QueryImpl(filterBuilder.allOf(filters));
    query.setSortBy(sortBy);
    query.setPageSize(PAGE_SIZE);

    return query;
  }

  public void setFilterBuilder(FilterBuilder filterBuilder) {
    this.filterBuilder = filterBuilder;
  }

  public void setRegistryStores(List<RegistryStore> registryStores) {
    this.registryStores = registryStores;
  }

  public void setFederationAdminService(FederationAdminService federationAdminService) {
    this.federationAdminService = federationAdminService;
  }

  public void setEnableDelete(boolean enableDelete) {
    this.enableDelete = enableDelete;
  }

  public void setExecutor(ScheduledExecutorService executor) {
    this.executor = executor;
  }

  public void setRefreshIntervalSeconds(int refreshIntervalSeconds) {
    if (this.refreshIntervalSeconds == refreshIntervalSeconds) {
      return;
    }
    this.refreshIntervalSeconds = refreshIntervalSeconds;
    if (scheduledTask != null) {
      this.scheduledTask.cancel(false);
      this.init();
    }
  }

  public void setTaskWaitTimeSeconds(Integer taskWaitTimeSeconds) {
    this.taskWaitTimeSeconds = taskWaitTimeSeconds;
  }

  public void destroy() {
    executor.shutdown();
    try {
      if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        executor.shutdownNow();
        if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
          LOGGER.error("Thread pool didn't terminate");
        }
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
    }
  }

  private void logRegistryMetacards(String message, Collection<Metacard> mcards) {
    if (LOGGER.isDebugEnabled()) {
      StringBuilder buffer = new StringBuilder();
      buffer.append(System.lineSeparator());
      for (Metacard mcard : mcards) {
        buffer.append(
            String.format(
                "%s: %s%s",
                mcard.getTitle(), RegistryUtility.getRegistryId(mcard), System.lineSeparator()));
      }
      LOGGER.debug("{}: {}", message, buffer.toString());
    }
  }

  /** Container for results from a single remote registry */
  static class RemoteResult {

    private String registryId;

    private Map<String, Metacard> remoteRegistryMetacards;

    public RemoteResult(String registryId, Map<String, Metacard> remoteRegistryMetacards) {
      this.registryId = registryId;
      this.remoteRegistryMetacards = remoteRegistryMetacards;
    }

    public String getRegistryId() {
      return registryId;
    }

    /** @return a map of remote registry metacards mapped by there metacard-id */
    public Map<String, Metacard> getRemoteRegistryMetacards() {
      return remoteRegistryMetacards;
    }
  }

  /** Container for all remote registry query results. */
  private static class RemoteRegistryResults {

    private Map<String, Metacard> remoteRegistryMetacards;

    private List<String> registryStoresQueried;

    private List<String> failureList;

    public RemoteRegistryResults(
        Map<String, Metacard> remoteRegistryMetacards,
        List<String> failureList,
        List<String> storesQueried) {
      this.remoteRegistryMetacards = remoteRegistryMetacards;
      this.failureList = failureList;
      this.registryStoresQueried = storesQueried;
    }

    /** @return a map of remote registry metacards mapped by there metacard-id */
    public Map<String, Metacard> getRemoteRegistryMetacards() {
      return new HashMap<>(remoteRegistryMetacards);
    }

    public List<String> getRegistryStoresQueried() {
      return new ArrayList<>(registryStoresQueried);
    }

    /** @return a list of stores that were queried but the query failed */
    public List<String> getFailureList() {
      return new ArrayList<>(failureList);
    }
  }
}
