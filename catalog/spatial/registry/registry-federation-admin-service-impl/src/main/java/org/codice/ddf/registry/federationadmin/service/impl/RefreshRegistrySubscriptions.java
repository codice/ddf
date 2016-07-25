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
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.codice.ddf.registry.api.RegistryStore;
import org.codice.ddf.registry.common.metacard.RegistryUtility;
import org.codice.ddf.registry.federationadmin.service.FederationAdminException;
import org.codice.ddf.registry.federationadmin.service.FederationAdminService;
import org.codice.ddf.security.common.Security;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;

/**
 * Though refreshRegistrySubscriptions in this class is a public method, it should never be called by any
 * class with external connections. This class is only meant to be accessed through a camel
 * route and should avoid elevating privileges for any other service or being exposed to any
 * other endpoint.
 */
@SuppressWarnings("PackageAccessibility")
public class RefreshRegistrySubscriptions {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(RefreshRegistrySubscriptions.class);

    private List<RegistryStore> registryStores;

    private FederationAdminService federationAdminService;

    public RefreshRegistrySubscriptions() {

    }

    /**
     * Do not call refreshRegistrySubscriptions directly! It is a public method, but is only meant
     * to be accessed through a camel route and should avoid elevating privileges for any other
     * service or being exposed to any other endpoint.
     *
     * @throws FederationAdminException
     */
    public void refreshRegistrySubscriptions() throws FederationAdminException {
        if (CollectionUtils.isEmpty(registryStores)) {
            return;
        }

        Map<String, Metacard> remoteRegistryMetacardsMap = getRemoteRegistryMetacardsMap();
        Map<String, Metacard> registryMetacardsMap = getRegistryMetacardsMap();

        List<Metacard> remoteMetacardsToUpdate = new ArrayList<>();
        List<Metacard> remoteMetacardsToCreate = new ArrayList<>();

        for (Map.Entry<String, Metacard> remoteEntry : remoteRegistryMetacardsMap.entrySet()) {
            if (registryMetacardsMap.containsKey(remoteEntry.getKey())) {
                Metacard existingMetacard = registryMetacardsMap.get(remoteEntry.getKey());
                boolean localNode = RegistryUtility.isLocalNode(existingMetacard);

                if (!localNode && remoteEntry.getValue()
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
                    Security.runAsAdminWithException(() -> federationAdminService.getRegistryMetacards());

            return registryMetacards.stream()
                    .filter(RegistryUtility::isRegistryMetacard)
                    .collect(Collectors.toMap(RegistryUtility::getRegistryId, Function.identity()));
        } catch (Exception e) {
            throw new FederationAdminException("Error querying for metacards ", e);
        }
    }

    private Map<String, Metacard> getRemoteRegistryMetacardsMap() throws FederationAdminException {
        Map<String, Metacard> remoteRegistryMetacards = new HashMap<>();
        Set<String> sourceIds = registryStores.stream()
                .filter(RegistryStore::isAvailable)
                .filter(RegistryStore::isPullAllowed)
                .map(RegistryStore::getId)
                .collect(Collectors.toSet());

        if (CollectionUtils.isEmpty(sourceIds)) {
            return remoteRegistryMetacards;
        }

        try {

            List<Metacard> fullList =
                    Security.runAsAdminWithException(() -> federationAdminService.getRegistryMetacards(
                            sourceIds));
            for (Metacard metacard : fullList) {
                String registryId = RegistryUtility.getRegistryId(metacard);
                if(registryId == null){
                    continue;
                }
                if (remoteRegistryMetacards.containsKey(registryId)) {
                    Metacard currentMetacard = remoteRegistryMetacards.get(registryId);
                    if (currentMetacard.getModifiedDate()
                            .before(metacard.getModifiedDate())) {
                        remoteRegistryMetacards.put(registryId, metacard);
                    }
                } else {
                    remoteRegistryMetacards.put(registryId, metacard);
                }
            }

        } catch (Exception e) {
            String message = "Error querying for subscribed metacards;";
            LOGGER.warn("{} For source ids: {}", message, sourceIds);
            throw new FederationAdminException(message, e);
        }

        return remoteRegistryMetacards;
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
            LOGGER.error("{} Metacard IDs: {}", message, remoteMetacardsToUpdate);
            throw new FederationAdminException(message, e);
        }
    }

    private void createRemoteEntries(List<Metacard> remoteMetacardsToCreate)
            throws FederationAdminException {
        try {
            Security.runAsAdminWithException(() -> federationAdminService.addRegistryEntries(
                    remoteMetacardsToCreate,
                    null));
        } catch (PrivilegedActionException e) {
            throw new FederationAdminException(e.getMessage());
        }
    }

    public void setRegistryStores(List<RegistryStore> registryStores) {
        this.registryStores = registryStores;
    }

    public void setFederationAdminService(FederationAdminService federationAdminService) {
        this.federationAdminService = federationAdminService;
    }
}
