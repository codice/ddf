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
package org.codice.ddf.registry.api.impl;

import ddf.catalog.data.Metacard;
import java.security.PrivilegedActionException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.codice.ddf.registry.api.internal.RegistryStore;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.common.metacard.RegistryUtility;
import org.codice.ddf.registry.federationadmin.service.internal.FederationAdminService;
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

/**
 * This handler cleans up registry entries in the local catalog when a remote registry connection is
 * removed.
 */
public class RegistryStoreCleanupHandler implements EventHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(RegistryStoreCleanupHandler.class);

  private FederationAdminService federationAdminService;

  private ExecutorService executor;

  private static final int SHUTDOWN_TIMEOUT_SECONDS = 60;

  private boolean cleanupRelatedMetacards = true;

  private Map<Object, RegistryStore> registryStorePidToServiceMap = new ConcurrentHashMap<>();

  public void bindRegistryStore(ServiceReference serviceReference) {
    BundleContext bundleContext = getBundleContext();

    if (serviceReference != null && bundleContext != null) {
      RegistryStore registryStore = (RegistryStore) bundleContext.getService(serviceReference);

      registryStorePidToServiceMap.put(
          serviceReference.getProperty(Constants.SERVICE_PID), registryStore);
    }
  }

  @Override
  public void handleEvent(Event event) {
    Object eventProperty = event.getProperty(EventConstants.EVENT);
    if (!cleanupRelatedMetacards
        || eventProperty == null
        || !(eventProperty instanceof ServiceEvent)) {
      return;
    }

    if (((ServiceEvent) eventProperty).getType() != ServiceEvent.UNREGISTERING) {
      return;
    }
    Object servicePid =
        ((ServiceEvent) event.getProperty(EventConstants.EVENT))
            .getServiceReference()
            .getProperty(Constants.SERVICE_PID);
    if (servicePid == null) {
      return;
    }

    RegistryStore service = registryStorePidToServiceMap.get(servicePid);
    if (service == null) {
      return;
    }
    registryStorePidToServiceMap.remove(servicePid);
    LOGGER.info("Removing registry entries associated with remote registry {}", service.getId());

    executor.execute(
        () -> {
          String registryId = service.getRegistryId();
          try {
            Security security = Security.getInstance();

            List<Metacard> metacards =
                security.runAsAdminWithException(
                    () ->
                        federationAdminService
                            .getInternalRegistryMetacards()
                            .stream()
                            .filter(
                                m ->
                                    RegistryUtility.getStringAttribute(
                                            m, RegistryObjectMetacardType.REMOTE_REGISTRY_ID, "")
                                        .equals(registryId))
                            .collect(Collectors.toList()));
            List<String> idsToDelete =
                metacards.stream().map(Metacard::getId).collect(Collectors.toList());
            if (!idsToDelete.isEmpty()) {
              if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                    "Removing {} registry entries that came from {}. Removed entries: {}",
                    metacards.size(),
                    service.getId(),
                    metacards
                        .stream()
                        .map(m -> m.getTitle() + ":" + m.getId())
                        .collect(Collectors.joining(", ")));
              }
              security.runAsAdminWithException(
                  () -> {
                    federationAdminService.deleteRegistryEntriesByMetacardIds(idsToDelete);
                    return null;
                  });
            }
          } catch (PrivilegedActionException e) {
            LOGGER.info(
                "Unable to clean up registry metacards after registry store {} was deleted",
                service.getId(),
                e);
          }
        });
  }

  public void destroy() {
    executor.shutdown();
    try {
      if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        executor.shutdownNow();
        if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
          LOGGER.debug("Thread pool failed to terminate");
        }
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();

      Thread.currentThread().interrupt();
    }
  }

  BundleContext getBundleContext() {
    Bundle bundle = FrameworkUtil.getBundle(this.getClass());
    if (bundle != null) {
      return bundle.getBundleContext();
    }
    return null;
  }

  public void setCleanupRelatedMetacards(boolean cleanupRelatedMetacards) {
    this.cleanupRelatedMetacards = cleanupRelatedMetacards;
  }

  public void setFederationAdminService(FederationAdminService federationAdminService) {
    this.federationAdminService = federationAdminService;
  }

  public void setExecutor(ExecutorService executor) {
    this.executor = executor;
  }
}
