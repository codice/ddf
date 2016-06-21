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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.registry.api.RegistryStore;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.publication.manager.RegistryPublicationManager;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.action.Action;
import ddf.action.ActionProvider;
import ddf.action.impl.ActionImpl;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.source.Source;

public class RegistryPublicationActionProvider implements ActionProvider {

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

    private String providerId;

    private ConfigurationAdmin configAdmin;

    private List<RegistryStore> registryStores;

    private RegistryPublicationManager registryPublicationManager;

    @Override
    public <T> List<Action> getActions(T subject) {
        String registryIdToPublish = getRegistryId(subject);
        if (StringUtils.isBlank(registryIdToPublish)) {
            return Collections.emptyList();
        }

        List<String> currentPublications = registryPublicationManager.getPublications()
                .get(registryIdToPublish);

        return registryStores.stream()
                .filter((registryStore) -> shouldRegistryPublishToStore(registryIdToPublish,
                        registryStore))
                .map(registryStore -> getAction(registryIdToPublish,
                        registryStore.getId(),
                        !currentPublications.contains(registryStore.getId())))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

    }

    @Override
    public String getId() {
        return providerId;
    }

    @Override
    public <T> boolean canHandle(T subject) {
        return StringUtils.isNotBlank(getRegistryId(subject));
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
            Metacard metacard = (Metacard) subject;
            if (metacard.getTags()
                    .contains(RegistryConstants.REGISTRY_TAG)) {
                Attribute registryIdAttribute =
                        metacard.getAttribute(RegistryObjectMetacardType.REGISTRY_ID);
                if (registryIdAttribute != null) {
                    return registryIdAttribute.getValue()
                            .toString();
                }
            }
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

    private boolean shouldRegistryPublishToStore(String registryId, RegistryStore registryStore) {
        // can't publish to yourself
        return (registryStore.isPushAllowed()
                && StringUtils.isNotBlank(registryStore.getRegistryId()) && !registryId.equals(
                registryStore.getRegistryId()));
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

    public void setRegistryPublicationManager(
            RegistryPublicationManager registryPublicationManager) {
        this.registryPublicationManager = registryPublicationManager;
    }
}
