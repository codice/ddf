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
package org.codice.ddf.catalog.resource.cache.impl;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Core;
import ddf.catalog.federation.FederationException;
import ddf.catalog.impl.FrameworkProperties;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.resource.download.ReliableResourceDownloadManager;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import java.util.Optional;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import org.codice.ddf.catalog.resource.cache.ResourceCache;
import org.codice.ddf.catalog.resource.cache.ResourceCacheServiceMBean;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** MBean used to interact with the resource cache. */
public class ResourceCacheService implements ResourceCacheServiceMBean {

  private static final Logger LOGGER = LoggerFactory.getLogger(ResourceCacheService.class);

  private static final String CLASS_NAME = ResourceCacheService.class.getName();

  private final ResourceCache resourceCache;

  private final ReliableResourceDownloadManager downloadManager;

  private final MBeanServer mBeanServer;

  private final ObjectName objectName;

  private final FrameworkProperties frameworkProperties;

  private final CatalogFramework catalogFramework;

  public ResourceCacheService(
      MBeanServer mBeanServer,
      ResourceCache resourceCache,
      ReliableResourceDownloadManager downloadManager,
      FrameworkProperties frameworkProperties,
      CatalogFramework catalogFramework)
      throws MalformedObjectNameException {
    this.mBeanServer = mBeanServer;
    this.resourceCache = resourceCache;
    this.downloadManager = downloadManager;
    this.objectName = new ObjectName(OBJECT_NAME);
    this.frameworkProperties = frameworkProperties;
    this.catalogFramework = catalogFramework;
  }

  public void init() throws MBeanRegistrationException {
    registerMBean();
    LOGGER.debug("Registered [{}] MBean under object name: [{}].", CLASS_NAME, objectName);
  }

  public void destroy() throws MBeanRegistrationException {
    if (objectName != null && mBeanServer != null) {
      unregisterMBean();
      LOGGER.debug("Unregistered [{}] MBean.", CLASS_NAME);
    }
  }

  @Override
  public boolean isCacheEnabled() {
    return downloadManager.isCacheEnabled();
  }

  @Override
  public boolean contains(Metacard metacard) {
    return downloadManager.isCacheEnabled() ? resourceCache.contains(metacard) : false;
  }

  @Override
  public boolean containsById(String metacardId) {
    Optional<Metacard> optionalMetacard = queryForMetacard(metacardId);
    return downloadManager.isCacheEnabled() && optionalMetacard.isPresent()
        ? contains(optionalMetacard.get())
        : false;
  }

  private Optional<Metacard> queryForMetacard(String metacardId) {
    Filter filter =
        frameworkProperties.getFilterBuilder().attribute(Core.ID).is().equalTo().text(metacardId);
    QueryRequest queryRequest = new QueryRequestImpl(new QueryImpl(filter), true);
    QueryResponse queryResponse = null;
    try {
      queryResponse = catalogFramework.query(queryRequest);
    } catch (UnsupportedQueryException | SourceUnavailableException | FederationException e) {
      LOGGER.error("Unable to lookup metacard for metacard id [{}].", metacardId);
      return Optional.empty();
    }
    return queryResponse != null && queryResponse.getResults().size() == 1
        ? Optional.of(queryResponse.getResults().get(0).getMetacard())
        : Optional.empty();
  }

  private void registerMBean() throws MBeanRegistrationException {
    if (!mBeanServer.isRegistered(objectName)) {
      try {
        // wrap the implementation in a StandardMBean so we can have the MBean interface
        // and implementation in different packages
        mBeanServer.registerMBean(
            new StandardMBean(this, ResourceCacheServiceMBean.class), objectName);
      } catch (InstanceAlreadyExistsException
          | MBeanRegistrationException
          | NotCompliantMBeanException e) {
        String message =
            String.format(
                "Unable to register [%s] MBean under object name: [%s].",
                CLASS_NAME, objectName.toString());
        LOGGER.debug(message, e);
        throw new MBeanRegistrationException(e, message);
      }
    } else {
      LOGGER.debug(
          "[{}] MBean is already registered under object name: [{}].", CLASS_NAME, objectName);
    }
  }

  private void unregisterMBean() throws MBeanRegistrationException {
    try {
      if (mBeanServer.isRegistered(objectName)) {
        mBeanServer.unregisterMBean(objectName);
      } else {
        LOGGER.debug(
            "Unable to unregister [{}] MBean under object name: [{}]. It is not registered.",
            CLASS_NAME,
            objectName);
      }
    } catch (MBeanRegistrationException | InstanceNotFoundException e) {
      String message =
          String.format(
              "Unable to unregistering [%s] MBean under object name: [%s].",
              CLASS_NAME, objectName);
      LOGGER.debug(message, e);
      throw new MBeanRegistrationException(e, message);
    }
  }
}
