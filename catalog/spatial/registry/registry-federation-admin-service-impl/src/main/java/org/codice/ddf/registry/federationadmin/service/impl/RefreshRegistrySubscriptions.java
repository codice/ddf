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

import java.security.Principal;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.collections.CollectionUtils;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.codice.ddf.registry.api.RegistryStore;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.federationadmin.service.FederationAdminException;
import org.codice.ddf.registry.federationadmin.service.FederationAdminService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;

@SuppressWarnings("PackageAccessibility")
public class RefreshRegistrySubscriptions {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(RefreshRegistrySubscriptions.class);

    private javax.security.auth.Subject subject;

    private final Set<String> pollableSourceIds = ConcurrentHashMap.newKeySet();

    private FederationAdminService federationAdminService;

    public void setFederationAdminService(FederationAdminService federationAdminService) {
        this.federationAdminService = federationAdminService;
    }

    public RefreshRegistrySubscriptions() {
        Set<Principal> principals = new HashSet<>();
        String localRoles = System.getProperty("karaf.local.roles", "");
        for (String role : localRoles.split(",")) {
            principals.add(new RolePrincipal(role));
        }
        subject = new javax.security.auth.Subject(true, principals, new HashSet(), new HashSet());
    }

    public void refreshRegistrySubscriptions() throws FederationAdminException {
        if (CollectionUtils.isEmpty(pollableSourceIds)) {
            return;
        }
        addOrUpdateMetacardsForRemoteRegistries();
    }

    private void addOrUpdateMetacardsForRemoteRegistries() throws FederationAdminException {
        addOrUpdateMetacardsForRemoteRegistries(pollableSourceIds);
    }

    private void addOrUpdateMetacardsForRemoteRegistries(Set<String> sourceIds)
            throws FederationAdminException {
        if (CollectionUtils.isEmpty(sourceIds)) {
            return;
        }

        Map<String, Metacard> remoteRegistryMetacardsMap = getRemoteRegistryMetacardsMap(sourceIds);
        Map<String, Metacard> registryMetacardsMap = getRegistryMetacardsMap();

        List<Metacard> remoteMetacardsToUpdate = new ArrayList<>();
        List<Metacard> remoteMetacardsToCreate = new ArrayList<>();

        for (Map.Entry<String, Metacard> remoteEntry : remoteRegistryMetacardsMap.entrySet()) {
            if (registryMetacardsMap.containsKey(remoteEntry.getKey())) {
                Metacard existingMetacard = registryMetacardsMap.get(remoteEntry.getKey());
                Attribute localNode =
                        existingMetacard.getAttribute(RegistryObjectMetacardType.REGISTRY_LOCAL_NODE);

                if (localNode == null && remoteEntry.getValue()
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

        Map<String, Metacard> registryMetacardsMap = new HashMap<>();
        List<Metacard> registryMetacards = federationAdminService.getRegistryMetacards();

        for (Metacard metacard : registryMetacards) {
            String registryId = metacard.getAttribute(RegistryObjectMetacardType.REGISTRY_ID)
                    .getValue()
                    .toString();
            registryMetacardsMap.put(registryId, metacard);
        }

        return registryMetacardsMap;
    }

    private Map<String, Metacard> getRemoteRegistryMetacardsMap(Set<String> sourceIds)
            throws FederationAdminException {
        Map<String, Metacard> remoteRegistryMetacards = new HashMap<>();
        try {

            List<Metacard> fullList = javax.security.auth.Subject.doAs(subject,
                    (PrivilegedExceptionAction<List<Metacard>>) () -> federationAdminService.getRegistryMetacards());
            for (Metacard metacard : fullList) {
                String registryId = metacard.getAttribute(RegistryObjectMetacardType.REGISTRY_ID)
                        .getValue()
                        .toString();
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
            javax.security.auth.Subject.doAs(subject, (PrivilegedExceptionAction<Void>) () -> {
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
            javax.security.auth.Subject.doAs(subject,
                    (PrivilegedExceptionAction<List<String>>) () -> federationAdminService.addRegistryEntries(
                            remoteMetacardsToCreate,
                            null));
        } catch (PrivilegedActionException e) {
            throw new FederationAdminException(e.getMessage());
        }
    }

    public void bindRegistryStore(ServiceReference serviceReference) {
        BundleContext bundleContext = getBundleContext();

        if (serviceReference != null && bundleContext != null) {
            RegistryStore registryStore =
                    (RegistryStore) bundleContext.getService(serviceReference);

            if (registryStore.isPullAllowed()) {
                pollableSourceIds.add(registryStore.getId());
                try {
                    addOrUpdateMetacardsForRemoteRegistries(Collections.singleton(registryStore.getId()));
                } catch (FederationAdminException e) {
                    LOGGER.warn(
                            "Trying to access the registry store before it has finished initializing. Couldn't update registry for new registry store (ID: {}). No actions required.",
                            registryStore.getId());
                }
            }
        }
    }

    public void unbindRegistryStore(ServiceReference serviceReference) {
        BundleContext bundleContext = getBundleContext();

        if (serviceReference != null && bundleContext != null) {
            RegistryStore registryStore =
                    (RegistryStore) bundleContext.getService(serviceReference);

            pollableSourceIds.remove(registryStore.getId());
        }
    }

    BundleContext getBundleContext() {
        Bundle bundle = FrameworkUtil.getBundle(this.getClass());
        if (bundle != null) {
            return bundle.getBundleContext();
        }
        return null;
    }

}
