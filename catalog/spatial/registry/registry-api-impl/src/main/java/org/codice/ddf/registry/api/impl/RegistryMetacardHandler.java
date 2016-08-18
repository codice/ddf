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
 */
package org.codice.ddf.registry.api.impl;

import java.io.Serializable;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.CollectionUtils;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.common.metacard.RegistryUtility;
import org.codice.ddf.registry.federationadmin.service.internal.FederationAdminException;
import org.codice.ddf.registry.federationadmin.service.internal.FederationAdminService;
import org.codice.ddf.security.common.Security;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.event.EventProcessor;

/**
 * RegistryMetacardHandler is responsible for taking internal registry entries received from remote
 * registries and make/update registry entries that are visible to the rest of the registry framework.
 * This is achieved through the used of the metacard-tags attribute. This class also ensures that even if a
 * particular entry comes from multiple sources only the newest one will be represented in  visible
 * registry entry.
 */
public class RegistryMetacardHandler implements EventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryMetacardHandler.class);

    private static final int SHUTDOWN_TIMEOUT_SECONDS = 60;

    private final ExecutorService executor;

    private final FederationAdminService federationAdminService;

    public RegistryMetacardHandler(ExecutorService executor,
            FederationAdminService federationAdminService) {
        this.executor = executor;
        this.federationAdminService = federationAdminService;
    }

    @Override
    public void handleEvent(Event event) {
        Metacard mcard = (Metacard) event.getProperty(EventProcessor.EVENT_METACARD);
        if (mcard == null || !RegistryUtility.isInternalRegistryMetacard(mcard)) {
            return;
        }
        LOGGER.debug("RegistryMetacardHandler processing event {}", event.getTopic());
        executor.execute(() -> processEvent(mcard, event.getTopic()));
    }

    private void processEvent(Metacard mcard, String topic) {
        try {
            Security.runAsAdminWithException(() -> {
                if (topic.equals(EventProcessor.EVENTS_TOPIC_DELETED)) {
                    processMetacardDelete(mcard);
                } else if (topic.equals(EventProcessor.EVENTS_TOPIC_CREATED) || topic.equals(
                        EventProcessor.EVENTS_TOPIC_UPDATED)) {
                    processMetacardCreateUpdate(mcard);
                }
                return null;
            });
        } catch (PrivilegedActionException e) {
            LOGGER.debug("Error processing registry metacard event.", e);
        }
    }

    private void processMetacardCreateUpdate(Metacard mcard) throws FederationAdminException {
        List<Metacard> metacards = federationAdminService.getRegistryMetacardsByRegistryIds(
                Collections.singletonList(RegistryUtility.getRegistryId(mcard)));
        if (CollectionUtils.isEmpty(metacards)) {
            prepareMetacard(mcard, null);
            federationAdminService.addRegistryEntry(mcard);
            LOGGER.debug("Successfully created new registry metacard");
        } else if (shouldUpdate(mcard, metacards.get(0))) {
            prepareMetacard(mcard,
                    metacards.get(0)
                            .getId());
            federationAdminService.updateRegistryEntry(mcard);
            LOGGER.debug("Successfully update registry metacard");
        }
    }

    /**
     * When an internal registry entry is deleted we need to check to see if there is an associated
     * visible registry metacard that needs to be deleted. A visible registry metacard should be
     * deleted if all associated internal registry entries have been deleted.
     *
     * @param mcard
     * @throws FederationAdminException
     */
    private void processMetacardDelete(Metacard mcard) throws FederationAdminException {
        String registryId = RegistryUtility.getRegistryId(mcard);
        List<Metacard> metacards = federationAdminService.getRegistryMetacardsByRegistryIds(
                Collections.singletonList(registryId));
        if (CollectionUtils.isNotEmpty(metacards)) {
            if (CollectionUtils.isEmpty(federationAdminService.getInternalRegistryMetacardsByRegistryId(
                    registryId))) {
                federationAdminService.deleteRegistryEntriesByRegistryIds(Collections.singletonList(
                        registryId));
            }
        }
    }

    /**
     * This is the method that converts an internal registry metacard with tag REGISTRY_TAG_INTERNAL
     * into a visible registry metacard with a tag of REGISTRY_TAG. Also removes other transient
     * metacard attributes that only internal entries have.
     *
     * @param metacard The internal registry metacard
     * @param id       The metacard id of the visible registry metacard. Can be null
     */
    private void prepareMetacard(Metacard metacard, String id) {
        List<Serializable> tags = new ArrayList<>();
        tags.addAll(metacard.getTags());
        tags.remove(RegistryConstants.REGISTRY_TAG_INTERNAL);
        tags.add(RegistryConstants.REGISTRY_TAG);
        metacard.setAttribute(new AttributeImpl(Metacard.TAGS, tags));
        metacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REMOTE_METACARD_ID,
                (Serializable) null));
        metacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REMOTE_REGISTRY_ID,
                (Serializable) null));
        metacard.setAttribute(new AttributeImpl(Metacard.ID, id));
    }

    private boolean shouldUpdate(Metacard newMetacard, Metacard oldMetacard) {
        return oldMetacard.getModifiedDate()
                .before(newMetacard.getModifiedDate());
    }

    public void destroy() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    LOGGER.debug("Thread pool didn't terminate");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}
