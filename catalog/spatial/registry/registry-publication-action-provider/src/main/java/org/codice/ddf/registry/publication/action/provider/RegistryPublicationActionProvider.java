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
package org.codice.ddf.registry.publication.action.provider;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.registry.api.RegistryStore;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.federationadmin.service.FederationAdminException;
import org.codice.ddf.registry.federationadmin.service.FederationAdminService;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.action.Action;
import ddf.action.ActionProvider;
import ddf.action.impl.ActionImpl;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.source.Source;

public class RegistryPublicationActionProvider implements ActionProvider, EventHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(RegistryPublicationActionProvider.class);

    private static final String REGISTRY_PATH = "/registries";

    private static final String PUBLICATION_PATH = "publication";

    private static final String PUBLISH_DESCRIPTION = "Publishes this nodes information to ";

    private static final String PUBLISH_OPERATION = "publish";

    private static final String PUBLISH_TITLE = "Publish to ";

    private static final String UNPUBLISH_DESCRIPTION = "Unpublishes this nodes information from ";

    private static final String UNPUBLISH_OPERATION = "unpublish";

    private static final String UNPUBLISH_TITLE = "Unpublish from ";

    private static final String HTTP_POST = "HTTP_POST";

    private static final String HTTP_DELETE = "HTTP_DELETE";

    private static final String METACARD_PROPERTY = "ddf.catalog.event.metacard";

    private static final String CREATED_TOPIC = "ddf/catalog/event/CREATED";

    private static final String UPDATED_TOPIC = "ddf/catalog/event/UPDATED";

    private static final String DELETED_TOPIC = "ddf/catalog/event/DELETED";

    private String providerId;

    private ConfigurationAdmin configAdmin;

    private FederationAdminService federationAdminService;

    private List<RegistryStore> registryStores;

    private Map<String, List<String>> publications = new ConcurrentHashMap<>();

    @Override
    public <T> List<Action> getActions(T subject) {
        String registryId = getRegistryId(subject);
        if (registryId == null) {
            return Collections.emptyList();
        }
        List<Action> actions = new ArrayList<>();
        List<String> currentPublications = publications.get(registryId);

        for (RegistryStore registry : registryStores) {
            //can't publish to yourself
            if (!registry.isPushAllowed() || StringUtils.isBlank(registry.getRegistryId())
                    || registryId.equals(registry.getRegistryId())) {
                continue;
            }

            CollectionUtils.addIgnoreNull(actions,
                    getAction(registryId,
                            registry.getId(),
                            !currentPublications.contains(registry.getId())));
        }

        return actions;
    }

    @Override
    public String getId() {
        return providerId;
    }

    @Override
    public <T> boolean canHandle(T subject) {
        return getRegistryId(subject) != null;
    }

    @Override
    public void handleEvent(Event event) {
        Metacard mcard = (Metacard) event.getProperty(METACARD_PROPERTY);
        if (mcard == null || !mcard.getTags()
                .contains(RegistryConstants.REGISTRY_TAG)) {
            return;
        }

        String registryId = mcard.getAttribute(RegistryObjectMetacardType.REGISTRY_ID)
                .getValue()
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
            publications.put(registryId, locations);
        } else if (event.getTopic()
                .equals(DELETED_TOPIC)) {
            publications.remove(registryId);
        }
    }

    public void init() throws FederationAdminException {
        List<Metacard> metacards = federationAdminService.getRegistryMetacards();
        for (Metacard mcard : metacards) {
            Attribute locations =
                    mcard.getAttribute(RegistryObjectMetacardType.PUBLISHED_LOCATIONS);
            if (locations != null) {
                publications.put(mcard.getAttribute(RegistryObjectMetacardType.REGISTRY_ID)
                                .getValue()
                                .toString(),
                        locations.getValues()
                                .stream()
                                .map(Object::toString)
                                .collect(Collectors.toList()));
            } else {
                publications.put(mcard.getAttribute(RegistryObjectMetacardType.REGISTRY_ID)
                        .getValue()
                        .toString(), Collections.emptyList());
            }
        }
    }

    private Action getAction(String regId, String destinationId, boolean publish) {

        URL url;
        String title = publish ? PUBLISH_TITLE + destinationId : UNPUBLISH_TITLE + destinationId;
        String description = publish ?
                PUBLISH_DESCRIPTION + destinationId :
                UNPUBLISH_DESCRIPTION + destinationId;
        String operation = publish ? PUBLISH_OPERATION : UNPUBLISH_OPERATION;
        String httpOp = publish ? HTTP_POST : HTTP_DELETE;
        try {

            String path = String.format("%s/%s/%s/%s",
                    REGISTRY_PATH,
                    regId,
                    PUBLICATION_PATH,
                    URLEncoder.encode(destinationId, "UTF-8"));
            URI uri = new URI(SystemBaseUrl.constructUrl(path, true));
            url = uri.toURL();

        } catch (MalformedURLException | URISyntaxException | UnsupportedEncodingException e) {
            LOGGER.error("Malformed URL exception", e);
            return null;
        }
        String id = String.format("%s.%s.%s", getId(), operation, httpOp);
        return new ActionImpl(id, title, description, url);
    }

    private <T> String getRegistryId(T subject) {
        if (subject instanceof Metacard) {
            Metacard mcard = (Metacard) subject;
            if (!mcard.getTags()
                    .contains(RegistryConstants.REGISTRY_TAG)) {
                return null;
            }
            return ((Metacard) subject).getAttribute(RegistryObjectMetacardType.REGISTRY_ID)
                    .getValue()
                    .toString();
        } else if (subject instanceof Source) {
            try {
                Configuration[] configurations = configAdmin.listConfigurations(String.format(
                        "(id=%s)",
                        ((Source) subject).getId()));
                return (String) Arrays.stream(configurations)
                        .map(Configuration::getProperties)
                        .map(p -> p.get(RegistryObjectMetacardType.REGISTRY_ID))
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null);

            } catch (IOException | InvalidSyntaxException e) {
                LOGGER.warn("Couldn't get configuration for {}", ((Source) subject).getId(), e);
            }
        } else if (subject instanceof Configuration) {
            return (String) ((Configuration) subject).getProperties()
                    .get(RegistryObjectMetacardType.REGISTRY_ID);
        }
        return null;
    }

    public void setProviderId(String id) {
        this.providerId = id;
    }

    public void setConfigAdmin(ConfigurationAdmin admin) {
        this.configAdmin = admin;
    }

    public void setRegistryStores(List<RegistryStore> registryStores) {
        this.registryStores = registryStores;
    }

    public void setFederationAdminService(FederationAdminService adminService) {
        this.federationAdminService = adminService;
    }

    public Map<String, List<String>> getPublications() {
        return publications;
    }
}
