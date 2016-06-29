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

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.codice.ddf.registry.api.RegistryStore;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.federationadmin.service.FederationAdminService;
import org.codice.ddf.registry.federationadmin.service.RegistryPublicationService;
import org.codice.ddf.security.common.Security;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.source.SourceMonitor;

/**
 * The RegistryStorePublisher controls the auto publication of RegistryStores. Upon creation,
 * the reference listener will call the bindRegistryStore method and publish if isAutoPush is
 * true. After editing the RegistryStore handleEvent will evaluate if the publish or unpublish
 * should occur.
 */
public class RegistryStorePublisher implements EventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryStorePublisher.class);

    private static final int SHUTDOWN_TIMEOUT_SECONDS = 60;

    private static final String PUBLISH = "publish";

    private static final String UNPUBLISH = "unpublish";

    private ConcurrentHashMap<String, Boolean> registryStoreMap = new ConcurrentHashMap<>();

    private RegistryPublicationService registryPublicationService;

    private FederationAdminService federationAdminService;

    private ScheduledExecutorService executor;

    public void setFederationAdminService(FederationAdminService federationAdminService) {
        this.federationAdminService = federationAdminService;
    }

    public void setRegistryPublicationService(
            RegistryPublicationService registryPublicationService) {
        this.registryPublicationService = registryPublicationService;
    }

    public void setExecutor(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    public void destroy() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    LOGGER.error("Thread pool failed to terminate");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    public void bindRegistryStore(ServiceReference<RegistryStore> reference) {
        BundleContext bundleContext = getBundleContext();

        if (reference == null || bundleContext == null
                || bundleContext.getService(reference) == null) {
            LOGGER.warn("Reference or BundleContext was null/unset.");
            return;
        }

        RegistryStore registryStore = bundleContext.getService(reference);

        Boolean previousState = registryStoreMap.put(registryStore.getConfigurationPid(),
                registryStore.isAutoPush());

        SourceMonitor storeRegisteredSourceMonitor = new SourceMonitor() {
            @Override
            public void setAvailable() {
                if (registryStore.isAutoPush()) {
                    registryPublish(registryStore, PUBLISH);
                }
            }

            @Override
            public void setUnavailable() {
            }
        };

        if (registryStore.isAvailable(storeRegisteredSourceMonitor) && registryStore.isAutoPush()) {
            if (previousState == null || !previousState) {
                registryPublish(registryStore, PUBLISH);
            }
        }
    }

    public void unbindRegistryStore(ServiceReference<RegistryStore> reference) {

        BundleContext bundleContext = getBundleContext();

        if (reference != null && bundleContext != null) {
            RegistryStore registryStore = bundleContext.getService(reference);

            registryStoreMap.remove(registryStore.getConfigurationPid());
        }
    }

    void registryPublish(RegistryStore registryStore, String publish) {

        if (registryStore.getRegistryId()
                .isEmpty()) {
            LOGGER.warn(String.format("RegistryStore missing id. Unable to complete %s request.",
                    publish));
            return;
        }

        executor.schedule(() -> {
            try {
                Optional<Metacard> registryIdentityMetacardOpt =
                        Security.runAsAdminWithException(() -> federationAdminService.getLocalRegistryIdentityMetacard());

                if (registryIdentityMetacardOpt.isPresent()) {
                    Metacard registryIdentityMetacard = registryIdentityMetacardOpt.get();
                    String localRegistryId = registryIdentityMetacard.getAttribute(
                            RegistryObjectMetacardType.REGISTRY_ID)
                            .getValue()
                            .toString();
                    Security.runAsAdminWithException(() -> {
                        if (publish.equals(PUBLISH)) {
                            registryPublicationService.publish(localRegistryId,
                                    registryStore.getRegistryId());
                        } else {
                            registryPublicationService.unpublish(localRegistryId,
                                    registryStore.getRegistryId());
                        }
                        return null;
                    });
                }

            } catch (Exception e) {
                LOGGER.error("Failed to {} registry configuration to {}",
                        publish,
                        ((RegistryStoreImpl) registryStore).getRemoteName());
            }
        }, 3, TimeUnit.SECONDS);
    }

    BundleContext getBundleContext() {
        Bundle bundle = FrameworkUtil.getBundle(this.getClass());
        if (bundle != null) {
            return bundle.getBundleContext();
        }
        return null;
    }

    @Override
    public void handleEvent(Event event) {
        if (((ServiceEvent) event.getProperty(EventConstants.EVENT)).getType()
                != ServiceEvent.MODIFIED) {
            return;
        }
        String pid = event.getProperty(Constants.SERVICE_PID)
                .toString();


        Boolean previousAutoPush = registryStoreMap.get(pid);

        if (previousAutoPush == null) {
            return;
        }

        BundleContext bundleContext = getBundleContext();

        RegistryStore registryStore =
                (RegistryStore) bundleContext.getService(((ServiceEvent) event.getProperty(
                        EventConstants.EVENT)).getServiceReference());

        if (!previousAutoPush && registryStore.isAutoPush()) {
            registryPublish(registryStore, PUBLISH);
            registryStoreMap.put(pid, true);
        } else if (previousAutoPush && !registryStore.isAutoPush()) {
            registryPublish(registryStore, UNPUBLISH);
            registryStoreMap.put(pid, false);
        }
    }
}
