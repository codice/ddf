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
package org.codice.ddf.catalog.harvest.listeners;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.catalog.harvest.HarvestException;
import org.codice.ddf.catalog.harvest.HarvestedResource;
import org.codice.ddf.catalog.harvest.Listener;
import org.codice.ddf.catalog.harvest.StorageAdaptor;
import org.codice.ddf.catalog.harvest.common.FileSystemPersistenceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistentListener implements Listener {

  private static final Logger LOGGER = LoggerFactory.getLogger(PersistentListener.class);

  private final StorageAdaptor adaptor;

  private final FileSystemPersistenceProvider persistenceProvider;

  public PersistentListener(StorageAdaptor adaptor, String pid) {
    this.adaptor = adaptor;
    persistenceProvider =
        new FileSystemPersistenceProvider("harvest/persistent/" + DigestUtils.sha1Hex(pid));
  }

  @Override
  public void onCreate(HarvestedResource resource) {
    String key = DigestUtils.sha1Hex(resource.getUri().toASCIIString());
    if (resourceNotCreated(key)) {
      String resourceId = null;
      try {
        resourceId = adaptor.create(resource);
      } catch (HarvestException e) {
        LOGGER.debug("Failed to create resource [{}].", resource.getName(), e);
      }

      if (StringUtils.isNotEmpty(resourceId)) {
        persistenceProvider.store(key, resourceId);
      }
    } else {
      LOGGER.debug("Already created resource [{}]. Doing nothing", resource.getName());
    }
  }

  @Override
  public void onUpdate(HarvestedResource resource) {
    String key = DigestUtils.sha1Hex(resource.getUri().toASCIIString());
    String resourceId = getResourceId(key);

    if (StringUtils.isNotEmpty(resourceId)) {
      try {
        adaptor.update(resource, resourceId);
      } catch (HarvestException e) {
        LOGGER.debug(
            "Failed to update resource [{}] using id [{}].", resource.getName(), resourceId, e);
      }
    }
  }

  @Override
  public void onDelete(String uri) {
    String key = DigestUtils.sha1Hex(uri);
    String resourceId = getResourceId(key);

    if (StringUtils.isNotEmpty(resourceId)) {
      try {
        adaptor.delete(resourceId);
        persistenceProvider.delete(key);
      } catch (HarvestException e) {
        LOGGER.debug(
            "Failed to delete resource with uri [{}] using id [{}]. Resources in this listener's cache may be out of sync.",
            uri,
            resourceId,
            e);
      }
    }
  }

  private boolean resourceNotCreated(String resourcePid) {
    return StringUtils.isEmpty(getResourceId(resourcePid));
  }

  private String getResourceId(String key) {
    if (persistenceProvider.loadAllKeys().contains(key)) {
      return (String) persistenceProvider.loadFromPersistence(key);
    }

    return null;
  }
}
