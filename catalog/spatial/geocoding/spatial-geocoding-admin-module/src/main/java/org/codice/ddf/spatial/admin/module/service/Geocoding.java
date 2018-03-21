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
package org.codice.ddf.spatial.admin.module.service;

import java.lang.management.ManagementFactory;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import org.apache.commons.io.FilenameUtils;
import org.codice.ddf.spatial.geocoding.GeoEntryExtractionException;
import org.codice.ddf.spatial.geocoding.GeoEntryExtractor;
import org.codice.ddf.spatial.geocoding.GeoEntryIndexer;
import org.codice.ddf.spatial.geocoding.GeoEntryIndexingException;
import org.codice.ddf.spatial.geocoding.GeoNamesRemoteDownloadException;
import org.codice.ddf.spatial.geocoding.ProgressCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Geocoding implements GeocodingMBean {

  private static final Logger LOGGER = LoggerFactory.getLogger(Geocoding.class);

  private static final String BASE_DIR = System.getProperty("user.dir") + "/content/store/";

  private GeoEntryExtractor geoEntryExtractor;

  private GeoEntryIndexer geoEntryIndexer;

  private int progress = 0;

  public Geocoding() {
    registerMbean();
  }

  private void registerMbean() {
    try {
      ObjectName objectName = new ObjectName(Geocoding.class.getName() + ":service=geocoding");
      MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
      tryRegisterMBean(mBeanServer, objectName);
    } catch (MalformedObjectNameException
        | InstanceAlreadyExistsException
        | MBeanRegistrationException
        | InstanceNotFoundException
        | NotCompliantMBeanException e) {
      LOGGER.info("Unable to create Geocoding Configuration MBean.", e);
    }
  }

  private void tryRegisterMBean(MBeanServer mBeanServer, ObjectName objectName)
      throws InstanceAlreadyExistsException, MBeanRegistrationException, InstanceNotFoundException,
          NotCompliantMBeanException {
    try {
      mBeanServer.registerMBean(this, objectName);
      LOGGER.debug("Registered Geocoding Configuration MBean under object name: {}", objectName);
    } catch (InstanceAlreadyExistsException e) {
      // Try to remove and re-register
      mBeanServer.unregisterMBean(objectName);
      mBeanServer.registerMBean(this, objectName);
      LOGGER.debug("Re-registered Geocoding Configuration MBean");
    } catch (MBeanRegistrationException | NotCompliantMBeanException e) {
      LOGGER.info("Could not register MBean [{}].", objectName);
    }
  }

  @Override
  public boolean updateGeoIndexWithUrl(String url, String createIndex) {
    progress = 0;
    String countryCode = FilenameUtils.getBaseName(url);
    url = FilenameUtils.getPath(url);
    LOGGER.debug("Downloading : {}{}.zip", url, countryCode);
    LOGGER.debug("Create Index : {}", createIndex);

    if (url != null) {
      geoEntryExtractor.setUrl(url);
    }
    return updateIndex(countryCode, createIndex.equals("true"));
  }

  @Override
  public boolean updateGeoIndexWithFilePath(
      String fileName, String contentFolderId, String createIndex) {
    progress = 0;
    fileName = BASE_DIR + contentFolderId + "/" + fileName;
    LOGGER.debug("Adding {} to index.  Create New Index : {}", fileName, createIndex);
    return updateIndex(fileName, createIndex.equals("true"));
  }

  @Override
  public int progressCallback() {
    return progress;
  }

  private boolean updateIndex(String resource, boolean createIndex) {

    final ProgressCallback progressCallback = this::setProgress;

    LOGGER.trace("Updating GeoNames Index...");

    try {
      geoEntryIndexer.updateIndex(resource, geoEntryExtractor, createIndex, progressCallback);
      LOGGER.trace("\nDone Updating GeoNames Index.");
      LOGGER.debug("Done Updating GeoNames Index with : {}", resource);
      return true;
    } catch (GeoEntryExtractionException e) {
      LOGGER.debug("Error extracting GeoNames data from resource {}", resource, e);
      return false;
    } catch (GeoEntryIndexingException e) {
      LOGGER.debug("Error indexing GeoNames data", e);
      return false;
    } catch (GeoNamesRemoteDownloadException e) {
      LOGGER.debug("Error downloading resource from remote source {}", resource, e);
      return false;
    }
  }

  public void setProgress(int progress) {
    this.progress = progress;
  }

  public GeoEntryExtractor getGeoEntryExtractor() {
    return this.geoEntryExtractor;
  }

  public void setGeoEntryExtractor(GeoEntryExtractor geoEntryExtractor) {
    this.geoEntryExtractor = geoEntryExtractor;
  }

  public GeoEntryIndexer getGeoEntryIndexer() {
    return this.geoEntryIndexer;
  }

  public void setGeoEntryIndexer(GeoEntryIndexer geoEntryIndexer) {
    this.geoEntryIndexer = geoEntryIndexer;
  }
}
