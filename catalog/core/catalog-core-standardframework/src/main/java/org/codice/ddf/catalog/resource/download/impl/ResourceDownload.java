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
package org.codice.ddf.catalog.resource.download.impl;

import ddf.catalog.CatalogFramework;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.impl.ResourceRequestById;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import java.io.IOException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import javax.ws.rs.core.Response.Status;
import org.codice.ddf.catalog.resource.cache.ResourceCacheServiceMBean;
import org.codice.ddf.catalog.resource.download.DownloadToLocalSiteException;
import org.codice.ddf.catalog.resource.download.ResourceDownloadMBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** MBean used to manage metacard resource downloads. */
public class ResourceDownload implements ResourceDownloadMBean {

  private static final Logger LOGGER = LoggerFactory.getLogger(ResourceDownload.class);

  private static final String CLASS_NAME = ResourceDownload.class.getName();

  private static final String ERROR_MESSAGE_TEMPLATE =
      "Unable to download the resource associated with metacard [%s] from source [%s] to the local site.";

  private final CatalogFramework catalogFramework;

  private final ResourceCacheServiceMBean resourceCacheMBean;

  private final MBeanServer mBeanServer;

  private final ObjectName objectName;

  public ResourceDownload(
      MBeanServer mBeanServer,
      CatalogFramework catalogFramework,
      ResourceCacheServiceMBean resourceCacheMBean)
      throws MalformedObjectNameException {
    this.mBeanServer = mBeanServer;
    this.catalogFramework = catalogFramework;
    this.resourceCacheMBean = resourceCacheMBean;
    this.objectName = new ObjectName(OBJECT_NAME);
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
  public void copyToLocalSite(String sourceId, String metacardId) throws MBeanException {
    LOGGER.debug(
        "Downloading resource associated with metacard id [{}] from source [{}] to the local site.",
        metacardId,
        sourceId);

    ResourceRequest resourceRequest = new ResourceRequestById(metacardId);

    if (!resourceCacheMBean.isCacheEnabled()) {
      String message = "Caching of resources is not enabled.";
      LOGGER.info(message);
      throw new MBeanException(
          new DownloadToLocalSiteException(Status.BAD_REQUEST, message), message);
    }

    try {
      LOGGER.debug(
          "Attempting to download the resource associated with metacard [{}] from source [{}] to the local site.",
          metacardId,
          sourceId);
      ResourceResponse resourceResponse = catalogFramework.getResource(resourceRequest, sourceId);

      if (resourceResponse == null) {
        String message = String.format(ERROR_MESSAGE_TEMPLATE, metacardId, sourceId);
        LOGGER.debug(message);
        throw new MBeanException(
            new DownloadToLocalSiteException(Status.INTERNAL_SERVER_ERROR, message), message);
      }
    } catch (IOException | ResourceNotSupportedException e) {
      String message = String.format(ERROR_MESSAGE_TEMPLATE, metacardId, sourceId);
      LOGGER.debug(message, e);
      throw new MBeanException(
          new DownloadToLocalSiteException(Status.INTERNAL_SERVER_ERROR, message), message);
    } catch (ResourceNotFoundException e) {
      String message =
          String.format(ERROR_MESSAGE_TEMPLATE, metacardId, sourceId)
              + " The resource could not be found.";
      LOGGER.debug(message, e);
      throw new MBeanException(
          new DownloadToLocalSiteException(Status.NOT_FOUND, message), message);
    }
  }

  private void registerMBean() throws MBeanRegistrationException {
    if (!mBeanServer.isRegistered(objectName)) {
      try {
        // wrap the implementation in a StandardMBean so we can have the MBean interface
        // and implementation in different packages
        mBeanServer.registerMBean(new StandardMBean(this, ResourceDownloadMBean.class), objectName);
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
