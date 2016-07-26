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
import org.codice.ddf.registry.federationadmin.service.FederationAdminException;
import org.codice.ddf.registry.federationadmin.service.FederationAdminService;
import org.codice.ddf.security.common.Security;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;

public class RegistryMetacardHandler implements EventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryMetacardHandler.class);

    private static final int SHUTDOWN_TIMEOUT_SECONDS = 60;

    private static final String METACARD_PROPERTY = "ddf.catalog.event.metacard";

    private static final String CREATED_TOPIC = "ddf/catalog/event/CREATED";

    private static final String UPDATED_TOPIC = "ddf/catalog/event/UPDATED";

    private static final String DELETED_TOPIC = "ddf/catalog/event/DELETED";

    private final ExecutorService executor;

    private final FederationAdminService federationAdminService;

    public RegistryMetacardHandler(ExecutorService executor,
            FederationAdminService federationAdminService) {
        this.executor = executor;
        this.federationAdminService = federationAdminService;
    }

    @Override
    public void handleEvent(Event event) {
        Metacard mcard = (Metacard) event.getProperty(METACARD_PROPERTY);
        if (mcard == null || !mcard.getTags().contains(RegistryConstants.REGISTRY_TAG_INTERNAL)) {
            return;
        }
        executor.execute(() -> processEvent(mcard, event.getTopic()));
    }

    private void processEvent(Metacard mcard, String topic) {
        try {
            Security.runAsAdminWithException(() -> {
                if (topic.equals(CREATED_TOPIC)) {
                    processMetacardCreateUpdate(mcard);
                } else if (topic.equals(UPDATED_TOPIC)) {
                    processMetacardCreateUpdate(mcard);
                } else if (topic.equals(DELETED_TOPIC)) {
                    processMetacardDelete(mcard);
                }
                return null;
            });
        } catch (PrivilegedActionException e) {
            LOGGER.error("Error processing registry metacard event.", e);
        }
    }

    private void processMetacardCreateUpdate(Metacard mcard) throws FederationAdminException {
        List<Metacard> metacards = federationAdminService
                .getRegistryMetacardsByRegistryIds(Collections.singletonList(getRegistryId(mcard)));
        if (CollectionUtils.isEmpty(metacards)) {
            preparedMetacard(mcard, null);
            federationAdminService.addRegistryEntry(mcard);

        } else if (shouldUpdate(mcard, metacards.get(0))) {
            preparedMetacard(mcard, metacards.get(0).getId());
            federationAdminService.updateRegistryEntry(mcard);
        }
    }

    private void processMetacardDelete(Metacard mcard) throws FederationAdminException {
        List<Metacard> metacards = federationAdminService
                .getRegistryMetacardsByRegistryIds(Collections.singletonList(getRegistryId(mcard)));
        if (CollectionUtils.isNotEmpty(metacards)) {
            if (CollectionUtils.isEmpty(federationAdminService
                    .getInternalRegistryMetacardsByRegistryId(getRegistryId(mcard)))) {
                federationAdminService.deleteRegistryEntriesByRegistryIds(
                        Collections.singletonList(getRegistryId(mcard)));
            }
        }
    }

    private void preparedMetacard(Metacard metacard, String id) {
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
        return oldMetacard.getModifiedDate().before(newMetacard.getModifiedDate());
    }

    private String getRegistryId(Metacard metacard) {
        return metacard.getAttribute(RegistryObjectMetacardType.REGISTRY_ID).getValue().toString();
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
}
