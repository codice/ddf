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
package org.codice.ddf.registry.publication.manager;

import ddf.catalog.data.Metacard;
import java.security.PrivilegedActionException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.common.metacard.RegistryUtility;
import org.codice.ddf.registry.federationadmin.service.internal.FederationAdminService;
import org.codice.ddf.security.common.Security;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegistryPublicationManager implements EventHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(RegistryPublicationManager.class);

  private static final int RETRY_INTERVAL = 30;

  private static final int SHUTDOWN_TIMEOUT_SECONDS = 60;

  private static final String METACARD_PROPERTY = "ddf.catalog.event.metacard";

  private static final String CREATED_TOPIC = "ddf/catalog/event/CREATED";

  private static final String UPDATED_TOPIC = "ddf/catalog/event/UPDATED";

  private static final String DELETED_TOPIC = "ddf/catalog/event/DELETED";

  private Map<String, List<String>> publications = new ConcurrentHashMap<>();

  private FederationAdminService federationAdminService;

  private ScheduledExecutorService executorService;

  @Override
  public void handleEvent(Event event) {
    Metacard mcard = (Metacard) event.getProperty(METACARD_PROPERTY);
    if (mcard == null || !RegistryUtility.isRegistryMetacard(mcard)) {
      return;
    }

    String registryId = RegistryUtility.getRegistryId(mcard);
    List<String> locations =
        RegistryUtility.getListOfStringAttribute(
            mcard, RegistryObjectMetacardType.PUBLISHED_LOCATIONS);

    if (event.getTopic().equals(CREATED_TOPIC) || event.getTopic().equals(UPDATED_TOPIC)) {
      publications.put(registryId, Collections.unmodifiableList(locations));
    } else if (event.getTopic().equals(DELETED_TOPIC)) {
      publications.remove(registryId);
    }
  }

  public void init() {
    executorService.submit(this::setPublications);
  }

  public void setPublications() {
    try {
      List<Metacard> metacards =
          Security.getInstance()
              .runAsAdminWithException(() -> federationAdminService.getRegistryMetacards());

      for (Metacard metacard : metacards) {
        String registryId = RegistryUtility.getRegistryId(metacard);
        if (registryId == null) {
          LOGGER.debug(
              "Warning metacard (id: {}} did not contain a registry id.", metacard.getId());
          continue;
        }

        List<String> locations =
            RegistryUtility.getListOfStringAttribute(
                metacard, RegistryObjectMetacardType.PUBLISHED_LOCATIONS);
        if (!locations.isEmpty()) {
          publications.put(registryId, Collections.unmodifiableList(locations));
        } else {
          publications.put(registryId, Collections.emptyList());
        }
      }
      this.destroy();
    } catch (PrivilegedActionException | RuntimeException e) {
      LOGGER.debug(
          "Error reading from local catalog. Catalog is probably not up yet. Will try again in {} seconds.",
          RETRY_INTERVAL);
      executorService.schedule(this::setPublications, RETRY_INTERVAL, TimeUnit.SECONDS);
    }
  }

  public void destroy() {
    executorService.shutdown();
    try {
      if (!executorService.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        executorService.shutdownNow();
        if (!executorService.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
          LOGGER.error(
              "Registry Publication Manager thread didn't terminate. This thread will continue until a system restart.");
        }
      }
    } catch (InterruptedException e) {
      executorService.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  public Map<String, List<String>> getPublications() {
    return Collections.unmodifiableMap(publications);
  }

  public void setFederationAdminService(FederationAdminService adminService) {
    this.federationAdminService = adminService;
  }

  public void setExecutorService(ScheduledExecutorService executorService) {
    this.executorService = executorService;
  }
}
