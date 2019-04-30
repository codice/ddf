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
import ddf.catalog.event.EventException;
import ddf.catalog.source.SourceMonitor;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.codice.ddf.registry.api.internal.RegistryStore;
import org.codice.ddf.registry.common.metacard.RegistryUtility;
import org.codice.ddf.registry.federationadmin.service.internal.FederationAdminService;
import org.codice.ddf.registry.federationadmin.service.internal.RegistryPublicationService;
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
 * The RegistryStorePublisher controls the auto publication of RegistryStores. Upon creation, the
 * reference listener will call the bindRegistryStore method and publish if isAutoPush is true.
 * After editing the RegistryStore handleEvent will evaluate if the publish or unpublish should
 * occur.
 */
public class RegistryStorePublisher implements EventHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(RegistryStorePublisher.class);

  private static final int SHUTDOWN_TIMEOUT_SECONDS = 60;

  private static final String PUBLISH = "publish";

  private static final String UNPUBLISH = "unpublish";

  private Set<String> registryStoreSet = Collections.newSetFromMap(new ConcurrentHashMap<>());

  private RegistryPublicationService registryPublicationService;

  private FederationAdminService federationAdminService;

  private ScheduledExecutorService executor;

  public void setFederationAdminService(FederationAdminService federationAdminService) {
    this.federationAdminService = federationAdminService;
  }

  public void setRegistryPublicationService(RegistryPublicationService registryPublicationService) {
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
          LOGGER.debug("Thread pool failed to terminate");
        }
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();

      Thread.currentThread().interrupt();
    }
  }

  public void bindRegistryStore(ServiceReference<RegistryStore> reference) {
    BundleContext bundleContext = getBundleContext();

    if (reference == null || bundleContext == null || bundleContext.getService(reference) == null) {
      return;
    }

    RegistryStore registryStore = bundleContext.getService(reference);

    registryStoreSet.add(registryStore.getConfigurationPid());

    SourceMonitor storeRegisteredSourceMonitor =
        new SourceMonitor() {
          @Override
          public void setAvailable() {
            if (registryStore.isAutoPush()) {
              registryPublish(registryStore, PUBLISH);
            }
          }

          @Override
          public void setUnavailable() {}
        };

    if (registryStore.isAvailable(storeRegisteredSourceMonitor) && registryStore.isAutoPush()) {
      registryPublish(registryStore, PUBLISH);
    }
  }

  public void unbindRegistryStore(ServiceReference<RegistryStore> reference) {

    BundleContext bundleContext = getBundleContext();

    if (reference != null && bundleContext != null) {
      RegistryStore registryStore = bundleContext.getService(reference);

      registryStoreSet.remove(registryStore.getConfigurationPid());
    }
  }

  void registryPublish(RegistryStore registryStore, String publish) {

    if (registryStore.getRegistryId().isEmpty()) {
      LOGGER.info("RegistryStore missing id. Unable to complete {} request.", publish);
      return;
    }

    executor.schedule(
        () -> {
          try {
            Security security = Security.getInstance();

            Optional<Metacard> registryIdentityMetacardOpt =
                security.runAsAdminWithException(
                    () -> federationAdminService.getLocalRegistryIdentityMetacard());

            if (registryIdentityMetacardOpt.isPresent()) {
              Metacard registryIdentityMetacard = registryIdentityMetacardOpt.get();
              String localRegistryId = RegistryUtility.getRegistryId(registryIdentityMetacard);
              if (localRegistryId == null) {
                throw new EventException();
              }
              security.runAsAdminWithException(
                  () -> {
                    if (publish.equals(PUBLISH)) {
                      registryPublicationService.publish(
                          localRegistryId, registryStore.getRegistryId());
                    } else {
                      registryPublicationService.unpublish(
                          localRegistryId, registryStore.getRegistryId());
                    }
                    return null;
                  });
            }

          } catch (Exception e) {
            LOGGER.debug(
                "Failed to {} registry configuration to {}",
                publish,
                ((RegistryStoreImpl) registryStore).getId());
          }
        },
        3,
        TimeUnit.SECONDS);
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
    String pid = event.getProperty(Constants.SERVICE_PID).toString();

    if (!registryStoreSet.contains(pid)) {
      return;
    }

    BundleContext bundleContext = getBundleContext();

    if (bundleContext == null) {
      return;
    }

    RegistryStore registryStore =
        (RegistryStore)
            bundleContext.getService(
                ((ServiceEvent) event.getProperty(EventConstants.EVENT)).getServiceReference());

    if (registryStore.isAutoPush()) {
      registryPublish(registryStore, PUBLISH);
      registryStoreSet.add(pid);
    } else {
      registryPublish(registryStore, UNPUBLISH);
      registryStoreSet.add(pid);
    }
  }
}
