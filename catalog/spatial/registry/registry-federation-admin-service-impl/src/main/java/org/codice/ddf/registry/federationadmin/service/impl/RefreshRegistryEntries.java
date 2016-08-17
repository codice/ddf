/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package org.codice.ddf.registry.federationadmin.service.impl;

import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.impl.PropertyNameImpl;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.UnsupportedQueryException;

/**
 * Though refreshRegistrySubscriptions in this class is a public method, it should never be called by any
 * class with external connections. This class is only meant to be accessed through a camel
 * route and should avoid elevating privileges for any other service or being exposed to any
 * other endpoint.
 */
@SuppressWarnings("PackageAccessibility")
public class RefreshRegistryEntries {
    private static final Logger LOGGER = LoggerFactory.getLogger(RefreshRegistryEntries.class);

    private static final int PAGE_SIZE = 1000;

    private List<RegistryStore> registryStores;

    private FederationAdminService federationAdminService;

    private FilterBuilder filterBuilder;

    public RefreshRegistryEntries() {

    }

    /**
     * Do not call refreshRegistrySubscriptions directly! It is a public method, but is only meant
     * to be accessed through a camel route and should avoid elevating privileges for any other
     * service or being exposed to any other endpoint.
     *
     * @throws FederationAdminException
     */
    public void refreshRegistryEntries() throws FederationAdminException {

        RemoteRegistryResults remoteResults = getRemoteRegistryMetacardsMap();
        Map<String, Metacard> remoteRegistryMetacardsMap = remoteResults
                .getRemoteRegistryMetacards();
        Map<String, Metacard> registryMetacardsMap = getRegistryMetacardsMap();

        List<Metacard> remoteMetacardsToUpdate = new ArrayList<>();
        List<Metacard> remoteMetacardsToCreate = new ArrayList<>();

        for (Map.Entry<String, Metacard> remoteEntry : remoteRegistryMetacardsMap.entrySet()) {
            if (registryMetacardsMap.containsKey(remoteEntry.getKey())) {
                Metacard existingMetacard = registryMetacardsMap.get(remoteEntry.getKey());

                if (!RegistryUtility.isLocalNode(existingMetacard) && remoteEntry.getValue()
                        .getModifiedDate()
                        .after(existingMetacard.getModifiedDate())) {
                    remoteMetacardsToUpdate.add(remoteEntry.getValue());
                }
            } else {
                remoteMetacardsToCreate.add(remoteEntry.getValue());
            }
        }
        if (CollectionUtils.isNotEmpty(remoteMetacardsToUpdate)) {
            writeRemoteUpdates(remoteMetacardsToUpdate);
        }
        if (CollectionUtils.isNotEmpty(remoteMetacardsToCreate)) {
            createRemoteEntries(remoteMetacardsToCreate);
        }
    }

    private Map<String, Metacard> getRegistryMetacardsMap() throws FederationAdminException {
        try {
            List<Metacard> registryMetacards =
                    Security.runAsAdminWithException(() -> federationAdminService.getInternalRegistryMetacards());

            return registryMetacards.stream()
                    .collect(Collectors.toMap(e -> RegistryUtility.getStringAttribute(e,
                            RegistryObjectMetacardType.REMOTE_METACARD_ID,
                            null), Function.identity()));
        } catch (Exception e) {
            throw new FederationAdminException("Error querying for metacards ", e);
        }
    }

    private RemoteRegistryResults getRemoteRegistryMetacardsMap() throws FederationAdminException {
        Map<String, Metacard> remoteRegistryMetacards = new HashMap<>();
        List<String> failedQueries = new ArrayList<>();
        List<String> localMetacardRegIds;
        try {
            List<Metacard> localMetacards =
                    Security.runAsAdminWithException(() -> federationAdminService.getLocalRegistryMetacards());
            localMetacardRegIds = localMetacards.stream()
                    .map(e -> RegistryUtility.getRegistryId(e))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new FederationAdminException("Error querying for local metacards ", e);
        }
        SourceResponse response;
        for (RegistryStore store : registryStores) {
            if (!store.isPullAllowed() || !store.isAvailable()) {
                continue;
            }
            try {
                response = store.query(new QueryRequestImpl(getBasicRegistryQuery()));
                remoteRegistryMetacards.putAll(response.getResults()
                        .stream()
                        .map(Result::getMetacard)
                        .filter(e -> !localMetacardRegIds.contains(RegistryUtility.getRegistryId(e)))
                        .collect(Collectors.toMap(Metacard::getId, Function.identity())));
            } catch (UnsupportedQueryException e) {
                LOGGER.debug("Unable to contact {} for registry subscription query",
                        store.getId(),
                        e);
                failedQueries.add(store.getRegistryId());
            }
        }
        return new RemoteRegistryResults(remoteRegistryMetacards, failedQueries);
    }

    private void writeRemoteUpdates(List<Metacard> remoteMetacardsToUpdate)
            throws FederationAdminException {
        try {
            Security.runAsAdminWithException(() -> {
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
            Security.runAsAdminWithException(
                    () -> federationAdminService.addRegistryEntries(remoteMetacardsToCreate, null));
        } catch (PrivilegedActionException e) {
            throw new FederationAdminException(e.getMessage());
        }
    }

    private Query getBasicRegistryQuery() {
        List<Filter> filters = new ArrayList<>();
        filters.add(filterBuilder.attribute(Metacard.CONTENT_TYPE).is().equalTo()
                .text(RegistryConstants.REGISTRY_NODE_METACARD_TYPE_NAME));
        filters.add(filterBuilder.attribute(Metacard.TAGS).is().equalTo()
                .text(RegistryConstants.REGISTRY_TAG));

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

    private static class RemoteRegistryResults {

        Map<String, Metacard> remoteRegistryMetacards;

        List<String> failureList;

        public RemoteRegistryResults(Map<String, Metacard> remoteRegistryMetacards,
                List<String> failureList) {
            this.remoteRegistryMetacards = remoteRegistryMetacards;
            this.failureList = failureList;
        }

        public Map<String, Metacard> getRemoteRegistryMetacards() {
            return remoteRegistryMetacards;
        }

        public List<String> getFailureList() {
            return failureList;
        }
    }
}
