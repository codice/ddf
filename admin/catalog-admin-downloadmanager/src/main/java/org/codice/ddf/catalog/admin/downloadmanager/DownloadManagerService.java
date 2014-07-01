/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package org.codice.ddf.catalog.admin.downloadmanager;


import ddf.catalog.event.retrievestatus.DownloadStatusInfo;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by localadmin on 7/1/2014.
 */
public class DownloadManagerService implements DownloadManagerServiceMBean {

    private ObjectName objectName;

    private MBeanServer mBeanServer;

    private DownloadStatusInfo downloadStatusInfo;

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadManagerService.class);

    public DownloadManagerService(DownloadStatusInfo downloadStatusInfo) {
        this.downloadStatusInfo = downloadStatusInfo;
        try {
            objectName = new ObjectName(DownloadStatusInfo.class.getName()
                    + ":service=download-manager");
            mBeanServer = ManagementFactory.getPlatformMBeanServer();
        } catch (MalformedObjectNameException mone) {
            LOGGER.info("Could not create objectName.", mone);
        }
    }

    public void init() {
        try {
            try {
                mBeanServer.registerMBean(this, objectName);
            } catch (InstanceAlreadyExistsException iaee) {
                LOGGER.info("Re-registering Download Manager MBean");
                mBeanServer.unregisterMBean(objectName);
                mBeanServer.registerMBean(this, objectName);
            }
        } catch (Exception e) {
            LOGGER.warn("Could not register MBean.", e);
        }
    }

    public void destroy() {
        try {
            if (objectName != null && mBeanServer != null) {
                mBeanServer.unregisterMBean(objectName);
            }
        } catch (Exception e) {
            LOGGER.warn("Exception unregistering MBean: ", e);
        }
    }
    public ArrayList<Map<String, String>> getAllDownloadsStatus() {
        ArrayList<Map<String, String>> allDownloadsStatus = new ArrayList<Map<String, String>>();
        for (String item : downloadStatusInfo.getAllDownloads()) {
            allDownloadsStatus.add(downloadStatusInfo.getDownloadStatus(item));
        }
        return allDownloadsStatus;
    }

    public Map<String, String> getDownloadStatus(String downloadIdentifier){
        return downloadStatusInfo.getDownloadStatus(downloadIdentifier);
    }

    public ArrayList<String> getAllDownloads(){
        return downloadStatusInfo.getAllDownloads();
    }

    public ArrayList<String> getAllDownloads(String userId){
        return downloadStatusInfo.getAllDownloads(userId);
    }

    public void removeDownloadInfo(String downloadIdentifier) { downloadStatusInfo.removeDownloadInfo(downloadIdentifier); }
}
