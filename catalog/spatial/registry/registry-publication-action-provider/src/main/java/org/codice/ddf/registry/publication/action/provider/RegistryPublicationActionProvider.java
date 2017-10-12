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
package org.codice.ddf.registry.publication.action.provider;

import ddf.action.Action;
import ddf.action.MultiActionProvider;
import ddf.action.impl.ActionImpl;
import ddf.catalog.data.Metacard;
import ddf.catalog.source.Source;
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
import org.codice.ddf.registry.api.internal.RegistryStore;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryUtility;
import org.codice.ddf.registry.publication.manager.RegistryPublicationManager;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegistryPublicationActionProvider implements MultiActionProvider {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(RegistryPublicationActionProvider.class);

  private static final String REGISTRY_PATH = "/internal/registries";

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

    List<String> currentPublications =
        registryPublicationManager
            .getPublications()
            .getOrDefault(registryIdToPublish, Collections.EMPTY_LIST);

    return registryStores
        .stream()
        .filter((registryStore) -> shouldRegistryPublishToStore(registryIdToPublish, registryStore))
        .map(
            registryStore ->
                getAction(
                    registryIdToPublish,
                    registryStore.getRegistryId(),
                    registryStore.getId(),
                    !currentPublications.contains(registryStore.getRegistryId())))
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

  private Action getAction(
      String regId, String destinationId, String destinationName, boolean publish) {

    URL url;
    String title = publish ? PUBLISH_TITLE + destinationName : UNPUBLISH_TITLE + destinationName;
    String description =
        publish ? PUBLISH_DESCRIPTION + destinationName : UNPUBLISH_DESCRIPTION + destinationName;
    String operation = publish ? PUBLISH_OPERATION : UNPUBLISH_OPERATION;
    String httpOp = publish ? HTTP_POST : HTTP_DELETE;
    try {

      String path =
          String.format(
              "%s/%s/%s/%s",
              REGISTRY_PATH, regId, PUBLICATION_PATH, URLEncoder.encode(destinationId, "UTF-8"));
      URI uri = new URI(SystemBaseUrl.constructUrl(path, true));
      url = uri.toURL();

    } catch (MalformedURLException | URISyntaxException | UnsupportedEncodingException e) {
      LOGGER.debug("Malformed URL exception", e);
      return null;
    }
    String id = String.format("%s.%s.%s", getId(), operation, httpOp);
    return new ActionImpl(id, title, description, url);
  }

  private <T> String getRegistryId(T subject) {
    if (subject instanceof Metacard) {
      if (RegistryUtility.isRegistryMetacard((Metacard) subject)) {
        return RegistryUtility.getRegistryId((Metacard) subject);
      }
    } else if (subject instanceof Source) {
      try {
        Configuration[] configurations =
            configAdmin.listConfigurations(String.format("(id=%s)", ((Source) subject).getId()));
        return (String)
            Arrays.stream(configurations)
                .map(Configuration::getProperties)
                .map(p -> p.get(RegistryConstants.CONFIGURATION_REGISTRY_ID_PROPERTY))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

      } catch (IOException | InvalidSyntaxException e) {
        LOGGER.info("Couldn't get configuration for {}", ((Source) subject).getId(), e);
      }
    } else if (subject instanceof Configuration) {
      return (String)
          ((Configuration) subject)
              .getProperties()
              .get(RegistryConstants.CONFIGURATION_REGISTRY_ID_PROPERTY);
    }
    return null;
  }

  private boolean shouldRegistryPublishToStore(String registryId, RegistryStore registryStore) {
    // can't publish to yourself
    return (registryStore.isPushAllowed()
        && StringUtils.isNotBlank(registryStore.getRegistryId())
        && !registryId.equals(registryStore.getRegistryId()));
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

  public void setRegistryPublicationManager(RegistryPublicationManager registryPublicationManager) {
    this.registryPublicationManager = registryPublicationManager;
  }
}
