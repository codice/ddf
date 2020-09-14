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
package org.codice.ddf.catalog.admin.downloadmanager;

import ddf.catalog.event.retrievestatus.DownloadStatusInfo;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadManagerService implements DownloadManagerServiceMBean {

  private static final Logger LOGGER = LoggerFactory.getLogger(DownloadManagerService.class);

  private ObjectName objectName;

  private MBeanServer mBeanServer;

  private DownloadStatusInfo downloadStatusInfo;

  public DownloadManagerService(DownloadStatusInfo downloadStatusInfo) {
    this.downloadStatusInfo = downloadStatusInfo;
    try {
      objectName = new ObjectName(DownloadStatusInfo.class.getName() + ":service=download-manager");
      mBeanServer = ManagementFactory.getPlatformMBeanServer();
    } catch (MalformedObjectNameException mone) {
      LOGGER.debug("Could not create objectName.", mone);
    }
  }

  public void init() {
    try {
      try {
        mBeanServer.registerMBean(this, objectName);
      } catch (InstanceAlreadyExistsException iaee) {
        LOGGER.debug("Re-registering Download Manager MBean");
        mBeanServer.unregisterMBean(objectName);
        mBeanServer.registerMBean(this, objectName);
      }
    } catch (Exception e) {
      LOGGER.info("Could not register MBean.", e);
    }
  }

  public void destroy() {
    try {
      if (objectName != null && mBeanServer != null) {
        mBeanServer.unregisterMBean(objectName);
      }
    } catch (Exception e) {
      LOGGER.debug("Exception unregistering MBean: ", e);
    }
  }

  @Override
  public List<Map<String, String>> getAllDownloadsStatus() {
    List<Map<String, String>> allDownloadsStatus = new ArrayList<Map<String, String>>();
    for (String item : downloadStatusInfo.getAllDownloads()) {
      allDownloadsStatus.add(downloadStatusInfo.getDownloadStatus(item));
    }
    return allDownloadsStatus;
  }

  @Override
  public Map<String, String> getDownloadStatus(String downloadIdentifier) {
    return downloadStatusInfo.getDownloadStatus(downloadIdentifier);
  }

  @Override
  public List<String> getAllDownloads() {
    return downloadStatusInfo.getAllDownloads();
  }

  @Override
  public List<String> getAllDownloads(String userId) {
    return downloadStatusInfo.getAllDownloads(userId);
  }

  @Override
  public void removeDownloadInfo(String downloadIdentifier) {
    downloadStatusInfo.removeDownloadInfo(downloadIdentifier);
  }

  @Override
  public void cancelDownload(String userId, String downloadIdentifier) {
    downloadStatusInfo.cancelDownload(userId, downloadIdentifier);
  }
}
