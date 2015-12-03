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
package ddf.catalog.event.retrievestatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.shiro.SecurityUtils;
import org.codice.ddf.activities.ActivityEvent;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.resource.download.ReliableResourceDownloader;
import ddf.security.SubjectUtils;

public class DownloadStatusInfoImpl implements DownloadStatusInfo {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadStatusInfoImpl.class);

    private static final String UNKNOWN = "UNKNOWN";

    private Map<String, ReliableResourceDownloader> downloaders = new HashMap<String, ReliableResourceDownloader>();

    private Map<String, String> downloadUsers = new HashMap<String, String>();

    private EventAdmin eventAdmin;

    public void setEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    public void addDownloadInfo(String downloadIdentifier, ReliableResourceDownloader downloader,
            ResourceResponse resourceResponse) {
        downloaders.put(downloadIdentifier, downloader);
        org.apache.shiro.subject.Subject shiroSubject = null;
        try {
            shiroSubject = SecurityUtils.getSubject();
        } catch (Exception e) {
            LOGGER.debug("Could not determine current user, using session id.");
        }
        String user = SubjectUtils
                .getName(shiroSubject, getProperty(resourceResponse, ActivityEvent.USER_ID_KEY));
        downloadUsers.put(downloadIdentifier, user);
    }

    public List<String> getAllDownloads() {
        return getAllDownloads(null);
    }

    public List<String> getAllDownloads(String userId) {

        List<String> allDownloads = new ArrayList<String>();
        if (null == userId) {
            for (Map.Entry<String, ReliableResourceDownloader> item : downloaders.entrySet()) {
                allDownloads.add(item.getKey());
            }
        } else {
            for (Map.Entry<String, ReliableResourceDownloader> item : downloaders.entrySet()) {
                if (item.getKey().substring(0, userId.length()).equals(userId)) {
                    allDownloads.add(item.getKey());
                }
            }
        }

        return allDownloads;
    }

    public Map<String, String> getDownloadStatus(String downloadIdentifier) {
        Map<String, String> statusMap = new HashMap<String, String>();
        ReliableResourceDownloader downloader = downloaders.get(downloadIdentifier);
        if (downloader != null) {
            Long downloadedBytes = downloader.getReliableResourceInputStreamBytesCached();
            try {
                Long totalBytes = Long.parseLong(downloader.getResourceSize());
                statusMap.put("percent", Long.toString((downloadedBytes * 100) / totalBytes));

            } catch (Exception e) {
                statusMap.put("percent", UNKNOWN);
            }
            statusMap.put("downloadId", downloadIdentifier);
            statusMap.put("status", downloader.getReliableResourceInputStreamState());
            statusMap.put("bytesDownloaded", Long.toString(downloadedBytes));
            statusMap.put("fileName", downloader.getResourceResponse().getResource().getName());
            statusMap.put("user", downloadUsers.get(downloadIdentifier));
        }
        return statusMap;
    }

    public void removeDownloadInfo(String downloadIdentifier) {
        downloaders.remove(downloadIdentifier);
        downloadUsers.remove(downloadIdentifier);
    }

    public void cancelDownload(String userId, String downloadIdentifier) {
        String downloadId = userId + downloadIdentifier;

        Map<String, String> propertiesMap = new HashMap<String, String>();
        propertiesMap.put(ActivityEvent.DOWNLOAD_ID_KEY, downloadId);

        Event event = new Event(ActivityEvent.EVENT_TOPIC_DOWNLOAD_CANCEL, propertiesMap);
        eventAdmin.postEvent(event);
    }

    private String getProperty(ResourceResponse resourceResponse, String property) {
        String response = "";

        if (resourceResponse.getRequest().containsPropertyName(property)) {
            response = (String) resourceResponse.getRequest().getPropertyValue(property);
            LOGGER.debug("resourceResponse {} property: {}", property, response);
        }

        return response;
    }
}
