/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.registry.publication.manager;

import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.federationadmin.service.FederationAdminService;
import org.codice.ddf.security.common.Security;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;

public class RegistryPublicationManager implements EventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryPublicationManager.class);

    private static final int RETRY_INTERVAL = 30;

    private static final String METACARD_PROPERTY = "ddf.catalog.event.metacard";

    private static final String CREATED_TOPIC = "ddf/catalog/event/CREATED";

    private static final String UPDATED_TOPIC = "ddf/catalog/event/UPDATED";

    private static final String DELETED_TOPIC = "ddf/catalog/event/DELETED";

    private static final java.lang.String KARAF_LOCAL_ROLES = "karaf.local.roles";

    private Map<String, List<String>> publications = new ConcurrentHashMap<>();

    private FederationAdminService federationAdminService;

    private ScheduledExecutorService executorService;

    @Override
    public void handleEvent(Event event) {
        Metacard mcard = (Metacard) event.getProperty(METACARD_PROPERTY);
        if (mcard == null || !mcard.getTags()
                .contains(RegistryConstants.REGISTRY_TAG)) {
            return;
        }

        Attribute registryIdAttribute = mcard.getAttribute(RegistryObjectMetacardType.REGISTRY_ID);
        if (registryIdAttribute == null || StringUtils.isBlank(registryIdAttribute.getValue()
                .toString())) {
            return;
        }
        String registryId = registryIdAttribute.getValue()
                .toString();

        Attribute locationAttribute =
                mcard.getAttribute(RegistryObjectMetacardType.PUBLISHED_LOCATIONS);
        List<String> locations = new ArrayList<>();
        if (locationAttribute != null) {
            locations = locationAttribute.getValues()
                    .stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }
        if (event.getTopic()
                .equals(CREATED_TOPIC) || event.getTopic()
                .equals(UPDATED_TOPIC)) {
            publications.put(registryId, Collections.unmodifiableList(locations));
        } else if (event.getTopic()
                .equals(DELETED_TOPIC)) {
            publications.remove(registryId);
        }
    }

    public void init() {

        try {
            List<Metacard> metacards =
                    Security.runAsAdminWithException(() -> federationAdminService.getRegistryMetacards());

            for (Metacard metacard : metacards) {
                Attribute registryIdAttribute =
                        metacard.getAttribute(RegistryObjectMetacardType.REGISTRY_ID);
                if (registryIdAttribute == null
                        || StringUtils.isBlank(registryIdAttribute.getValue()
                        .toString())) {
                    LOGGER.warn("Warning metacard (id: {}} did not contain a registry id.",
                            metacard.getId());
                    continue;
                }
                Attribute locations =
                        metacard.getAttribute(RegistryObjectMetacardType.PUBLISHED_LOCATIONS);

                String registryId = registryIdAttribute.getValue()
                        .toString();
                if (locations != null) {
                    publications.put(registryId,
                            Collections.unmodifiableList(locations.getValues()
                                    .stream()
                                    .map(Object::toString)
                                    .collect(Collectors.toList())));
                } else {
                    publications.put(registryId, Collections.emptyList());
                }
            }
        } catch (PrivilegedActionException e) {
            LOGGER.warn(
                    "Error reading from local catalog. Catalog is probably not up yet. Will try again later");
            executorService.schedule(this::init, RETRY_INTERVAL, TimeUnit.SECONDS);
        }
    }

    public void destroy() {
        executorService.shutdown();
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
