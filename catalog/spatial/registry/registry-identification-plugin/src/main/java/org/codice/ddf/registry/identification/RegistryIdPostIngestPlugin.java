/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.registry.identification;

import java.security.PrivilegedActionException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.common.metacard.RegistryUtility;
import org.codice.ddf.security.common.Security;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostIngestPlugin;
import ddf.catalog.util.impl.Requests;

/**
 * The RegistryIdPostIngestPlugin collects a set of registry-ids that are currently in the system
 * and makes them available to the IdentificationPlugin for duplication checking.
 * <p>
 * Initially the catalog is queried to initialize the set of registry-ids. After that the list is
 * maintained by the process methods for create and delete responses.
 */
public class RegistryIdPostIngestPlugin implements PostIngestPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryIdPostIngestPlugin.class);

    private static final int RETRY_INTERVAL = 30;

    private static final int SHUTDOWN_TIMEOUT_SECONDS = 60;

    private static final int PAGE_SIZE = 1000;

    private ScheduledExecutorService executorService;

    private CatalogFramework catalogFramework;

    private FilterBuilder filterBuilder;

    private Set<String> registryIds = ConcurrentHashMap.newKeySet();

    private Set<String> localRegistryIds = ConcurrentHashMap.newKeySet();

    private Set<String> remoteMetacardIds = ConcurrentHashMap.newKeySet();

    private Security security;

    public RegistryIdPostIngestPlugin() {
        security = Security.getInstance();
    }

    public RegistryIdPostIngestPlugin(Security security) {
        this.security = security;
    }

    public Set<String> getRegistryIds() {
        return Collections.unmodifiableSet(registryIds);
    }

    public Set<String> getRemoteMetacardIds() {
        return remoteMetacardIds;
    }

    public Set<String> getLocalRegistryIds() {
        return localRegistryIds;
    }

    @Override
    public CreateResponse process(CreateResponse input) throws PluginExecutionException {
        if (input != null && input.getCreatedMetacards() != null
                && Requests.isLocal(input.getRequest())) {
            addIdsFromMetacards(input.getCreatedMetacards());
        }
        return input;
    }

    @Override
    public UpdateResponse process(UpdateResponse input) throws PluginExecutionException {
        return input;
    }

    @Override
    public DeleteResponse process(DeleteResponse input) throws PluginExecutionException {
        if (input != null && input.getDeletedMetacards() != null
                && Requests.isLocal(input.getRequest())) {
            removeIdsForMetacards(input.getDeletedMetacards());
        }
        return input;
    }

    /**
     * Init method initializes the id sets from the catalog. If the catalog is not available it
     * will retry later.
     */
    public void init() {
        try {
            List<Metacard> registryMetacards;
            Filter registryFilter = filterBuilder.anyOf(filterBuilder.attribute(Metacard.TAGS)
                            .is()
                            .equalTo()
                            .text(RegistryConstants.REGISTRY_TAG),
                    filterBuilder.attribute(Metacard.TAGS)
                            .is()
                            .equalTo()
                            .text(RegistryConstants.REGISTRY_TAG_INTERNAL));
            QueryImpl query = new QueryImpl(registryFilter);
            query.setPageSize(PAGE_SIZE);
            QueryRequest request = new QueryRequestImpl(query);

            QueryResponse response =
                    security.runAsAdminWithException(() -> security.runWithSubjectOrElevate(() -> catalogFramework.query(
                            request)));

            if (response == null) {
                throw new PluginExecutionException(
                        "Failed to initialize RegistryIdPostIngestPlugin. Query for registry metacards came back null");
            }

            registryMetacards = response.getResults()
                    .stream()
                    .map(Result::getMetacard)
                    .collect(Collectors.toList());
            addIdsFromMetacards(registryMetacards);
        } catch (PrivilegedActionException | PluginExecutionException e) {
            LOGGER.debug("Error getting registry metacards. Will try again later");
            executorService.schedule(this::init, RETRY_INTERVAL, TimeUnit.SECONDS);
        }
    }

    public void destroy() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    LOGGER.debug("Thread pool didn't terminate");
                }
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }

    public void setFilterBuilder(FilterBuilder filterBuilder) {
        this.filterBuilder = filterBuilder;
    }

    public void setCatalogFramework(CatalogFramework framework) {
        this.catalogFramework = framework;
    }

    public void setExecutorService(ScheduledExecutorService executorService) {
        this.executorService = executorService;
    }

    private void addIdsFromMetacards(List<Metacard> metacards) {
        for (Metacard mcard : metacards) {
            if (RegistryUtility.isRegistryMetacard(mcard)) {
                registryIds.add(RegistryUtility.getRegistryId(mcard));
                if (RegistryUtility.isLocalNode(mcard)) {
                    localRegistryIds.add(RegistryUtility.getRegistryId(mcard));
                }
            } else if (RegistryUtility.isInternalRegistryMetacard(mcard)) {
                remoteMetacardIds.add(RegistryUtility.getStringAttribute(mcard,
                        RegistryObjectMetacardType.REMOTE_METACARD_ID,
                        ""));
            }
        }
    }

    private void removeIdsForMetacards(List<Metacard> metacards) {
        for (Metacard mcard : metacards) {
            if (RegistryUtility.isRegistryMetacard(mcard)) {
                registryIds.remove(RegistryUtility.getRegistryId(mcard));
                if (RegistryUtility.isLocalNode(mcard)) {
                    localRegistryIds.remove(RegistryUtility.getRegistryId(mcard));
                }
            } else if (RegistryUtility.isInternalRegistryMetacard(mcard)) {
                remoteMetacardIds.remove(RegistryUtility.getStringAttribute(mcard,
                        RegistryObjectMetacardType.REMOTE_METACARD_ID,
                        ""));
            }
        }
    }
}
