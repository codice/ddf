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
package org.codice.ddf.registry.publication;

import java.security.PrivilegedActionException;
import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.common.metacard.RegistryUtility;
import org.codice.ddf.registry.federationadmin.service.internal.RegistryPublicationService;
import org.codice.ddf.security.common.Security;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;

/**
 * RegistryPublicationHandler is an org.osgi.service.event.EventHandler that listens for registry
 * metacard updates and forwards changes on to any publish locations.
 */
public class RegistryPublicationHandler implements EventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryPublicationHandler.class);

    private static final String METACARD_PROPERTY = "ddf.catalog.event.metacard";

    private static final int SHUTDOWN_TIMEOUT_SECONDS = 60;

    private static final int PUBLICATION_DELAY = 3; //seconds

    private final RegistryPublicationService registryPublicationService;

    private final ScheduledExecutorService executor;

    public RegistryPublicationHandler(RegistryPublicationService registryPublicationService,
            ScheduledExecutorService service) {
        this.registryPublicationService = registryPublicationService;
        this.executor = service;
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

    @Override
    public void handleEvent(Event event) {
        Metacard mcard = (Metacard) event.getProperty(METACARD_PROPERTY);
        if (mcard == null || !mcard.getTags()
                .contains(RegistryConstants.REGISTRY_TAG)) {
            return;
        }
        executor.schedule(() -> processUpdate(mcard), PUBLICATION_DELAY, TimeUnit.SECONDS);
    }

    private void processUpdate(Metacard mcard) {
        if (RegistryUtility.getListOfStringAttribute(mcard,
                RegistryObjectMetacardType.PUBLISHED_LOCATIONS)
                .isEmpty()) {
            return;
        }

        Attribute lastPublished = mcard.getAttribute(RegistryObjectMetacardType.LAST_PUBLISHED);
        Date datePublished = null;
        if (lastPublished != null) {
            datePublished = (Date) lastPublished.getValue();
        }

        //If we haven't published this metacard before or if we have had an update
        //since the last time we published send an update to the remote location
        if ((datePublished == null || datePublished.before(mcard.getModifiedDate()))) {
            try {
                Security security = Security.getInstance();

                security.runAsAdminWithException(() -> {
                    registryPublicationService.update(mcard);
                    return null;
                });
            } catch (PrivilegedActionException e) {
                LOGGER.warn("Unable to send update for {}. Try unpublishing and republishing {}",
                        mcard.getTitle(),
                        mcard.getTitle());
            }
        }

    }
}
