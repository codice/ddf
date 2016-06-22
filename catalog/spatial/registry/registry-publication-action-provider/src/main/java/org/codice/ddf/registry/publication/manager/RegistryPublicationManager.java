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

import java.security.Principal;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.security.auth.Subject;

import org.apache.commons.lang3.StringUtils;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.federationadmin.service.FederationAdminException;
import org.codice.ddf.registry.federationadmin.service.FederationAdminService;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;

public class RegistryPublicationManager implements EventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryPublicationManager.class);

    private static final String METACARD_PROPERTY = "ddf.catalog.event.metacard";

    private static final String CREATED_TOPIC = "ddf/catalog/event/CREATED";

    private static final String UPDATED_TOPIC = "ddf/catalog/event/UPDATED";

    private static final String DELETED_TOPIC = "ddf/catalog/event/DELETED";

    private static final java.lang.String KARAF_LOCAL_ROLES = "karaf.local.roles";

    private Map<String, List<String>> publications = new ConcurrentHashMap<>();

    private FederationAdminService federationAdminService;

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

    public void init() throws FederationAdminException, PrivilegedActionException {

        // Change from here
        // Remove this subject stuff and change to use Security.runAsAdmin() once the PrivilegedException option has been implemented
        Set<Principal> principals = new HashSet<>();
        String localRoles = System.getProperty(KARAF_LOCAL_ROLES, "");
        for (String role : localRoles.split(",")) {
            principals.add(new RolePrincipal(role));
        }
        Subject subject = new Subject(true, principals, new HashSet(), new HashSet());

        PrivilegedExceptionAction<List<Metacard>> federationAdminServiceAction =
                () -> federationAdminService.getRegistryMetacards();
        List<Metacard> metacards = subject.doAs(subject, federationAdminServiceAction);
        // To here

        for (Metacard metacard : metacards) {
            Attribute registryIdAttribute =
                    metacard.getAttribute(RegistryObjectMetacardType.REGISTRY_ID);
            if (registryIdAttribute == null || StringUtils.isBlank(registryIdAttribute.getValue()
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
    }

    public Map<String, List<String>> getPublications() {
        return Collections.unmodifiableMap(publications);
    }

    public void setFederationAdminService(FederationAdminService adminService) {
        this.federationAdminService = adminService;
    }
}
